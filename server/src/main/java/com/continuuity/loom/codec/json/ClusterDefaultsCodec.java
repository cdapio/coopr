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

import com.continuuity.loom.admin.ClusterDefaults;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link com.continuuity.loom.admin.ClusterTemplate}.
 */
public class ClusterDefaultsCodec extends AbstractCodec<ClusterDefaults> {

  @Override
  public JsonElement serialize(ClusterDefaults clusterDefaults, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("services", context.serialize(clusterDefaults.getServices()));
    jsonObj.add("provider", context.serialize(clusterDefaults.getProvider()));
    jsonObj.add("hardwaretype", context.serialize(clusterDefaults.getHardwaretype()));
    jsonObj.add("imagetype", context.serialize(clusterDefaults.getImagetype()));
    jsonObj.add("dnsSuffix", context.serialize(clusterDefaults.getDnsSuffix()));
    jsonObj.add("config", context.serialize(clusterDefaults.getConfig()));

    return jsonObj;
  }

  @Override
  public ClusterDefaults deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    Set<String> services = context.deserialize(jsonObj.get("services"),
                                               new TypeToken<Set<String>>() {}.getType());
    String provider = context.deserialize(jsonObj.get("provider"), String.class);
    String hardwaretype = context.deserialize(jsonObj.get("hardwaretype"), String.class);
    String imagetype = context.deserialize(jsonObj.get("imagetype"), String.class);
    String dnsSuffix = context.deserialize(jsonObj.get("dnsSuffix"), String.class);
    JsonObject config = context.deserialize(jsonObj.get("config"), JsonObject.class);

    return new ClusterDefaults(services, provider, hardwaretype, imagetype, dnsSuffix, config);
  }
}
