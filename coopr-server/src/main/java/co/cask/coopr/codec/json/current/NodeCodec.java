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
package co.cask.coopr.codec.json.current;

import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.spec.service.Service;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link Node}. Used for backwards compatibility.
 */
public class NodeCodec implements JsonDeserializer<Node> {
  @Override
  public Node deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String id = context.deserialize(jsonObj.get("id"), String.class);
    String clusterId = context.deserialize(jsonObj.get("clusterId"), String.class);
    HashSet<Service> services = context.deserialize(jsonObj.get("services"),
                                                    new TypeToken<Set<Service>>() { }.getType());
    NodeProperties properties = context.deserialize(jsonObj.get("properties"), NodeProperties.class);
    List<Node.Action> actions = context.deserialize(jsonObj.get("actions"),
                                                    new TypeToken<List<Node.Action>>() { }.getType());
    JsonObject provisionerResults = jsonObj.getAsJsonObject("provisionerResults");

    return new Node(id, clusterId, services, properties, actions, provisionerResults);
  }
}
