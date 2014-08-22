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

package com.continuuity.loom.spec.plugin;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Specification for a plugin resource type.
 */
public class ResourceTypeSpecification {
  private final ResourceTypeFormat format;

  public ResourceTypeSpecification(ResourceTypeFormat format) {
    Preconditions.checkArgument(format != null, "Format must be given.");
    this.format = format;
  }

  /**
   * Get the format of the resource.
   *
   * @return Format of the resource.
   */
  public ResourceTypeFormat getFormat() {
    return format;
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
