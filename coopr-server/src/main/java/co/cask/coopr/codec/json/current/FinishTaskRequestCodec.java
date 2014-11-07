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

import co.cask.coopr.http.request.FinishTaskRequest;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Codec for deserializing a {@link co.cask.coopr.http.request.FinishTaskRequest}.
 * Used to make sure required fields are present.
 */
public class FinishTaskRequestCodec implements JsonDeserializer<FinishTaskRequest> {

  @Override
  public FinishTaskRequest deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String workerId = context.deserialize(jsonObj.get("workerId"), String.class);
    String provisionerId = context.deserialize(jsonObj.get("provisionerId"), String.class);
    String tenantId = context.deserialize(jsonObj.get("tenantId"), String.class);
    String taskId = context.deserialize(jsonObj.get("taskId"), String.class);
    String stdout = context.deserialize(jsonObj.get("stdout"), String.class);
    String stderr = context.deserialize(jsonObj.get("stderr"), String.class);
    Integer status = context.deserialize(jsonObj.get("status"), Integer.class);
    String hostname = context.deserialize(jsonObj.get("hostname"), String.class);
    Map<String, String> ipAddresses = context.deserialize(jsonObj.get("ipaddresses"),
                                                          new TypeToken<Map<String, String>>() { }.getType());
    JsonObject result = context.deserialize(jsonObj.get("result"), JsonObject.class);

    return new FinishTaskRequest(workerId, provisionerId, tenantId, taskId,
                                 stdout, stderr, status, hostname, ipAddresses, result);
  }
}
