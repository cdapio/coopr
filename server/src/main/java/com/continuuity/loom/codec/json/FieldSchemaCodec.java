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

import com.continuuity.loom.admin.FieldSchema;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link com.continuuity.loom.admin.FieldSchema}. Used so that the constructor
 * is called to avoid null values where they do not make sense, and to use 'default' as a key.
 */
public class FieldSchemaCodec extends AbstractCodec<FieldSchema> {

  @Override
  public JsonElement serialize(FieldSchema fieldSchema, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("label", context.serialize(fieldSchema.getLabel()));
    jsonObj.add("type", context.serialize(fieldSchema.getType()));
    jsonObj.add("tip", context.serialize(fieldSchema.getTip()));
    jsonObj.add("default", context.serialize(fieldSchema.getDefaultValue()));
    jsonObj.add("override", context.serialize(fieldSchema.getOverride()));
    jsonObj.add("options", context.serialize(fieldSchema.getOptions()));

    return jsonObj;
  }

  @Override
  public FieldSchema deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String label = context.deserialize(jsonObj.get("label"), String.class);
    String fieldType = context.deserialize(jsonObj.get("type"), String.class);
    String tip = context.deserialize(jsonObj.get("tip"), String.class);
    String defaultValue = context.deserialize(jsonObj.get("default"), String.class);
    Boolean override = context.deserialize(jsonObj.get("override"), Boolean.class);
    Set<String> options = context.deserialize(jsonObj.get("options"), new TypeToken<Set<String>>() {}.getType());

    return new FieldSchema(label, fieldType, tip, options, defaultValue, override);
  }
}
