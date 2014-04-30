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

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.Actions;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.List;

/**
 * Service for performing operations on {@link ClusterTask}s.
 */
public class TaskService {
  private final ClusterStore clusterStore;
  private final Actions actions = Actions.getInstance();
  private final LoomStats loomStats;

  @Inject
  public TaskService(ClusterStore clusterStore, LoomStats loomStats) {
    this.clusterStore = clusterStore;
    this.loomStats = loomStats;
  }

  /**
   * Get the rollback task that should run if the given task fails.
   *
   * @param task Task that needs to get rolled back.
   * @return Cluster task that will roll back the given failed task.
   * @throws TaskException
   */
  public ClusterTask getRollbackTask(ClusterTask task) throws TaskException {
    ProvisionerAction rollback = actions.getRollbackActions().get(task.getTaskName());
    if (rollback == null) {
      return null;
    }

    // Note: rollback tasks do not have nodeId
    // There are cases when we don't associate a nodeId with a task so that the node properties don't get overridden
    // by the task output.
    // Eg. deleting a box during a rollback operation since we reuse nodeIds.
    TaskId rollbackTaskId = clusterStore.getNewTaskId(JobId.fromString(task.getJobId()));
    ClusterTask rollbackTask = new ClusterTask(rollback, rollbackTaskId, null,
                                               task.getService(), task.getClusterAction(),
                                               task.getAttempts().get(task.currentAttemptIndex()).getConfig());
    rollbackTask.setConfig(task.getConfig());

    // Associate the rollback task with the task attempt
    task.getAttempts().get(task.currentAttemptIndex()).setRollbackTask(rollbackTask);
    return rollbackTask;
  }

  /**
   * Get tasks that must be run in order to retry the given task that failed on a given node in a given cluster.
   * For example, to retry a service installation, we just retry the task again. However, to retry a node confirm,
   * we need to create another node first and then confirm that node.
   *
   * @param cluster Cluster associated with the failed task.
   * @param task Task that failed and must be retried.
   * @param node Node the task failed on.
   * @return List of tasks that must be executed to retry the given failed task.
   * @throws TaskException
   */
  public List<ClusterTask> getRetryTask(Cluster cluster, ClusterTask task, Node node) throws TaskException {
    ProvisionerAction retryAction = actions.getRetryAction().get(task.getTaskName());

    // If no retry action, return self.
    if (retryAction == null) {
      return ImmutableList.of(task);
    }

    // Get service object
    Service serviceObj = null;
    for (Service s : node.getServices()) {
      if (s.getName().equals(task.getService())) {
        serviceObj = s;
      }
    }

    List<ProvisionerAction> taskOrder = actions.getActionOrder().get(task.getClusterAction());
    if (taskOrder == null) {
      throw new IllegalStateException("Not able to get task order for action " + task.getClusterAction());
    }

    List<ClusterTask> retryTasks = Lists.newArrayList();
    // Create tasks from retry task to current task.
    int retryActionIndex = taskOrder.indexOf(retryAction);
    int currentActionIndex = taskOrder.indexOf(task.getTaskName());
    for (int i = retryActionIndex; i < currentActionIndex; ++i) {
      ProvisionerAction action = taskOrder.get(i);
      TaskId retryTaskId = clusterStore.getNewTaskId(JobId.fromString(task.getJobId()));
      ClusterTask retry = new ClusterTask(action, retryTaskId, task.getNodeId(), task.getService(),
                                          task.getClusterAction(),
                                          TaskConfig.getConfig(cluster, node, serviceObj, action));
      retryTasks.add(retry);
    }
    retryTasks.add(task);
    return retryTasks;
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#FAILED} and the status of the cluster to some given
   * status.
   *
   * @param job Job to fail.
   * @param cluster Cluster to set the status for.
   * @param status Status to set the cluster to.
   * @param message Error message.
   * @throws Exception
   */
  public void failJobAndSetClusterStatus(ClusterJob job, Cluster cluster, Cluster.Status status, String message)
    throws Exception {
    loomStats.getFailedClusterStats().incrementStat(job.getClusterAction());

    cluster.setStatus(status);
    clusterStore.writeCluster(cluster);

    job.setJobStatus(ClusterJob.Status.FAILED);
    job.setStatusMessage(message);
    clusterStore.writeClusterJob(job);
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#FAILED} and the status of the given cluster to
   * {@link com.continuuity.loom.cluster.Cluster.Status#TERMINATED}.
   *
   * @param job Job to fail.
   * @param cluster Cluster to terminate.
   * @param message Error message.
   * @throws Exception
   */
  public void failJobAndTerminateCluster(ClusterJob job, Cluster cluster, String message) throws Exception {
    failJobAndSetClusterStatus(job, cluster, Cluster.Status.TERMINATED, message);
  }

  /**
   * Starts a task by setting the status of the task to {@link ClusterTask.Status#IN_PROGRESS} and the submit time
   * to the current timestamp.
   *
   * @param clusterTask Task to start.
   * @throws Exception
   */
  public void startTask(ClusterTask clusterTask) throws Exception {
    clusterTask.setStatus(ClusterTask.Status.IN_PROGRESS);
    clusterTask.setSubmitTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // Update stats
    loomStats.getProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  /**
   * Drop a task by setting the status of the task to {@link ClusterTask.Status#DROPPED} and the status time to the
   * current timestamp. Tasks can be dropped if there is no longer any point in executing them. For example, if another
   * task in the same stage has failed, the entire job cannot complete so there is no point in executing any
   * unexecuted task in the job.
   *
   * @param clusterTask Task to drop.
   * @throws Exception
   */
  public void dropTask(ClusterTask clusterTask) throws Exception {
    clusterTask.setStatus(ClusterTask.Status.DROPPED);
    clusterTask.setStatusTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // Update stats
    loomStats.getDroppedProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  /**
   * Fail a task by setting the status of the task to {@link ClusterTask.Status#FAILED} and the status time to the
   * current timestamp and the status code to the given code.
   *
   * @param clusterTask Task to fail.
   * @param status Status code of the failed task.
   * @throws Exception
   */
  public void failTask(ClusterTask clusterTask, int status) throws Exception {
    clusterTask.setStatus(ClusterTask.Status.FAILED);
    clusterTask.setStatusCode(status);
    clusterTask.setStatusTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // Update stats
    loomStats.getFailedProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  /**
   * Complete a task by setting the status of the task to {@link ClusterTask.Status#COMPLETE} and the status time to
   * the current timestamp and the status code to the given code.
   *
   * @param clusterTask Task to complete.
   * @param status Status code of the completed task.
   * @throws Exception
   */
  public void completeTask(ClusterTask clusterTask, int status) throws Exception {
    clusterTask.setStatus(ClusterTask.Status.COMPLETE);
    clusterTask.setStatusCode(status);
    clusterTask.setStatusTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // update stats
    loomStats.getSuccessfulProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

}
