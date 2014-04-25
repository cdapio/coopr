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
import com.continuuity.loom.admin.ParameterType;
import com.continuuity.loom.admin.ParametersSpecification;
import com.continuuity.loom.admin.ProviderType;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Codec for deserializing a {@link com.continuuity.loom.admin.ParametersSpecification}. Used so that the constructor
 * is called to avoid null values where they do not make sense.
 */
public class ParametersSpecificationCodec implements JsonDeserializer<ParametersSpecification> {

  @Override
  public ParametersSpecification deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    Map<String, FieldSchema> fields =
      context.deserialize(jsonObj.get("fields"), new TypeToken<Map<String, FieldSchema>>() {}.getType());
    Set<Set<String>> required =
      context.deserialize(jsonObj.get("required"), new TypeToken<Set<Set<String>>>() {}.getType());

    return new ParametersSpecification(fields, required);
  }
}
