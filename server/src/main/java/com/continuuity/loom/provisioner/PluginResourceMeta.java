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
package com.continuuity.loom.provisioner;

import com.continuuity.loom.admin.NamedEntity;
import com.google.common.base.Objects;

/**
 * Metadata about a plugin resource, such as the name, version, and whether or not it is the active version.
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

  /**
   * Get the version.
   *
   * @return Version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Get whether or not the resource is active.
   *
   * @return Whether or not the resource is active
   */
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
