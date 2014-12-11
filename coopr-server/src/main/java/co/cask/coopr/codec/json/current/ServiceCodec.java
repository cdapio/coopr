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
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.service.ServiceDependencies;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link Service}.
 */
public class ServiceCodec extends AbstractBaseEntityCodec<Service> {
  private static final Type ACTIONS_TYPE = new TypeToken<Map<ProvisionerAction, ServiceAction>>() { }.getType();
  private static final Type LINKS_TYPE = new TypeToken<Set<Link>>() { }.getType();

  @Override
  protected void addChildFields(Service service, JsonObject jsonObj, JsonSerializationContext context) {
    jsonObj.add("dependencies", context.serialize(service.getDependencies()));
    JsonObject provisioner = new JsonObject();
    provisioner.add("actions", context.serialize(service.getProvisionerActions()));
    jsonObj.add("provisioner", provisioner);
    jsonObj.add("links", context.serialize(service.getLinks()));
  }

  @Override
  protected BaseEntity.Builder<Service> getBuilder(JsonObject jsonObj, JsonDeserializationContext context) {
    ServiceDependencies dependencies = context.deserialize(jsonObj.get("dependencies"), ServiceDependencies.class);

    JsonObject provisioner = context.deserialize(jsonObj.get("provisioner"), JsonObject.class);
    Map<ProvisionerAction, ServiceAction> actions = Collections.emptyMap();
    if (provisioner != null) {
      actions = context.deserialize(provisioner.get("actions"), ACTIONS_TYPE);
    }
    Set<Link> links = context.deserialize(jsonObj.get("links"), LINKS_TYPE);

    return Service.builder()
      .setDependencies(dependencies)
      .setProvisionerActions(actions)
      .setLinks(links);
  }
}
