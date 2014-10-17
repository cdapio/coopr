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
package co.cask.coopr.spec.service;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import co.cask.coopr.spec.ProvisionerAction;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

/**
 * A service defines a set of services it is dependent on, as well as a mapping of
 * {@link co.cask.coopr.spec.ProvisionerAction} to {@link ServiceAction} that provisioners will need to execute
 * when performing cluster operations such as creation and deletion.
 */
public final class Service extends BaseEntity {
  private final ServiceDependencies dependencies;
  private final Map<ProvisionerAction, ServiceAction> provisionerActions;
  private final Set<Link> links;

  private Service(BaseEntity.Builder baseBuilder, ServiceDependencies dependencies,
                  Map<ProvisionerAction, ServiceAction> provisionerActions, Set<Link> links) {
    super(baseBuilder);
    this.dependencies = dependencies;
    this.provisionerActions = provisionerActions;
    this.links = links;
  }

  /**
   * Get the {@link ServiceDependencies} of this service.
   *
   * @return Dependencies of this service.
   */
  public ServiceDependencies getDependencies() {
    return dependencies;
  }

  /**
   * Get the mapping of {@link ProvisionerAction} to {@link ServiceAction} for this service.
   *
   * @return Map of action types to {@link ServiceAction} for this service.
   */
  public Map<ProvisionerAction, ServiceAction> getProvisionerActions() {
    return provisionerActions;
  }

  /**
   * Get an immutable set of links the service wants to expose.
   *
   * @return Immutable set of links the service wants to expose.
   */
  public Set<Link> getLinks() {
    return links;
  }

  /**
   * Get a builder for creating a service.
   *
   * @return Builder for creating a service.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating a service.
   */
  public static class Builder extends BaseEntity.Builder<Service> {
    private ServiceDependencies dependencies = ServiceDependencies.EMPTY_SERVICE_DEPENDENCIES;
    private Map<ProvisionerAction, ServiceAction> provisionerActions = ImmutableMap.of();
    private Set<Link> links = ImmutableSet.of();

    @Override
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setDependencies(ServiceDependencies dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    public Builder setProvisionerActions(Map<ProvisionerAction, ServiceAction> actions) {
      this.provisionerActions = ImmutableMap.copyOf(actions);
      return this;
    }

    public Builder setLinks(Set<Link> links) {
      this.links = links == null ? ImmutableSet.<Link>of() : ImmutableSet.copyOf(links);
      return this;
    }

    @Override
    public Service build() {
      return new Service(this, dependencies, provisionerActions, links);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Service)) {
      return false;
    }
    Service other = (Service) o;
    return super.equals(other) &&
      Objects.equal(dependencies, other.dependencies) &&
      Objects.equal(provisionerActions, other.provisionerActions) &&
      Objects.equal(links, other.links);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), dependencies, provisionerActions, links);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("dependencies", dependencies)
      .add("provisionerActions", provisionerActions)
      .add("links", links)
      .toString();
  }
}
