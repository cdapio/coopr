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
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Methods for getting config to attach to a cluster task.
 */
public class TaskConfig {
  private static final Logger LOG = LoggerFactory.getLogger(TaskConfig.class);
  private static final JsonSerde JSON_SERDE = new JsonSerde();

  /**
   * Create a configuration json object to use for a given cluster, node, service, and action.
   *
   * @param cluster Cluster to use.
   * @param node Node that configuration will be sent to.
   * @param service Service that the configuration is for.
   * @param action Action that will be performed that needs the configuration.
   * @return Configuration json object to use for a given cluster, node, service, and action.
   */
  public static JsonObject getConfig(Cluster cluster, Node node, Service service, ProvisionerAction action) {
    JsonObject jsonObject = new JsonObject();

    // cluster config
    // If user config is present use it, otherwise use default cluster config.
    if (cluster.getConfig() == null || cluster.getConfig().entrySet().isEmpty()) {
      jsonObject.add("cluster", cluster.getClusterTemplate().getClusterDefaults().getConfig());
    } else {
      jsonObject.add("cluster", cluster.getConfig());
    }

    // service config
    if (service != null) {
      JsonObject serviceObject = new JsonObject();
      serviceObject.addProperty("name", service.getName());
      ServiceAction serviceAction = service.getProvisionerActions().get(action);

      // If service action is null for a service, then the task can be ignored.
      if (serviceAction == null) {
        LOG.debug("Service {} with no service action for provisioner action {}. Not generating config.", service,
                  action);
        return null;
      }

      serviceObject.add("action", JSON_SERDE.getGson().toJsonTree(serviceAction));
      jsonObject.add("service", serviceObject);
    }

    // node config
    for (Map.Entry<String, JsonElement> entry : node.getProperties().entrySet()) {
      jsonObject.add(entry.getKey(), entry.getValue());
    }

    // provider config
    jsonObject.add("provider", JSON_SERDE.getGson().toJsonTree(cluster.getProvider()));

    return jsonObject;
  }

  /**
   * Add all the properties from the node into the given json object configuration.
   *
   * @param jsonObject Configuration to update.
   * @param node Node containing properties that need to be copied to the given configuration.
   * @return Updated json object.
   */
  public static JsonObject updateNodeProperties(JsonObject jsonObject, Node node) {
    for (Map.Entry<String, JsonElement> entry : node.getProperties().entrySet()) {
      jsonObject.add(entry.getKey(), entry.getValue());
    }
    return jsonObject;
  }

  /**
   * Add all node properties to a json object configuration.
   *
   * @param jsonObject Configuration to add node properties to.
   * @param clusterNodes Set of nodes whose properties need to be added to the configuration.
   * @return Updated json object.
   */
  public static JsonObject addNodeList(JsonObject jsonObject, Set<Node> clusterNodes) {
    JsonObject nodesJson = new JsonObject();
    for (Node node : clusterNodes) {
      JsonObject nodeJson = new JsonObject();
      for (Node.Properties property : Node.Properties.values()) {
        String key = property.name().toLowerCase();
        JsonElement value = node.getProperties().get(key);
        if (value != null) {
          nodeJson.add(key, value);
        }
      }

      nodesJson.add(node.getId(), nodeJson);
    }

    jsonObject.add("nodes", nodesJson);
    return jsonObject;
  }
}
