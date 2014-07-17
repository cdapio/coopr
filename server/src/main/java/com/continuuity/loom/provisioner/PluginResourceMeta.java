package com.continuuity.loom.provisioner;

import com.continuuity.loom.admin.NamedEntity;
import com.google.common.base.Objects;

/**
 *
 */
public class PluginResourceMeta extends NamedEntity {
  private final String version;
  private final boolean active;

  public PluginResourceMeta(String name, String version) {
    this(name, version, false);
  }

  public PluginResourceMeta(String name, String version, boolean active) {
    super(name);
    this.version = version;
    this.active = active;
  }

  public String getVersion() {
    return version;
  }

  public boolean isActive() {
    return active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PluginResourceMeta)) {
      return false;
    }

    PluginResourceMeta that = (PluginResourceMeta) o;

    return Objects.equal(name, that.name) &&
      Objects.equal(version, that.version) &&
      Objects.equal(active, that.active);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, version, active);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("version", version)
      .add("active", active)
      .toString();
  }
}
