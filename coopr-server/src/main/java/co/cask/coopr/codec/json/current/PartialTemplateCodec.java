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
import co.cask.coopr.spec.template.PartialTemplate;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

/**
 * Codec for serializing/deserializing a {@link co.cask.coopr.spec.template.PartialTemplate}.
 */
public class PartialTemplateCodec extends AbstractTemplateCodec<PartialTemplate> {

  @Override
  protected void addChildFields(AbstractTemplate template, JsonObject jsonObj, JsonSerializationContext context) {
    super.addChildFields(template, jsonObj, context);
    PartialTemplate partialTemplate = (PartialTemplate) template;
    jsonObj.add("immutable", context.serialize(partialTemplate.isImmutable()));
  }

  @Override
  protected BaseEntity.Builder<PartialTemplate> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    PartialTemplate.PartialTemplateBuilder builder = (PartialTemplate.PartialTemplateBuilder)
      super.getBuilder(jsonObj, context);
    builder.setImmutable(context.<Boolean>deserialize(jsonObj.get("immutable"), Boolean.class));
    return builder;
  }

  @Override
  protected AbstractTemplate.Builder getConcreteBuilder() {
    return PartialTemplate.builder();
  }
}
