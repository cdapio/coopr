/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.codec.json;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link Service}.
 */
public class ServiceCodec extends AbstractCodec<Service> {

  @Override
  public JsonElement serialize(Service service, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    jsonObj.add("name", context.serialize(service.getName()));
    jsonObj.add("description", context.serialize(service.getDescription()));
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
    Set<String> dependsOn = context.deserialize(jsonObj.get("dependson"),
                                                new TypeToken<Set<String>>() {}.getType());
    ServiceDependencies dependencies = context.deserialize(jsonObj.get("dependencies"), ServiceDependencies.class);
    // for backwards compatibility.
    // TODO: take out next major release (1.0)
    if (dependsOn != null && !dependsOn.isEmpty()) {
      if (dependencies == null) {
        dependencies = new ServiceDependencies(null, null, null, new ServiceStageDependencies(dependsOn, null));
      } else if (dependencies.getRuntime().getRequires().isEmpty()) {
        dependencies =
          new ServiceDependencies(dependencies.getProvides(), dependencies.getConflicts(), dependencies.getInstall(),
                                  new ServiceStageDependencies(dependsOn, dependencies.getRuntime().getUses()));
      }
    }

    JsonObject provisioner = context.deserialize(jsonObj.get("provisioner"), JsonObject.class);
    Map<ProvisionerAction, ServiceAction> actions = Collections.emptyMap();
    if (provisioner != null) {
      actions = context.deserialize(provisioner.get("actions"),
                                   new TypeToken<Map<ProvisionerAction, ServiceAction>>(){}.getType());
    }

    return new Service(name, description, dependencies, actions);
  }
}
