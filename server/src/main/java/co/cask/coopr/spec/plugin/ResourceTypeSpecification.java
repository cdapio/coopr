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

package co.cask.coopr.spec.plugin;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Specification for a plugin resource type. Includes the format of the resource, and if it's a file format, optionally
 * includes the file permissions as well.
 */
public class ResourceTypeSpecification {
  private final ResourceTypeFormat format;
  private final String permissions;

  public ResourceTypeSpecification(ResourceTypeFormat format, String permissions) {
    Preconditions.checkArgument(format != null, "Format must be given.");
    this.format = format;
    validatePermissions(format, permissions);
    this.permissions = permissions;
  }

  private void validatePermissions(ResourceTypeFormat format, String permissions) {
    if (permissions != null) {
      Preconditions.checkArgument(format == ResourceTypeFormat.FILE, "permissions are only allowed for file format.");
      int length = permissions.length();
      String errMsg = permissions + " is an invalid file permission";
      Preconditions.checkArgument(length == 3 || length == 4, errMsg);
      for (int i = 0; i < length; i++) {
        Preconditions.checkArgument(Character.isDigit(permissions.charAt(i)), errMsg);
      }
    }
  }

  /**
   * Get the format of the resource.
   *
   * @return Format of the resource.
   */
  public ResourceTypeFormat getFormat() {
    return format;
  }

  /**
   * Get the permissions of the resource.
   *
   * @return Permissions of the resource.
   */
  public String getPermissions() {
    return permissions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResourceTypeSpecification that = (ResourceTypeSpecification) o;

    return Objects.equal(format, that.format);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(format);
  }
}
