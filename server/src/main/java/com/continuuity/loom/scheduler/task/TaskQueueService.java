/*
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.store.ClusterStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Manages handing out tasks from task queue, and recording status after the task is done.
 */
public class TaskQueueService {
  private static final Logger LOG = LoggerFactory.getLogger(TaskQueueService.class);

  private final TrackingQueue taskQueue;
  private final ClusterStore clusterStore;
  private final TrackingQueue jobQueue;
  private final TaskService taskService;
  private final NodeService nodeService;
  private final LoomStats loomStats;

  @Inject
  public TaskQueueService(@Named(Constants.Queue.PROVISIONER) TrackingQueue taskQueue,
                          @Named(Constants.Queue.JOB) TrackingQueue jobQueue,
                          ClusterStore clusterStore, TaskService taskService, NodeService nodeService,
                          LoomStats loomStats) {
    this.taskQueue = taskQueue;
    this.clusterStore = clusterStore;
    this.jobQueue = jobQueue;
    this.taskService = taskService;
    this.nodeService = nodeService;
    this.loomStats = loomStats;
  }

  /**
   * Returns the next task from task queue that can be handed out for provisioning.
   * When it goes through the task queue, if it gets a task whose job is already marked as FAILED then
   * the task gets marked as DROPPED, and is skipped.
   *
   * @param workerId worker ID of the provisioner worker.
   * @return Task JSON to be handed over to the provisioner.
   * @throws Exception
   */
  public String takeNextClusterTask(String workerId) throws Exception {
    loomStats.setQueueLength(taskQueue.size());

    ClusterTask clusterTask = null;
    String taskJson = null;

    while (clusterTask == null) {
      Element task = taskQueue.take(workerId);
      if (task == null) {
        break;
      }

      clusterTask = clusterStore.getClusterTask(TaskId.fromString(task.getId()));
      if (clusterTask != null) {
        String jobId = clusterTask.getJobId();
        ClusterJob clusterJob = clusterStore.getClusterJob(JobId.fromString(jobId));

        if (clusterJob == null || clusterJob.getJobStatus() == ClusterJob.Status.FAILED) {
          // we don't want to give out tasks for failed jobs.  Remove from the queue and move on.
          taskQueue.recordProgress(workerId, clusterTask.getTaskId(),
                                   TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "Skipped due to job failure.");
          taskService.dropTask(clusterTask);
          jobQueue.add(new Element(clusterTask.getJobId()));
          clusterTask = null;
        } else {
          taskJson = task.getValue();
          startNodeAction(clusterTask);
        }
      } else {
        LOG.error("Got empty task JSON for {}, skipping it.", task.getId());
        taskQueue.recordProgress(workerId, task.getId(), TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                 "Skipped due to empty task JSON.");
      }
    }

    LOG.trace("task {} given to worker {}", clusterTask, workerId);

    return taskJson;
  }

  /**
   * Records the status of a finished task from provisioner.
   * Only the worker who currently owns the task can update the status.
   *
   * @param taskId Task ID of the finished task.
   * @param workerId worker ID of the provisioner worker.
   * @param status status code, zero means success and non-zero is failure.
   * @param result result of the task.
   * @param stdout stdout of the task.
   * @param stderr stderr of the task.
   * @throws Exception
   */
  public void finishClusterTask(String taskId, String workerId, int status, JsonObject result,
                                String stdout, String stderr) throws Exception {
    loomStats.setQueueLength(taskQueue.size());

    TrackingQueue.PossessionState state =
      taskQueue.recordProgress(workerId, taskId, TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "");

    if (state != TrackingQueue.PossessionState.POSSESSES) {
      LOG.warn("Worker {} is not owner of task {}", workerId, taskId);
      throw new  IllegalStateException("Worker is not the owner of the task");
    }

    // Queue update was successful, now update the task object
    ClusterTask clusterTask = clusterStore.getClusterTask(TaskId.fromString(taskId));

    if (status == 0) {
      LOG.debug("Successful finish of the task reported. Task {} by worker {}", taskId, workerId);
      taskService.completeTask(clusterTask, status);
    } else {
      LOG.debug("Failure to finish task reported. Task {} by worker {}", taskId, workerId);
      taskService.failTask(clusterTask, status);
    }

    finishNodeAction(clusterTask, result, stdout, stderr);

    // Schedule the job for processing
    jobQueue.add(new Element(clusterTask.getJobId()));
  }

  void startNodeAction(ClusterTask clusterTask) throws Exception {
    // Update node properties if task is associated with a nodeId.
    // There are cases when we don't associate a nodeId with a task so that the node properties don't get overridden
    // by the task output.
    // Eg. deleting a box during a rollback operation since we reuse nodeIds.
    if (clusterTask.getNodeId() != null) {
      Node node = clusterStore.getNode(clusterTask.getNodeId());
      if (node == null) {
        LOG.error("Cannot find node {} for task {} to update the properties",
                  clusterTask.getNodeId(), clusterTask.getTaskId());
      } else {
        nodeService.startAction(node, clusterTask.getTaskId(), clusterTask.getService(),
                                clusterTask.getTaskName().name());
      }
    }
  }

  void finishNodeAction(ClusterTask clusterTask, JsonObject result, String stdout,
                        String stderr) throws Exception {
    // Update node properties if task is associated with a nodeId.
    // There are cases when we don't associate a nodeId with a task so that the node properties don't get overridden
    // by the task output.
    // Eg. deleting a box during a rollback operation since we reuse nodeIds.
    if (clusterTask.getNodeId() != null) {
      Node node = clusterStore.getNode(clusterTask.getNodeId());
      if (node == null) {
        LOG.error("Cannot find node {} for task {} to update the properties",
                  clusterTask.getNodeId(), clusterTask.getTaskId());
      } else {
        // Update node properties
        for (Map.Entry<String, JsonElement> entry : result.entrySet()) {
          node.getProperties().add(entry.getKey(), entry.getValue());
        }

        // Update node action
        if (clusterTask.getStatus() == ClusterTask.Status.COMPLETE) {
          nodeService.completeAction(node);
        } else {
          nodeService.failAction(node, stdout, stderr);
        }

        LOG.trace("Updated Node = {}", node);
      }
    }
  }

}
