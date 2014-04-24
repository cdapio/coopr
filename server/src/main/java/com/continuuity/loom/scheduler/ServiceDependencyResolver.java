package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * This dependency resolver takes services placed on a cluster, some {@link ClusterAction} that needs to be performed,
 * and determines for a given {@link ProvisionerAction} to be performed on a {@link Service}, what other actions need
 * to be performed on what services before the given action can be performed.
 */
public class ServiceDependencyResolver {
  private final Map<String, Service> clusterServices;
  private final SetMultimap<String, String> providesIndex;
  private final SetMultimap<String, String> installServiceDependencies;
  private final SetMultimap<String, String> reversedInstallServiceDependencies;
  private final SetMultimap<String, String> runtimeServiceDependencies;
  private final SetMultimap<String, String> reversedRuntimeServiceDependencies;
  private final SetMultimap<ActionOnService, ActionOnService> clusterDependencies;

  public ServiceDependencyResolver(Actions actions, Map<String, Service> clusterServices) {
    this.clusterServices = ImmutableMap.copyOf(clusterServices);
    this.providesIndex = ImmutableSetMultimap.copyOf(getProvidesIndex());
    this.installServiceDependencies = minimizeDependencies(
      new Function<Service, Set<String>>() {
        @Override
        public Set<String> apply(Service input) {
          return replaceProvidedServices(input.getDependencies().getInstall().getDependencies());
        }
      });
    this.runtimeServiceDependencies = minimizeDependencies(
      new Function<Service, Set<String>>() {
        @Override
        public Set<String> apply(Service input) {
          return replaceProvidedServices(input.getDependencies().getRuntime().getDependencies());
        }
      });
    this.reversedInstallServiceDependencies = reverseDependencies(installServiceDependencies);
    this.reversedRuntimeServiceDependencies = reverseDependencies(runtimeServiceDependencies);
    this.clusterDependencies = HashMultimap.create();
    Set<Actions.Dependency> serviceActionDependencies = actions.getActionDependencies();

    for (Service service : clusterServices.values()) {

      for (Actions.Dependency actionDependency : serviceActionDependencies) {
        // for example:
        // there's an action dependency from start to initialize, and this service has an initialize action,
        // we need to add a dependency from all the start of our dependent services to initialize of this service.
        clusterDependencies.putAll(new ActionOnService(actionDependency.getTo(), service.getName()),
                                   getDirectActionDependencies(service, actionDependency));
      }
    }
  }

  /**
   * Given a service and action, return a set of {@link ActionOnService} describing what other actions on services must
   * be performed before the given service and action can be performed.
   *
   * @param service Service to check.
   * @param action Action to check.
   * @return Set of actions on services that must be performed before the input can be performed.
   */
  public Set<ActionOnService> getDirectDependentActions(String service, ProvisionerAction action) {
    return clusterDependencies.get(new ActionOnService(action, service));
  }

  /**
   * Returns whether or not service1 depends on service2 in some way for runtime actions, either directly or indirectly.
   *
   * @param service1 Service to check dependency for.
   * @param service2 Service to check dependency on.
   * @return True if service 1 depends on service 2 directly or indirectly, false if not.
   */
  public boolean runtimeDependsOn(String service1, String service2) {
    return doesDependOn(service1, service2, runtimeServiceDependencies);
  }

  SetMultimap<String, String> getInstallServiceDependencies() {
    return installServiceDependencies;
  }

  SetMultimap<String, String> getRuntimeServiceDependencies() {
    return runtimeServiceDependencies;
  }

  SetMultimap<ActionOnService, ActionOnService> getClusterDependencies() {
    return clusterDependencies;
  }

  private SetMultimap<String, String> reverseDependencies(SetMultimap<String, String> dependencies) {
    // if service A -> { service B, service C} is in the forward dependencies,
    // service B -> { service A } and service C -> { service A } would be in the reverse dependencies.
    SetMultimap<String, String> reversedDependencies = HashMultimap.create();
    for (Map.Entry<String, String> entry : dependencies.entries()) {
      reversedDependencies.put(entry.getValue(), entry.getKey());
    }
    return reversedDependencies;
  }

  /**
   * Builds a mapping from provided service -> { services that provide the service }, allowing lookups of what actual
   * services provide some given service. Will not contain entries for services that provide themselves. In other words,
   * the key will never be in the value.
   */
  private SetMultimap<String, String> getProvidesIndex() {
    SetMultimap<String, String> index = HashMultimap.create();
    for (Service service : clusterServices.values()) {
      String serviceName = service.getName();
      for (String provide : service.getDependencies().getProvides()) {
        if (!provide.equals(serviceName)) {
          index.put(provide, serviceName);
        }
      }
    }
    return index;
  }

