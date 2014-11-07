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
package co.cask.coopr.cluster;

import co.cask.coopr.macro.Expander;
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.service.Service;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a machine in a cluster.
 */
public class Node implements Comparable<Node> {
  private final String id;
  private final String clusterId;
  private final Set<Service> services;
  private final NodeProperties properties;
  private final List<Action> actions;
  private final JsonObject provisionerResults;
  private List<Link> links;

  /**
   * Node status.
   */
  public enum Status {
    IN_PROGRESS,
    COMPLETE,
    FAILED
  }

  public Node(String id, String clusterId, Set<Service> services, NodeProperties properties) {
    this(id, clusterId, services, properties, Lists.<Action>newArrayList(), new JsonObject());
  }

  public Node(String id, String clusterId, Set<Service> services,
              NodeProperties properties, List<Action> actions, JsonObject provisionerResults) {
    this.id = id;
    this.clusterId = clusterId;
    this.services = Sets.newHashSet(services);
    this.properties = properties == null ? NodeProperties.builder().build() : properties;
    this.actions = actions == null ? Lists.<Action>newArrayList() : actions;
    this.provisionerResults = provisionerResults == null ? new JsonObject() : provisionerResults;
  }

  /**
   * Get the id of the node. Id is unique across clusters.
   *
   * @return Id of the node.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the id of the cluster the node is a part of.
   *
   * @return Id of the cluster the node is a part of.
   */
  public String getClusterId() {
    return clusterId;
  }

  /**
   * Get the set of {@link Service}s on the node. This is a copy of the services and may be out of sync with current
   * versions of the services.
   *
   * @return Set of services on the node.
   */
  public Set<Service> getServices() {
    return ImmutableSet.copyOf(services);
  }

  /**
   * Get node properties, such as IP address and hostname.
   *
   * @return Node properties.
   */
  public NodeProperties getProperties() {
    return properties;
  }

  /**
   * Get data returned by provisioners that should be passed on to future tasks.
   *
   * @return Data returned by provisioners that should be passed on to future tasks
   */
  public JsonObject getProvisionerResults() {
    return provisionerResults;
  }

  /**
   * Add an action to the list of actions that have been performed on the node.
   *
   * @param action Action to add.
   */
  public void addAction(Action action) {
    actions.add(action);
  }

  /**
   * Remove the first action in the list of actions that have been performed on the node.
   *
   * @return Action that was removed.
   */
  public Action removeFirstAction() {
    if (actions.isEmpty()) {
      return null;
    }
    return actions.remove(0);
  }

  /**
   * Get the list of actions that have been performed on the node.
   *
   * @return List of actions performed on the node.
   */
  public List<Action> getActions() {
    return ImmutableList.copyOf((actions));
  }

  /**
   * Get an immutable list of service links on the node. Unless {@link #populateLinks(Cluster, java.util.Set)} is
   * called first, this will return null.
   *
   * @return Immutable list of service links on the node.
   */
  public List<Link> getLinks() {
    return links;
  }

  /**
   * Add a service to the node.
   *
   * @param service Service to add to the node.
   */
  public void addService(Service service) {
    this.services.add(service);
  }

  /**
   * Add the provisioner results from a task to the current provisioner results.
   *
   * @param results Results of a task to add to the overall node results
   */
  public void addResults(JsonObject results) {
    for (Map.Entry<String, JsonElement> entry : results.entrySet()) {
      this.provisionerResults.add(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Get all service links on the node, combine them all, and expand any macros in them. Only useful for display
   * purposes.
   */
  public void populateLinks(Cluster cluster, Set<Node> nodes) {
    // take links from the services on this node, expand any self macros that may be there, and combine them all
    ImmutableList.Builder<Link> linksBuilder = ImmutableList.builder();
    for (Service service : services) {
      for (Link link : service.getLinks()) {
        try {
          // The service link may have macros like %host.self% that should get expanded.
          linksBuilder.add(new Link(link.getLabel(), Expander.expand(link.getUrl(), cluster, nodes, this)));
        } catch (Exception e) {
          // if we couldn't expand the macro, just use the original string
          linksBuilder.add(link);
        }
      }
    }
    this.links = linksBuilder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Node node = (Node) o;

    return id.equals(node.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public int compareTo(Node node) {
    return id.compareTo(node.getId());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", id)
      .add("clusterId", clusterId)
      .add("services", services)
      .add("properties", properties)
      .add("actions", actions)
      .toString();
  }

  /**
   * Defines an action on a node.
   */
  public static class Action {
    private final String taskId;
    private final String service;
    private final String action;
    private final long submitTime;

    private long statusTime;
    private Status status;
    private String stderr;
    private String stdout;

    public Action(String taskId, String service, String action) {
      this.taskId = taskId;
      this.service = service;
      this.action = action;
      this.submitTime = System.currentTimeMillis();
      this.status = Status.IN_PROGRESS;
    }

    /**
     * Get the id of the task for this action.
     *
     * @return Id of the task for this action.
     */
    public String getTaskId() {
      return taskId;
    }

    /**
     * Get the action, such as create, confirm, bootstrap, install, configure, etc.
     *
     * @return The action.
     */
    public String getAction() {
      return action;
    }

    /**
     * Get the service name the action is for, or empty if this is not a service action.
     *
     * @return service name the action is for, or empty if this is not a service action.
     */
    public String getService() {
      return service;
    }

    /**
     * Get the timestamp in milliseconds when the action was submitted.
     *
     * @return Timestamp in milliseconds when the action was submitted.
     */
    public long getSubmitTime() {
      return submitTime;
    }

    /**
     * Get the timestamp in milliseconds for the last time the status was updated for the action.
     *
     * @return Timestamp in milliseconds for the last time the status was updated for the action.
     */
    public long getStatusTime() {
      return statusTime;
    }

    /**
     * Set the timestamp in milliseconds for the last time the status was updated for the action.
     *
     * @param statusTime Timestamp in milliseconds to set for the last time the status was updated for the action.
     */
    public void setStatusTime(long statusTime) {
      this.statusTime = statusTime;
    }

    /**
     * Get the status of the action.
     *
     * @return Status of the action.
     */
    public Status getStatus() {
      return status;
    }

    /**
     * Set the status of the action.
     *
     * @param status Status of the action to set.
     */
    public void setStatus(Status status) {
      this.status = status;
    }

    /**
     * Get stderr associated with the action.
     *
     * @return Stderr associated with the action.
     */
    public String getStderr() {
      return stderr;
    }

    /**
     * Set stderr for the action.
     *
     * @param stderr Stderr to set for the action.
     */
    public void setStderr(String stderr) {
      this.stderr = stderr;
    }

    /**
     * Get stdout associated with the action.
     *
     * @return Stdout associated with the action.
     */
    public String getStdout() {
      return stdout;
    }

    /**
     * Set stdout for the action.
     *
     * @param stdout Stdout to set for the action.
     */
    public void setStdout(String stdout) {
      this.stdout = stdout;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
        .add("taskId", taskId)
        .add("service", service)
        .add("action", action)
        .add("submitTime", submitTime)
        .add("statusTime", statusTime)
        .add("status", status)
        .add("stderr", stderr)
        .add("stdout", stdout)
        .toString();
    }
  }
}
