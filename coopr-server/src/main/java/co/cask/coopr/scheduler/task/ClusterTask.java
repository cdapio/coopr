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
package co.cask.coopr.scheduler.task;

import co.cask.coopr.account.Account;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.spec.ProvisionerAction;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A task is some action that must be executed on a single node in the cluster. Some examples include creating a node,
 * installing some service, configuring a service, and deleting a node.
 */
public class ClusterTask {

  /**
   * Task status.
   */
  public enum Status {
    NOT_SUBMITTED,
    IN_PROGRESS,
    COMPLETE,
    FAILED,
    DROPPED
  }

  private final String taskId;
  private final String jobId;
  private final String clusterId;
  private final ProvisionerAction taskName;
  private final ClusterAction clusterAction;
  private final String nodeId;
  private final String service;
  private String clusterTemplateName;
  private Account account;
  private List<TaskAttempt> attempts;

  public ClusterTask(ProvisionerAction taskName, TaskId taskId, String nodeId, String service,
                     ClusterAction clusterAction, String clusterTemplateName, Account account) {
    this.taskId = taskId.getId();
    this.jobId = String.valueOf(taskId.getJobId().getId());
    this.clusterId = taskId.getClusterId();
    this.taskName = taskName;
    this.clusterAction = clusterAction;
    this.nodeId = nodeId;
    this.service = service;
    this.attempts = Lists.newArrayList();
    //TODO: populate clusterTemplateName and account field for existing tasks: https://issues.cask.co/browse/COOPR-593
    this.clusterTemplateName = clusterTemplateName;
    this.account = account;
    addAttempt();
  }
  
  int currentAttemptIndex() {
    return attempts.size() - 1;
  }

  /**
   * Sets the submit time for the latest task attempt.
   *
   * @param submitTime task submit time in ms.
   */
  public void setSubmitTime(long submitTime) {
    attempts.get(currentAttemptIndex()).setSubmitTime(submitTime);
  }

  /**
   * Sets the status code for the latest task attempt.
   *
   * @param statusCode status code.
   */
  public void setStatusCode(int statusCode) {
    attempts.get(currentAttemptIndex()).setStatusCode(statusCode);
  }

  /**
   * Sets the status message for the latest task attempt.
   *
   * @param statusMessage status message.
   */
  public void setStatusMessage(String statusMessage) {
    attempts.get(currentAttemptIndex()).setStatusMessage(statusMessage);
  }

  /**
   * Sets the status time for the latest task attempt.
   *
   * @param statusTime status time.
   */
  public void setStatusTime(long statusTime) {
    attempts.get(currentAttemptIndex()).setStatusTime(statusTime);
  }

  /**
   * Sets the status for the latest task attempt.
   *
   * @param status task status.
   */
  public void setStatus(Status status) {
    attempts.get(currentAttemptIndex()).setStatus(status);
  }

  /**
   * Get the id of the task.
   *
   * @return Id of the task.
   */
  public String getTaskId() {
    return taskId;
  }

  /**
   * Get the {@link ProvisionerAction} that this task executes.
   *
   * @return Provisioner action that this task executes.
   */
  public ProvisionerAction getTaskName() {
    return taskName;
  }

  /**
   * Get the id of the node the task will run on.
   *
   * @return Id of the node the task will run on.
   */
  public String getNodeId() {
    return nodeId;
  }

  /**
   * Get the latest task attempt submit time.
   *
   * @return latest task attempt submit time.
   */
  public long getSubmitTime() {
    return attempts.get(currentAttemptIndex()).getSubmitTime();
  }

  /**
   * Get the latest task attempt status.
   *
   * @return latest task attempt status.
   */
  public int getStatusCode() {
    return attempts.get(currentAttemptIndex()).getStatusCode();
  }

  /**
   * Get the latest task attempt status message.
   *
   * @return latest task attempt status message.
   */
  public String getStatusMessage() {
    return attempts.get(currentAttemptIndex()).getStatusMessage();
  }

  /**
   * Get the latest task attempt status time.
   *
   * @return latest task attempt status time.
   */
  public long getStatusTime() {
    return attempts.get(currentAttemptIndex()).getStatusTime();
  }

  /**
   * Get the id of the job this task is a part of.
   *
   * @return Id of the job this task is a part of.
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * Get the latest task attempt status.
   *
   * @return latest task attempt status.
   */
  public Status getStatus() {
    return attempts.get(currentAttemptIndex()).getStatus();
  }

  /**
   * Get the id of the cluster this task is for.
   *
   * @return Id of the cluster this task is for.
   */
  public String getClusterId() {
    return clusterId;
  }

  /**
   * Get the number of times this task has been attempted. A task without any errors and retries will return 1.
   *
   * @return Number of times this task has been attempted.
   */
  public int getNumAttempts() {
    return attempts.size();
  }

  /**
   * Get the service this task is for or empty if the task is unrelated to a service. Some examples are create and
   * delete tasks since they are concerned only with the machine itself and not concerned about services on the node.
   *
   * @return Name of the service the task is for or empty if it is not a service task.
   */
  public String getService() {
    return service;
  }

  /**
   * Get the cluster level action this task is for.
   *
   * @return Cluster level action this task is for.
   */
  public ClusterAction getClusterAction() {
    return clusterAction;
  }

  /**
   * Get a list of all task attempts.
   *
   * @return List of all task attempts.
   */
  List<TaskAttempt> getAttempts() {
    return attempts;
  }

  /**
   * Retrieves template name of the cluster this task is for.
   *
   * @return template name of the cluster this task is for.
   */
  public String getClusterTemplateName() {
    return clusterTemplateName;
  }

  /**
   * Retrieves the account this task is created by.
   *
   * @return the account this task is created by.
   */
  public Account getAccount() {
    return account;
  }

  /**
   * Add a new attempt at this task.
   */
  public void addAttempt() {
    // copy config into task attempt
    attempts.add(new TaskAttempt(attempts.size() + 1));
  }

  /**
   * Return whether or not the task failed before it could create any resources, such as a node or a disk.
   *
   * @return true if the task failed before it could create any resources, false if not
   */
  public boolean failedBeforeCreate() {
    int code = getStatusCode();
    return clusterAction == ClusterAction.CLUSTER_CREATE && code > 199 && code < 300;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("taskId", taskId)
      .add("jobId", jobId)
      .add("clusterId", clusterId)
      .add("taskName", taskName)
      .add("clusterAction", clusterAction)
      .add("nodeId", nodeId)
      .add("service", service)
      .add("attempts", attempts)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ClusterTask) || o == null) {
      return false;
    }
    ClusterTask other = (ClusterTask) o;
    return Objects.equal(taskId, other.taskId) &&
      Objects.equal(jobId, other.jobId) &&
      Objects.equal(clusterId, other.clusterId) &&
      Objects.equal(taskName, other.taskName) &&
      Objects.equal(clusterAction, other.clusterAction) &&
      Objects.equal(nodeId, other.nodeId) &&
      Objects.equal(service, other.service);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(taskId, jobId, clusterId, taskName, clusterAction, nodeId, service);
  }
}