  /**
   * Given services, this method prunes unnecessary dependencies, leaving only first order dependencies. For example,
   * if A depends on B and C, and if B depends on C, this will minimize A's dependencies so it just has A depends on B.
   * It will also flatten dependencies. If A depends on B, and B-1 provides B, and there is no B, then A depends on
   * B-1 is placed in the dependencies. If both B-1 and B-2 provide B, then A depends on both B-1 and B-2.
   */
  private SetMultimap<String, String> minimizeDependencies(Function<Service, Set<String>> getDependencies) {
    SetMultimap<String, String> minimized = HashMultimap.create();

    // for each service, get the set of services that it directly depends on. A dependency is a direct dependency if
    // it is at most one edge away in the dependency tree.
    for (Service service : clusterServices.values()) {
      Set<String> givenDependencies = getDependencies.apply(service);
      Set<String> indirectDependencies = Sets.newHashSet();

      // new dependencies are the dependent services that we have not seen yet
      Set<String> newDependencies = Sets.newHashSet(givenDependencies);
      while (!newDependencies.isEmpty()) {
        Set<String> newIndirectDependencies = Sets.newHashSet();
        // for each new dependent service
        for (String dependentServiceName : newDependencies) {
          // find the dependencies of the dependent service
          Service dependentService = clusterServices.get(dependentServiceName);
          Set<String> dependenciesOfDependency = getDependencies.apply(dependentService);
          // if any of these dependencies are not already in the set of indirect dependencies, we need to check them
          newIndirectDependencies.addAll(Sets.difference(dependenciesOfDependency, indirectDependencies));
        }
        // add any new indirect dependencies
        indirectDependencies.addAll(newIndirectDependencies);
        newDependencies = newIndirectDependencies;
      }
      // only care about direct dependencies.
      minimized.putAll(service.getName(), Sets.difference(givenDependencies, indirectDependencies));
    }
    return minimized;
  }

  /**
   * Given a set of service names, return an expanded set that replaces any provided service in the set by the actual
   * service that provides it. For example, is A-1 provides A, then with an input set of { A, B }, the output will be
   * { A-1, B }.
   */
  private Set<String> replaceProvidedServices(Set<String> serviceNames) {
    Set<String> provided = Sets.newHashSet();
    for (String serviceName : serviceNames) {
      if (providesIndex.containsKey(serviceName)) {
        // the index will not contain services that provide only themselves.
        provided.addAll(providesIndex.get(serviceName));
      } else {
        provided.add(serviceName);
      }
    }
    return provided;
  }

  /**
   * Search through the action dependency graph, looking for direct action dependencies.  For example:
   * service A depends on service B, which depends on service C
   * service B contains no start action, while service A and service C both contain a start action.
   * therefore start A depends on start B makes no sense.  We need it to instead be start A depends on start C.
   *
   * @param service Service whose direct action dependencies we are looking for.
   * @param actionDependency Action dependency we are searching with.
   * @return Direct action dependencies of the given service and action dependency.
   */
  private Set<ActionOnService> getDirectActionDependencies(Service service, Actions.Dependency actionDependency) {
    if (!service.getProvisionerActions().containsKey(actionDependency.getTo())) {
      return ImmutableSet.of();
    }
    ProvisionerAction fromAction = actionDependency.getFrom();
    SetMultimap<String, String> serviceDependencies;
    if (actionDependency.getIsReversed() && fromAction.isInstallTimeAction()) {
      serviceDependencies = reversedInstallServiceDependencies;
    } else if (actionDependency.getIsReversed() && fromAction.isRuntimeAction()) {
      serviceDependencies = reversedRuntimeServiceDependencies;
    } else if (fromAction.isInstallTimeAction()) {
      serviceDependencies = installServiceDependencies;
    } else if (fromAction.isRuntimeAction()) {
      serviceDependencies = runtimeServiceDependencies;
    } else {
      return ImmutableSet.of();
    }
    Queue<String> nextLevel = new LinkedList<String>();
    Set<ActionOnService> directDependencies = Sets.newHashSet();

    nextLevel.addAll(serviceDependencies.get(service.getName()));
    while (!nextLevel.isEmpty()) {
      // parent service is one that this service depends on, whether its directly or indirectly
      String parentServiceName = nextLevel.remove();
      Service parentService = clusterServices.get(parentServiceName);
      // check if we depend on this service and action
      if (parentService.getProvisionerActions().containsKey(fromAction)) {
        directDependencies.add(new ActionOnService(fromAction, parentServiceName));
      } else {
        // we only depend on the service, but the action is undefined.  Add the service's dependencies to check their
        // actions as well
        nextLevel.addAll(serviceDependencies.get(parentServiceName));
      }
    }

    // at this point, its possible there are dependencies of each other in the direct dependency set
    Set<ActionOnService> toRemove = Sets.newHashSet();
    for (ActionOnService directDependency1 : directDependencies) {
      // if a service depends on another service in the set, we can remove the service it depends on
      for (ActionOnService directDependency2 : directDependencies) {
        if (doesDependOn(directDependency1.getService(), directDependency2.getService(), serviceDependencies)) {
          toRemove.add(directDependency2);
        }
      }
    }
    directDependencies.removeAll(toRemove);

    return directDependencies;
  }

  /**
   * Returns whether or not service1 depends on service2 in some way, either directly or indirectly.
   *
   * @param service1 Service to check dependency for.
   * @param service2 Service to check dependency on.
   * @param dependencies Minimized service dependencies.
   * @return True if service 1 depends on service 2 directly or indirectly, false if not.
   */
  static boolean doesDependOn(String service1, String service2, Multimap<String, String> dependencies) {
    if (service1.equals(service2)) {
      return false;
    }
    Queue<String> servicesToSearch = new LinkedList<String>();
    servicesToSearch.addAll(dependencies.get(service1));
    while (!servicesToSearch.isEmpty()) {
      String dependentService = servicesToSearch.remove();
      if (dependentService.equals(service2)) {
        return true;
      }
      servicesToSearch.addAll(dependencies.get(dependentService));
    }
    return false;
  }

}
