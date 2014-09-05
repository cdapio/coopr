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
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

/**
 * Codec for serializing/deserializing {@link co.cask.coopr.provisioner.plugin.ResourceMeta} objects. Used so
 * that enums are handled correctly.
 */
public class PluginResourceMetaCodec extends AbstractCodec<ResourceMeta> {

  @Override
  public JsonElement serialize(ResourceMeta meta, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(meta.getName()));
    jsonObj.add("version", context.serialize(meta.getVersion()));
    jsonObj.add("status", context.serialize(meta.getStatus()));

    return jsonObj;
  }

  @Override
  public ResourceMeta deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String name = context.deserialize(jsonObj.get("name"), String.class);
    Integer version = context.deserialize(jsonObj.get("version"), Integer.class);
    ResourceStatus status = context.deserialize(jsonObj.get("status"), ResourceStatus.class);

    return new ResourceMeta(name, version, status);
  }
}
