package com.continuuity.loom.provisioner;

import com.continuuity.loom.store.provisioner.PluginType;
import com.google.common.base.Objects;

/**
 *
 */
public class PluginResourceType {
  private final PluginType pluginType;
  private final String pluginName;
  private final String resourceType;

  public PluginResourceType(PluginType pluginType, String pluginName, String resourceType) {
    this.pluginType = pluginType;
    this.pluginName = pluginName;
    this.resourceType = resourceType;
  }

  public PluginType getPluginType() {
    return pluginType;
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getResourceType() {
    return resourceType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PluginResourceType)) {
      return false;
    }

    PluginResourceType that = (PluginResourceType) o;

    return Objects.equal(pluginType, that.pluginType) &&
      Objects.equal(pluginName, that.pluginName) &&
      Objects.equal(resourceType, that.resourceType);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pluginType, pluginName, resourceType);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("pluginType", pluginType)
      .add("pluginTypeId", pluginName)
      .add("resourceType", resourceType)
      .toString();
  }
}
