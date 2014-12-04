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
import co.cask.coopr.spec.Provider;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for serializing/deserializing a {@link Provider}.
 */
public class ProviderCodec extends AbstractBaseEntityCodec<Provider> {
  private static final Type PROVISIONER_FIELDS_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

  @Override
  protected void addChildFields(Provider provider, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("providertype", context.serialize(provider.getProviderType()));
    jsonObj.add("provisioner", context.serialize(provider.getProvisionerFields()));
  }

  @Override
  protected BaseEntity.Builder<Provider> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    return Provider.builder()
      .setProviderType(context.<String>deserialize(jsonObj.get("providertype"), String.class))
      .setProvisionerFields(context.<Map<String, Object>>deserialize(
        jsonObj.get("provisioner"), PROVISIONER_FIELDS_TYPE));
  }
}
