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

import co.cask.coopr.http.request.ClusterConfigureRequest;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for deserializing a {@link co.cask.coopr.http.request.ClusterConfigureRequest},
 * used so some validation is done on required fields.
 */
public class ClusterConfigureRequestCodec implements JsonDeserializer<ClusterConfigureRequest> {

  @Override
  public ClusterConfigureRequest deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();
    Map<String, Object> providerFields =
      context.deserialize(jsonObj.get("providerFields"), new TypeToken<Map<String, String>>() { }.getType());
    Boolean restart = context.deserialize(jsonObj.get("restart"), Boolean.class);
    JsonObject config = context.deserialize(jsonObj.get("config"), JsonObject.class);

    return new ClusterConfigureRequest(providerFields, config, restart);
  }
}
