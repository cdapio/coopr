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
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.layout.ClusterRequest;
import com.continuuity.loom.layout.Solver;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Polls a queue which contains {@link ClusterRequest} and a cluster id, and
 * runs the solver to determine what the cluster layout should be for the specified cluster and cluster request.
 * If the solver fails to find a valid solution, statuses are updated accordingly. If the solver finds a valid solution,
 * the cluster is sent on to the {@link ClusterScheduler} by writing to a queue that the cluster scheduler reads.
 */
public class SolverScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(SolverScheduler.class);
  private static final Gson GSON = new Gson();

  private final String id;
  private final Solver solver;
  private final ClusterStore clusterStore;
  private final TrackingQueue solverQueue;
  private final TrackingQueue clusterQueue;
  private final ListeningExecutorService executorService;
  private final TaskService taskService;
  private final LoomStats loomStats;

  @Inject
  SolverScheduler(@Named("scheduler.id") String id, Solver solver, ClusterStore clusterStore,
                  @Named("solver.queue") TrackingQueue solverQueue,
                  @Named("cluster.queue") TrackingQueue clusterQueue,
                  @Named("solver.executor.service") ListeningExecutorService executorService,
                  TaskService taskService, LoomStats loomStats) {
    this.id = id;
    this.solver = solver;
    this.clusterStore = clusterStore;
    this.solverQueue = solverQueue;
    this.clusterQueue = clusterQueue;
    this.executorService = executorService;
    this.taskService = taskService;
    this.loomStats = loomStats;
  }

  @Override
  public void run() {
    try {
      while (true) {
        final Element solveElement = solverQueue.take(id);
        if (solveElement == null) {
          return;
        }

        final ListenableFuture<String> future = executorService.submit(new SolverRunner(solveElement));
        future.addListener(new Runnable() {
          @Override
          public void run() {
            try {
              solverQueue.recordProgress(id, solveElement.getId(),
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

    private SolverRunner(Element solveElement) {
      this.solveElement = solveElement;
    }

    @Override
    public String call() {
      try {
        ClusterRequest request = GSON.fromJson(solveElement.getValue(), ClusterRequest.class);

        String clusterId = solveElement.getId();
        LOG.debug("Got a request to solve cluster {}", clusterId);

        Cluster cluster = clusterStore.getCluster(clusterId);
        if (cluster == null) {
          LOG.error("Got a request to solve cluster {}, but the cluster does not exist.", clusterId);
          return "No cluster object";
        }

        // Get cluster job for solving.
        ClusterJob solverJob = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
        ClusterJob createJob = null;
        try {
          solverJob.setJobStatus(ClusterJob.Status.RUNNING);
          clusterStore.writeClusterJob(solverJob);

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
              String.format("Could not solve cluster id %s named %s with template %s and %d machines", clusterId,
                            request.getName(), request.getClusterTemplateName(), request.getNumMachines()));

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
          JobId clusterJobId = clusterStore.getNewJobId(clusterId);
          createJob = new ClusterJob(clusterJobId, ClusterAction.CLUSTER_CREATE);
          cluster.addJob(createJob.getJobId());
          clusterStore.writeClusterJob(createJob);

          clusterStore.writeCluster(cluster);

          // TODO: loom status update should happen in TaskService.
          loomStats.getClusterStats().incrementStat(ClusterAction.CLUSTER_CREATE);

          LOG.debug("added a cluster create request to the queue");
          clusterQueue.add(new Element(cluster.getId(), ClusterAction.CLUSTER_CREATE.name()));

          return "Solved";
        } catch (Throwable e) {
          LOG.error("Got exception when solving, cancelling job: ", e);

          ClusterJob toFailJob = createJob == null ? solverJob : createJob;
          taskService.failJobAndTerminateCluster(toFailJob, cluster, "Exception while solving layout");

          return "Exception while solving layout";
        }
      } catch (Throwable e) {
        LOG.error("Got exception: ", e);
        return "Exception while solving layout";
      }
    }
  }
}
