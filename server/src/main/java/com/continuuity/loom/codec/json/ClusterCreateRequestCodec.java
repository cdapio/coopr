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
package com.continuuity.loom.codec.json;

import com.continuuity.loom.layout.ClusterCreateRequest;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for deserializing a {@link com.continuuity.loom.layout.ClusterCreateRequest}, used so some validation is done
 * on required fields.
 */
public class ClusterCreateRequestCodec implements JsonDeserializer<ClusterCreateRequest> {

  @Override
  public ClusterCreateRequest deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String name = context.deserialize(jsonObj.get("name"), String.class);
    String description = context.deserialize(jsonObj.get("description"), String.class);
    String clusterTemplate = context.deserialize(jsonObj.get("clusterTemplate"), String.class);
    Integer numMachines = context.deserialize(jsonObj.get("numMachines"), Integer.class);
    String provider = context.deserialize(jsonObj.get("provider"), String.class);
    Set<String> services = context.deserialize(jsonObj.get("services"),
                                               new TypeToken<Set<String>>() {}.getType());
    String hardwaretype = context.deserialize(jsonObj.get("hardwaretype"), String.class);
    String imagetype = context.deserialize(jsonObj.get("imagetype"), String.class);
    Long initialLeaseDuration = context.deserialize(jsonObj.get("initialLeaseDuration"), Long.class);
    String dnsSuffix = context.deserialize(jsonObj.get("dnsSuffix"), String.class);
    JsonObject config = context.deserialize(jsonObj.get("config"), JsonObject.class);

    return new ClusterCreateRequest(name, description, clusterTemplate, numMachines, provider, services, hardwaretype,
                              imagetype, initialLeaseDuration, dnsSuffix, config);
  }
}
