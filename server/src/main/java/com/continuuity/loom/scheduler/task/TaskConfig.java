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

import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.cluster.NodeProperties;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Methods for getting config to attach to a cluster task.
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

  public Provider getProvider() {
    return provider;
  }

  public Map<String, NodeProperties> getNodes() {
    return nodes;
  }

  public TaskServiceAction getTaskServiceAction() {
    return taskServiceAction;
  }

  public JsonObject getClusterConfig() {
    return clusterConfig;
  }

  public NodeProperties getNodeProperties() {
    return nodeProperties;
  }

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
    return Objects.hashCode(nodeProperties, provider, taskServiceAction, provisionerResults,clusterConfig, nodes);
  }
}
