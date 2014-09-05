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
package com.continuuity.loom.codec.json.upgrade;

import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.cluster.NodeProperties;
import com.continuuity.loom.spec.service.Service;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for deserializing a {@link Node}. Used for backwards compatibility.
 */
public class NodeUpgradeCodec implements JsonDeserializer<Node> {

  @Override
  public Node deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String id = context.deserialize(jsonObj.get("id"), String.class);
    String clusterId = context.deserialize(jsonObj.get("clusterId"), String.class);
    Set<Service> services = context.deserialize(jsonObj.get("services"), new TypeToken<Set<Service>>() {}.getType());

    JsonObject properties = jsonObj.getAsJsonObject("properties");
    String ipaddress = context.deserialize(properties.get("ipaddress"), String.class);
    String hostname = context.deserialize(properties.get("hostname"), String.class);
    Integer nodenum = context.deserialize(properties.get("nodenum"), Integer.class);
    if (nodenum == null) {
      nodenum = 0;
    }
    Set<String> automators = context.deserialize(properties.get("automators"),
                                                 new TypeToken<Set<String>>() {}.getType());
    Set<String> nodeServices = context.deserialize(properties.get("services"),
                                                   new TypeToken<Set<String>>() {}.getType());
    String hardwaretype = context.deserialize(properties.get("hardwaretype"), String.class);
    String imagetype = context.deserialize(properties.get("imagetype"), String.class);
    String flavor = context.deserialize(properties.get("flavor"), String.class);
    String image = context.deserialize(properties.get("image"), String.class);
    NodeProperties nodeProperties = NodeProperties.builder()
      .setHostname(hostname)
      .addIPAddress("access_v4", ipaddress)
      .setNodenum(nodenum)
      .setHardwaretype(hardwaretype)
      .setImagetype(imagetype)
      .setFlavor(flavor)
      .setImage(image)
      .setAutomators(automators)
      .setServiceNames(nodeServices)
      .setSSHUser("root")
      .build();
    return new Node(id, clusterId, services, nodeProperties);
  }
}
