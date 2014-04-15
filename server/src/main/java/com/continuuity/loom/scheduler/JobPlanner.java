package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.dag.TaskDag;
import com.continuuity.loom.scheduler.dag.TaskNode;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.utils.ImmutablePair;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Given a {@link ClusterJob} and a set of {@link Node}s belonging to the cluster, the planner will create a plan for
 * carrying out the job, based on the type of job being performed and dependencies between cluster services.
 */
public class JobPlanner {
  private static final Logger LOG = LoggerFactory.getLogger(JobPlanner.class);
  private static final Actions actions = new Actions();
  private final ClusterAction clusterAction;
  private final Set<String> nodesToPlan;
  private final Set<String> servicesToPlan;
  private final Multimap<String, Node> serviceNodeMap;
  private final Map<String, Service> serviceMap;
  private final Map<String, Node> nodeMap;
  private final SetMultimap<String, String> serviceDependencies;

  public JobPlanner(ClusterJob job, Set<Node> clusterNodes) {
    this.clusterAction = job.getClusterAction();
    this.nodesToPlan = job.getPlannedNodes();
    this.serviceNodeMap = ArrayListMultimap.create();
    this.serviceMap = Maps.newHashMap();
    this.nodeMap = Maps.newHashMap();

    for (Node node : clusterNodes) {
      for (Service service : node.getServices()) {
        serviceNodeMap.put(service.getName(), node);
        serviceMap.put(service.getName(), service);
      }
      nodeMap.put(node.getId(), node);
    }
    this.serviceDependencies = minimizeDependencies(serviceMap);

    if (job.getPlannedServices() != null) {
      this.servicesToPlan = clusterAction == ClusterAction.RESTART_SERVICES ?
        ImmutableSet.copyOf(expandServicesToRestart(job.getPlannedServices())) : job.getPlannedServices();
    } else {
      this.servicesToPlan = null;
    }
  }

  public Map<String, Service> getServiceMap() {
    return serviceMap;
  }

  public Map<String, Node> getNodeMap() {
    return nodeMap;
  }

  /**
   * Create a plan of tasks to be executed in order to perform the cluster operation. Each item in the list represents
   * a stage of tasks that can be performed. All tasks in a stage may be run in parallel, but every task in a stage
   * must be successfully completed before moving on to the next stage.
   *
   * @return Plan of tasks to be executed in order to perform a cluster operation.
   */
  public List<Set<TaskNode>> linearizeDependentTasks() {
    TaskDag taskDag = createTaskDag();

    long start = System.currentTimeMillis();
    List<Set<TaskNode>> linearizedTasks = taskDag.linearize();
    long dur = System.currentTimeMillis() - start;
    LOG.debug("took {} ms to linearize action plan.", dur);

    return linearizedTasks;
  }

  /**
   * Given services, this method prunes unnecessary dependencies, leaving only first order dependencies. For example,
   * if A depends on B and C, and if B depends on C, this will minimize A's dependencies so it just has A depends on B.
   *
   * @param serviceMap Map of service name to {@link Service}.
   * @return Minimized dependencies.
   */
  static SetMultimap<String, String> minimizeDependencies(Map<String, Service> serviceMap) {
    SetMultimap<String, String> minimized = HashMultimap.create();

    // if A depends on B and C, and if B depends on C, minimize A's dependencies so it just has A depends on B.
    // basically, we only want first order dependencies.
    for (Service service : serviceMap.values()) {
      Set<String> directDependencies = service.getDependsOn();
      Set<String> indirectDependencies = Sets.newHashSet();
      // new dependencies are the dependent services that we have not seen yet
      Set<String> newDependencies = Sets.newHashSet(directDependencies);
      while (!newDependencies.isEmpty()) {
        Set<String> nextNewDependencies = Sets.newHashSet();
        // for each new dependency, see if any of their dependencies are new too
        for (String dependency : newDependencies) {
          Service serviceDependency = serviceMap.get(dependency);
          nextNewDependencies.addAll(Sets.difference(serviceDependency.getDependsOn(), indirectDependencies));
        }
        // add any new dependencies
        indirectDependencies.addAll(nextNewDependencies);
        newDependencies = nextNewDependencies;
      }
      // only care about direct dependencies.
      minimized.putAll(service.getName(), Sets.difference(directDependencies, indirectDependencies));
    }
    return minimized;
  }

