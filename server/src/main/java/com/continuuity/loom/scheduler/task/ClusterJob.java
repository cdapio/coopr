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

import com.continuuity.loom.scheduler.ClusterAction;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A job contains information related to what needs to be done to perform and coordinate a cluster operation. It keeps
 * track of node level tasks that must be executed, and the stages in which tasks must be executed.
 */
public class ClusterJob {

  /**
   * Job status.
   */
  public enum Status {
    NOT_SUBMITTED,
    RUNNING,
    COMPLETE,
    FAILED
  }
  private final String jobId;
  private final String clusterId;
  private final ClusterAction clusterAction;
  private final List<Set<String>> stagedTasks;
  private final Set<String> plannedServices;
  private final Set<String> plannedNodes;
  private int currentStageNumber;
  private final Map<String, ClusterTask.Status> taskStatus;
  private Status jobStatus;
  private String statusMessage;

  /**
   * Create a cluster job with the given job id that represent the given action to perform on a cluster across all
   * services and nodes on the cluster.
   *
   * @param jobId Id of the job.
   * @param clusterAction Action the job is carrying out.
   */
  public ClusterJob(JobId jobId, ClusterAction clusterAction) {
    this(jobId, clusterAction, null, null);
  }

  /**
   * Create a cluster job with the given job id that represent the given action to perform on a cluster, for the given
   * services on the given nodes. Null values for services or nodes indicates that the job covers all cluster services
   * or all cluster nodes.
   *
   * @param jobId Id of the job.
   * @param clusterAction Action the job is carrying out.
   * @param plannedServices Services affected by the job, with null indicating all services.
   * @param plannedNodes Nodes affected by the job, with null indicating all nodes.
   */
  public ClusterJob(JobId jobId, ClusterAction clusterAction, Set<String> plannedServices, Set<String> plannedNodes) {
    this.jobId = jobId.getId();
    this.clusterId = jobId.getClusterId();
    this.clusterAction = clusterAction;
    this.stagedTasks = Lists.newArrayList();
    this.currentStageNumber = 0;
    this.jobStatus = Status.NOT_SUBMITTED;
    this.plannedServices = plannedServices;
    this.plannedNodes = plannedNodes;
    taskStatus = Maps.newHashMap();
  }


  /**
   * Get the id of the job.
   *
   * @return Id of the job.
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * Get the id of the cluster this job is for.
   *
   * @return Id of the cluster this job is for.
   */
  public String getClusterId() {
    return clusterId;
  }

  /**
   * Get the {@link ClusterAction} this job is trying to perform.
   *
   * @return Cluster action this job is trying to perform.
   */
  public ClusterAction getClusterAction() {
    return clusterAction;
  }

  /**
   * Get the ids of tasks that need to be run in order to perform the cluster operation broken down into stages. Each
   * stage comprises one or more tasks which can all be run in parallel. However, tasks in one stage cannot be started
   * until all the tasks in the previous stage have successfully completed.
   *
   * @return Ids of tasks that need to be run broken down into stages.
   */
  public List<Set<String>> getStagedTasks() {
    return stagedTasks;
  }

  /**
   * Get cluster services to include in job planning. Services not in the set should not be included in job planning,
   * with a null value indicating that all services should be included in planning.
   *
   * @return Cluster services to be included in job planning.
   */
  public Set<String> getPlannedServices() {
    return plannedServices;
  }

  /**
   * Get cluster nodes to include in job planning. Services not in the set should not be included in job planning,
   * with a null value indicating that all nodes should be included in planning.
   *
   * @return Cluster nodes to be included in job planning.
   */
  public Set<String> getPlannedNodes() {
    return plannedNodes;
  }

  /**
   * Add a stage of tasks to the job.
   *
   * @param tasks Set of tasks that can be run in parallel in a single stage.
   */
  public void addStage(Set<String> tasks) {
    stagedTasks.add(tasks);
    for (String task : tasks) {
      taskStatus.put(task, ClusterTask.Status.NOT_SUBMITTED);
    }
  }

  /**
   * Get all the task ids for the current stage.
   *
   * @return Set of task ids for the current stage.
   */
  public Set<String> getCurrentStage() {
    return stagedTasks.get(currentStageNumber);
  }

  /**
   * Clear all tasks for the job. May be used when the job has failed and there is no point in continuing.
   */
  public void clearTasks() {
    stagedTasks.clear();
    taskStatus.clear();
  }

  /**
   * Inserts a list of tasks after this stage. This is required for retries.
   *
   * @param tasks tasks to insert after the current stage.
   */
  public void insertTasksAfterCurrentStage(List<String> tasks) {
    for (int i = 0; i < tasks.size(); ++i) {
      stagedTasks.add(currentStageNumber + 1 + i, ImmutableSet.of(tasks.get(i)));
    }
  }

  /**
   * Return whether or not there is another stage of tasks to execute.
   *
   * @return true if there is another stage of tasks, false otherwise.
   */
  public boolean hasNextStage() {
    return currentStageNumber < stagedTasks.size() - 1;
  }

  /**
   * Move on to the next stage. Should be called once all the tasks in the current stage have successfully completed.
   */
  public void advanceStage() {
    ++currentStageNumber;
  }

  /**
   * Get the current stage number.
   *
   * @return Current stage number.
   */
  public int getCurrentStageNumber() {
    return currentStageNumber;
  }

  /**
   * Set the status of a specific task.
   *
   * @param taskId Id of the task whose status must be set.
   * @param status Status to set the task status to.
   */
  public void setTaskStatus(String taskId, ClusterTask.Status status) {
    this.taskStatus.put(taskId, status);
  }

  /**
   * Get a mapping of all task ids to their status.
   *
   * @return Mapping of all task ids to their status.
   */
  public Map<String, ClusterTask.Status> getTaskStatus() {
    return taskStatus;
  }

  /**
   * Set the status of the entire job.
   *
   * @param jobStatus Status to set the job status to.
   */
  public void setJobStatus(Status jobStatus) {
    this.jobStatus = jobStatus;
  }

  /**
   * Get the status of the job.
   *
   * @return Status of the job.
   */
  public Status getJobStatus() {
    return jobStatus;
  }

  /**
   * Get the status message for the job.
   *
   * @return Status message for the job.
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Set the status message for the job.
   *
   * @param statusMessage Status message to set for the job.
   */
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("jobId", jobId)
      .add("clusterId", clusterId)
      .add("clusterAction", clusterAction)
      .add("stagedTasks", stagedTasks)
      .add("currentStageNumber", currentStageNumber)
      .add("taskStatus", taskStatus)
      .add("jobStatus", jobStatus)
      .add("statusMessage", statusMessage)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ClusterJob)) {
      return false;
    }
    ClusterJob other = (ClusterJob) o;
    return Objects.equal(jobId, other.jobId) &&
      Objects.equal(clusterId, other.clusterId) &&
      Objects.equal(stagedTasks, other.stagedTasks) &&
      Objects.equal(currentStageNumber, other.currentStageNumber) &&
      Objects.equal(taskStatus, other.taskStatus) &&
      Objects.equal(jobStatus, other.jobStatus) &&
      Objects.equal(statusMessage, statusMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(jobId, clusterId, stagedTasks, currentStageNumber, taskStatus, jobStatus, statusMessage);
  }
}
