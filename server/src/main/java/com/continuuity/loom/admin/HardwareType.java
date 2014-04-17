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

import java.util.Map;

/**
 * A hardware type defines some hardware that will be used when constructing a cluster, as well as a mapping to
 * different providers and information needed by the provisioner to create the hardware for each provider, which should
 * contain the provider specific flavor and any other information needed to create the hardware.
 */
public final class HardwareType extends NamedEntity {
  private final String description;
  private final Map<String, Map<String, String>> providerMap;

  public HardwareType(String name, String description, Map<String, Map<String, String>> providerMap) {
    super(name);
    this.description = description;
    this.providerMap = providerMap;
  }

  /**
   * Get the description of this hardware type.
   *
   * @return Description of hardware type.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the provider map for this hardware type.  The provider map has provider names as its keys, and a map of
   * any key values needed by the provisioners to create the hardware given that provider.  For example, all cloud
   * providers require the 'flavor' key, which specifies which provider-specific flavor to use.
   *
   * @return Mapping of provider to data needed to provision hardware from the provider.
   */
  public Map<String, Map<String, String>> getProviderMap() {
    return providerMap;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof HardwareType)) {
      return false;
    }
    HardwareType other = (HardwareType) o;
    return Objects.equal(name, other.name) &&
      Objects.equal(description, other.description) &&
      providerMap.equals(other.providerMap);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, providerMap);
  }
}
