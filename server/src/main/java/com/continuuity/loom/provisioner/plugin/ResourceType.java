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
package com.continuuity.loom.provisioner.plugin;

import com.google.common.base.Objects;

/**
 * Type of plugin resource. Includes the plugin type (ex: automator or provider), plugin name (ex: chef-solo or shell),
 * and resource type (ex: cookbook, script).
 */
public class ResourceType {
  private final Type type;
  private final String pluginName;
  private final String resourceType;

  public ResourceType(Type type, String pluginName, String resourceType) {
    this.type = type;
    this.pluginName = pluginName;
    this.resourceType = resourceType;
  }

  /**
   * Get the plugin type.
   *
   * @return Plugin type
   */
  public Type getType() {
    return type;
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
   * Get the resource type.
   *
   * @return Resource type
   */
  public String getResourceType() {
    return resourceType;
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

    return Objects.equal(type, that.type) &&
      Objects.equal(pluginName, that.pluginName) &&
      Objects.equal(resourceType, that.resourceType);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, pluginName, resourceType);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("pluginTypeId", pluginName)
      .add("resourceType", resourceType)
      .toString();
  }
}
