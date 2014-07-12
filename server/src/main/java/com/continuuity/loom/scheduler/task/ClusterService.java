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

import com.continuuity.loom.account.Account;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.http.request.AddServicesRequest;
import com.continuuity.loom.layout.ClusterCreateRequest;
import com.continuuity.loom.layout.ClusterLayout;
import com.continuuity.loom.layout.InvalidClusterException;
import com.continuuity.loom.layout.Solver;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.SolverRequest;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.cluster.ClusterStoreView;
import com.continuuity.loom.store.entity.EntityStoreService;
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

import java.io.IOException;
import java.util.Set;

/**
 * Service for performing operations on clusters.
 */
public class ClusterService {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterService.class);

  private final ClusterStoreService clusterStoreService;
  private final ClusterStore clusterStore;
  private final EntityStoreService entityStoreService;
  private final ZKClient zkClient;
  private final LoomStats loomStats;
  private final Solver solver;
  private final IdService idService;
  private final Gson gson;
  private final QueueGroup clusterQueues;
  private final QueueGroup solverQueues;
  private final QueueGroup jobQueues;

  @Inject
  public ClusterService(ClusterStoreService clusterStoreService,
                        EntityStoreService entityStoreService,
                        @Named(Constants.Queue.CLUSTER) QueueGroup clusterQueues,
                        @Named(Constants.Queue.SOLVER) QueueGroup solverQueues,
                        @Named(Constants.Queue.JOB) QueueGroup jobQueues,
                        ZKClient zkClient,
                        LoomStats loomStats,
                        Solver solver,
                        IdService idService,
                        JsonSerde jsonSerde) {
    this.clusterStoreService = clusterStoreService;
    this.clusterStore = clusterStoreService.getSystemView();
    this.entityStoreService = entityStoreService;
    this.zkClient = ZKClients.namespace(zkClient, Constants.LOCK_NAMESPACE);
    this.loomStats = loomStats;
    this.solver = solver;
    this.idService = idService;
    this.gson = jsonSerde.getGson();
    this.clusterQueues = clusterQueues;
    this.solverQueues = solverQueues;
    this.jobQueues = jobQueues;
  }

  /**
   * Submit a request to create a cluster, creating a placeholder cluster object and adding a task to solve for a
   * layout to the solver queue.
   *
   * @param clusterCreateRequest Request to create a cluster.
   * @param account Account of the user making the request.
   * @return Id of the cluster that will be created.
   * @throws IOException if there was some error writing to stores.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public String requestClusterCreate(ClusterCreateRequest clusterCreateRequest, Account account)
    throws IOException, IllegalAccessException {
    String name = clusterCreateRequest.getName();
    int numMachines = clusterCreateRequest.getNumMachines();
    String templateName = clusterCreateRequest.getClusterTemplate();
    LOG.debug(String.format("Received a request to create cluster %s with %d machines from template %s", name,
                            numMachines, templateName));
    String clusterId = idService.getNewClusterId();
    Cluster cluster = new Cluster(clusterId, account, name, System.currentTimeMillis(),
                                  clusterCreateRequest.getDescription(), null, null,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(),
                                  clusterCreateRequest.getConfig());
    JobId clusterJobId = idService.getNewJobId(clusterId);
    ClusterJob clusterJob = new ClusterJob(clusterJobId, ClusterAction.SOLVE_LAYOUT);
    cluster.setLatestJobId(clusterJob.getJobId());

    LOG.trace("Writing cluster {} to store", cluster);
    clusterStoreService.getView(account).writeCluster(cluster);
    clusterStore.writeClusterJob(clusterJob);

    LOG.debug("adding create cluster element to solverQueue");
    SolverRequest solverRequest = new SolverRequest(SolverRequest.Type.CREATE_CLUSTER,
                                                    gson.toJson(clusterCreateRequest));
    solverQueues.add(account.getTenantId(), new Element(cluster.getId(), gson.toJson(solverRequest)));

    loomStats.getClusterStats().incrementStat(ClusterAction.SOLVE_LAYOUT);
    return cluster.getId();
  }

  /**
   * Request deletion of a given cluster that the user has permission to delete.
   *
   * @param clusterId Id of the cluster to delete.
   * @param account Account of the user making the request.
   * @throws IOException if there was some error writing to stores.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public void requestClusterDelete(String clusterId, Account account)
    throws IOException, IllegalAccessException {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      ClusterStoreView view = clusterStoreService.getView(account);
      Cluster cluster = view.getCluster(clusterId);
      JobId deleteJobId = idService.getNewJobId(clusterId);
      ClusterJob deleteJob = new ClusterJob(deleteJobId, ClusterAction.CLUSTER_DELETE);
      deleteJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(deleteJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);

      LOG.debug("Writing cluster {} to store with delete job {}", clusterId, deleteJobId);
      view.writeCluster(cluster);
      clusterStore.writeClusterJob(deleteJob);

      loomStats.getClusterStats().incrementStat(ClusterAction.CLUSTER_DELETE);
      clusterQueues.add(account.getTenantId(), new Element(clusterId, ClusterAction.CLUSTER_DELETE.name()));
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
   * @param account Account of the user that is trying to reconfigure the cluster.
   * @param restartServices Whether or not services should be restarted as part of the reconfigure.
   * @param config New value of the config to use.
   * @throws IOException if there was so error writing to stores.
   * @throws MissingClusterException if there is no cluster for the given id.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public void requestClusterReconfigure(String clusterId, Account account, boolean restartServices, JsonObject config)
    throws IOException, MissingClusterException, IllegalAccessException {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      ClusterStoreView view = clusterStoreService.getView(account);
      Cluster cluster = view.getCluster(clusterId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user "
                                            + account.getUserId() + " does not exist");
      }
      if (!Cluster.Status.CONFIGURABLE_STATES.contains(cluster.getStatus())) {
        throw new IllegalStateException("cluster " + clusterId + " is not in a configurable state");
      }
      JobId configureJobId = idService.getNewJobId(clusterId);

      ClusterAction action =
        restartServices ? ClusterAction.CLUSTER_CONFIGURE_WITH_RESTART : ClusterAction.CLUSTER_CONFIGURE;
      ClusterJob configureJob = new ClusterJob(configureJobId, action);
      configureJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(configureJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);

      LOG.debug("Writing cluster {} to store with configure job {}", clusterId, configureJobId);
      cluster.setConfig(config);
      view.writeCluster(cluster);
      clusterStore.writeClusterJob(configureJob);

      loomStats.getClusterStats().incrementStat(action);
      clusterQueues.add(account.getTenantId(), new Element(clusterId, action.name()));
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
   * @param account Account of the user that is trying to perform an action on the cluster service.
   * @param action Action to perform on the service.
   * @param service Service to perform the action on. Null means perform the action on all services.
   * @throws IOException if there was so error writing to stores.
   * @throws MissingClusterException if there is no cluster for the given id.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public void requestServiceRuntimeAction(String clusterId, Account account, ClusterAction action, String service)
    throws IOException, MissingClusterException, IllegalAccessException {
    Preconditions.checkArgument(ClusterAction.SERVICE_RUNTIME_ACTIONS.contains(action),
                                action + " is not a service runtime action.");
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      ClusterStoreView view = clusterStoreService.getView(account);
      Cluster cluster = view.getCluster(clusterId);
      if (cluster == null || (service != null && !cluster.getServices().contains(service))) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user "
                                            + account.getUserId() + " does not exist");
      }
      if (!Cluster.Status.SERVICE_ACTIONABLE_STATES.contains(cluster.getStatus())) {
        throw new IllegalStateException(
          "cluster " + clusterId + " is not in a state where service actions can be performed");
      }
      JobId jobId = idService.getNewJobId(clusterId);

      ClusterJob job = new ClusterJob(jobId, action, service == null ? null : ImmutableSet.of(service), null);
      job.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(job.getJobId());
      cluster.setStatus(Cluster.Status.PENDING);
      view.writeCluster(cluster);
      clusterStore.writeClusterJob(job);

      loomStats.getClusterStats().incrementStat(action);
      clusterQueues.add(account.getTenantId(), new Element(clusterId, action.name()));
    } finally {
      lock.release();
    }
  }

  public void requestAbortJob(String clusterId, Account account) throws IOException, MissingClusterException {
    ClusterStoreView view = clusterStoreService.getView(account);
    // First read cluster without locking
    Cluster cluster = view.getCluster(clusterId);
    if (cluster == null) {
      throw new MissingClusterException("cluster " + clusterId + " not found.");
    }

    if (cluster.getStatus() == Cluster.Status.TERMINATED || cluster.getStatus() != Cluster.Status.PENDING) {
      return;
    }

    // Get latest job
    ClusterJob clusterJob = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));

    // If job not running, nothing to abort
    if (clusterJob.getJobStatus() == ClusterJob.Status.FAILED ||
      clusterJob.getJobStatus() == ClusterJob.Status.COMPLETE) {
      // Reschedule the job.
      jobQueues.add(account.getTenantId(), new Element(clusterJob.getJobId()));
      return;
    }

    // Job can be aborted only when CLUSTER_CREATE is RUNNING
    if (!(clusterJob.getClusterAction() == ClusterAction.CLUSTER_CREATE &&
      clusterJob.getJobStatus() == ClusterJob.Status.RUNNING)) {
      throw new IllegalStateException("Cannot be aborted at this time.");
    }

    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      cluster = view.getCluster(clusterId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " not found.");
      }

      if (cluster.getStatus() == Cluster.Status.TERMINATED || cluster.getStatus() != Cluster.Status.PENDING) {
        return;
      }

      clusterJob = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));

      // If job already done, return.
      if (clusterJob.getJobStatus() == ClusterJob.Status.COMPLETE ||
        clusterJob.getJobStatus() == ClusterJob.Status.FAILED) {
        return;
      }

      clusterJob.setJobStatus(ClusterJob.Status.FAILED);
      clusterJob.setStatusMessage("Aborted by user.");
      clusterStore.writeClusterJob(clusterJob);
      // Reschedule the job.
      jobQueues.add(account.getTenantId(), new Element(clusterJob.getJobId()));
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
   * @param account Account of the user that is trying to add services to the cluster.
   * @param addRequest Request to add services.
   * @throws IOException if there was some problem writing to stores.
   * @throws MissingClusterException if there is no cluster with the given id.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public void requestAddServices(String clusterId, Account account, AddServicesRequest addRequest)
    throws IOException, MissingClusterException, IllegalAccessException {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      ClusterStoreView view = clusterStoreService.getView(account);
      Cluster cluster = view.getCluster(clusterId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user "
                                            + account.getUserId() + " does not exist");
      }
      if (cluster.getStatus() != Cluster.Status.ACTIVE) {
        throw new IllegalStateException(
          "cluster " + clusterId + " is not in a state where services can be added to it.");
      }
      JobId jobId = idService.getNewJobId(clusterId);
      solver.validateServicesToAdd(cluster, addRequest.getServices());

      ClusterAction action = ClusterAction.ADD_SERVICES;
      ClusterJob job = new ClusterJob(jobId, action, addRequest.getServices(), null);
      job.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(job.getJobId());
      cluster.setStatus(Cluster.Status.PENDING);
      view.writeCluster(cluster);
      clusterStore.writeClusterJob(job);

      loomStats.getClusterStats().incrementStat(action);
      SolverRequest solverRequest = new SolverRequest(SolverRequest.Type.ADD_SERVICES, gson.toJson(addRequest));
      clusterQueues.add(account.getTenantId(), new Element(clusterId, gson.toJson(solverRequest)));
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
   * @param account Account of the user trying to sync the cluster.
   * @throws IOException if there was some problem writing to stores.
   * @throws MissingEntityException if there is no cluster with the given id, or if the template no longer exists.
   * @throws InvalidClusterException if syncing the template would result in an invalid cluster.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public void syncClusterToCurrentTemplate(String clusterId, Account account)
    throws IOException, MissingEntityException, InvalidClusterException, IllegalAccessException {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      ClusterStoreView view = clusterStoreService.getView(account);
      Cluster cluster = view.getCluster(clusterId);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " owned by user "
                                            + account.getUserId() + " does not exist");
      }
      if (cluster.getStatus() != Cluster.Status.ACTIVE) {
        throw new IllegalStateException(
          "cluster " + clusterId + " is not in a state where services can be added to it.");
      }

      String templateName = cluster.getClusterTemplate().getName();
      ClusterTemplate currentTemplate = entityStoreService.getView(account).getClusterTemplate(templateName);
      if (currentTemplate == null) {
        LOG.info("tried to sync template {} for cluster {}, but the template no longer exists",
                 templateName, clusterId);
        throw new MissingEntityException("template " + templateName + " no longer exists");
      }
      Set<Node> clusterNodes = view.getClusterNodes(clusterId);
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
      view.writeCluster(cluster);
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
   * @param account account of the user requesting the change.
   * @param expireTime new expire time.
   * @throws IOException if there was some problem writing to stores.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   */
  public void changeExpireTime(String clusterId, Account account, long expireTime) throws IOException,
    IllegalAccessException {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      Cluster cluster = clusterStoreService.getView(account).getCluster(clusterId);

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
      clusterStoreService.getView(account).writeCluster(cluster);
    } finally {
      lock.release();
    }
  }
}
