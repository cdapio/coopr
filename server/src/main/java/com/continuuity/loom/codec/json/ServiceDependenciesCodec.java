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

import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link com.continuuity.loom.admin.ServiceDependencies}.
 */
public class ServiceDependenciesCodec extends AbstractCodec<ServiceDependencies> {

  @Override
  public JsonElement serialize(ServiceDependencies serviceDependencies, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("provides", context.serialize(serviceDependencies.getProvides()));
    jsonObj.add("conflicts", context.serialize(serviceDependencies.getConflicts()));
    jsonObj.add("install", context.serialize(serviceDependencies.getInstall()));
    jsonObj.add("runtime", context.serialize(serviceDependencies.getRuntime()));

    return jsonObj;
  }

  @Override
  public ServiceDependencies deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    Set<String> provides = context.deserialize(jsonObj.get("provides"), new TypeToken<Set<String>>() {}.getType());
    Set<String> conflicts = context.deserialize(jsonObj.get("conflicts"), new TypeToken<Set<String>>() {}.getType());
    ServiceStageDependencies install = context.deserialize(jsonObj.get("install"), ServiceStageDependencies.class);
    ServiceStageDependencies runtime = context.deserialize(jsonObj.get("runtime"), ServiceStageDependencies.class);

    return new ServiceDependencies(provides, conflicts, install, runtime);
  }
}
