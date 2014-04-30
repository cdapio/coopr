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
package com.continuuity.loom.admin;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Defines hardwaretypes, imagetypes, and services that a cluster is compatible with.
 */
public final class Compatibilities {
  public static final Compatibilities EMPTY_COMPATIBILITIES = new Compatibilities(null, null, null);
  private final Set<String> hardwaretypes;
  private final Set<String> imagetypes;
  private final Set<String> services;

  public Compatibilities(Set<String> hardwaretypes, Set<String> imagetypes, Set<String> services) {
    this.hardwaretypes = hardwaretypes == null ? ImmutableSet.<String>of() : hardwaretypes;
    this.imagetypes = imagetypes == null ? ImmutableSet.<String>of() : imagetypes;
    this.services = services == null ? ImmutableSet.<String>of() : services;
  }

  /**
   * Get the set of hardware types that are compatible with the cluster.  An empty set means all types are compatible.
   *
   * @return Set of hardware types that are compatible with the cluster.  An empty set means all types are compatible.
   */
  public Set<String> getHardwaretypes() {
    return hardwaretypes;
  }

  /**
   * Get the set of image types that are compatible with the cluster.  An empty set means all types are compatible.
   *
   * @return Set of image types that are compatible with the cluster.  An empty set means all types are compatible.
   */
  public Set<String> getImagetypes() {
    return imagetypes;
  }

  /**
   * Get the set of services that are compatible with the cluster.  An empty set means all services are compatible.
   *
   * @return Set of services that are compatible with the cluster.  An empty set means all services are compatible.
   */
  public Set<String> getServices() {
    return services;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Compatibilities)) {
      return false;
    }
    Compatibilities other = (Compatibilities) o;
    return Objects.equal(hardwaretypes, other.hardwaretypes) &&
      Objects.equal(imagetypes, other.imagetypes) &&
      Objects.equal(services, other.services);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hardwaretypes, imagetypes, services);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("hardwaretypes", hardwaretypes)
      .add("imagetypes", imagetypes)
      .add("services", services)
      .toString();
  }
}
