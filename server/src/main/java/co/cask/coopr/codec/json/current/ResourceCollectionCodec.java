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

import co.cask.coopr.common.utils.ImmutablePair;
import co.cask.coopr.provisioner.plugin.PluginType;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.spec.plugin.ResourceTypeFormat;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Codec for serializing a {@link co.cask.coopr.provisioner.plugin.ResourceCollection}
 * for use in sending requests to provisioners.
 */
public class ResourceCollectionCodec implements JsonSerializer<ResourceCollection> {

  @Override
  public JsonElement serialize(ResourceCollection src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject out = new JsonObject();
    out.add("automatortypes", new JsonObject());
    out.add("providertypes", new JsonObject());

    /*
     * Transform it into something like:
     *
     * "resources": {
     *   "automatortypes": {
     *     "chef-solo": {
     *       "cookbooks": {
     *         "format": "archive|file",
     *         "active": [
     *           {
     *             "name":"reactor",
     *             "version":9
     *           }
     *         ]
     *       }
     *     },
     *     ...
     *   },
     * }
     */
    for (ImmutablePair<ResourceType, ResourceTypeSpecification> key : src.getResources().keySet()) {
      ResourceType resourceType = key.getFirst();
      ResourceTypeSpecification typeSpec = key.getSecond();
      ResourceTypeFormat format = typeSpec.getFormat();
      String permissions = typeSpec.getPermissions();
      String pluginTypeStr = pluginTypeToStr(resourceType.getPluginType());
      String pluginName = resourceType.getPluginName();
      String resourceTypeStr = resourceType.getTypeName();
      // ex: json object for automatortypes
      JsonObject pluginTypeObj = out.getAsJsonObject(pluginTypeStr);
      if (!pluginTypeObj.has(pluginName)) {
        pluginTypeObj.add(pluginName, new JsonObject());
      }
      // ex: json object for chef-solo
      JsonObject pluginObj = pluginTypeObj.getAsJsonObject(pluginName);
      if (!pluginObj.has(resourceTypeStr)) {
        pluginObj.add(resourceTypeStr, new JsonObject());
      }
      // ex: json object for cookbooks
      JsonObject resourceListObj = pluginObj.getAsJsonObject(resourceTypeStr);
      // write the format
      resourceListObj.add("format", context.serialize(format));
      if (permissions != null) {
        resourceListObj.addProperty("permissions", permissions);
      }
      // write the list of active resources
      JsonArray activeList = new JsonArray();
      for (ResourceMeta meta : src.getResources().get(key)) {
        JsonObject metaObj = new JsonObject();
        metaObj.addProperty("name", meta.getName());
        metaObj.addProperty("version", meta.getVersion());
        activeList.add(metaObj);
      }
      resourceListObj.add("active", activeList);
    }

    return out;
  }

  private String pluginTypeToStr(PluginType pluginType) {
    if (pluginType == PluginType.AUTOMATOR) {
      return "automatortypes";
    } else if (pluginType == PluginType.PROVIDER) {
      return "providertypes";
    } else {
      throw new IllegalArgumentException("Unknown plugin type " + pluginType);
    }
  }
}
