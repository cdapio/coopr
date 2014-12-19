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

  private static final String SERVICES_KEY = "services";
  private static final String PROVIDER_KEY = "provider";
  private static final String HARDWARE_TYPE_KEY = "hardwaretype";
  private static final String IMAGE_TYPE_KEY = "imagetype";
  private static final String DNS_SUFFIX_KEY = "dnsSuffix";
  private static final String CONFIG_KEY = "config";

  @Override
  public JsonElement serialize(ClusterDefaults clusterDefaults, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();
    jsonObj.add(SERVICES_KEY, context.serialize(clusterDefaults.getServices()));
    jsonObj.add(PROVIDER_KEY, context.serialize(clusterDefaults.getProvider()));
    jsonObj.add(HARDWARE_TYPE_KEY, context.serialize(clusterDefaults.getHardwaretype()));
    jsonObj.add(IMAGE_TYPE_KEY, context.serialize(clusterDefaults.getImagetype()));
    jsonObj.add(DNS_SUFFIX_KEY, context.serialize(clusterDefaults.getDnsSuffix()));
    jsonObj.add(CONFIG_KEY, context.serialize(clusterDefaults.getConfig()));
    return jsonObj;
  }

  @Override
  public ClusterDefaults deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();
    JsonObject config = jsonObj.get(CONFIG_KEY) != null ?
      context.<JsonObject>deserialize(jsonObj.get(CONFIG_KEY), JsonObject.class) : new JsonObject();
    Set<String> services = Sets.newLinkedHashSet();

    JsonElement rawServices = jsonObj.get(SERVICES_KEY);
    if(rawServices != null) {
      for (JsonElement service : rawServices.getAsJsonArray()) {
        if (service instanceof JsonPrimitive) {
          services.add(service.getAsString());
        } else if (service instanceof JsonObject) {
          String name = ((JsonObject) service).get("name").getAsString();
          JsonElement internalServiceConfig = ((JsonObject) service).get(CONFIG_KEY);
          services.add(name);
          config.add(name, internalServiceConfig);
        }
      }
    }

    return ClusterDefaults.builder()
      .setServices(services)
      .setProvider(context.<String>deserialize(jsonObj.get(PROVIDER_KEY), String.class))
      .setHardwaretype(context.<String>deserialize(jsonObj.get(HARDWARE_TYPE_KEY), String.class))
      .setImagetype(context.<String>deserialize(jsonObj.get(IMAGE_TYPE_KEY), String.class))
      .setDNSSuffix(context.<String>deserialize(jsonObj.get(DNS_SUFFIX_KEY), String.class))
      .setConfig(config)
      .build();
  }
}
