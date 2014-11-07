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
import co.cask.coopr.spec.HardwareType;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for serializing/deserializing a {@link HardwareType}.
 */
public class HardwareTypeCodec extends AbstractBaseEntityCodec<HardwareType> {
  private static final Type PROVIDERMAP_TYPE = new TypeToken<Map<String, Map<String, String>>>() { }.getType();

  @Override
  protected void addChildFields(HardwareType hardwareType, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("providermap", context.serialize(hardwareType.getProviderMap()));
  }

  @Override
  protected BaseEntity.Builder<HardwareType> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    return HardwareType.builder()
      .setProviderMap(context.<Map<String, Map<String, String>>>deserialize(
        jsonObj.get("providermap"), PROVIDERMAP_TYPE));
  }
}
