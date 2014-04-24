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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.macro.Expander;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskConfig;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schedules a cluster job. Polls a queue containing job ids to coordinate. Each time it gets a job id from the queue,
 * it will examine the status of tasks for the job's current stage and take the appropriate action. If all tasks in the
 * stage successfully completed, the job will be moved to the next stage and all tasks in the stage will be scheduled.
 * If some task was failed, the appropriate retry and rollback actions are taken for the task. If the job itself fails,
 * unneeded tasks are dropped and cluster and job state is managed. If all tasks for the job have completed, status
 * is updated across the job and cluster.
 */
public class JobScheduler implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);
  private static final String consumerId = "jobscheduler";

  private final ClusterStore clusterStore;
  private final TrackingQueue provisionerQueue;
  private final JsonSerde jsonSerde;
  private final TrackingQueue jobQueue;
  private final ZKClient zkClient;
  private final TaskService taskService;
  private final int maxTaskRetries;
  private final LoomStats loomStats;

  @Inject
  public JobScheduler(ClusterStore clusterStore, @Named("nodeprovisioner.queue") TrackingQueue provisionerQueue,
                      JsonSerde jsonSerde, @Named("internal.job.queue") TrackingQueue jobQueue, ZKClient zkClient,
                      TaskService taskService, @Named(Constants.MAX_ACTION_RETRIES) int maxTaskRetries,
                      LoomStats loomStats) {
    this.clusterStore = clusterStore;
    this.provisionerQueue = provisionerQueue;
    this.jsonSerde = jsonSerde;
    this.jobQueue = jobQueue;
    this.zkClient = ZKClients.namespace(zkClient, Constants.LOCK_NAMESPACE);
    this.taskService = taskService;
    this.maxTaskRetries = maxTaskRetries;
    this.loomStats = loomStats;
  }

  @Override
  public void run() {
    try {
      while (true) {
        Element element = jobQueue.take(consumerId);
        if (element == null) {
          return;
        }
        String jobIdStr = element.getValue();

        LOG.debug("Got job {} to schedule", jobIdStr);
        JobId jobId = JobId.fromString(jobIdStr);
        ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + jobId.getClusterId());
        try {
          lock.acquire();
          ClusterJob job = clusterStore.getClusterJob(jobId);
          LOG.trace("Scheduling job {}", job);
          Set<String> currentStage = job.getCurrentStage();

          // Check how many tasks are completed/not-submitted
          int completedTasks = 0;
          int inProgressTasks = 0;
          Set<ClusterTask> notSubmittedTasks = Sets.newHashSet();
          Set<ClusterTask> retryTasks = Sets.newHashSet();
          LOG.debug("Verifying task statuses for stage {} for job {}", job.getCurrentStageNumber(), jobIdStr);
          for (String taskId : currentStage) {
            ClusterTask task = clusterStore.getClusterTask(TaskId.fromString(taskId));
            job.setTaskStatus(task.getTaskId(), task.getStatus());
            LOG.debug("Status of task {} is {}", taskId, task.getStatus());
            if (task.getStatus() == ClusterTask.Status.COMPLETE) {
              ++completedTasks;
            } else if (task.getStatus() == ClusterTask.Status.NOT_SUBMITTED) {
              notSubmittedTasks.add(task);
            } else if (task.getStatus() == ClusterTask.Status.FAILED) {
              // If max retries has not reached, retry task. Else, fail job.
              if (task.getNumAttempts() < maxTaskRetries) {
                retryTasks.add(task);
              } else {
                job.setJobStatus(ClusterJob.Status.FAILED);
              }
            } else if (task.getStatus() == ClusterTask.Status.IN_PROGRESS) {
              ++inProgressTasks;
            }
          }

          Cluster cluster = clusterStore.getCluster(job.getClusterId());

          // If the job has not failed continue with scheduling other tasks.
          if (job.getJobStatus() != ClusterJob.Status.FAILED) {
            Set<Node> clusterNodes = clusterStore.getClusterNodes(job.getClusterId());
            Map<String, Node> nodeMap = Maps.newHashMap();
            for (Node node : clusterNodes) {
              nodeMap.put(node.getId(), node);
            }

            // Handle retry tasks if any
            if (!retryTasks.isEmpty()) {
              for (ClusterTask task : retryTasks) {
                notSubmittedTasks.add(scheduleRetry(cluster, job, task, nodeMap.get(task.getNodeId())));
              }
            }

            // Submit any tasks not yet submitted
            if (!notSubmittedTasks.isEmpty()) {
              for (final ClusterTask task : notSubmittedTasks) {
                Node taskNode = nodeMap.get(task.getNodeId());
                TaskConfig.updateNodeProperties(task.getConfig(), taskNode);

                // Add the node list
                TaskConfig.addNodeList(task.getConfig(), clusterNodes);

                // TODO: do this only once and save it
                if (!task.getTaskName().isHardwareAction()) {
                  try {
                    task.setConfig(Expander.expand(task.getConfig(), null, clusterNodes, taskNode).getAsJsonObject());
                  } catch (Throwable e) {
                    LOG.error("Exception while expanding macros for task {}", task.getTaskId(), e);
                    taskService.failTask(task, -1);
                    job.setStatusMessage("Exception while expanding macros: " + e.getMessage());
                    // no need to schedule more tasks since the job is considered failed even if one task fails.
                    jobQueue.add(new Element(jobIdStr));
                    break;
                  }
                }

                LOG.debug("Submitting task {}", task.getTaskId());
                LOG.trace("Task {}", task);
                SchedulableTask schedulableTask = new SchedulableTask(task);
                LOG.trace("Schedulable task {}", schedulableTask);

                // Submit task
                // Note: the job has to be scheduled for processing when the task is complete.
                provisionerQueue.add(new Element(task.getTaskId(),
                                                 jsonSerde.getGson().toJson(schedulableTask)));

                job.setTaskStatus(task.getTaskId(), ClusterTask.Status.IN_PROGRESS);
                taskService.startTask(task);
              }
            }

            // Note: before moving cluster out of pending state, make sure that all in progress jobs are done.
            // If all tasks are completed then move to next stage
            if (completedTasks == currentStage.size()) {
              if (job.hasNextStage()) {
                LOG.debug("Advancing to next stage {} for job {}", job.getCurrentStageNumber(), job.getJobId());
                job.advanceStage();
                jobQueue.add(new Element(jobIdStr));
              } else {
                job.setJobStatus(ClusterJob.Status.COMPLETE);
                LOG.debug("Job {} is complete", jobIdStr);

                loomStats.getSuccessfulClusterStats().incrementStat(job.getClusterAction());

                // Update cluster status
                if (job.getClusterAction() == ClusterAction.CLUSTER_DELETE) {
                  cluster.setStatus(Cluster.Status.TERMINATED);
                } else {
                  cluster.setStatus(Cluster.Status.ACTIVE);
                }
                clusterStore.writeCluster(cluster);
              }
            }
          } else if (inProgressTasks == 0) {
            // Job failed and no in progress tasks remaining, update cluster status
            ClusterAction clusterAction = job.getClusterAction();
            loomStats.getFailedClusterStats().incrementStat(clusterAction);
            cluster.setStatus(clusterAction.getFailureStatus());
            clusterStore.writeCluster(cluster);
          }

          clusterStore.writeClusterJob(job);
        } finally {
          lock.release();
          jobQueue.recordProgress(consumerId, element.getId(), TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "");
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }

  ClusterTask scheduleRetry(Cluster cluster, ClusterJob job, ClusterTask task, Node node) throws Exception {
    // Schedule rollback task before retrying
    scheduleRollbackTask(task);

    task.addAttempt();
    List<ClusterTask> retryTasks = taskService.getRetryTask(cluster, task, node);

    if (retryTasks.size() == 1) {
      LOG.trace("Only one retry task for job {} for task {}", job, task);
      return retryTasks.get(0);
    }

    // store all retry tasks
    for (ClusterTask t : retryTasks) {
      clusterStore.writeClusterTask(t);
    }

    // Remove self from current stage
    job.getCurrentStage().remove(task.getTaskId());
    // Add first retry task to current stage
    job.getCurrentStage().add(retryTasks.get(0).getTaskId());
    // Add the rest of retry tasks after current stage. TODO: this needs to be revisited.
    job.insertTasksAfterCurrentStage(ImmutableList.copyOf(Iterables.transform(Iterables.skip(retryTasks, 1),
                                                                              CLUSTER_TASK_STRING_FUNCTION)));
    LOG.trace("Retry job {} for task {}", job, task);

    return retryTasks.get(0);
  }

  void scheduleRollbackTask(ClusterTask task) throws Exception {
    ClusterTask rollbackTask = taskService.getRollbackTask(task);

    if (rollbackTask == null) {
      LOG.debug("No rollback task for {}", task.getTaskId());
      return;
    }

    clusterStore.writeClusterTask(rollbackTask);

    SchedulableTask schedulableTask = new SchedulableTask(rollbackTask);
    LOG.debug("Submitting rollback task {} for task {}", rollbackTask.getTaskId(), task.getTaskId());
    LOG.trace("Task = {}. Rollback task = {}", task, rollbackTask);

    // No need to retry roll back tasks.
    provisionerQueue.add(new Element(rollbackTask.getTaskId(),
                                     jsonSerde.getGson().toJson(schedulableTask)));
  }

  private static final Function<ClusterTask, String> CLUSTER_TASK_STRING_FUNCTION =
    new Function<ClusterTask, String>() {
      @Override
      public String apply(ClusterTask clusterTask) {
        return clusterTask.getTaskId();
      }
    };
}