  // finds direct action dependencies, given the minimized service dependencies.
  static SetMultimap<ImmutablePair<String, ProvisionerAction>, ImmutablePair<String, ProvisionerAction>>
  findDirectActionDependencies(SetMultimap<String, String> forwardDependencies,
                               Set<Actions.Dependency> actionDependencies, Map<String, Service> serviceMap) {

    SetMultimap<ImmutablePair<String, ProvisionerAction>,
      ImmutablePair<String, ProvisionerAction>> result = HashMultimap.create();

    // if service A -> { service B, service C} is in the forward dependencies,
    // service B -> { service A } and service C -> { service A } would be in the reverse dependencies.
    SetMultimap<String, String> reversedDependencies = HashMultimap.create();
    for (Map.Entry<String, String> entry : forwardDependencies.entries()) {
      reversedDependencies.put(entry.getValue(), entry.getKey());
    }

    if (actionDependencies != null) {
      for (Service service : serviceMap.values()) {
        for (Actions.Dependency actionDependency : actionDependencies) {
          SetMultimap<String, String> dependencies = actionDependency.getIsReversed() ?
            reversedDependencies : forwardDependencies;
          result.putAll(ImmutablePair.of(service.getName(), actionDependency.getTo()),
                        getDirectActionDependencies(service, actionDependency, dependencies, serviceMap));
        }
      }
    }

    return result;
  }

