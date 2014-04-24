package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.dag.TaskDag;
import com.continuuity.loom.scheduler.dag.TaskNode;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Given a {@link ClusterJob} and a set of {@link Node}s belonging to the cluster, the planner will create a plan for
 * carrying out the job, based on the type of job being performed and dependencies between cluster services.
 */
public class JobPlanner {
  private static final Logger LOG = LoggerFactory.getLogger(JobPlanner.class);
  private static final Actions actions = Actions.getInstance();
  private final ClusterAction clusterAction;
  private final Set<String> nodesToPlan;
  private final Set<String> servicesToPlan;
  private final Multimap<String, Node> serviceNodeMap;
  private final Map<String, Service> serviceMap;
  private final Map<String, Node> nodeMap;
  private final ServiceDependencyResolver dependencyResolver;

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

    this.dependencyResolver = new ServiceDependencyResolver(actions, serviceMap);
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
   * Creates a DAG (directed acyclic graph) of tasks to execute in order to perform the cluster job.
   *
   * @return Task dag for the cluster operation.
   */
  TaskDag createTaskDag() {
    long start = System.currentTimeMillis();
    TaskDag taskDag = new TaskDag();
    List<ProvisionerAction> actionOrder = actions.getActionOrder().get(clusterAction);

    for (Node node : nodeMap.values()) {
      if (!shouldPlanNode(node)) {
        continue;
      }
      for (Service service : node.getServices()) {
        if (!shouldPlanService(service)) {
          continue;
        }

        addDependencies(taskDag, actionOrder, service, node);
      }
    }
    long dur = System.currentTimeMillis() - start;
    LOG.debug("took {} ms to create action plan.", dur);
    return taskDag;
  }

  private void addDependencies(TaskDag taskDag, List<ProvisionerAction> actionOrder, Service service, Node node) {
    // Add tasks for this service in the order they need to be run.
    String prevTask = null;
    String prevService = null;
    for (ProvisionerAction task : actionOrder) {
      // Hardware tasks remain the same for a node for all services.
      String effectiveService = task.isHardwareAction() ? "" : service.getName();
      // if this is a hardware task, or if the service has defined this provisioner action, add a node or edge
      // to the tag.  In other words, if the service does not have this provisioner action defined, no need to
      // add a node to the dag for it.
      if (task.isHardwareAction() || service.getProvisionerActions().containsKey(task)) {
        if (prevTask != null) {
          taskDag.addDependency(new TaskNode(node.getId(), prevTask, prevService),
                                new TaskNode(node.getId(), task.name(), effectiveService));
        } else {
          taskDag.addTaskNode(new TaskNode(node.getId(), task.name(), effectiveService));
        }
        prevTask = task.name();
        prevService = effectiveService;
      }

      for (ActionOnService dependentServiceAction :
        dependencyResolver.getDirectDependentActions(service.getName(), task)) {

        String dependentServiceName = dependentServiceAction.getService();
        ProvisionerAction dependentAction = dependentServiceAction.getAction();
        // each node that the dependent service exist on must perform the from action before we perform the
        // to action for the service on this node.
        for (Node fromNode : serviceNodeMap.get(dependentServiceName)) {
          taskDag.addDependency(new TaskNode(fromNode.getId(), dependentAction.name(), dependentServiceName),
                                new TaskNode(node.getId(), task.name(), service.getName()));
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
          if (dependencyResolver.runtimeDependsOn(service, serviceToRestart)) {
            additionalServicesToRestart.add(service);
          }
        }
      }
      expandedServices.addAll(additionalServicesToRestart);
    } while (!additionalServicesToRestart.isEmpty());
    return expandedServices;
  }
}
