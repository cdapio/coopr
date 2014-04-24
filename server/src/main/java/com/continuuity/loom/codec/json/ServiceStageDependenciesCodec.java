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

import com.continuuity.loom.admin.ServiceStageDependencies;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for deserializing a {@link com.continuuity.loom.admin.ServiceStageDependencies}. Just used to make sure
 * null checks in the constructor are used.
 */
public class ServiceStageDependenciesCodec extends AbstractCodec<ServiceStageDependencies> {

  @Override
  public JsonElement serialize(ServiceStageDependencies serviceStageDependencies,
                               Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("requires", context.serialize(serviceStageDependencies.getRequires()));
    jsonObj.add("uses", context.serialize(serviceStageDependencies.getUses()));

    return jsonObj;
  }

  @Override
  public ServiceStageDependencies deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    Set<String> requires = context.deserialize(jsonObj.get("requires"), new TypeToken<Set<String>>() {}.getType());
    Set<String> uses = context.deserialize(jsonObj.get("uses"), new TypeToken<Set<String>>() {}.getType());

    return new ServiceStageDependencies(requires, uses);
  }
}
