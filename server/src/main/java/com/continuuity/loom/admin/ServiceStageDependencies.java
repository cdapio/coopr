package com.continuuity.loom.admin;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Service stage dependencies are dependencies specific to a stage of service life. There are two types of dependencies,
 * requires and uses. If serviceA requires serviceB, placing serviceA on a cluster cannot be done without also placing
 * serviceB on the cluster. It will also enforce a specific ordering of tasks when a cluster operation plan is made.
 * For example, if serviceA requires serviceB in the install stage, the planner will make sure serviceB is installed
 * before serviceA is installed. If serviceA requires serviceB in the runtime stage, the planner will ensure that
 * serviceB is started before serviceA is initialized and started, and that serviceA is stopped before serviceB is
 * stopped. Uses is like required, except it does not enforce the presence of another service. It will just enforce
 * task ordering if the service used is present on the cluster. For example, if in the install stage, serviceC uses
 * serviceD, the planner will ensure serviceD is installed before serviceC is installed if both are on the cluster.
 * However, unlike requires, it is legal for serviceC to be placed on a cluster without serviceD. As such, there is no
 * reason for the same service to be in both the requires and uses fields, since uses is a subset of requires.
 */
public class ServiceStageDependencies {
  public static final ServiceStageDependencies EMPTY_SERVICE_STAGE_DEPENDENCIES =
    new ServiceStageDependencies(null, null);
  private final Set<String> requires;
  private final Set<String> uses;
  private Set<String> dependencies;

  /**
   * Create stage dependencies with the given requires and uses rules.
   *
   * @param requires Set of services this service requires.
   * @param uses Set of services this service uses.
   */
  public ServiceStageDependencies(Set<String> requires, Set<String> uses) {
    this.requires = requires == null ? ImmutableSet.<String>of() : requires;
    this.uses = uses == null ? ImmutableSet.<String>of() : uses;
    this.dependencies = Sets.union(this.requires, this.uses);
  }

  /**
   * Get the set of required services.
   *
   * @return Set of required services.
   */
  public Set<String> getRequires() {
    return requires;
  }

  /**
   * Get the set of used services.
   *
   * @return Set of used services.
   */
  public Set<String> getUses() {
    return uses;
  }

  /**
   * Get all dependencies. This is just the union of requires and uses.
   *
   * @return All dependencies, which is just the union of requires and uses.
   */
  public Set<String> getDependencies() {
    return dependencies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServiceStageDependencies)) {
      return false;
    }

    ServiceStageDependencies that = (ServiceStageDependencies) o;

    return Objects.equal(requires, that.requires) && Objects.equal(uses, that.uses);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requires, uses);
  }
}
