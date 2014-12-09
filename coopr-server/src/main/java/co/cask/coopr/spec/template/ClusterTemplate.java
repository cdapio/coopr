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

import co.cask.coopr.spec.BaseVersionedEntity;
import co.cask.coopr.spec.Link;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A cluster template defines different types of clusters that will be available for users to create.  Templates
 * define a set of services that are allowed to be placed on the cluster plus some service and layout constraints that
 * will be used to determine which services to place on which nodes, and what hardware and images to use.  A cluster
 * template also specifies the full set of configuration key values that are needed on the cluster.
 */
public final class ClusterTemplate extends BaseVersionedEntity {
  private final ClusterDefaults clusterDefaults;
  private final Constraints constraints;
  private final Compatibilities compatibilities;
  private final Administration administration;
  private final Set<Link> links;

  private ClusterTemplate(BaseVersionedEntity.Builder baseBuilder, ClusterDefaults clusterDefaults,
                          Compatibilities compatibilities, Constraints constraints, Administration administration,
                          Set<Link> links) {
    super(baseBuilder);
    Preconditions.checkArgument(clusterDefaults != null, "cluster defaults must be specified");
    this.clusterDefaults = clusterDefaults;
    this.constraints = constraints;
    this.compatibilities = compatibilities;
    this.administration = administration;
    this.links = links;
  }

  /**
   * Get the {@link Compatibilities} for the cluster.
   *
   * @return {@link Compatibilities} for the cluster.
   */
  public Compatibilities getCompatibilities() {
    return compatibilities;
  }

  /**
   * Get the constraints that specify how the cluster should be laid out.
   *
   * @return {@link Constraints} specifying how the cluster should be laid out.
   */
  public Constraints getConstraints() {
    return constraints;
  }

  /**
   * Get the {@link ClusterDefaults} that will be used unless the user replaces them at create time.
   *
   * @return {@link ClusterDefaults} for the template.
   */
  public ClusterDefaults getClusterDefaults() {
    return clusterDefaults;
  }

  /**
   * Get administration settings like lease time for managing the cluster.
   *
   * @return Administration settings for managing the cluster.
   */
  public Administration getAdministration() {
    return administration;
  }

  /**
   * Get immutable set of cluster links to services on the cluster.
   *
   * @return Immutable set of cluster links to services on the cluster
   */
  public Set<Link> getLinks() {
    return links;
  }

  /**
   * Get a builder for creating cluster templates.
   *
   * @return Builder for creating cluster templates.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating cluster templates.
   */
  public static class Builder extends BaseVersionedEntity.Builder<ClusterTemplate> {
    private ClusterDefaults clusterDefaults;
    private Constraints constraints = Constraints.EMPTY_CONSTRAINTS;
    private Compatibilities compatibilities = Compatibilities.EMPTY_COMPATIBILITIES;
    private Administration administration = Administration.EMPTY_ADMINISTRATION;
    private Set<Link> links = ImmutableSet.of();

    @Override
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public Builder setIcon(String icon) {
      this.icon = icon;
      return this;
    }

    @Override
    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setClusterDefaults(ClusterDefaults clusterDefaults) {
      this.clusterDefaults = clusterDefaults;
      return this;
    }

    public Builder setConstraints(Constraints constraints) {
      this.constraints = constraints == null ? Constraints.EMPTY_CONSTRAINTS : constraints;
      return this;
    }

    public Builder setCompatibilities(Compatibilities compatibilities) {
      this.compatibilities = compatibilities == null ? Compatibilities.EMPTY_COMPATIBILITIES : compatibilities;
      return this;
    }

    public Builder setAdministration(Administration administration) {
      this.administration = administration == null ? Administration.EMPTY_ADMINISTRATION : administration;
      return this;
    }

    public Builder setLinks(Set<Link> links) {
      this.links = links == null ? ImmutableSet.<Link>of() : ImmutableSet.copyOf(links);
      return this;
    }

    public ClusterTemplate build() {
      return new ClusterTemplate(this, clusterDefaults, compatibilities, constraints, administration, links);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ClusterTemplate)) {
      return false;
    }
    ClusterTemplate other = (ClusterTemplate) o;
    return super.equals(other) &&
      Objects.equal(compatibilities, other.compatibilities) &&
      Objects.equal(constraints, other.constraints) &&
      Objects.equal(administration, other.administration) &&
      Objects.equal(links, other.links);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), compatibilities, constraints, administration, links);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("clusterDefaults", clusterDefaults)
      .add("constraints", constraints)
      .add("compatibilities", compatibilities)
      .add("administration", administration)
      .add("links", links)
      .toString();
  }
}
