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

import java.util.UUID;

/**
 * Metadata about a plugin resource, including a unique id, a name, a version, and status.
 */
public class PluginResourceMeta extends NamedEntity {
  private final String resourceId;
  private final String version;
  private final PluginResourceStatus status;

  private PluginResourceMeta(String resourceId, String name, String version, PluginResourceStatus status) {
    super(name);
    this.resourceId = resourceId;
    this.version = version;
    this.status = status;
  }

  /**
   * Create metadata for a new plugin resource, with a unique id.
   *
   * @param name Name of the resource
   * @param version Version of the resource
   * @return Metadata for a new plugin resource, with a unique id
   */
  public static PluginResourceMeta createNew(String name, String version) {
    return new PluginResourceMeta(UUID.randomUUID().toString(), name, version, PluginResourceStatus.INACTIVE);
  }

  /**
   * Create metadata for an existing resource, with a pre-existing id.
   *
   * @param id Id of the resource
   * @param name Name of the resource
   * @param version Version of the resource
   * @param status Status of the resource
   * @return metadata for an existing resource, with a pre-existing id
   */
  public static PluginResourceMeta fromExisting(String id, String name, String version, PluginResourceStatus status) {
    return new PluginResourceMeta(id, name, version, status);
  }

  /**
   * Get the id.
   *
   * @return Id
   */
  public String getResourceId() {
    return resourceId;
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
   * Get the status.
   *
   * @return Status
   */
  public PluginResourceStatus getStatus() {
    return status;
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

    return Objects.equal(resourceId, that.resourceId) &&
      Objects.equal(name, that.name) &&
      Objects.equal(version, that.version) &&
      Objects.equal(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(resourceId, name, version, status);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("resourceId", resourceId)
      .add("name", name)
      .add("version", version)
      .add("status", status)
      .toString();
  }
}
