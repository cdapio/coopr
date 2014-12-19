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
<<<<<<< HEAD
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static org.apache.commons.lang.StringUtils.isNotBlank;
=======
import com.google.common.collect.ImmutableSet;
>>>>>>> upstream/develop

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
<<<<<<< HEAD

    public Merger merger() {
      return new Merger();
    }
=======
>>>>>>> upstream/develop

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
<<<<<<< HEAD

    public class Merger {
      private final Set<String> immutables = Sets.newHashSet();

      public Merger setInitialTemplate(ClusterTemplate initialTemplate) {
        parent = initialTemplate.parent;
        includes = initialTemplate.includes;

        return this;
      }

      public Merger merge(Collection<AbstractTemplate> templates) throws TemplateImmutabilityException {
        for (AbstractTemplate template : templates) {
          if (template instanceof ClusterTemplate) {
            copyFullProps(template);
          } else {
            Boolean immutable = ((PartialTemplate) template).isImmutable();
            copyMainProps(template, immutable);
          }
        }
        return this;
      }

      public ClusterTemplate.Builder builder(){
        validateFields();
        return Builder.this;
      }

      public Merger merge(AbstractTemplate template) throws Exception {
        return merge(Arrays.asList(template));
      }

      private void validateFields() {
        Preconditions.checkArgument(clusterDefaults.getServices() != null && !clusterDefaults.getServices().isEmpty(),
                                    "default services must be specified");
        Preconditions.checkArgument(clusterDefaults.getProvider() != null, "default provider must be specified");
        Preconditions.checkArgument(clusterDefaults.getDnsSuffix() == null ||
                                      StringUtils.isValidDNSSuffix(clusterDefaults.getDnsSuffix()),
                                    clusterDefaults.getDnsSuffix() + " is an invalid DNS suffix.");
      }

      //for partial includes
      private void copyMainProps(AbstractTemplate from, boolean isImmutable) throws TemplateImmutabilityException {
        //merge defaults
        mergeSet(clusterDefaults.getServices(), from.getClusterDefaults().getServices());

        //merge defaults config
        JsonObject fromDefaultsConfig = from.getClusterDefaults().getConfig();
        for (Map.Entry<String, JsonElement> fromConfigItem : fromDefaultsConfig.entrySet()) {
          JsonObject thisConfig = clusterDefaults.getConfig();
          String name = fromConfigItem.getKey();
          JsonElement value = fromConfigItem.getValue();
          if (immutables.contains(name) && thisConfig.has(name)) {
            throw new TemplateImmutabilityException(name + " can't be overridden due immutability. Overrides in " + from.getName());
          }
          thisConfig.add(name, value);
          if (isImmutable) {
            immutables.add(name);
          }
        }
        //merge compatibilities services
        mergeSet(compatibilities.getServices(), from.getCompatibilities().getServices());
      }

      //for parent extension
      private void copyFullProps(AbstractTemplate from) throws TemplateImmutabilityException {
        copyMainProps(from, false);

        //merge defaults options
        ClusterDefaults fromDefaults = from.getClusterDefaults();
        if(isNotBlank(fromDefaults.getDnsSuffix())) clusterDefaults.setDnsSuffix(fromDefaults.getDnsSuffix());
        if(isNotBlank(fromDefaults.getHardwaretype())) clusterDefaults.setHardwaretype(fromDefaults.getHardwaretype());
        if(isNotBlank(fromDefaults.getImagetype())) clusterDefaults.setImagetype(fromDefaults.getImagetype());
        if(isNotBlank(fromDefaults.getProvider())) clusterDefaults.setProvider(fromDefaults.getProvider());

        //merge constraints
        mergeMap(constraints.getServiceConstraints(), from.getConstraints().getServiceConstraints());
        mergeSet(constraints.getLayoutConstraint().getServicesThatMustCoexist(),
                 from.getConstraints().getLayoutConstraint().getServicesThatMustCoexist());
        mergeSet(constraints.getLayoutConstraint().getServicesThatMustNotCoexist(),
                 from.getConstraints().getLayoutConstraint().getServicesThatMustNotCoexist());
        if (!from.getConstraints().getSizeConstraint().equals(SizeConstraint.EMPTY)) {
          constraints.sizeConstraint = from.getConstraints().getSizeConstraint();
        }

        //merge compatibilities
        mergeSet(compatibilities.getHardwaretypes(), from.getCompatibilities().getHardwaretypes());
        mergeSet(compatibilities.getImagetypes(), from.getCompatibilities().getImagetypes());
        mergeSet(compatibilities.getServices(), from.getCompatibilities().getServices());

        //merge admin lease duration
        if (!from.getAdministration().getLeaseDuration().equals(LeaseDuration.FOREVER_LEASE_DURATION)) {
          administration.leaseDuration = from.getAdministration().getLeaseDuration();
        }

        //merge base entity properties
        if (isNotBlank(from.getName())) name = from.getName();
        if (isNotBlank(from.getLabel())) label = from.getLabel();
        if (isNotBlank(from.getIcon())) icon = from.getIcon();
        if (isNotBlank(from.getDescription())) description = from.getDescription();
      }

      @SuppressWarnings("unchecked")
      private void mergeSet(Set dest, Set from) {
        if (dest != null && from != null) {
          dest.addAll(from);
        }
      }

      @SuppressWarnings("unchecked")
      private void mergeMap(Map dest, Map from) {
        if (dest != null && from != null) {
          dest.putAll(from);
        }
      }
    }
=======
>>>>>>> upstream/develop
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
