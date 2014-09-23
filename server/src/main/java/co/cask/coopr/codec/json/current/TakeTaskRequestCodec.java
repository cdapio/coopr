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

import co.cask.coopr.http.request.TakeTaskRequest;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Codec for deserializing a {@link TakeTaskRequest}. Used to make sure required fields are present.
 */
public class TakeTaskRequestCodec implements JsonDeserializer<TakeTaskRequest> {

  @Override
  public TakeTaskRequest deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String workerId = context.deserialize(jsonObj.get("workerId"), String.class);
    String provisionerId = context.deserialize(jsonObj.get("provisionerId"), String.class);
    String tenantId = context.deserialize(jsonObj.get("tenantId"), String.class);

    return new TakeTaskRequest(workerId, provisionerId, tenantId);
  }
}
