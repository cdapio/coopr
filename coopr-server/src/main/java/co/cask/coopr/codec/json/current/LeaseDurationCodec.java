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
import co.cask.coopr.spec.template.LeaseDuration;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

/**
 * Codec for serializing/deserializing a {@link co.cask.coopr.spec.template.LeaseDuration}.
 */
public class LeaseDurationCodec extends AbstractCodec<LeaseDuration> {
  @Override
  public LeaseDuration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    if (jsonObj.has("initial") && jsonObj.get("initial").isJsonPrimitive() &&
      jsonObj.has("max") && jsonObj.get("max").isJsonPrimitive() &&
      jsonObj.has("step") && jsonObj.get("step").isJsonPrimitive()) {

      JsonPrimitive initial = jsonObj.getAsJsonPrimitive("initial");
      JsonPrimitive max = jsonObj.getAsJsonPrimitive("max");
      JsonPrimitive step = jsonObj.getAsJsonPrimitive("step");

      LeaseDuration.Builder builder = new LeaseDuration.Builder();

      if (initial.isNumber()) {
        builder.setInitial(initial.getAsNumber().longValue());
      } else if (initial.isString()) {
        builder.setInitial(initial.getAsString());
      } else {
        throw new IllegalArgumentException("Invalid format for leaseDuration: " + jsonObj.toString());
      }

      if (max.isNumber()) {
        builder.setMax(max.getAsNumber().longValue());
      } else if (max.isString()) {
        builder.setMax(max.getAsString());
      } else {
        throw new IllegalArgumentException("Invalid format for leaseDuration: " + jsonObj.toString());
      }

      if (step.isNumber()) {
        builder.setStep(step.getAsNumber().longValue());
      } else if (step.isString()) {
        builder.setStep(step.getAsString());
      } else {
        throw new IllegalArgumentException("Invalid format for leaseDuration: " + jsonObj.toString());
      }

      return builder.build();
    }

    throw new IllegalArgumentException("Invalid format for leaseDuration: " + jsonObj.toString());
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
