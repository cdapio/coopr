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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Machine providers are instances of openstack, aws, rackspace, or joyent that can provision machines.  Providers are
 * referenced by {@link ImageType} and {@link HardwareType}.
 */
public final class Provider extends NamedEntity {
  private final String description;
  private final Provider.Type providerType;
  private final Map<String, Map<String, String>> provisionerData;

  public Provider(String name, String description, Provider.Type providerType,
                         Map<String, Map<String, String>> provisionerData) {
    super(name);
    Preconditions.checkArgument(providerType != null, "invalid provider type.");
    this.description = description;
    this.providerType = providerType;
    this.provisionerData = provisionerData == null ? ImmutableMap.<String, Map<String, String>>of() : provisionerData;
  }

  /**
   * Types of providers.
   * NOTE: enum name has to match knife plugin name.
   */
  public static enum Type {
    OPENSTACK,
    EC2,
    RACKSPACE,
    JOYENT
  }

  /**
   * Get the description of the provider.
   *
   * @return Description of provider.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the {@link Provider.Type} of the provider.
   *
   * @return {@link Provider.Type} of the provider.
   */
  public Type getProviderType() {
    return providerType;
  }

  /**
   * Get data needed by provisioners for this provider.  This will almost always include an 'auth' key that contains
   * a mapping of provider specific details required for provisioning, such as api endpoints, usernames, keys, etc.
   *
   * @return Data needed to provision machines from this provider.
   */
  public Map<String, Map<String, String>> getProvisionerData() {
    return provisionerData;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Provider)) {
      return false;
    }
    Provider other = (Provider) o;
    return Objects.equal(name, other.name) &&
      Objects.equal(description, other.description) &&
      Objects.equal(providerType, other.providerType) &&
      Objects.equal(provisionerData, other.provisionerData);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, providerType, provisionerData);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("description", description)
      .add("providerType", providerType)
      .add("provisionerData", provisionerData)
      .toString();
  }
}
