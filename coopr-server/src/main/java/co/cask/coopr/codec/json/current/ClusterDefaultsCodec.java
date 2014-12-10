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

import co.cask.coopr.codec.json.AbstractCodec;
import co.cask.coopr.spec.template.ClusterDefaults;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link co.cask.coopr.spec.template.ClusterTemplate}.
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
    JsonObject config = context.deserialize(jsonObj.get("config"), JsonObject.class);
    Set<String> services = Sets.newHashSet();

    JsonArray rawServices = jsonObj.get("services").getAsJsonArray();
    for (JsonElement service : rawServices) {
      if(service instanceof JsonPrimitive){
        services.add(service.getAsString());
      } else if (service instanceof JsonObject){
        String name = ((JsonObject) service).get("name").getAsString();
        JsonElement internalServiceConfig = ((JsonObject) service).get("config");
        services.add(name);
        config.add(name, internalServiceConfig);
      }
    }

    return ClusterDefaults.builder()
      .setServices(services)
      .setProvider(context.<String>deserialize(jsonObj.get("provider"), String.class))
      .setHardwaretype(context.<String>deserialize(jsonObj.get("hardwaretype"), String.class))
      .setImagetype(context.<String>deserialize(jsonObj.get("imagetype"), String.class))
      .setDNSSuffix(context.<String>deserialize(jsonObj.get("dnsSuffix"), String.class))
      .setConfig(config)
      .build();
  }
}
