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
package com.continuuity.loom.codec.json.current;

import com.continuuity.loom.codec.json.AbstractCodec;
import com.continuuity.loom.spec.HardwareType;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for serializing/deserializing a {@link HardwareType}.
 */
public class HardwareTypeCodec extends AbstractCodec<HardwareType> {

  @Override
  public JsonElement serialize(HardwareType hardwareType, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(hardwareType.getName()));
    jsonObj.add("icon", context.serialize(hardwareType.getIcon()));
    jsonObj.add("description", context.serialize(hardwareType.getDescription()));
    jsonObj.add("providermap", context.serialize(hardwareType.getProviderMap()));

    return jsonObj;
  }

  @Override
  public HardwareType deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String name = context.deserialize(jsonObj.get("name"), String.class);
    String icon = context.deserialize(jsonObj.get("icon"), String.class);
    String description = context.deserialize(jsonObj.get("description"), String.class);
    Map<String, Map<String, String>> providerMap =
      context.deserialize(jsonObj.get("providermap"), new TypeToken<Map<String, Map<String, String>>>() {}.getType());

    return new HardwareType(name, icon, description, providerMap);
  }
}
