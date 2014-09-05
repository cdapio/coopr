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
package co.cask.coopr.provisioner.plugin;

import com.google.common.base.Objects;

/**
 * Type of plugin resource. Includes the plugin type (ex: automator or provider), plugin name (ex: chef-solo or shell),
 * and type name (ex: cookbook, script). A resource type and a {@link ResourceMeta} uniquely identify a resource.
 */
public class ResourceType {
  private final PluginType pluginType;
  private final String pluginName;
  private final String typeName;

  public ResourceType(PluginType pluginType, String pluginName, String typeName) {
    this.pluginType = pluginType;
    this.pluginName = pluginName;
    this.typeName = typeName;
  }

  /**
   * Get the plugin type.
   *
   * @return Plugin type
   */
  public PluginType getPluginType() {
    return pluginType;
  }

  /**
   * Get the plugin name.
   *
   * @return Plugin name
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * Get the name of the resource type.
   *
   * @return Name of the resource type
   */
  public String getTypeName() {
    return typeName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ResourceType)) {
      return false;
    }

    ResourceType that = (ResourceType) o;

    return Objects.equal(pluginType, that.pluginType) &&
      Objects.equal(pluginName, that.pluginName) &&
      Objects.equal(typeName, that.typeName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pluginType, pluginName, typeName);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", pluginType)
      .add("pluginTypeId", pluginName)
      .add("typeName", typeName)
      .toString();
  }
}
