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

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A cluster template defines different types of clusters that will be available for users to create.  Templates
 * define a set of services that are allowed to be placed on the cluster plus some service and layout constraints that
 * will be used to determine which services to place on which nodes, and what hardware and images to use.  A cluster
 * template also specifies the full set of configuration key values that are needed on the cluster.
 */
public final class ClusterTemplate extends AbstractTemplate {

  Parent parent;
  Set<Include> includes;

  protected ClusterTemplate(BaseEntity.Builder baseBuilder, ClusterDefaults clusterDefaults,
                            Compatibilities compatibilities, Constraints constraints, Administration administration,
                            Set<Link> links, Parent parent, Set<Include> includes) {
    super(baseBuilder, clusterDefaults, compatibilities, constraints, administration, links);

    this.parent = parent;
    this.includes = includes;
  }

  /**
   * Get the parent template name.
   *
   * @return parent name for the template.
   */
  public Parent getParent() {
    return parent;
  }

  /**
   * Get the partial template names from this cluster template.
   *
   * @return included partial template names.
   */
  public Set<Include> getIncludes() {
    return includes;
  }

  /**
   * Get a builder for creating cluster templates.
   *
   * @return Builder for creating cluster templates.
   */
  public static ClusterTemplate.Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating cluster templates.
   */
  public static class Builder extends AbstractTemplate.Builder<ClusterTemplate, Builder> {

    private Parent parent;
    private Set<Include> includes = ImmutableSet.of();

    @Override
    public ClusterTemplate build() {
      return new ClusterTemplate(this, clusterDefaults, compatibilities, constraints, administration,
                                 links, parent, includes);
    }

    @Override
    protected ClusterTemplate.Builder getThis() {
      return this;
    }

    public ClusterTemplate.Builder setParent(Parent parent) {
      this.parent = parent;
      return this;
    }

    public ClusterTemplate.Builder setIncludes(Set<Include> includes) {
      this.includes = includes == null ? ImmutableSet.<Include>of() : ImmutableSet.copyOf(includes);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ClusterTemplate)) {
      return false;
    }
    ClusterTemplate other = (ClusterTemplate) o;
    return super.equals(other) &&
      Objects.equal(includes, other.includes) &&
      Objects.equal(parent, other.parent);
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
      .add("includes", includes)
      .add("parent", parent)
      .toString();
  }
}
