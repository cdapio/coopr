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
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.http.request.FinishTaskRequest;
import com.continuuity.loom.http.request.TakeTaskRequest;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Manages handing out tasks from task queue, and recording status after the task is done.
 */
public class TaskQueueService {
  private static final Logger LOG = LoggerFactory.getLogger(TaskQueueService.class);

  private final ClusterStore clusterStore;
  private final TaskService taskService;
  private final NodeService nodeService;
  private final TenantProvisionerService tenantProvisionerService;
  private final LoomStats loomStats;
  private final QueueGroup taskQueues;
  private final QueueGroup jobQueues;

  @Inject
  private TaskQueueService(@Named(Constants.Queue.PROVISIONER) QueueGroup taskQueues,
                           @Named(Constants.Queue.JOB) QueueGroup jobQueues,
                           ClusterStoreService clusterStoreService,
                           TenantProvisionerService tenantProvisionerService,
                           TaskService taskService,
                           NodeService nodeService,
                           LoomStats loomStats) {
    this.clusterStore = clusterStoreService.getSystemView();
    this.taskService = taskService;
    this.nodeService = nodeService;
    this.tenantProvisionerService = tenantProvisionerService;
    this.loomStats = loomStats;
    this.taskQueues = taskQueues;
    this.jobQueues = jobQueues;
  }

  /**
   * Returns the next task from task queue that can be handed out for provisioning.
   * When it goes through the task queue, if it gets a task whose job is already marked as FAILED then
   * the task gets marked as DROPPED, and is skipped.
   *
   * @param takeRequest Request to take a task.
   * @return Task JSON to be handed over to the provisioner.
   * @throws MissingEntityException if there is no provisioner for the provisioner id in the request.
   * @throws IOException if there was an error persisting task information.
   */
  public String takeNextClusterTask(TakeTaskRequest takeRequest) throws IOException, MissingEntityException {
    //loomStats.setQueueLength(taskQueues.size(queueName));
    String queueName = takeRequest.getTenantId();
    String workerId = takeRequest.getWorkerId();
    String provisionerId = takeRequest.getProvisionerId();

    if (tenantProvisionerService.getProvisioner(provisionerId) == null) {
      throw new MissingEntityException("provisioner " + provisionerId + " not found.");
    }

    ClusterTask clusterTask = null;
    String taskJson = null;

    while (clusterTask == null) {
      Element task = taskQueues.take(queueName, workerId);
      if (task == null) {
        break;
      }

      clusterTask = clusterStore.getClusterTask(TaskId.fromString(task.getId()));
      if (clusterTask != null) {
        String jobId = clusterTask.getJobId();
        ClusterJob clusterJob = clusterStore.getClusterJob(JobId.fromString(jobId));

        if (clusterJob == null || clusterJob.getJobStatus() == ClusterJob.Status.FAILED) {
          // we don't want to give out tasks for failed jobs.  Remove from the queue and move on.
          taskQueues.recordProgress(workerId, queueName, clusterTask.getTaskId(),
                                    TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                    "Skipped due to job failure.");
          taskService.dropTask(clusterTask);
          jobQueues.add(queueName, new Element(clusterTask.getJobId()));
          clusterTask = null;
        } else {
          taskJson = task.getValue();
          startNodeAction(clusterTask);
        }
      } else {
        LOG.error("Got empty task JSON for {}, skipping it.", task.getId());
        taskQueues.recordProgress(workerId, queueName, task.getId(),
                                  TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
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
   * @param finishRequest Request to finish a task.
   * @throws MissingEntityException if there is no provisioner for the provisioner id in the request.
   * @throws IOException if there was an error persisting task information.
   */
  public void finishClusterTask(FinishTaskRequest finishRequest) throws MissingEntityException, IOException {
    // TODO: implement per tenant queue statistics
    //loomStats.setQueueLength(taskQueue.size());

    String workerId = finishRequest.getWorkerId();
    String queueName = finishRequest.getTenantId();
    String taskId = finishRequest.getTaskId();
    String provisionerId = finishRequest.getProvisionerId();

    if (tenantProvisionerService.getProvisioner(provisionerId) == null) {
      throw new MissingEntityException("provisioner " + provisionerId + " not found.");
    }

    TrackingQueue.PossessionState state =
      taskQueues.recordProgress(workerId, queueName, taskId, TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "");

    if (state != TrackingQueue.PossessionState.POSSESSES) {
      LOG.warn("Worker {} is not owner of task {}", workerId, taskId);
      throw new  IllegalStateException("Worker is not the owner of the task");
    }

    // Queue update was successful, now update the task object
    ClusterTask clusterTask = clusterStore.getClusterTask(TaskId.fromString(taskId));

    int status = finishRequest.getStatus();
    if (status == 0) {
      LOG.debug("Successful finish of the task reported. Task {} by worker {}", taskId, workerId);
      taskService.completeTask(clusterTask, status);
    } else {
      LOG.debug("Failure to finish task reported. Task {} by worker {}", taskId, workerId);
      taskService.failTask(clusterTask, status);
    }

    finishNodeAction(clusterTask, finishRequest.getResult(), finishRequest.getStdout(), finishRequest.getStderr());

    // Schedule the job for processing
    jobQueues.add(queueName, new Element(clusterTask.getJobId()));
  }

  void startNodeAction(ClusterTask clusterTask) throws IOException {
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
                        String stderr) throws IOException {
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
