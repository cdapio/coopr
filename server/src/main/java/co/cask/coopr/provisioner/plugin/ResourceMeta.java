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

import co.cask.coopr.spec.NamedEntity;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Metadata about a plugin resource, including a name, version, and status.
 */
public class ResourceMeta extends NamedEntity {
  private final int version;
  private final ResourceStatus status;

  public ResourceMeta(String name, int version) {
    this(name, version, ResourceStatus.INACTIVE);
  }

  public ResourceMeta(String name, Integer version, ResourceStatus status) {
    super(name);
    Preconditions.checkArgument(version != null && version >= 0, "Version must be non-null and positive.");
    this.version = version;
    this.status = status;
  }

  /**
   * Get the version.
   *
   * @return Version
   */
  public int getVersion() {
    return version;
  }

  /**
   * Get the status.
   *
   * @return Status
   */
  public ResourceStatus getStatus() {
    return status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ResourceMeta)) {
      return false;
    }

    ResourceMeta that = (ResourceMeta) o;

    return Objects.equal(name, that.name) &&
      version == that.version &&
      Objects.equal(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, version, status);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("version", version)
      .add("status", status)
      .toString();
  }
}
