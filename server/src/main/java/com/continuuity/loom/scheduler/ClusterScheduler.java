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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.scheduler.dag.TaskDag;
import com.continuuity.loom.scheduler.dag.TaskNode;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskConfig;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.ClusterStore;
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
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Schedule clusters to be provisioned. Polls a queue that contains elements specifying what cluster action needs to
 * be performed on what cluster. The scheduler determines a plan for how to perform the cluster action and stores
 * all plan information into a cluster job, then writes to a queue to tell the {@link JobScheduler} to start
 * scheduling tasks to perform the cluster action.
 */
public class ClusterScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterScheduler.class);

  private final String id;
  private final ClusterStore clusterStore;
  private final TrackingQueue inputQueue;
  private final TrackingQueue jobQueue;
  private final TaskService taskService;

  private final Actions actions;

  @Inject
  public ClusterScheduler(@Named("scheduler.id") String id, ClusterStore clusterStore,
                          @Named("cluster.queue") TrackingQueue inputQueue,
                          @Named("internal.job.queue") TrackingQueue jobQueue,
                          TaskService taskService) {
    this.id = id;
    this.clusterStore = clusterStore;
    this.inputQueue = inputQueue;
    this.jobQueue = jobQueue;
    this.taskService = taskService;
    this.actions = new Actions();
  }

  @Override
  public void run() {
    try {
      while (true) {
        Element clusterElement = inputQueue.take(id);
        if (clusterElement == null) {
          return;
        }

        Cluster cluster = clusterStore.getCluster(clusterElement.getId());
        ClusterJob job = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
        try {
          ClusterAction clusterAction = ClusterAction.valueOf(clusterElement.getValue());
          LOG.debug("Got cluster {} with action {}", cluster.getName(), clusterAction);

          List<ProvisionerAction> actionOrder = actions.getActionOrder().get(clusterAction);
          if (actionOrder == null) {
            LOG.error("Cluster action {} does not have any provisioner actions defined", clusterAction);
            inputQueue.recordProgress(id, clusterElement.getId(), TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                      "No actions defined");
            continue;
          }

          Set<Node> clusterNodes = clusterStore.getClusterNodes(cluster.getId());
          if (clusterNodes == null || clusterNodes.isEmpty()) {
            LOG.error("Cluster {} has no nodes defined", cluster.getId());
            inputQueue.recordProgress(id, clusterElement.getId(), TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                      "No nodes defined");
            continue;
          }

          LOG.trace("Cluster {}", cluster);

          // First create service -> node map
          Multimap<String, Node> serviceNodeMap = ArrayListMultimap.create();
          Map<String, Service> serviceMap = Maps.newHashMap();
          Map<String, Node> nodeMap = Maps.newHashMap();
          for (Node node : clusterNodes) {
            for (Service service : node.getServices()) {
              serviceNodeMap.put(service.getName(), node);
              serviceMap.put(service.getName(), service);
            }
            nodeMap.put(node.getId(), node);
          }


          List<Set<TaskNode>> linearizedTasks =
            linearizeDependentTasks(clusterAction, clusterNodes, serviceNodeMap, serviceMap);

          // Create cluster tasks.
          List<Set<ClusterTask>> clusterTasks = createClusterTasks(linearizedTasks, cluster, job, serviceMap,
                                                                   clusterAction, nodeMap);

          // Make sure multiple actions on a same node do not happen simultaneously.
          clusterTasks = deDupNodePerStage(clusterTasks);

          for (Set<ClusterTask> stageTasks : clusterTasks) {
            job.addStage(Sets.newHashSet(Iterables.transform(stageTasks, CLUSTER_TASK_STRING_FUNCTION)));
          }

          job.setJobStatus(ClusterJob.Status.RUNNING);

          LOG.debug("Persisting cluster job {}", job.getJobId());
          LOG.trace("Cluster Job = {}", job);

          clusterStore.writeCluster(cluster);

          // Note: writing job status as RUNNING, will allow other operations on the job
          // (like cancel, etc.) to happen in parallel.
          clusterStore.writeClusterJob(job);

          jobQueue.add(new Element(job.getJobId()));

          inputQueue.recordProgress(id, clusterElement.getId(), TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                    "Scheduled");
        } catch (Throwable e) {
          LOG.error("Got exception while scheduling. Failing the job: ", e);

          // Clear staged tasks, and fail task
          job.clearTasks();
          taskService.failJobAndTerminateCluster(job, cluster, "Failed to schedule the action");

          inputQueue.recordProgress(id, clusterElement.getId(), TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY,
                                    "Exception during scheduling");
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception:", e);
    }
  }

  List<Set<TaskNode>> linearizeDependentTasks(ClusterAction clusterAction, Set<Node> clusterNodes,
                                              Multimap<String, Node> serviceNodeMap, Map<String, Service> serviceMap) {
    TaskDag taskDag = createTaskDag(clusterAction, actions, clusterNodes, serviceNodeMap, serviceMap);

    long start = System.currentTimeMillis();
    List<Set<TaskNode>> linearizedTasks = taskDag.linearize();
    long dur = System.currentTimeMillis() - start;
    LOG.debug("took {} ms to linearize action plan.", dur);

    return linearizedTasks;
  }

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
  findDirectActionDependencies(SetMultimap<String, String> minimizedDependencies,
                               Set<Actions.Dependency> actionDependencies, Map<String, Service> serviceMap) {

    SetMultimap<ImmutablePair<String, ProvisionerAction>,
      ImmutablePair<String, ProvisionerAction>> result = HashMultimap.create();

    if (actionDependencies != null) {
      for (Service service : serviceMap.values()) {
        for (Actions.Dependency actionDependency : actionDependencies) {
          result.putAll(ImmutablePair.of(service.getName(), actionDependency.getTo()),
                        getDirectActionDependencies(service, actionDependency, minimizedDependencies, serviceMap));
        }
      }
    }

    return result;
  }

  // search through action dependency graph, looking for direct action dependencies.  For example:
  // service A depends on service B, which depends on service C
  // service B contains no start action, while service A and service C both contain a start action.
  // therefore start A depends on start B makes no sense.  We need it to instead be start A depends on start C.
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

  // true if service1 depends on service2 in some way, either directly or indirectly
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

  static TaskDag createTaskDag(ClusterAction clusterAction, Actions actions, Set<Node> clusterNodes,
                               Multimap<String, Node> serviceNodeMap, Map<String, Service> serviceMap) {
    long start = System.currentTimeMillis();
    TaskDag taskDag = new TaskDag();
    List<ProvisionerAction> actionOrder = actions.getActionOrder().get(clusterAction);
    Set<Actions.Dependency> actionDependencies = actions.getActionDependency().get(clusterAction);
    SetMultimap<String, String> serviceDependencies = minimizeDependencies(serviceMap);

    SetMultimap<ImmutablePair<String, ProvisionerAction>, ImmutablePair<String, ProvisionerAction>>
      directActionDependencies = findDirectActionDependencies(serviceDependencies, actionDependencies, serviceMap);

    for (Node node : clusterNodes) {
      for (Service service : node.getServices()) {
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

        // Add service action dependencies
        if (actionDependencies != null) {
          for (Actions.Dependency actionDependency : actionDependencies) {
            for (ImmutablePair<String, ProvisionerAction> dependentServiceAction :
              directActionDependencies.get(ImmutablePair.of(service.getName(), actionDependency.getTo()))) {

              String dependentServiceName = dependentServiceAction.getFirst();
              ProvisionerAction dependentAction = dependentServiceAction.getSecond();
              // each node that the dependent service exist on must perform the from action before we perform the
              // to action for the service on this node.
              if (actionDependency.getIsReversed()) {
                for (Node fromNode : serviceNodeMap.get(dependentServiceName)) {
                  taskDag.addDependency(new TaskNode(node.getId(), actionDependency.getTo().name(), service.getName()),
                                        new TaskNode(fromNode.getId(), dependentAction.name(), dependentServiceName));
                }
              } else {
                for (Node fromNode : serviceNodeMap.get(dependentServiceName)) {
                  taskDag.addDependency(new TaskNode(fromNode.getId(), dependentAction.name(), dependentServiceName),
                                        new TaskNode(node.getId(), actionDependency.getTo().name(), service.getName()));
                }
              }
            }
          }
        }
      }
    }
    long dur = System.currentTimeMillis() - start;
    LOG.debug("took {} ms to create action plan.", dur);
    return taskDag;
  }

  List<Set<ClusterTask>> createClusterTasks(List<Set<TaskNode>> tasks, Cluster cluster, ClusterJob job,
                                            Map<String, Service> serviceMap, ClusterAction clusterAction,
                                            Map<String, Node> nodeMap)
    throws Exception {
    List<Set<ClusterTask>> runnableTasks = Lists.newArrayListWithExpectedSize(tasks.size());
    for (Set<TaskNode> taskNodes : tasks) {
      // Create tasks for a stage
      Set<ClusterTask> stageTasks = Sets.newHashSet();
      for (TaskNode taskNode : taskNodes) {
        // Get the config for the task
        Node node = nodeMap.get(taskNode.getHostId());
        Service service = serviceMap.get(taskNode.getService());
        JsonObject taskConfig =
          TaskConfig.getConfig(cluster, node, service, ProvisionerAction.valueOf(taskNode.getTaskName()));
        if (taskConfig == null) {
          LOG.debug("Not scheduling {} for job {} since config is null", taskNode, job.getJobId());
          continue;
        }

        TaskId taskId = clusterStore.getNewTaskId(JobId.fromString(job.getJobId()));
        ClusterTask task = new ClusterTask(ProvisionerAction.valueOf(taskNode.getTaskName()), taskId,
                                           taskNode.getHostId(), taskNode.getService(), clusterAction,
                                           taskConfig);
        clusterStore.writeClusterTask(task);
        stageTasks.add(task);
      }
      if (!stageTasks.isEmpty()) {
        runnableTasks.add(stageTasks);
      }
    }
    return runnableTasks;
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

  private static final Function<ClusterTask, String> CLUSTER_TASK_STRING_FUNCTION =
    new Function<ClusterTask, String>() {
      @Override
      public String apply(ClusterTask clusterTask) {
        return clusterTask.getTaskId();
      }
    };
}
