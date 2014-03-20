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

import com.continuuity.loom.admin.ServiceAction;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

/**
 * Codec for serializing/deserializing a {@link com.continuuity.loom.admin.ServiceAction}.
 */
public class ServiceActionCodec extends AbstractCodec<ServiceAction> {

  @Override
  public JsonElement serialize(ServiceAction action, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("type", context.serialize(action.getType()));
    jsonObj.add("script", context.serialize(action.getScript()));
    jsonObj.add("data", context.serialize(action.getData()));

    return jsonObj;
  }

  @Override
  public ServiceAction deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String actionType = context.deserialize(jsonObj.get("type"), String.class);
    String script = context.deserialize(jsonObj.get("script"), String.class);
    String data = context.deserialize(jsonObj.get("data"), String.class);

    return new ServiceAction(actionType, script, data);
  }
}
