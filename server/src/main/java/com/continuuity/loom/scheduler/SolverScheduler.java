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

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.GroupElement;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.http.request.AddServicesRequest;
import com.continuuity.loom.http.request.ClusterCreateRequest;
import com.continuuity.loom.layout.Solver;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Polls a queue which contains {@link com.continuuity.loom.http.request.ClusterCreateRequest} and a cluster id, and
 * runs the solver to determine what the cluster layout should be for the specified cluster and cluster request.
 * If the solver fails to find a valid solution, statuses are updated accordingly. If the solver finds a valid solution,
 * the cluster is sent on to the {@link ClusterScheduler} by writing to a queue that the cluster scheduler reads.
 */
public class SolverScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SolverScheduler.class);

  private final String id;
  private final Solver solver;
  private final ClusterStore clusterStore;
  private final ListeningExecutorService executorService;
  private final TaskService taskService;
  private final LoomStats loomStats;
  private final IdService idService;
  private final Gson gson;
  private final QueueGroup solverQueues;
  private final QueueGroup clusterQueues;

  @Inject
  private SolverScheduler(@Named("scheduler.id") String id, Solver solver,
                          ClusterStoreService clusterStoreService,
                          @Named(Constants.Queue.SOLVER) QueueGroup solverQueues,
                          @Named(Constants.Queue.CLUSTER) QueueGroup clusterQueues,
                          @Named("solver.executor.service") ListeningExecutorService executorService,
                          TaskService taskService, LoomStats loomStats, IdService idService, Gson gson) {
    this.id = id;
    this.solver = solver;
    this.clusterStore = clusterStoreService.getSystemView();
    this.executorService = executorService;
    this.taskService = taskService;
    this.loomStats = loomStats;
    this.idService = idService;
    this.gson = gson;
    this.solverQueues = solverQueues;
    this.clusterQueues = clusterQueues;
  }

  @Override
  public void run() {
    try {
      while (true) {
        final GroupElement gElement = solverQueues.take(id);
        if (gElement == null) {
          return;
        }
        final Element solveElement = gElement.getElement();

        final ListenableFuture<String> future = executorService.submit(new SolverRunner(gElement));
        future.addListener(new Runnable() {
          @Override
          public void run() {
            try {
              solverQueues.recordProgress(id, gElement.getQueueName(), solveElement.getId(),
                                          TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, future.get());
            } catch (Exception e) {
              LOG.error("Unable to record progress for cluster {}", solveElement.getId());
            }
          }
        }, executorService);
      }
    } catch (Exception e) {
      LOG.error("Got exception:", e);
    }
  }

  private class SolverRunner implements Callable<String> {
    private final Element solveElement;
    private final String clusterId;
    private final String queueName;
    private ClusterJob solverJob;
    private ClusterJob plannerJob;

    private SolverRunner(GroupElement gElement) {
      this.solveElement = gElement.getElement();
      this.queueName = gElement.getQueueName();
      this.clusterId = solveElement.getId();
      this.solverJob = null;
      this.plannerJob = null;
    }

    @Override
    public String call() {
      try {
        LOG.debug("Got a request to solve cluster {}", clusterId);

        Cluster cluster = clusterStore.getCluster(clusterId);
        if (cluster == null) {
          LOG.error("Got a request to solve cluster {}, but the cluster does not exist.", clusterId);
          return "No cluster object";
        }

        // Get cluster job for solving.
        solverJob = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
        SolverRequest solverRequest = gson.fromJson(solveElement.getValue(), SolverRequest.class);
        try {
          solverJob.setJobStatus(ClusterJob.Status.RUNNING);
          clusterStore.writeClusterJob(solverJob);

          switch (solverRequest.getType()) {
            case CREATE_CLUSTER:
              return solveClusterCreate(cluster, gson.fromJson(solverRequest.getJsonRequest(),
                                                               ClusterCreateRequest.class));
            case ADD_SERVICES:
              return solveAddServices(cluster, gson.fromJson(solverRequest.getJsonRequest(), AddServicesRequest.class));
            default:
              return "unknown solver request type " + solverRequest.getType();
          }

        } catch (Throwable e) {
          LOG.error("Got exception when solving, cancelling job: ", e);

          ClusterJob toFailJob = plannerJob == null ? solverJob : plannerJob;
          switch(solverRequest.getType()) {
            case CREATE_CLUSTER:
              taskService.failJobAndTerminateCluster(toFailJob, cluster, "Exception while solving layout.");
              break;
            case ADD_SERVICES:
              // if the solver job is complete, we have changed the cluster state so cluster status is inconsistent.
              // if we haven't change completed solving, no state has changed so the cluster can go back to an
              // active state.
              // TODO: just rollback to previous state once we keep track of history.
              Cluster.Status clusterStatus = solverJob.getJobStatus() == ClusterJob.Status.COMPLETE ?
                Cluster.Status.INCONSISTENT : Cluster.Status.ACTIVE;
              taskService.failJobAndSetClusterStatus(toFailJob, cluster, clusterStatus,
                                                     "Exception while solving layout.");
              break;
          }

          return "Exception while solving layout";
        }
      } catch (Throwable e) {
        LOG.error("Got exception: ", e);
        return "Exception while solving layout";
      }
    }

    private String solveAddServices(Cluster cluster, AddServicesRequest request) throws Exception {

      Set<Node> clusterNodes = clusterStore.getClusterNodes(cluster.getId());
      Set<Node> changedNodes;
      String servicesStr = Joiner.on(',').join(request.getServices());
      try {
        changedNodes = solver.addServicesToCluster(cluster, clusterNodes, request.getServices());
      } catch (IllegalArgumentException e) {
        LOG.debug("Could not add services {} to cluster {}.", servicesStr, cluster.getId(), e);
        return "Unable to solve layout: " + e.getMessage();
      }

      if (changedNodes == null) {
        return "Unable to solve layout.";
      }

      // Solving succeeded, schedule planning.
      solverJob.setJobStatus(ClusterJob.Status.COMPLETE);
      clusterStore.writeClusterJob(solverJob);

      // TODO: loom status update should happen in TaskService.
      loomStats.getSuccessfulClusterStats().incrementStat(ClusterAction.SOLVE_LAYOUT);

      // TODO: stuff like this should be wrapped in a transaction
      Set<String> changedNodeIds = Sets.newHashSet();
      for (Node node : changedNodes) {
        clusterStore.writeNode(node);
        changedNodeIds.add(node.getId());
      }
      clusterStore.writeCluster(cluster);

      // Create new Job for creating cluster.
      JobId clusterJobId = idService.getNewJobId(cluster.getId());
      ClusterJob createJob = new ClusterJob(clusterJobId, ClusterAction.ADD_SERVICES,
                                            request.getServices(), changedNodeIds);
      cluster.setLatestJobId(createJob.getJobId());
      clusterStore.writeClusterJob(createJob);

      clusterStore.writeCluster(cluster);

      // TODO: loom status update should happen in TaskService.
      loomStats.getClusterStats().incrementStat(ClusterAction.ADD_SERVICES);

      LOG.debug("added a cluster add services request to the queue");
      clusterQueues.add(queueName, new Element(cluster.getId(), ClusterAction.ADD_SERVICES.name()));
      return "Solved";
    }

    private String solveClusterCreate(Cluster cluster, ClusterCreateRequest request) throws Exception {

      long start = System.nanoTime();
      Map<String, Node> clusterNodes =  null;
      String errorMessage = "Layout solving failed";
      try {
        clusterNodes = solver.solveClusterNodes(cluster, request);
      } catch (IllegalArgumentException e) {
        LOG.error("Layout solving failed due to impossible constraints.", e);
        errorMessage = errorMessage + ": " + e.getMessage();
      }

      long duration = (System.nanoTime() - start) / 1000000;
      LOG.debug("took {} ms to solve layout.", duration);

      // If nodes is empty or null, then solving failed. Fail solving job and return.
      if (clusterNodes == null || clusterNodes.isEmpty()) {
        LOG.error(
          String.format("Could not solve cluster id %s named %s with template %s and %d machines", cluster.getId(),
                        request.getName(), request.getClusterTemplate(), request.getNumMachines()));

        // Fail job updates loom stats
        taskService.failJobAndTerminateCluster(solverJob, cluster, errorMessage);
        return "Unable to solve layout";
      }

      // Solving succeeded, schedule cluster creation.
      solverJob.setJobStatus(ClusterJob.Status.COMPLETE);
      clusterStore.writeClusterJob(solverJob);

      // TODO: loom status update should happen in TaskService.
      loomStats.getSuccessfulClusterStats().incrementStat(ClusterAction.SOLVE_LAYOUT);

      for (Node node : clusterNodes.values()) {
        clusterStore.writeNode(node);
      }

      // Create new Job for creating cluster.
      JobId clusterJobId = idService.getNewJobId(cluster.getId());
      ClusterJob createJob = new ClusterJob(clusterJobId, ClusterAction.CLUSTER_CREATE);
      cluster.setLatestJobId(createJob.getJobId());
      clusterStore.writeClusterJob(createJob);

      clusterStore.writeCluster(cluster);

      // TODO: loom status update should happen in TaskService.
      loomStats.getClusterStats().incrementStat(ClusterAction.CLUSTER_CREATE);

      LOG.debug("added a cluster create request to the queue");
      clusterQueues.add(queueName, new Element(cluster.getId(), ClusterAction.CLUSTER_CREATE.name()));

      return "Solved";
    }
  }
}
