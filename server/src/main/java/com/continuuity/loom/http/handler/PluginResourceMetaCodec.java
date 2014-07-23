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
package com.continuuity.loom.http.handler;

import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Serializer for {@link PluginResourceMeta}. Used to do the enum serialization/deserialization correctly.
 */
public class PluginResourceMetaCodec implements JsonSerializer<PluginResourceMeta> {

  @Override
  public JsonElement serialize(PluginResourceMeta meta, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(meta.getName()));
    jsonObj.add("version", context.serialize(meta.getVersion()));
    jsonObj.add("resourceId", context.serialize(meta.getResourceId()));
    jsonObj.add("status", context.serialize(meta.getStatus()));

    return jsonObj;
  }
}
