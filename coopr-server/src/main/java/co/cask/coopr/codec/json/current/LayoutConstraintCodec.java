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
import co.cask.coopr.spec.template.LayoutConstraint;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link LayoutConstraint}.
 */
public class LayoutConstraintCodec extends AbstractCodec<LayoutConstraint> {

  @Override
  public JsonElement serialize(LayoutConstraint layoutConstraint, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("mustcoexist", context.serialize(layoutConstraint.getServicesThatMustCoexist()));
    jsonObj.add("cantcoexist", context.serialize(layoutConstraint.getServicesThatMustNotCoexist()));

    return jsonObj;
  }

  @Override
  public LayoutConstraint deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    Set<Set<String>> servicesThatMustCoexist =
      context.deserialize(jsonObj.get("mustcoexist"), new TypeToken<Set<Set<String>>>() { }.getType());
    Set<Set<String>> servicesThatMustNotCoexist =
      context.deserialize(jsonObj.get("cantcoexist"), new TypeToken<Set<Set<String>>>() { }.getType());

    return new LayoutConstraint(servicesThatMustCoexist, servicesThatMustNotCoexist);
  }
}
