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
import co.cask.coopr.spec.plugin.ParameterType;
import co.cask.coopr.spec.plugin.ParametersSpecification;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for deserializing a {@link co.cask.coopr.spec.plugin.ProviderType}.
 * Used so that the constructor is called to avoid null values where they do not make sense.
 */
public class ProviderTypeCodec extends AbstractBaseEntityCodec<ProviderType> {
  private static final Type PARAMETERS_TYPE =
    new TypeToken<Map<ParameterType, ParametersSpecification>>() { }.getType();
  private static final Type RESOURCES_TYPE =
    new TypeToken<Map<String, ResourceTypeSpecification>>() { }.getType();

  @Override
  protected void addChildFields(ProviderType providerType, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("parameters", context.serialize(providerType.getParameters()));
    jsonObj.add("resourceTypes", context.serialize(providerType.getResourceTypes()));
  }

  @Override
  protected BaseEntity.Builder<ProviderType> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    return ProviderType.builder()
      .setParameters(context.<Map<ParameterType, ParametersSpecification>>deserialize(
        jsonObj.get("parameters"), PARAMETERS_TYPE))
      .setResourceTypes(context.<Map<String, ResourceTypeSpecification>>deserialize(
        jsonObj.get("resourceTypes"), RESOURCES_TYPE));
  }
}
