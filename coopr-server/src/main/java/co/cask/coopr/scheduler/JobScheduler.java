/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.scheduler;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.GroupElement;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.common.zookeeper.LockService;
import co.cask.coopr.macro.Expander;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.SchedulableTask;
import co.cask.coopr.scheduler.task.TaskConfig;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.scheduler.task.TaskService;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterStoreService;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

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
  private final LockService lockService;
  private final TaskService taskService;
  private final int maxTaskRetries;
  private final Gson gson;
  private final QueueGroup jobQueues;
  private final QueueGroup provisionerQueues;

  @Inject
  private JobScheduler(ClusterStoreService clusterStoreService,
                       QueueService queueService,
                       LockService lockService,
                       TaskService taskService,
                       Configuration conf,
                       Gson gson) {
    this.clusterStore = clusterStoreService.getSystemView();
    this.lockService = lockService;
    this.taskService = taskService;
    this.maxTaskRetries = conf.getInt(Constants.MAX_ACTION_RETRIES);
    this.gson = gson;
    this.jobQueues = queueService.getQueueGroup(QueueType.JOB);
    this.provisionerQueues = queueService.getQueueGroup(QueueType.PROVISIONER);
  }

  @Override
  public void run() {
    try {
      Iterator<GroupElement> jobIter = jobQueues.takeIterator(consumerId);
      while (jobIter.hasNext()) {
        GroupElement gElement = jobIter.next();
        String queueName = gElement.getQueueName();
        Element element = gElement.getElement();
        String jobIdStr = element.getValue();

        LOG.debug("Got job {} to schedule", jobIdStr);
        JobId jobId = JobId.fromString(jobIdStr);
        Lock lock = lockService.getJobLock(queueName, jobId.getClusterId());
        lock.lock();
        try {
          ClusterJob job = clusterStore.getClusterJob(jobId);
          Cluster cluster = clusterStore.getCluster(job.getClusterId());
          // this can happen if 2 tasks complete around the same time and the first one places the job in the queue,
          // sees 0 in progress tasks, and sets the cluster status. The job is still in the queue as another element
          // from the 2nd task and gets here.  In that case, no need to go further.
          if (cluster.getStatus() != Cluster.Status.PENDING) {
            continue;
          }
          if (job.getJobStatus() == ClusterJob.Status.PAUSED) {
            continue;
          }
          LOG.trace("Scheduling job {}", job);
          Set<String> currentStage = job.getCurrentStage();

          // Check how many tasks are completed/not-submitted
          boolean jobFailed = job.getJobStatus() == ClusterJob.Status.FAILED;
          int completedTasks = 0;
          int inProgressTasks = 0;
          Set<ClusterTask> notSubmittedTasks = Sets.newHashSet();
          Set<ClusterTask> retryTasks = Sets.newHashSet();
          // TODO: avoid looking up every single task every time, or at least do a batch lookup
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
                jobFailed = true;
              }
            } else if (task.getStatus() == ClusterTask.Status.IN_PROGRESS) {
              ++inProgressTasks;
            }
          }

          // If the job has not failed continue with scheduling other tasks.
          if (!jobFailed) {

            Set<Node> clusterNodes = clusterStore.getClusterNodes(job.getClusterId());
            Map<String, Node> nodeMap = Maps.newHashMap();
            for (Node node : clusterNodes) {
              nodeMap.put(node.getId(), node);
            }

            // Handle retry tasks if any
            if (!retryTasks.isEmpty()) {
              for (ClusterTask task : retryTasks) {
                notSubmittedTasks.add(scheduleRetry(job, task));
              }
            }

            // Submit any tasks not yet submitted
            if (!notSubmittedTasks.isEmpty()) {
              submitTasks(notSubmittedTasks, cluster, nodeMap, clusterNodes, job, queueName);
            }

            // Note: before moving cluster out of pending state, make sure that all in progress tasks are done.
            // If all tasks are completed then move to next stage
            if (completedTasks == currentStage.size()) {
              if (job.hasNextStage()) {
                LOG.debug("Advancing to next stage {} for job {}", job.getCurrentStageNumber(), job.getJobId());
                job.advanceStage();
                jobQueues.add(queueName, new Element(jobIdStr));
              } else {
                taskService.completeJob(job, cluster);
              }
            }
            clusterStore.writeClusterJob(job);
          } else if (inProgressTasks == 0) {
            // special case: if all tasks were create tasks and all of them failed before they created anything,
            // set the cluster state to 'terminated' instead of letting it go to 'incomplete'.
            if (job.getClusterAction() == ClusterAction.CLUSTER_CREATE && allCreateTasksFailed(job)) {
              String message = job.getStatusMessage();
              // job could have been aborted before any tasks were taken. Keep abort message if that was the case.
              if (message == null || message.isEmpty()) {
                message = "Unable to create nodes, please check your provider settings";
              }
              taskService.failJobAndTerminateCluster(job, cluster, message);
            } else {
              // Job failed and no in progress tasks remaining, update cluster status
              taskService.failJobAndSetClusterStatus(job, cluster);
            }
          } else {
            // Job failed but tasks are still in progress, wait for them to finish before setting cluster status
            taskService.failJob(job);
          }
        } finally {
          lock.unlock();
          jobQueues.recordProgress(consumerId, queueName, element.getId(),
                                  TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "");
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }

  // check that every task that ran failed, and that every failure was a cluster create, and that every failure
  // failed in a way where no resources were actually created (for ex, if provider settings are wrong).
  private boolean allCreateTasksFailed(ClusterJob job) throws IOException {
    for (Map.Entry<String, ClusterTask.Status> entry : job.getTaskStatus().entrySet()) {
      String taskId = entry.getKey();
      ClusterTask.Status taskStatus = entry.getValue();
      // no task can succeed or be in progress
      if (taskStatus == ClusterTask.Status.COMPLETE || taskStatus == ClusterTask.Status.IN_PROGRESS) {
        return false;
      }
      if (taskStatus == ClusterTask.Status.FAILED) {
        // looks up every failed task... not so great.  But it should be roughly equal to the # of nodes in the cluster.
        ClusterTask task = clusterStore.getClusterTask(TaskId.fromString(taskId));
        // check it is a create task
        if (!task.failedBeforeCreate()) {
          return false;
        }
      }
    }
    // if we get here, we only have failed, dropped, or not submitted tasks, and all the failed tasks failed before
    // they could create anything
    return true;
  }

  private void submitTasks(Set<ClusterTask> notSubmittedTasks, Cluster cluster, Map<String, Node> nodeMap,
                           Set<Node> clusterNodes, ClusterJob job, String queueName) throws Exception {
    JsonObject unexpandedClusterConfig = cluster.getConfig();

    for (final ClusterTask task : notSubmittedTasks) {
      Node taskNode = nodeMap.get(task.getNodeId());
      JsonObject clusterConfig = unexpandedClusterConfig;

      // TODO: do this only once and save it
      if (!task.getTaskName().isHardwareAction()) {
        try {
          // expansion does not modify the original input, but creates a new object
          clusterConfig = Expander.expand(clusterConfig, null, cluster, clusterNodes, taskNode).getAsJsonObject();
        } catch (Throwable e) {
          LOG.error("Exception while expanding macros for task {}", task.getTaskId(), e);
          taskService.failTask(task, -1);
          job.setStatusMessage("Exception while expanding macros: " + e.getMessage());
          // no need to schedule more tasks since the job is considered failed even if one task fails.
          jobQueues.add(queueName, new Element(job.getJobId()));
          break;
        }
      }

      Service tService = null;
      for (Service service : taskNode.getServices()) {
        if (service.getName().equals(task.getService())) {
          tService = service;
          break;
        }
      }
      TaskConfig taskConfig = TaskConfig.from(cluster, taskNode, tService, clusterConfig,
                                              task.getTaskName(), clusterNodes);
      LOG.debug("Submitting task {}", task.getTaskId());
      LOG.trace("Task {}", task);
      SchedulableTask schedulableTask = new SchedulableTask(task, taskConfig);
      LOG.trace("Schedulable task {}", schedulableTask);

      // Submit task
      // Note: the job has to be scheduled for processing when the task is complete.
      provisionerQueues.add(
        queueName, new Element(task.getTaskId(), gson.toJson(schedulableTask)));

      job.setTaskStatus(task.getTaskId(), ClusterTask.Status.IN_PROGRESS);
      taskService.startTask(task);
    }
  }

  ClusterTask scheduleRetry(ClusterJob job, ClusterTask task) throws Exception {
    task.addAttempt();
    List<ClusterTask> retryTasks = taskService.getRetryTask(task);

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

  private static final Function<ClusterTask, String> CLUSTER_TASK_STRING_FUNCTION =
    new Function<ClusterTask, String>() {
      @Override
      public String apply(ClusterTask clusterTask) {
        return clusterTask.getTaskId();
      }
    };
}
