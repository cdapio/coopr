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
package co.cask.coopr.scheduler.dag;

import com.google.common.base.Objects;

/**
 * Represents an action as a node of the TaskDag. It contains the task, service, and the host where the task runs.
 */
public class TaskNode {
  private final String hostId;
  private final String taskName;
  private final String service;

  public TaskNode(String hostId, String taskName, String service) {
    this.hostId = hostId;
    this.taskName = taskName;
    this.service = service;
  }

  /**
   * Get the id of the {@link co.cask.coopr.cluster.Node} associated with this task node. It is called
   * host id to avoid confusion between a TaskNode, which is a node in the DAG but which represents a task, and a node
   * in a cluster, which is a machine with some hardware, image, and services on it.
   *
   * @return id of the {@link co.cask.coopr.cluster.Node} this task will be executed on.
   */
  public String getHostId() {
    return hostId;
  }

  /**
   * Get the name of the task to execute. For example, create, confirm, bootstrap, install, etc.
   *
   * @return Name of the task to execute.
   */
  public String getTaskName() {
    return taskName;
  }

  /**
   * Get the name of the service the task is for or empty if the task is a task on the machine on not on a service
   * on the machine.
   *
   * @return Name of the service the task is for or empty if the task is not related to a service.
   */
  public String getService() {
    return service;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskNode taskNode = (TaskNode) o;

    if (hostId != null ? !hostId.equals(taskNode.hostId) : taskNode.hostId != null) {
      return false;
    }
    if (service != null ? !service.equals(taskNode.service) : taskNode.service != null) {
      return false;
    }
    if (taskName != null ? !taskName.equals(taskNode.taskName) : taskNode.taskName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = hostId != null ? hostId.hashCode() : 0;
    result = 31 * result + (taskName != null ? taskName.hashCode() : 0);
    result = 31 * result + (service != null ? service.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("hostId", hostId)
      .add("taskName", taskName)
      .add("service", service)
      .toString();
  }
}
