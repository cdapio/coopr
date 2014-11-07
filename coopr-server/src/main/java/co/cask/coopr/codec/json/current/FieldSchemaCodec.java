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
import co.cask.coopr.spec.plugin.FieldSchema;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link co.cask.coopr.spec.plugin.FieldSchema}.
 * Used so that the constructor is called to avoid null values where they do not make sense,
 * and to use 'default' as a key.
 */
public class FieldSchemaCodec extends AbstractCodec<FieldSchema> {

  @Override
  public JsonElement serialize(FieldSchema fieldSchema, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("label", context.serialize(fieldSchema.getLabel()));
    jsonObj.add("type", context.serialize(fieldSchema.getType()));
    jsonObj.add("tip", context.serialize(fieldSchema.getTip()));
    jsonObj.add("default", context.serialize(fieldSchema.getDefaultValue()));
    jsonObj.add("override", context.serialize(fieldSchema.isOverride()));
    jsonObj.add("sensitive", context.serialize(fieldSchema.isSensitive()));
    jsonObj.add("options", context.serialize(fieldSchema.getOptions()));

    return jsonObj;
  }

  @Override
  public FieldSchema deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    return FieldSchema.builder()
      .setLabel(context.<String>deserialize(jsonObj.get("label"), String.class))
      .setType(context.<String>deserialize(jsonObj.get("type"), String.class))
      .setTip(context.<String>deserialize(jsonObj.get("tip"), String.class))
      .setDefaultValue(context.<Object>deserialize(jsonObj.get("default"), Object.class))
      .setOverride(context.<Boolean>deserialize(jsonObj.get("override"), Boolean.class))
      .setSensitive(context.<Boolean>deserialize(jsonObj.get("sensitive"), Boolean.class))
      .setOptions(context.<Set<String>>deserialize(jsonObj.get("options"), new TypeToken<Set<String>>() { }.getType()))
      .build();
  }
}
