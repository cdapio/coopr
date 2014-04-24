package com.continuuity.loom.admin;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Service dependencies define what a service provides, other services it conflicts with, install time dependencies,
 * and runtime dependencies. See {@link ServiceStageDependencies} for a description of what types of dependencies can
 * be defined at install time and runtime for services. Provides is a way for a service to say it satisfies the
 * dependency it provides, and is useful when there are multiple services that provide the same sort of functionality.
 * For example, an admin may create both a "mysql-db" and "oracle-db" that both provide the "sql-db" service. Some other
 * service "app-server" requires a database, but does not care what type of database it is. This allows the admin to
 * configure the "app-server" service to require just the "sql-db" service, which will ensure some service that provides
 * the "sql-db" service to be present on the cluster. A service always provides itself.
 * Conflicts defines other services that cannot be present on the
 * cluster with this service. Real service names must be given here instead of "provided" names. For example, suppose
 * service "myapp-1.0", "myapp-1.1", and "myapp-2.0" all provide "myapp". If another service "yourapp" is configured
 * to conflict with "myapp", it will still be possible for "yourapp" and "myapp-1.0" to exist on the same cluster.
 * Instead, the admin would have to configure "yourapp" to conflict with "myapp-1.0", "myapp-1.1", and "myapp-2.0".
 */
public class ServiceDependencies {
  public static final ServiceDependencies EMPTY_SERVICE_DEPENDENCIES = new ServiceDependencies(null, null, null, null);
  private final Set<String> provides;
  private final Set<String> conflicts;
  private final ServiceStageDependencies install;
  private final ServiceStageDependencies runtime;
  // for convenience, this stores a union of the install and runtime requirements. Field is not serialized.
  private final Set<String> requiredServices;

  public ServiceDependencies(Set<String> provides, Set<String> conflicts, ServiceStageDependencies install,
                             ServiceStageDependencies runtime) {
    this.provides = provides == null ? ImmutableSet.<String>of() : provides;
    this.conflicts = conflicts == null ? ImmutableSet.<String>of() : conflicts;
    this.install = install == null ? ServiceStageDependencies.EMPTY_SERVICE_STAGE_DEPENDENCIES : install;
    this.runtime = runtime == null ? ServiceStageDependencies.EMPTY_SERVICE_STAGE_DEPENDENCIES : runtime;
    this.requiredServices = Sets.union(this.install.getRequires(), this.runtime.getRequires());
  }

  /**
   * Get the set of services this service provides.
   *
   * @return Set of services this service provides.
   */
  public Set<String> getProvides() {
    return provides;
  }

  /**
   * Get the set of services this service conflicts with.
   *
   * @return Set of services this service conflicts with.
   */
  public Set<String> getConflicts() {
    return conflicts;
  }

  /**
   * Get the install time dependencies.
   *
   * @return Install time dependencies.
   */
  public ServiceStageDependencies getInstall() {
    return install;
  }

  /**
   * Get the run time dependencies.
   *
   * @return Run time dependencies.
   */
  public ServiceStageDependencies getRuntime() {
    return runtime;
  }

  /**
   * Get the set of required services, which are services that must also be on the same cluster.
   *
   * @return Set of required services.
   */
  public Set<String> getRequiredServices() {
    return requiredServices;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServiceDependencies)) {
      return false;
    }

    ServiceDependencies that = (ServiceDependencies) o;

    return Objects.equal(provides, that.provides) &&
      Objects.equal(conflicts, that.conflicts) &&
      Objects.equal(install, that.install) &&
      Objects.equal(runtime, that.runtime);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(provides, conflicts, install, runtime);
  }
}
