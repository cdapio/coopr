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

import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

/**
 * Codec for serializing/deserializing a {@link ClusterTemplate}.
 */
public class ClusterTemplateCodec extends AbstractCodec<ClusterTemplate> {

  @Override
  public JsonElement serialize(ClusterTemplate clusterTemplate, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(clusterTemplate.getName()));
    jsonObj.add("description", context.serialize(clusterTemplate.getDescription()));
    jsonObj.add("defaults", context.serialize(clusterTemplate.getClusterDefaults()));
    jsonObj.add("compatibility", context.serialize(clusterTemplate.getCompatibilities()));
    jsonObj.add("constraints", context.serialize(clusterTemplate.getConstraints(), Constraints.class));
    jsonObj.add("administration", context.serialize(clusterTemplate.getAdministration(), Administration.class));

    return jsonObj;
  }

  @Override
  public ClusterTemplate deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String name = context.deserialize(jsonObj.get("name"), String.class);
    String description = context.deserialize(jsonObj.get("description"), String.class);
    ClusterDefaults defaults = context.deserialize(jsonObj.get("defaults"), ClusterDefaults.class);
    Compatibilities compatibilites = context.deserialize(jsonObj.get("compatibility"), Compatibilities.class);
    Constraints constraints = context.deserialize(jsonObj.get("constraints"), Constraints.class);
    Administration administration = context.deserialize(jsonObj.get("administration"), Administration.class);

    return new ClusterTemplate(name, description, defaults, compatibilites, constraints, administration);
  }
}