  /**
   * Search through the action dependency graph, looking for direct action dependencies.  For example:
   * service A depends on service B, which depends on service C
   * service B contains no start action, while service A and service C both contain a start action.
   * therefore start A depends on start B makes no sense.  We need it to instead be start A depends on start C.
   *
   * @param service Service whose direct action dependencies we are looking for.
   * @param actionDependency Action dependency we are searching with.
   * @param minimizedDependencies Minimized service dependencies.
   * @param serviceMap Map of service name to {@link Service}.
   * @return Direct action dependencies of the given service and action dependency.
   */
  static Set<ImmutablePair<String, ProvisionerAction>> getDirectActionDependencies(
    Service service, Actions.Dependency actionDependency, SetMultimap<String, String> minimizedDependencies,
    Map<String, Service> serviceMap) {

    if (!service.getProvisionerActions().containsKey(actionDependency.getTo())) {
      return ImmutableSet.of();
    }
    ProvisionerAction fromAction = actionDependency.getFrom();

    Queue<String> nextLevel = new LinkedList<String>();
    Set<ImmutablePair<String, ProvisionerAction>> directDependencies = Sets.newHashSet();

    nextLevel.addAll(minimizedDependencies.get(service.getName()));
    while (!nextLevel.isEmpty()) {
      // parent service is one that this service depends on, whether its directly or indirectly
      String parentServiceName = nextLevel.remove();
      Service parentService = serviceMap.get(parentServiceName);
      // check if we depend on this service and action
      if (parentService.getProvisionerActions().containsKey(fromAction)) {
        directDependencies.add(ImmutablePair.<String, ProvisionerAction>of(parentServiceName, fromAction));
      } else {
        // we only depend on the service, but the action is undefined.  Add the service's dependencies to check their
        // actions as well
        nextLevel.addAll(minimizedDependencies.get(parentServiceName));
      }
    }

    // at this point, its possible there are dependencies of each other in the direct dependency set
    Set<ImmutablePair<String, ProvisionerAction>> toRemove = Sets.newHashSet();
    for (ImmutablePair<String, ProvisionerAction> directDependency1 : directDependencies) {
      // if a service depends on another service in the set, we can remove the service it depends on
      for (ImmutablePair<String, ProvisionerAction> directDependency2 : directDependencies) {
        if (dependsOn(directDependency1.getFirst(), directDependency2.getFirst(), minimizedDependencies)) {
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
  static boolean dependsOn(String service1, String service2, Multimap<String, String> dependencies) {
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

  /**
   * Creates a DAG (directed acyclic graph) of tasks to execute in order to perform the cluster job.
   *
   * @return Task dag for the cluster operation.
   */
  TaskDag createTaskDag() {
    long start = System.currentTimeMillis();
    TaskDag taskDag = new TaskDag();
    List<ProvisionerAction> actionOrder = actions.getActionOrder().get(clusterAction);
    Set<Actions.Dependency> actionDependencies = actions.getActionDependency().get(clusterAction);

    SetMultimap<ImmutablePair<String, ProvisionerAction>, ImmutablePair<String, ProvisionerAction>>
      directActionDependencies = findDirectActionDependencies(serviceDependencies, actionDependencies, serviceMap);

    for (Node node : nodeMap.values()) {
      if (shouldPlanNode(node)) {
        for (Service service : node.getServices()) {
          if (shouldPlanService(service)) {
            addActionOrderDependencies(taskDag, actionOrder, service, node);

            if (actionDependencies != null) {
              addServiceActionDependencies(taskDag, actionDependencies, service, node, directActionDependencies);
            }
          }
        }
      }
    }
    long dur = System.currentTimeMillis() - start;
    LOG.debug("took {} ms to create action plan.", dur);
    return taskDag;
  }

  private void addActionOrderDependencies(TaskDag taskDag, List<ProvisionerAction> actionOrder,
                                          Service service, Node node) {

    // Add tasks for this service in the order they need to be run.
    String prevTask = null;
    String prevService = null;
    for (ProvisionerAction task : actionOrder) {
      // Hardware tasks remain the same for a node for all services.
      String effectiveService = actions.getHardwareActions().contains(task) ? "" : service.getName();
      // if this is a hardware task, or if the service has defined this provisioner action, add a node or edge
      // to the tag.  In other words, if the service does not have this provisioner action defined, no need to
      // add a node to the dag for it.
      if (actions.getHardwareActions().contains(task) || service.getProvisionerActions().containsKey(task)) {
        if (prevTask != null) {
          taskDag.addDependency(new TaskNode(node.getId(), prevTask, prevService),
                                new TaskNode(node.getId(), task.name(), effectiveService));
        } else {
          taskDag.addTaskNode(new TaskNode(node.getId(), task.name(), effectiveService));
        }
        prevTask = task.name();
        prevService = effectiveService;
      }
    }
  }

  private void addServiceActionDependencies(TaskDag taskDag, Set<Actions.Dependency> actionDependencies,
                                            Service service, Node node,
                                            SetMultimap<ImmutablePair<String, ProvisionerAction>,
                                              ImmutablePair<String, ProvisionerAction>> directActionDependencies) {

    for (Actions.Dependency actionDependency : actionDependencies) {
      for (ImmutablePair<String, ProvisionerAction> dependentServiceAction :
        directActionDependencies.get(ImmutablePair.of(service.getName(), actionDependency.getTo()))) {

        String dependentServiceName = dependentServiceAction.getFirst();
        ProvisionerAction dependentAction = dependentServiceAction.getSecond();
        // each node that the dependent service exist on must perform the from action before we perform the
        // to action for the service on this node.
        for (Node fromNode : serviceNodeMap.get(dependentServiceName)) {
          taskDag.addDependency(new TaskNode(fromNode.getId(), dependentAction.name(), dependentServiceName),
                                new TaskNode(node.getId(), actionDependency.getTo().name(), service.getName()));
        }
      }
    }
  }

  /**
   * Makes sure that not more than one task per host is present in a stage. If task t1 and t2 for the same host are
   * present in stage i, then it create a new stage i.5 and adds either t1 or t2 to i.5.
   * Task i.5 will execute after i, but before i+1, so as to satisfy all dependencies.
   * @param tasks Initial task list, where there may be multiple tasks on same host in any stage.
   * @return the deduped task list.
   */
  static List<Set<ClusterTask>> deDupNodePerStage(List<Set<ClusterTask>> tasks) {
    // Map contains nodeId and task.
    List<Map<String, ClusterTask>> deDupTasks = Lists.newArrayList();

    for (Set<ClusterTask> stage : tasks) {
      List<Map<String, ClusterTask>> newStages = Lists.newArrayList();
      for (ClusterTask task : stage) {
        if (!addToExistingStage(task, newStages)) {
          Map<String, ClusterTask> newStage = Maps.newHashMap();
          newStage.put(task.getNodeId(), task);
          newStages.add(newStage);
        }
      }
      deDupTasks.addAll(newStages);
    }

    return Lists.newArrayList(Iterables.transform(deDupTasks, MAP_CLUSTER_TASK_FUNCTION));
  }

  static boolean addToExistingStage(ClusterTask task, List<Map<String, ClusterTask>> stages) {
    for (Map<String, ClusterTask> newStage : stages) {
      if (!newStage.containsKey(task.getNodeId())) {
        newStage.put(task.getNodeId(), task);
        return true;
      }
    }
    return false;
  }

  private static final Function<Map<String, ClusterTask>, Set<ClusterTask>> MAP_CLUSTER_TASK_FUNCTION =
    new Function<Map<String, ClusterTask>, Set<ClusterTask>>() {
      @Override
      public Set<ClusterTask> apply(Map<String, ClusterTask> stringTaskNodeMap) {
        return Sets.newHashSet(stringTaskNodeMap.values());
      }
    };


  private boolean shouldPlanNode(Node node) {
    return nodesToPlan == null || nodesToPlan.contains(node.getId());
  }

  private boolean shouldPlanService(Service service) {
    return servicesToPlan == null || servicesToPlan.contains(service.getName());
  }

  // if svc A depends on svc B and we're asked to restart svc B, we actually need to restart both svc A and svc B.
  private Set<String> expandServicesToRestart(Set<String> servicesToRestart) {
    Set<String> expandedServices = Sets.newHashSet(servicesToRestart);
    Set<String> additionalServicesToRestart = Sets.newHashSet();
    do {
      additionalServicesToRestart.clear();
      for (String service : Sets.difference(serviceMap.keySet(), expandedServices)) {
        for (String serviceToRestart : expandedServices) {
          if (dependsOn(service, serviceToRestart, serviceDependencies)) {
            additionalServicesToRestart.add(service);
          }
        }
      }
      expandedServices.addAll(additionalServicesToRestart);
    } while (!additionalServicesToRestart.isEmpty());
    return expandedServices;
  }
}
