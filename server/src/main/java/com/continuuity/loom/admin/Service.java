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
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

/**
 * A service defines a set of services it is dependent on, as well as a mapping of {@link ProvisionerAction} to
 * {@link ServiceAction} that provisioners will need to execute when performing cluster operations
 * such as creation and deletion.
 */
public final class Service extends NamedEntity {
  private final String description;
  private final Set<String> dependsOn;
  private final Map<ProvisionerAction, ServiceAction> provisionerActions;

  public Service(String name, String description, Set<String> dependsOn,
                 Map<ProvisionerAction, ServiceAction> provisionerActions) {
    super(name);
    Preconditions.checkArgument(provisionerActions != null, "service must contain provisioner actions");
    this.description = description;
    this.dependsOn = dependsOn == null ? ImmutableSet.<String>of() : dependsOn;
    this.provisionerActions = provisionerActions;
  }

  /**
   * Get the description of this service.
   *
   * @return Description of service.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the set of services this depends on.  Dependencies will be used to determine the order of certain actions
   * that happen on the cluster, as well as for cluster verification.
   *
   * @return Set of services this service depends on.
   */
  public Set<String> getDependsOn() {
    return dependsOn;
  }

  /**
   * Get the mapping of {@link ProvisionerAction} to {@link ServiceAction} for this service.
   *
   * @return Map of action types to {@link ServiceAction} for this service.
   */
  public Map<ProvisionerAction, ServiceAction> getProvisionerActions() {
    return provisionerActions;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Service) || o == null) {
      return false;
    }
    Service other = (Service) o;
    return Objects.equal(name, other.name) &&
      Objects.equal(description, other.description) &&
      Objects.equal(dependsOn, other.dependsOn) &&
      Objects.equal(provisionerActions, other.provisionerActions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, dependsOn, provisionerActions);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("description", description)
      .add("dependsOn", dependsOn)
      .add("provisionerActions", provisionerActions)
      .toString();
  }
}
