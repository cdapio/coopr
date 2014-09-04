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
package co.cask.coopr.spec.template;

import co.cask.coopr.spec.NamedIconEntity;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * A cluster template defines different types of clusters that will be available for users to create.  Templates
 * define a set of services that are allowed to be placed on the cluster plus some service and layout constraints that
 * will be used to determine which services to place on which nodes, and what hardware and images to use.  A cluster
 * template also specifies the full set of configuration key values that are needed on the cluster.
 */
public final class ClusterTemplate extends NamedIconEntity {
  private final String description;
  private final ClusterDefaults clusterDefaults;
  private final Constraints constraints;
  private final Compatibilities compatibilities;
  private final Administration administration;

  public ClusterTemplate(String name, String logolink, String description, ClusterDefaults clusterDefaults,
                         Compatibilities compatibilities, Constraints constraints, Administration administration) {
    super(name, logolink);
    Preconditions.checkArgument(clusterDefaults != null, "cluster defaults must be specified");
    this.clusterDefaults = clusterDefaults;
    this.description = description == null ? "" : description;
    this.constraints = constraints == null ? Constraints.EMPTY_CONSTRAINTS : constraints;
    this.compatibilities = compatibilities == null ? Compatibilities.EMPTY_COMPATIBILITIES : compatibilities;
    this.administration = administration == null ? Administration.EMPTY_ADMINISTRATION : administration;
  }

  public ClusterTemplate(String name, String description, ClusterDefaults clusterDefaults,
                         Compatibilities compatibilities, Constraints constraints, Administration administration) {
    this(name, null, description, clusterDefaults, compatibilities, constraints, administration);
  }

  /**
   * Get the description of the cluster template.
   *
   * @return Description of the cluster template.
   */
  public String getDescription() {
    return description;
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ClusterTemplate)) {
      return false;
    }
    ClusterTemplate other = (ClusterTemplate) o;
    return super.equals(other) &&
      Objects.equal(description, other.description) &&
      Objects.equal(compatibilities, other.compatibilities) &&
      Objects.equal(constraints, other.constraints) &&
      Objects.equal(administration, other.administration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), description, compatibilities, constraints, administration);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("description", description)
      .add("clusterDefaults", clusterDefaults)
      .add("constraints", constraints)
      .add("compatibilities", compatibilities)
      .add("administration", administration)
      .toString();
  }
}
