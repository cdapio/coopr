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
package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.http.AddServicesRequest;
import com.continuuity.loom.layout.ClusterLayout;
import com.continuuity.loom.layout.InvalidClusterException;
import com.continuuity.loom.layout.Solver;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.SolverRequest;
import com.continuuity.loom.store.ClusterStore;
import com.continuuity.loom.store.EntityStore;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Service for performing operations on clusters.
 */
public class ClusterService {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterService.class);
  private static final Gson GSON = new JsonSerde().getGson();

  private final ClusterStore clusterStore;
  private final EntityStore entityStore;
  private final TrackingQueue clusterQueue;
  private final TrackingQueue solverQueue;
  private final ZKClient zkClient;
  private final LoomStats loomStats;
  private final Solver solver;

  @Inject
  public ClusterService(ClusterStore clusterStore,
                        EntityStore entityStore,
                        @Named(Constants.Queue.CLUSTER) TrackingQueue clusterQueue,
                        @Named(Constants.Queue.SOLVER) TrackingQueue solverQueue,
                        ZKClient zkClient,
                        LoomStats loomStats,
                        Solver solver) {
    this.clusterStore = clusterStore;
    this.entityStore = entityStore;
    this.clusterQueue = clusterQueue;
    this.solverQueue = solverQueue;
    this.zkClient = ZKClients.namespace(zkClient, Constants.LOCK_NAMESPACE);
    this.loomStats = loomStats;
    this.solver = solver;
  }

  /**
   * Request deletion of a given cluster that the user has permission to delete.
   *
   * @param clusterId Id of the cluster to delete.
   * @param userId Id of the owner of the cluster, or the admin user id.
   * @throws Exception
   */
  public void requestClusterDelete(String clusterId, String userId) throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);
      JobId deleteJobId = clusterStore.getNewJobId(clusterId);
      ClusterJob deleteJob = new ClusterJob(deleteJobId, ClusterAction.CLUSTER_DELETE);
      deleteJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(deleteJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);

      LOG.debug("Writing cluster {} to store with delete job {}", clusterId, deleteJobId);
      clusterStore.writeCluster(cluster);
      clusterStore.writeClusterJob(deleteJob);

      loomStats.getClusterStats().incrementStat(ClusterAction.CLUSTER_DELETE);
      clusterQueue.add(new Element(clusterId, ClusterAction.CLUSTER_DELETE.name()));
    } finally {
      lock.release();
    }
  }

  /**
   * Put in a request to reconfigure services on an active cluster.
   * Throws a {@link MissingClusterException} if no cluster owned by the user is found and an
   * {@link IllegalStateException} if the cluster is not in a state where the action can be performed.
   *
   * @param clusterId Id of cluster to reconfigure.
   * @param userId User that is trying to reconfigure the cluster.
   * @param restartServices Whether or not services should be restarted as part of the reconfigure.
   * @param config New value of the config to use.
   * @throws Exception
   */
  public void requestClusterReconfigure(String clusterId, String userId, boolean restartServices, JsonObject config)
    throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user " + userId + " does not exist");
      }
      if (!Cluster.Status.CONFIGURABLE_STATES.contains(cluster.getStatus())) {
        throw new IllegalStateException("cluster " + clusterId + " is not in a configurable state");
      }
      JobId configureJobId = clusterStore.getNewJobId(clusterId);

      ClusterAction action =
        restartServices ? ClusterAction.CLUSTER_CONFIGURE_WITH_RESTART : ClusterAction.CLUSTER_CONFIGURE;
      ClusterJob configureJob = new ClusterJob(configureJobId, action);
      configureJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(configureJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);

      LOG.debug("Writing cluster {} to store with configure job {}", clusterId, configureJobId);
      cluster.setConfig(config);
      clusterStore.writeCluster(cluster);
      clusterStore.writeClusterJob(configureJob);

      loomStats.getClusterStats().incrementStat(action);
      clusterQueue.add(new Element(clusterId, action.name()));
    } finally {
      lock.release();
    }
  }

  /**
   * Put in a request to perform an action on a specific service on an active cluster, or on all cluster services.
   * Throws a {@link MissingClusterException} if no cluster owned by the user is found and an
   * {@link IllegalStateException} if the cluster is not in a state where the action can be performed.
   *
   * @param clusterId Id of cluster perform the service action on.
   * @param userId User that is trying to perform an action on the cluster service.
   * @param action Action to perform on the service.
   * @param service Service to perform the action on. Null means perform the action on all services.
   * @throws Exception
   */
  public void requestServiceRuntimeAction(String clusterId, String userId, ClusterAction action, String service)
    throws Exception {
    Preconditions.checkArgument(ClusterAction.SERVICE_RUNTIME_ACTIONS.contains(action),
                                action + " is not a service runtime action.");
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);
      if (cluster == null || (service != null && !cluster.getServices().contains(service))) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user " + userId + " does not exist");
      }
      if (!Cluster.Status.SERVICE_ACTIONABLE_STATES.contains(cluster.getStatus())) {
        throw new IllegalStateException(
          "cluster " + clusterId + " is not in a state where service actions can be performed");
      }
      JobId jobId = clusterStore.getNewJobId(clusterId);

      ClusterJob job = new ClusterJob(jobId, action, service == null ? null : ImmutableSet.of(service), null);
      job.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(job.getJobId());
      cluster.setStatus(Cluster.Status.PENDING);
      clusterStore.writeCluster(cluster);
      clusterStore.writeClusterJob(job);

      loomStats.getClusterStats().incrementStat(action);
      clusterQueue.add(new Element(clusterId, action.name()));
    } finally {
      lock.release();
    }
  }

  /**
   * Put in a request to add services to an active cluster.
   * Throws a {@link MissingClusterException} if no cluster owned by the user is found and an
   * {@link IllegalStateException} if the cluster is not in a state where the action can be performed.
   *
   * @param clusterId Id of cluster to add services to.
   * @param userId User that is trying to add services to the cluster.
   * @param addRequest Request to add services.
   * @throws Exception
   */
  public void requestAddServices(String clusterId, String userId, AddServicesRequest addRequest)
    throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user " + userId + " does not exist");
      }
      if (cluster.getStatus() != Cluster.Status.ACTIVE) {
        throw new IllegalStateException(
          "cluster " + clusterId + " is not in a state where services can be added to it.");
      }
      JobId jobId = clusterStore.getNewJobId(clusterId);
      solver.validateServicesToAdd(cluster, addRequest.getServices());

      ClusterAction action = ClusterAction.ADD_SERVICES;
      ClusterJob job = new ClusterJob(jobId, action, addRequest.getServices(), null);
      job.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(job.getJobId());
      cluster.setStatus(Cluster.Status.PENDING);
      clusterStore.writeCluster(cluster);
      clusterStore.writeClusterJob(job);

      loomStats.getClusterStats().incrementStat(action);
      SolverRequest solverRequest = new SolverRequest(SolverRequest.Type.ADD_SERVICES, GSON.toJson(addRequest));
      solverQueue.add(new Element(clusterId, GSON.toJson(solverRequest)));
    } finally {
      lock.release();
    }
  }

  /**
   * Sync the template of the cluster to the current version of the template.
   * Throws a {@link MissingClusterException} if no cluster owned by the user is found, an
   * {@link IllegalStateException} if the cluster is not in a state where it can by synced, or a
   * {@link MissingEntityException} if the template no longer exists.
   *
   * @param clusterId Id of the cluster to sync.
   * @param userId Id of the user trying to sync the cluster.
   */
  public void syncClusterToCurrentTemplate(String clusterId, String userId) throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user " + userId + " does not exist");
      }
      if (cluster.getStatus() != Cluster.Status.ACTIVE) {
        throw new IllegalStateException(
          "cluster " + clusterId + " is not in a state where services can be added to it.");
      }

      String templateName = cluster.getClusterTemplate().getName();
      ClusterTemplate currentTemplate = entityStore.getClusterTemplate(templateName);
      if (currentTemplate == null) {
        LOG.info("tried to sync template {} for cluster {}, but the template no longer exists",
                 templateName, clusterId);
        throw new MissingEntityException("template " + templateName + " no longer exists");
      }
      Set<Node> clusterNodes = clusterStore.getClusterNodes(clusterId, userId);
      if (clusterNodes.isEmpty()) {
        throw new MissingEntityException("could not find cluster nodes");
      }
      ClusterLayout clusterLayout = ClusterLayout.fromNodes(clusterNodes, currentTemplate.getConstraints());
      // don't allow updating the template if it would result in an invalid cluster.
      if (!clusterLayout.isCompatibleWithTemplate(currentTemplate)) {
        throw new InvalidClusterException("updating the template would result in an invalid cluster");
      }
      // all good, update the template and save it
      cluster.setClusterTemplate(currentTemplate);
      clusterStore.writeCluster(cluster);
    } finally {
      lock.release();
    }
  }

  /**
   * Changes expire time of a cluster.
   * The new expire time has to be greater than the cluster create time, and the increment requested should be less
   * than the step size, and new expire time should not be greater than the max duration defined in the template.
   *
   * @param clusterId cluster Id.
   * @param userId user requesting the change.
   * @param expireTime new expire time.
   * @throws Exception
   */
  public void changeExpireTime(String clusterId, String userId, long expireTime) throws Exception {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = getUserCluster(clusterId, userId);

      if (cluster.getStatus() == Cluster.Status.TERMINATED) {
        throw new IllegalArgumentException("Cannot change expire time of terminated cluster");
      }

      if (cluster.getStatus() == Cluster.Status.PENDING) {
        throw new IllegalArgumentException("Cannot change expire time of cluster at this time");
      }

      if (expireTime < cluster.getCreateTime()) {
        throw new IllegalArgumentException("New expire time cannot be lesser than create time");
      }

      long step = cluster.getClusterTemplate().getAdministration().getLeaseDuration().getStep();
      step = step == 0 ? Long.MAX_VALUE : step;

      long clusterMaxDuration = cluster.getClusterTemplate().getAdministration().getLeaseDuration().getMax();
      if (clusterMaxDuration != 0 && expireTime - cluster.getExpireTime() > step) {
        throw new IllegalArgumentException("New expire time cannot be greater than allowed increment");
      }

      if (clusterMaxDuration != 0 && cluster.getCreateTime() + clusterMaxDuration < expireTime) {
        throw new IllegalArgumentException("Cluster cannot be prolonged beyond its max duration");
      }

      cluster.setExpireTime(expireTime);
      LOG.debug("Prolonging lease of cluster {} by {} to {}", clusterId, expireTime, cluster.getExpireTime());
      clusterStore.writeCluster(cluster);
    } finally {
      lock.release();
    }
  }

  /**
   * Get a specific cluster that the user has permission to get, or null if no such cluster exists.
   *
   * @param clusterId Id of the cluster to get.
   * @param userId Id of the owner of the cluster, or the admin user id.
   * @return Cluster with the id given and owned by the user given, or null if no such cluster exists.
   * @throws Exception
   */
  public Cluster getUserCluster(String clusterId, String userId) throws Exception {
    Cluster cluster;
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      cluster = clusterStore.getCluster(clusterId);
    } else {
      cluster = clusterStore.getCluster(clusterId, userId);
    }
    return cluster;
  }

  /**
   * Get the jobs associated with the given cluster that the user has permission to get.
   *
   * @param clusterId Id of the cluster associated with the jobs to get.
   * @param userId Id of the owner of the cluster, or the admin user id.
   * @return List of cluster jobs performed or being performed on the cluster. Will be empty if none exist.
   * @throws Exception
   */
  public List<ClusterJob> getClusterJobs(String clusterId, String userId) throws Exception {
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      return clusterStore.getClusterJobs(clusterId, -1);
    } else {
      return clusterStore.getClusterJobs(clusterId, userId, -1);
    }
  }

  /**
   * Get all the clusters that the user has permission to get.
   *
   * @param userId Id of the user.
   * @return List of all clusters owned by the user, or list of all clusters if the user is the admin.
   * @throws Exception
   */
  public List<Cluster> getAllUserClusters(String userId) throws Exception {
    List<Cluster> clusters;
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      clusters = clusterStore.getAllClusters();
    } else {
      clusters = clusterStore.getAllClusters(userId);
    }
    return clusters;
  }

  /**
   * Get all the nodes in a cluster that the user has permission to get.
   *
   * @param clusterId Id of the cluster.
   * @param userId Id of the user.
   * @return Set of all nodes in the cluster owned by the user or the user is the admin.
   * @throws Exception
   */
  public Set<Node> getClusterNodes(String clusterId, String userId) throws Exception {
    if (userId.equals(Constants.ADMIN_USER) || userId.equals(Constants.SYSTEM_USER)) {
      return clusterStore.getClusterNodes(clusterId);
    } else {
      return clusterStore.getClusterNodes(clusterId, userId);
    }
  }
}
