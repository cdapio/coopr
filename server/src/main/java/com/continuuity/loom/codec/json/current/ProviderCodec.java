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

import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.codec.json.AbstractCodec;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for serializing/deserializing a {@link Provider}.
 */
public class ProviderCodec extends AbstractCodec<Provider> {

  @Override
  public JsonElement serialize(Provider provider, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(provider.getName()));
    jsonObj.add("description", context.serialize(provider.getDescription()));
    jsonObj.add("providertype", context.serialize(provider.getProviderType()));
    jsonObj.add("provisioner", context.serialize(provider.getProvisionerFields()));

    return jsonObj;
  }

  @Override
  public Provider deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String name = context.deserialize(jsonObj.get("name"), String.class);
    String description = context.deserialize(jsonObj.get("description"), String.class);
    String providerType = context.deserialize(jsonObj.get("providertype"), String.class);
    Map<String, String> provisionerFields = context.deserialize(jsonObj.get("provisioner"),
                                                                new TypeToken<Map<String, String>>() {}.getType());

    return new Provider(name, description, providerType, provisionerFields);
  }
}
