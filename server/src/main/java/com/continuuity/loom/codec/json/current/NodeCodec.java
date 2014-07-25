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
package com.continuuity.loom.codec.json.current;

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Node;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Codec for serializing/deserializing a {@link Node}. Used for backwards compatibility.
 */
public class NodeCodec implements JsonDeserializer<Node> {
  @Override
  public Node deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String id = context.deserialize(jsonObj.get("id"), String.class);
    String clusterId = context.deserialize(jsonObj.get("clusterId"), String.class);

    return new Node(id, clusterId, new HashSet<Service>() {}, new HashMap<String, String>());
  }
}
