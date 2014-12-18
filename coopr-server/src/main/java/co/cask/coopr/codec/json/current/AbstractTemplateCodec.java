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

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.template.AbstractTemplate;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Abstract Codec for template serializing/deserializing.
 */
public abstract class AbstractTemplateCodec<T extends AbstractTemplate> extends AbstractBaseEntityCodec<T> {

  protected static final Type LINKS_TYPE = new com.google.common.reflect.TypeToken<Set<Link>>() { }.getType();

  private static final String DEFAULTS_KEY = "defaults";
  private static final String COMPATIBILITY_KEY = "compatibility";
  private static final String CONSTRAINTS_KEY = "constraints";
  private static final String ADMINISTRATION_KEY = "administration";
  private static final String LINKS_KEY = "links";

  @Override
  protected void addChildFields(T template, JsonObject jsonObj, JsonSerializationContext context) {

    jsonObj.add(DEFAULTS_KEY, context.serialize(template.getClusterDefaults()));
    jsonObj.add(COMPATIBILITY_KEY, context.serialize(template.getCompatibilities()));
    jsonObj.add(CONSTRAINTS_KEY, context.serialize(template.getConstraints()));
    jsonObj.add(ADMINISTRATION_KEY, context.serialize(template.getAdministration()));
    jsonObj.add(LINKS_KEY, context.serialize(template.getLinks()));
  }

  @Override
  @SuppressWarnings("unchecked")
  protected BaseEntity.Builder<T> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    Compatibilities compatibilities = jsonObj.get(COMPATIBILITY_KEY) != null ?
      context.<Compatibilities>deserialize(jsonObj.get(COMPATIBILITY_KEY), Compatibilities.class) :
      Compatibilities.EMPTY_COMPATIBILITIES;

    ClusterDefaults clusterDefaults = jsonObj.get(DEFAULTS_KEY) != null ?
      context.<ClusterDefaults>deserialize(jsonObj.get(DEFAULTS_KEY), ClusterDefaults.class) :
      ClusterDefaults.EMPTY_CLUSTER_DEFAULTS;

    Constraints constraints = jsonObj.get(CONSTRAINTS_KEY) != null ?
      context.<Constraints>deserialize(jsonObj.get(CONSTRAINTS_KEY), Constraints.class) : Constraints.EMPTY_CONSTRAINTS;

    Administration administration = jsonObj.get(ADMINISTRATION_KEY) != null ?
      context.<Administration>deserialize(jsonObj.get(ADMINISTRATION_KEY), Administration.class) :
      Administration.EMPTY_ADMINISTRATION;

    return getConcreteBuilder()
      .setClusterDefaults(clusterDefaults)
      .setCompatibilities(compatibilities)
      .setConstraints(constraints)
      .setAdministration(administration)
      .setLinks(context.<Set<Link>>deserialize(jsonObj.get(LINKS_KEY), LINKS_TYPE));
  }

  protected abstract AbstractTemplate.Builder getConcreteBuilder();
}
