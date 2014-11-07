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

import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.codec.json.AbstractCodec;
import co.cask.coopr.scheduler.task.TaskConfig;
import co.cask.coopr.scheduler.task.TaskServiceAction;
import co.cask.coopr.spec.Provider;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * To serialize/deserialize a {@link TaskConfig}. The serialized result is sent directly to provisioners.
 */
public class TaskConfigCodec extends AbstractCodec<TaskConfig> {

  @Override
  public JsonElement serialize(TaskConfig taskConfig, Type type, JsonSerializationContext context) {
    JsonObject jsonObj = new JsonObject();

    // this is sent directly to provisioners. It's a mess right now but can't change unless the provisioner changes too.
    jsonObj.add("cluster", taskConfig.getClusterConfig());
    jsonObj.add("service", context.serialize(taskConfig.getTaskServiceAction(), TaskServiceAction.class));
    jsonObj.addProperty("hostname", taskConfig.getNodeProperties().getHostname());
    jsonObj.add("ipaddresses", context.serialize(taskConfig.getNodeProperties().getIPAddresses()));
    jsonObj.addProperty("nodenum", taskConfig.getNodeProperties().getNodenum());
    jsonObj.addProperty("sshuser", taskConfig.getNodeProperties().getSshUser());
    // these should be changed to be the full object instead of the name
    jsonObj.add("hardwaretype", context.serialize(taskConfig.getNodeProperties().getHardwaretype(), String.class));
    jsonObj.add("imagetype", context.serialize(taskConfig.getNodeProperties().getImagetype(), String.class));
    // these should go away once the full hardwaretype/imagetype is included
    jsonObj.addProperty("flavor", taskConfig.getNodeProperties().getFlavor());
    jsonObj.addProperty("image", taskConfig.getNodeProperties().getImage());
    jsonObj.add("automators", context.serialize(taskConfig.getNodeProperties().getAutomators()));
    jsonObj.add("services", context.serialize(taskConfig.getNodeProperties().getServices()));
    jsonObj.add("provider", context.serialize(taskConfig.getProvider()));
    jsonObj.add("nodes", context.serialize(taskConfig.getNodes()));
    jsonObj.add("service", context.serialize(taskConfig.getTaskServiceAction()));

    // gross...
    for (Map.Entry<String, JsonElement> entry : taskConfig.getProvisionerResults().entrySet()) {
      jsonObj.add(entry.getKey(), entry.getValue());
    }

    return jsonObj;
  }

  @Override
  public TaskConfig deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {

    // make a copy since we'll be doing removes. This is because the provisioner results are at the
    // same level as all the other fields because... hacks
    JsonObject jsonObj = shallowCopy(json.getAsJsonObject());

    Provider provider = context.deserialize(jsonObj.remove("provider"), Provider.class);
    Map<String, NodeProperties> nodePropertiesMap =
      context.deserialize(jsonObj.remove("nodes"), new TypeToken<Map<String, NodeProperties>>() { }.getType());
    JsonObject clusterConfig = context.deserialize(jsonObj.remove("cluster"), JsonObject.class);
    TaskServiceAction taskServiceAction = context.deserialize(jsonObj.remove("service"), TaskServiceAction.class);
    // build node properties
    String hostname = context.deserialize(jsonObj.remove("hostname"), String.class);
    Map<String, String> ipaddresses = context.deserialize(jsonObj.remove("ipaddresses"),
                                                          new TypeToken<Map<String, String>>() { }.getType());
    String imagetype = context.deserialize(jsonObj.remove("imagetype"), String.class);
    String hardwaretype = context.deserialize(jsonObj.remove("hardwaretype"), String.class);
    String flavor = context.deserialize(jsonObj.remove("flavor"), String.class);
    String image = context.deserialize(jsonObj.remove("image"), String.class);
    String sshUser = context.deserialize(jsonObj.remove("sshuser"), String.class);
    Integer nodeNum = context.deserialize(jsonObj.remove("nodenum"), Integer.class);
    Set<String> serviceNames =
      context.deserialize(jsonObj.remove("services"), new TypeToken<Set<String>>() { }.getType());
    Set<String> automators =
      context.deserialize(jsonObj.remove("automators"), new TypeToken<Set<String>>() { }.getType());
    NodeProperties nodeProperties = NodeProperties.builder()
      .setHostname(hostname)
      .setIPAddresses(ipaddresses)
      .setNodenum(nodeNum)
      .setHardwaretype(hardwaretype)
      .setImagetype(imagetype)
      .setFlavor(flavor)
      .setImage(image)
      .setSSHUser(sshUser)
      .setAutomators(automators)
      .setServiceNames(serviceNames)
      .build();

    // what's left is the provisioner results
    return new TaskConfig(nodeProperties, provider, nodePropertiesMap, taskServiceAction, clusterConfig, jsonObj);
  }

  private JsonObject shallowCopy(JsonObject o) {
    JsonObject out = new JsonObject();
    for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
      out.add(entry.getKey(), entry.getValue());
    }
    return out;
  }
}
