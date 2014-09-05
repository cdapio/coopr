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

import com.google.common.base.Objects;

/**
 * A Gson serializable task object that will be handed off to provisioners.
 */
public class SchedulableTask {
  private final String taskId;
  private final String jobId;
  private final String clusterId;
  private final String taskName;
  private final String nodeId;
  private final TaskConfig config;

  public SchedulableTask(ClusterTask clusterTask, TaskConfig config) {
    this.taskId = clusterTask.getTaskId();
    this.jobId = clusterTask.getJobId();
    this.clusterId = clusterTask.getClusterId();
    this.taskName = clusterTask.getTaskName().name();
    this.nodeId = clusterTask.getNodeId();
    this.config = config;
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
   * Get the id of the job this task is for.
   *
   * @return Id of the job this task is for.
   */
  public String getJobId() {
    return jobId;
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
   * Get the name of the task. For example, create, confirm, bootstrap, install, configure, etc.
   *
   * @return Name of the task.
   */
  public String getTaskName() {
    return taskName;
  }

  /**
   * Get the id of the node this task should run on.
   *
   * @return Id of the node this task should run on.
   */
  public String getNodeId() {
    return nodeId;
  }

  /**
   * Get the configuration json object needed by the provisioner.
   *
   * @return Configuration json object needed by the provisioner.
   */
  public TaskConfig getConfig() {
    return config;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("taskId", taskId)
      .add("jobId", jobId)
      .add("clusterId", clusterId)
      .add("taskName", taskName)
      .add("nodeId", nodeId)
      .add("config", config)
      .toString();
  }
}
