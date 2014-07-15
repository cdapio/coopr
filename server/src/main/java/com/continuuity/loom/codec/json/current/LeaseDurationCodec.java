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

import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.codec.json.AbstractCodec;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

/**
 * Codec for serializing/deserializing a {@link com.continuuity.loom.admin.LeaseDuration}.
 */
public class LeaseDurationCodec extends AbstractCodec<LeaseDuration> {
  @Override
  public LeaseDuration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    Long initial = context.deserialize(jsonObj.get("initial"), Long.class);
    Long max = context.deserialize(jsonObj.get("max"), Long.class);
    Long step = context.deserialize(jsonObj.get("step"), Long.class);

    return new LeaseDuration(initial, max, step);
  }

  @Override
  public JsonElement serialize(LeaseDuration leaseDuration, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("initial", context.serialize(leaseDuration.getInitial()));
    jsonObj.add("max", context.serialize(leaseDuration.getMax()));
    jsonObj.add("step", context.serialize(leaseDuration.getStep()));

    return jsonObj;
  }
}
