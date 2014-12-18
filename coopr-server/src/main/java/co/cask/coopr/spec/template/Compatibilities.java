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
package co.cask.coopr.spec.template;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Defines hardwaretypes, imagetypes, and services that a cluster is compatible with.
 */
public final class Compatibilities {
  public static final Compatibilities EMPTY_COMPATIBILITIES = builder().build();
  final Set<String> hardwaretypes;
  final Set<String> imagetypes;
  final Set<String> services;

  private Compatibilities(Set<String> hardwaretypes, Set<String> imagetypes, Set<String> services) {
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

  /**
   * Return whether or not the given services are compatible.
   *
   * @param services Services to check compatibility of.
   * @return True if all services are compatible, false if not.
   */
  public boolean compatibleWithServices(Set<String> services) {
    return this.services.isEmpty() || this.services.containsAll(services);
  }

  /**
   * Return whether or not the given service is compatible.
   *
   * @param service Service to check compatibility of.
   * @return True if the service is compatible, false if not.
   */
  public boolean compatibleWithService(String service) {
    return services.isEmpty() || services.contains(service);
  }

  /**
   * Return whether or not the given image types are compatible.
   *
   * @param imagetypes Image types to check compatibility of.
   * @return True if the image types are compatible, false if not.
   */
  public boolean compatibleWithImageTypes(Set<String> imagetypes) {
    return this.imagetypes.isEmpty() || this.imagetypes.containsAll(imagetypes);
  }
  /**
   * Return whether or not the given image type is compatible.
   *
   * @param imageType Image type to check compatibility of.
   * @return True if the image type is compatible, false if not.
   */
  public boolean compatibleWithImageType(String imageType) {
    return imagetypes.isEmpty() || imagetypes.contains(imageType);
  }

  /**
   * Return whether or not the given hardware types are compatible.
   *
   * @param hardwaretypes Hardware types to check compatibility of.
   * @return True if the hardware types are compatible, false if not.
   */
  public boolean compatibleWithHardwareTypes(Set<String> hardwaretypes) {
    return this.hardwaretypes.isEmpty() || this.hardwaretypes.containsAll(hardwaretypes);
  }
  /**
   * Return whether or not the given hardware type is compatible.
   *
   * @param hardwareType Hardware type to check compatibility of.
   * @return True if the hardware type is compatible, false if not.
   */
  public boolean compatibleWithHardwareType(String hardwareType) {
    return hardwaretypes.isEmpty() || hardwaretypes.contains(hardwareType);
  }

  /**
   * Get a builder for creating compatibilities.
   *
   * @return Builder for creating compatibilities.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating Compatibilities.
   */
  public static class Builder {
    private Set<String> hardwaretypes = ImmutableSet.of();
    private Set<String> imagetypes = ImmutableSet.of();
    private Set<String> services = ImmutableSet.of();

    public Builder setHardwaretypes(Set<String> hardwaretypes) {
      this.hardwaretypes = hardwaretypes == null ? ImmutableSet.<String>of() : hardwaretypes;
      return this;
    }

    public Builder setHardwaretypes(String... hardwaretypes) {
      this.hardwaretypes = ImmutableSet.copyOf(hardwaretypes);
      return this;
    }

    public Builder setImagetypes(Set<String> imagetypes) {
      this.imagetypes = imagetypes == null ? ImmutableSet.<String>of() : imagetypes;
      return this;
    }

    public Builder setImagetypes(String... imagetypes) {
      this.imagetypes = ImmutableSet.copyOf(imagetypes);
      return this;
    }

    public Builder setServices(Set<String> services) {
      this.services = services == null ? ImmutableSet.<String>of() : services;
      return this;
    }

    public Builder setServices(String... services) {
      this.services = ImmutableSet.copyOf(services);
      return this;
    }

    public Compatibilities build() {
      return new Compatibilities(hardwaretypes, imagetypes, services);
    }
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
