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
import co.cask.coopr.spec.template.AbstractTemplate;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Include;
import co.cask.coopr.spec.template.Parent;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link ClusterTemplate}.
 */
public class ClusterTemplateCodec extends AbstractTemplateCodec<ClusterTemplate> {

  private static final Type INCLUDES_TYPE = new com.google.common.reflect.TypeToken<Set<Include>>() { }.getType();

  private static final String EXTENDS_KEY = "extends";
  private static final String INCLUDES_KEY = "includes";

  @Override
  protected void addChildFields(ClusterTemplate template, JsonObject jsonObj, JsonSerializationContext context) {
    super.addChildFields(template, jsonObj, context);

    jsonObj.add(EXTENDS_KEY, context.serialize(template.getParent()));
    jsonObj.add(INCLUDES_KEY, context.serialize(template.getIncludes()));
  }

  @Override
  protected BaseEntity.Builder<ClusterTemplate> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    ClusterTemplate.Builder builder = (ClusterTemplate.Builder)
      super.getBuilder(jsonObj, context);
    builder.setParent(context.<Parent>deserialize(jsonObj.get(EXTENDS_KEY), Parent.class))
      .setIncludes(context.<Set<Include>>deserialize(jsonObj.get(INCLUDES_KEY), INCLUDES_TYPE));
    return builder;
  }

  @Override
  protected AbstractTemplate.Builder getConcreteBuilder() {
    return ClusterTemplate.builder();
  }
}
