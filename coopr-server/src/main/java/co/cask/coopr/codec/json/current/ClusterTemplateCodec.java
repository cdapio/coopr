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
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link ClusterTemplate}.
 */
public class ClusterTemplateCodec extends AbstractCodec<ClusterTemplate> {

  @Override
  public JsonElement serialize(ClusterTemplate clusterTemplate, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(clusterTemplate.getName()));
    jsonObj.add("icon", context.serialize(clusterTemplate.getIcon()));
    jsonObj.add("description", context.serialize(clusterTemplate.getDescription()));
    jsonObj.add("defaults", context.serialize(clusterTemplate.getClusterDefaults()));
    jsonObj.add("compatibility", context.serialize(clusterTemplate.getCompatibilities()));
    jsonObj.add("constraints", context.serialize(clusterTemplate.getConstraints()));
    jsonObj.add("administration", context.serialize(clusterTemplate.getAdministration()));
    jsonObj.add("links", context.serialize(clusterTemplate.getLinks()));

    return jsonObj;
  }

  @Override
  public ClusterTemplate deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    return ClusterTemplate.builder()
      .setName(context.<String>deserialize(jsonObj.get("name"), String.class))
      .setIcon(context.<String>deserialize(jsonObj.get("icon"), String.class))
      .setDescription(context.<String>deserialize(jsonObj.get("description"), String.class))
      .setClusterDefaults(context.<ClusterDefaults>deserialize(jsonObj.get("defaults"), ClusterDefaults.class))
      .setCompatibilities(context.<Compatibilities>deserialize(jsonObj.get("compatibility"), Compatibilities.class))
      .setConstraints(context.<Constraints>deserialize(jsonObj.get("constraints"), Constraints.class))
      .setAdministration(context.<Administration>deserialize(jsonObj.get("administration"), Administration.class))
      .setLinks(context.<Set<Link>>deserialize(jsonObj.get("links"), new TypeToken<Set<Link>>() {}.getType()))
      .build();
  }
}
