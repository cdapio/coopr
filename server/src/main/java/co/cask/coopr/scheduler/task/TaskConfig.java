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

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Map;

/**
 * The config section of a cluster task sent to provisioners. Part of a {@link SchedulableTask}.
 */
public class TaskConfig {
  private final NodeProperties nodeProperties;
  private final Provider provider;
  // list of other nodes in the cluster
  private final Map<String, NodeProperties> nodes;
  // service action to perform
  private final TaskServiceAction taskServiceAction;
  // arbitrary cluster config that comes from the cluster template
  private final JsonObject clusterConfig;
  private final JsonObject provisionerResults;

  /**
   * Create a task config from the given input.
   *
   * @param cluster Cluster the task is operating on.
   * @param node Node the task should take place on.
   * @param service Service the task is operating on.
   *                May be null for tasks that are on the node itself but not on a service.
   * @param clusterConfig Cluster config with expanded macros.
   * @param action Action to perform.
   * @param clusterNodes Collection of all nodes in the cluster.
   * @return Task config created from the given input.
   */
  public static TaskConfig from(Cluster cluster, Node node, Service service, JsonObject clusterConfig,
                                ProvisionerAction action, Collection<Node> clusterNodes) {
    Provider provider = cluster.getProvider();
    // will be null if the config is for a node action like create, confirm, bootstrap, delete
    TaskServiceAction taskServiceAction = service == null ? null :
      new TaskServiceAction(service.getName(), service.getProvisionerActions().get(action));
    ImmutableMap.Builder builder = ImmutableMap.<String, NodeProperties>builder();
    if (clusterNodes != null) {
      for (Node clusterNode : clusterNodes) {
        builder.put(clusterNode.getId(), clusterNode.getProperties());
      }
    }
    JsonObject provisionerResults = node.getProvisionerResults();
    return new TaskConfig(node.getProperties(), provider, builder.build(),
                          taskServiceAction, clusterConfig, provisionerResults);
  }

  public TaskConfig(NodeProperties nodeProperties, Provider provider, Map<String, NodeProperties> nodes,
                    TaskServiceAction taskServiceAction, JsonObject clusterConfig, JsonObject provisionerResults) {
    this.nodeProperties = nodeProperties;
    this.provider = provider;
    this.nodes = nodes;
    this.taskServiceAction = taskServiceAction;
    this.clusterConfig = clusterConfig;
    this.provisionerResults = provisionerResults;
  }

  /**
   * Get the provider to use to perform node operations.
   *
   * @return Provider to use to perform node operations.
   */
  public Provider getProvider() {
    return provider;
  }

  /**
   * Get the mapping of node id to node properties for all nodes in the cluster.
   *
   * @return Mapping of node id to node properties for all nodes in the cluster.
   */
  public Map<String, NodeProperties> getNodes() {
    return nodes;
  }

  /**
   * Get the condensed service object containing just the relevant action to perform.
   *
   * @return Condensed service object containing just the relevant action to perform.
   */
  public TaskServiceAction getTaskServiceAction() {
    return taskServiceAction;
  }

  /**
   * Get the cluster config with macros expanded.
   *
   * @return Cluster config with macros expanded.
   */
  public JsonObject getClusterConfig() {
    return clusterConfig;
  }

  /**
   * Get the properties of the node the task should be performed on.
   *
   * @return Properties of the node the task should be performed on.
   */
  public NodeProperties getNodeProperties() {
    return nodeProperties;
  }

  /**
   * Get the payload returned by provisioners that should be passed on to current and future tasks.
   *
   * @return Payload returned by provisioners that should be passed on to current and future tasks.
   */
  public JsonObject getProvisionerResults() {
    return provisionerResults;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskConfig that = (TaskConfig) o;

    return Objects.equal(nodeProperties, that.nodeProperties) &&
      Objects.equal(provider, that.provider) &&
      Objects.equal(taskServiceAction, that.taskServiceAction) &&
      Objects.equal(provisionerResults, that.provisionerResults) &&
      Objects.equal(clusterConfig, that.clusterConfig) &&
      Objects.equal(nodes, that.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nodeProperties, provider, taskServiceAction, provisionerResults, clusterConfig, nodes);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("nodeProperties", nodeProperties)
      .add("provider", provider)
      .add("taskServiceAction", taskServiceAction)
      .add("provisionerResults", provisionerResults)
      .add("clusterConfig", clusterConfig)
      .add("nodes", nodes)
      .toString();
  }
}
