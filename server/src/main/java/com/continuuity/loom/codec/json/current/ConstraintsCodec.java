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

import com.continuuity.loom.codec.json.AbstractCodec;
import com.continuuity.loom.spec.template.Constraints;
import com.continuuity.loom.spec.template.LayoutConstraint;
import com.continuuity.loom.spec.template.ServiceConstraint;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for serializing/deserializing a {@link Constraints}.
 */
public class ConstraintsCodec extends AbstractCodec<Constraints> {

  @Override
  public JsonElement serialize(Constraints constraints, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("layout", context.serialize(constraints.getLayoutConstraint(), LayoutConstraint.class));
    jsonObj.add("services", context.serialize(constraints.getServiceConstraints()));

    return jsonObj;
  }

  @Override
  public Constraints deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    LayoutConstraint layoutConstraint = context.deserialize(jsonObj.get("layout"), LayoutConstraint.class);
    Map<String, ServiceConstraint> serviceConstraints =
      context.deserialize(jsonObj.get("services"),
                          new TypeToken<Map<String, ServiceConstraint>>() {}.getType());

    return new Constraints(serviceConstraints, layoutConstraint);
  }
}
