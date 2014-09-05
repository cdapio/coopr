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
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.service.ServiceDependencies;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

/**
 * Codec for serializing/deserializing a {@link Service}.
 */
public class ServiceCodec extends AbstractCodec<Service> {

  @Override
  public JsonElement serialize(Service service, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(service.getName()));
    jsonObj.add("description", context.serialize(service.getDescription()));
    jsonObj.add("icon", context.serialize(service.getIcon()));
    jsonObj.add("dependencies", context.serialize(service.getDependencies()));
    JsonObject provisioner = new JsonObject();
    provisioner.add("actions", context.serialize(service.getProvisionerActions()));
    jsonObj.add("provisioner", provisioner);

    return jsonObj;
  }

  @Override
  public Service deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    String name = context.deserialize(jsonObj.get("name"), String.class);
    String description = context.deserialize(jsonObj.get("description"), String.class);
    String icon = context.deserialize(jsonObj.get("icon"), String.class);
    ServiceDependencies dependencies = context.deserialize(jsonObj.get("dependencies"), ServiceDependencies.class);

    JsonObject provisioner = context.deserialize(jsonObj.get("provisioner"), JsonObject.class);
    Map<ProvisionerAction, ServiceAction> actions = Collections.emptyMap();
    if (provisioner != null) {
      actions = context.deserialize(provisioner.get("actions"),
                                   new TypeToken<Map<ProvisionerAction, ServiceAction>>(){}.getType());
    }

    return new Service(name, icon, description, dependencies, actions);
  }
}
