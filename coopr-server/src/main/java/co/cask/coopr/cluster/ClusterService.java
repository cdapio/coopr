/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.cluster;

import co.cask.coopr.account.Account;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.zookeeper.IdService;
import co.cask.coopr.common.zookeeper.LockService;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.layout.ClusterLayout;
import co.cask.coopr.layout.InvalidClusterException;
import co.cask.coopr.layout.Solver;
import co.cask.coopr.management.ServerStats;
import co.cask.coopr.provisioner.QuotaException;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.SolverRequest;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.MissingClusterException;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.plugin.FieldSchema;
import co.cask.coopr.spec.plugin.ParameterType;
import co.cask.coopr.spec.plugin.PluginFields;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.template.AbstractTemplate;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Include;
import co.cask.coopr.spec.template.Parent;
import co.cask.coopr.spec.template.PartialTemplate;
import co.cask.coopr.spec.template.SizeConstraint;
import co.cask.coopr.spec.template.TemplateImmutabilityException;
import co.cask.coopr.spec.template.TemplateMerger;
import co.cask.coopr.spec.template.TemplateNotFoundException;
import co.cask.coopr.spec.template.TemplateValidationException;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.cluster.ClusterStoreView;
import co.cask.coopr.store.credential.CredentialStore;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Service for performing operations on clusters.
 */
public class ClusterService {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterService.class);

  private final ClusterStoreService clusterStoreService;
  private final ClusterStore clusterStore;
  private final EntityStoreService entityStoreService;
  private final TenantProvisionerService tenantProvisionerService;
  private final CredentialStore credentialStore;
  private final LockService lockService;
  private final ServerStats serverStats;
  private final Solver solver;
  private final IdService idService;
  private final Gson gson;
  private final QueueGroup clusterQueues;
  private final QueueGroup solverQueues;
  private final QueueGroup jobQueues;
  private final TemplateMerger templateMerger;

  @Inject
  public ClusterService(ClusterStoreService clusterStoreService,
                        EntityStoreService entityStoreService,
                        TenantProvisionerService tenantProvisionerService,
                        QueueService queueService,
                        LockService lockService,
                        ServerStats serverStats,
                        Solver solver,
                        IdService idService,
                        CredentialStore credentialStore,
                        Gson gson, TemplateMerger templateMerger) {
    this.clusterStoreService = clusterStoreService;
    this.clusterStore = clusterStoreService.getSystemView();
    this.entityStoreService = entityStoreService;
    this.tenantProvisionerService = tenantProvisionerService;
    this.credentialStore = credentialStore;
    this.lockService = lockService;
    this.serverStats = serverStats;
    this.solver = solver;
    this.idService = idService;
    this.gson = gson;
    this.clusterQueues = queueService.getQueueGroup(QueueType.CLUSTER);
    this.solverQueues = queueService.getQueueGroup(QueueType.SOLVER);
    this.jobQueues = queueService.getQueueGroup(QueueType.JOB);
    this.templateMerger = templateMerger;
  }

  /**
   * Get a list of summaries of all clusters visible to the given account that are in one of the given states.
   *
   * @param account Account to get cluster summaries for.
   * @param states Returned clusters must be in one of these states.
   * @return List of summaries of all clusters visible to the given account.
   * @throws IOException if there was an exception reading the cluster data from the store.
   */
  public List<ClusterSummary> getClusterSummaries(Account account, Set<Cluster.Status> states) throws IOException {
    if (states == null || states.isEmpty()) {
      return clusterStoreService.getView(account).getAllClusterSummaries();
    } else {
      return clusterStoreService.getView(account).getAllClusterSummaries(states);
    }
  }

  /**
   * Submit a request to create a cluster, creating a placeholder cluster object and adding a task to solve for a
   * layout to the solver queue.
   *
   * @param clusterCreateRequest Request to create a cluster.
   * @param account Account of the user making the request.
   * @return Id of the cluster that will be created.
   * @throws IOException if there was some error writing to stores.
   * @throws QuotaException if the operation would cause the tenant quotas to be exceeded.
   * @throws MissingEntityException if some required entity such as the provider or template does not exist.
   * @throws InvalidClusterException if the requested cluster violates the template constraints.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   * @throws MissingFieldsException if there are required fields missing from the request.
   */
  public String requestClusterCreate(ClusterCreateRequest clusterCreateRequest, Account account) throws IOException,
    QuotaException, MissingEntityException, InvalidClusterException, IllegalAccessException, MissingFieldsException {
    // the create lock is shared across an entire tenant and is needed so that concurrent create requests
    // cannot cause the quota to be exceeded if they both read the old value and both add clusters and nodes
    Lock lock = lockService.getClusterCreateLock(account.getTenantId());
    lock.lock();
    try {
      if (!tenantProvisionerService.satisfiesTenantQuotas(
        account.getTenantId(), 1, clusterCreateRequest.getNumMachines())) {
        throw new QuotaException("Creating the cluster would cause cluster or node quotas to be violated.");
      }

      Cluster cluster = createUnsolvedCluster(account, clusterCreateRequest);
      prepareClusterForOperation(cluster, clusterCreateRequest);
      JobId clusterJobId = idService.getNewJobId(cluster.getId());
      ClusterJob clusterJob = new ClusterJob(clusterJobId, ClusterAction.SOLVE_LAYOUT);
      cluster.setLatestJobId(clusterJob.getJobId());

      LOG.trace("Writing cluster {} to store", cluster);
      clusterStoreService.getView(account).writeCluster(cluster);
      clusterStore.writeClusterJob(clusterJob);

      LOG.debug("adding create cluster element to solverQueue");
      SolverRequest solverRequest = new SolverRequest(SolverRequest.Type.CREATE_CLUSTER,
                                                      gson.toJson(clusterCreateRequest));
      solverQueues.add(account.getTenantId(), new Element(cluster.getId(), gson.toJson(solverRequest)));

      serverStats.getClusterStats().incrementStat(ClusterAction.SOLVE_LAYOUT);
      return cluster.getId();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Request deletion of a given cluster that the user has permission to delete.
   *
   * @param clusterId Id of the cluster to delete.
   * @param account Account of the user making the request.
   * @param request Request to delete the cluster, containing optional provider fields.
   * @throws IOException if there was some error writing to stores.
   * @throws MissingEntityException if some entity required to complete, such as the provider type
   *                                used to create the cluster, could not be found.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   * @throws MissingFieldsException if there are required fields missing from the request.
   */
  public void requestClusterDelete(String clusterId, Account account, ClusterOperationRequest request)
    throws IOException, IllegalAccessException, MissingEntityException, MissingFieldsException {
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
      JobId deleteJobId = idService.getNewJobId(clusterId);
      ClusterJob deleteJob = new ClusterJob(deleteJobId, ClusterAction.CLUSTER_DELETE);
      deleteJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(deleteJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);
      prepareClusterForOperation(cluster, request);

      LOG.debug("Writing cluster {} to store with delete job {}", clusterId, deleteJobId);
      clusterStoreService.getView(account).writeCluster(cluster);
      clusterStore.writeClusterJob(deleteJob);

      serverStats.getClusterStats().incrementStat(ClusterAction.CLUSTER_DELETE);
      clusterQueues.add(account.getTenantId(), new Element(clusterId, ClusterAction.CLUSTER_DELETE.name()));
    } finally {
      lock.unlock();
    }
  }

  /**
   * Put in a request to reconfigure services on an active cluster.
   * Throws a {@link co.cask.coopr.scheduler.task.MissingClusterException} if no cluster owned by the user is found and
   * an {@link IllegalStateException} if the cluster is not in a state where the action can be performed.
   *
   * @param clusterId Id of cluster to reconfigure.
   * @param account Account of the user that is trying to reconfigure the cluster.
   * @param request Request to configure the cluster, containing the new config plus other options.
   * @throws IOException if there was so error writing to stores.
   * @throws MissingEntityException if some entity required to complete, such as the cluster or the provider type
   *                                used to create the cluster, could not be found.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   * @throws MissingFieldsException if there are required fields missing from the request.
   */
  public void requestClusterReconfigure(String clusterId, Account account, ClusterConfigureRequest request)
    throws IOException, MissingEntityException, IllegalAccessException, MissingFieldsException {
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
      if (!Cluster.Status.CONFIGURABLE_STATES.contains(cluster.getStatus())) {
        throw new IllegalStateException("cluster " + clusterId + " is not in a configurable state");
      }
      JobId configureJobId = idService.getNewJobId(clusterId);
      boolean restartServices = request.getRestart();
      JsonObject config = request.getConfig();

      ClusterAction action =
        restartServices ? ClusterAction.CLUSTER_CONFIGURE_WITH_RESTART : ClusterAction.CLUSTER_CONFIGURE;
      ClusterJob configureJob = new ClusterJob(configureJobId, action);
      configureJob.setJobStatus(ClusterJob.Status.RUNNING);
      cluster.setLatestJobId(configureJobId.getId());
      cluster.setStatus(Cluster.Status.PENDING);

      LOG.debug("Writing cluster {} to store with configure job {}", clusterId, configureJobId);
      cluster.setConfig(config);
      prepareClusterForOperation(cluster, request);
      clusterStoreService.getView(account).writeCluster(cluster);
      clusterStore.writeClusterJob(configureJob);

      serverStats.getClusterStats().incrementStat(action);
      clusterQueues.add(account.getTenantId(), new Element(clusterId, action.name()));
    } finally {
      lock.unlock();
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
   * @param request Service action request, containing optional provider fields.
   * @throws IOException if there was so error writing to stores.
   * @throws MissingEntityException if some entity required to complete, such as the cluster or the provider type
   *                                used to create the cluster, could not be found.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   * @throws MissingFieldsException if there are required fields missing from the request.
   */
  public void requestServiceRuntimeAction(String clusterId, Account account, ClusterAction action, String service,
                                          ClusterOperationRequest request)
    throws IOException, MissingEntityException, IllegalAccessException, MissingFieldsException {
    Preconditions.checkArgument(ClusterAction.SERVICE_RUNTIME_ACTIONS.contains(action),
                                action + " is not a service runtime action.");
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
      if (service != null && !cluster.getServices().contains(service)) {
        throw new MissingEntityException("service " + service + " does not exist on cluster " + clusterId);
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
      prepareClusterForOperation(cluster, request);
      clusterStoreService.getView(account).writeCluster(cluster);
      clusterStore.writeClusterJob(job);

      serverStats.getClusterStats().incrementStat(action);
      clusterQueues.add(account.getTenantId(), new Element(clusterId, action.name()));
    } finally {
      lock.unlock();
    }
  }

  public void requestAbortJob(String clusterId, Account account) throws IOException, MissingClusterException {
    ClusterStoreView view = clusterStoreService.getView(account);
    // First read cluster without locking
    Cluster cluster = getCluster(clusterId, account);

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

    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
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
      lock.unlock();
    }
  }

  /**
   * Request to pause a cluster job that is currently running.
   *
   * @param clusterId Id of the cluster.
   * @param account Account of the user that is trying to pause a cluster job.
   * @throws IOException if there was some error writing to stores.
   * @throws MissingClusterException if no cluster owned by the user is found.
   */
  public void requestPauseJob(String clusterId, Account account) throws IOException, MissingClusterException {
    LOG.debug("request to pause job for cluster: {}", clusterId);
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
      LOG.debug("cluster info: {}", cluster);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " not found.");
      }

      if (cluster.getStatus() != Cluster.Status.PENDING) {
        return;
      }

      ClusterJob clusterJob = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
      LOG.debug("latest job info: {}", clusterJob);

      // If job already done, return.
      if (clusterJob.getJobStatus() == ClusterJob.Status.COMPLETE ||
        clusterJob.getJobStatus() == ClusterJob.Status.FAILED) {
        return;
      }

      clusterJob.setJobStatus(ClusterJob.Status.PAUSED);
      clusterJob.setStatusMessage("Paused by user.");
      clusterStore.writeClusterJob(clusterJob);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Request to resume a cluster job that was previously paused.
   *
   * @param clusterId Id of the cluster.
   * @param account Account of the user that is trying to resume a cluster job.
   * @throws IOException if there was some error writing to stores.
   * @throws MissingClusterException if no cluster owned by the user is found.
   */
  public void requestResumeJob(String clusterId, Account account) throws IOException, MissingClusterException {
    LOG.debug("request to resume job for cluster: {}", clusterId);
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
      LOG.debug("cluster info: {}", cluster);
      if (cluster == null) {
        throw new MissingClusterException("cluster " + clusterId + " not found.");
      }

      if (cluster.getStatus() != Cluster.Status.PENDING) {
        return;
      }

      ClusterJob clusterJob = clusterStore.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
      LOG.debug("latest job info: {}", clusterJob);

      // If job is not paused, return.
      if (clusterJob.getJobStatus() != ClusterJob.Status.PAUSED) {
        return;
      }

      clusterJob.setJobStatus(ClusterJob.Status.RUNNING);
      clusterJob.setStatusMessage("Resumed by user.");
      clusterStore.writeClusterJob(clusterJob);
      // Reschedule the job.
      jobQueues.add(account.getTenantId(), new Element(clusterJob.getJobId()));
    } finally {
      lock.unlock();
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
   * @throws MissingEntityException if some entity required to complete, such as the cluster or the provider type
   *                                used to create the cluster, could not be found.
   * @throws IllegalAccessException if the operation is not allowed for the given account.
   * @throws MissingFieldsException if there are required fields missing from the request.
   */
  public void requestAddServices(String clusterId, Account account, AddServicesRequest addRequest)
    throws IOException, MissingEntityException, IllegalAccessException, MissingFieldsException {
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
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
      prepareClusterForOperation(cluster, addRequest);
      clusterStoreService.getView(account).writeCluster(cluster);
      clusterStore.writeClusterJob(job);

      serverStats.getClusterStats().incrementStat(action);
      SolverRequest solverRequest = new SolverRequest(SolverRequest.Type.ADD_SERVICES, gson.toJson(addRequest));
      solverQueues.add(account.getTenantId(), new Element(clusterId, gson.toJson(solverRequest)));
    } finally {
      lock.unlock();
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
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);
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
      ClusterStoreView view = clusterStoreService.getView(account);
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
      lock.unlock();
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
    IllegalAccessException, MissingClusterException {
    Lock lock = lockService.getClusterLock(account.getTenantId(), clusterId);
    lock.lock();
    try {
      Cluster cluster = getCluster(clusterId, account);

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
      lock.unlock();
    }
  }

  private Cluster createUnsolvedCluster(Account account, ClusterCreateRequest createRequest)
    throws MissingEntityException, IOException, InvalidClusterException {

    String name = createRequest.getName();
    int numMachines = createRequest.getNumMachines();
    String templateName = createRequest.getClusterTemplate();
    LOG.debug(String.format("Received a request to create cluster %s with %d machines from template %s", name,
                            numMachines, templateName));

    String clusterId = idService.getNewClusterId();
    Cluster.Builder builder = Cluster.builder()
      .setAccount(account)
      .setName(name)
      .setDescription(createRequest.getDescription())
      .setID(clusterId);

    EntityStoreView entityStore = entityStoreService.getView(account);
    // make sure the template exists
    ClusterTemplate template = entityStore.getClusterTemplate(templateName);
    if (template == null) {
      throw new MissingEntityException("cluster template " + templateName + " does not exist.");
    }

    // check cluster size constraints
    SizeConstraint sizeConstraint = template.getConstraints().getSizeConstraint();
    sizeConstraint.verify(numMachines);
    builder.setClusterTemplate(template);

    // set lease times
    long requestedLease = createRequest.getInitialLeaseDuration();
    long leaseDuration = template.getAdministration().getLeaseDuration().calcInitialLease(requestedLease);
    long createTime = System.currentTimeMillis();
    builder.setCreateTime(createTime);
    // Lease duration of 0 is forever.
    builder.setExpireTime(leaseDuration == 0 ? 0 : createTime + leaseDuration);

    Provider provider = getProvider(template, createRequest, entityStore);
    builder.setProvider(provider);

    Set<String> serviceNames = getServices(template, createRequest);
    builder.setServices(serviceNames);

    JsonObject config = getConfig(template, createRequest);
    builder.setConfig(config);

    return builder.build();
  }

  // get the provider requested, or the default provider if there was none given in the request
  private Provider getProvider(ClusterTemplate template, ClusterCreateRequest request, EntityStoreView entityStore)
    throws IOException, MissingEntityException {
    // make sure the provider exists
    String providerName = request.getProvider();
    if (providerName == null || providerName.isEmpty()) {
      providerName = template.getClusterDefaults().getProvider();
    }
    Provider provider = entityStore.getProvider(providerName);
    if (provider == null) {
      throw new MissingEntityException("provider " + providerName + " does not exist.");
    }

    return provider;
  }

  // try to add provider fields to the cluster's provider object, returning missing fields if there are any.
  private void prepareClusterForOperation(Cluster cluster, ClusterOperationRequest request)
    throws IOException, MissingEntityException, MissingFieldsException {
    Provider provider = cluster.getProvider();
    Account account = cluster.getAccount();
    String clusterId = cluster.getId();
    Map<String, Object> providerFields = request == null ?
      Maps.<String, Object>newHashMap() : request.getProviderFields();

    // make sure the provider type for the provider exists.
    // Will need this to add any provider fields given in the request
    String providerTypeName = provider.getProviderType();
    ProviderType providerType = entityStoreService.getView(account).getProviderType(providerTypeName);
    if (providerType == null) {
      throw new MissingEntityException("provider type " + providerTypeName + " does not exist.");
    }

    // check all required user fields are present
    // if there are no fields in the request, they may be in the credential store
    if (providerFields.isEmpty()) {
      try {
        Map<String, Object> existingSensitiveFields = credentialStore.get(account.getTenantId(), clusterId);
        if (existingSensitiveFields != null) {
          providerFields = existingSensitiveFields;
        }
      } catch (IOException e) {
        // its possible we get an exception looking up the fields, but we didn't need them anyway.
        // so log an error and proceed.  If we needed the fields, it will fail below when checking required fields.
        LOG.error("Exception looking up sensitive fields for account {} for cluster {}.", account, clusterId, e);
      }
    }
    Set<String> allProviderFields = Sets.union(provider.getProvisionerFields().keySet(), providerFields.keySet());
    List<Map<String, FieldSchema>> missingFields = providerType.getMissingFields(ParameterType.USER, allProviderFields);
    if (!missingFields.isEmpty()) {
      throw new MissingFieldsException(missingFields);
    }

    PluginFields pluginFields = providerType.groupFields(providerFields);
    // take sensitive fields out and write them to the credential store
    // this will overwrite anything that's already there
    Map<String, Object> sensitiveFields = pluginFields.getSensitive();
    if (!sensitiveFields.isEmpty()) {
      LOG.trace("writing fields {} to credential store for account {} and cluster {}.",
                sensitiveFields.keySet(), account, clusterId);
      credentialStore.set(account.getTenantId(), clusterId, sensitiveFields);
    }
    // add non sensitive fields to the provider
    Map<String, Object> nonSensitiveFields = pluginFields.getNonsensitive();
    if (!nonSensitiveFields.isEmpty()) {
      provider.addFields(nonSensitiveFields);
    }
  }

  private JsonObject getConfig(ClusterTemplate template, ClusterCreateRequest createRequest) {
    // use the config from the request if it exists. Otherwise use the template default
    JsonObject config = createRequest.getConfig();
    if (config == null) {
      config = template.getClusterDefaults().getConfig();
    }
    return config;
  }

  private Set<String> getServices(ClusterTemplate template, ClusterCreateRequest createRequest) {
    // set cluster service names. Dependency checking is done later on since it involves a lot of potential lookups.
    Set<String> serviceNames = createRequest.getServices();
    if (serviceNames == null || serviceNames.isEmpty()) {
      serviceNames = template.getClusterDefaults().getServices();
    }
    return serviceNames;
  }

  // get the specified cluster, throwing an exception if it does not exist
  private Cluster getCluster(String clusterId, Account account) throws IOException, MissingClusterException {
    Cluster cluster = clusterStoreService.getView(account).getCluster(clusterId);
    if (cluster == null) {
      throw new MissingClusterException("cluster " + clusterId + " does not exist");
    }
    return cluster;
  }

  /**
   * Build cluster template from provided includes and parents.
   *
   * @param clusterTemplate Cluster template which can contains includes and parents.
   * @param account Account of the user that is trying to resolve a cluster template.
   * @return Cluster Template with merged body from includes and parents.
   * @throws IOException if there was some error writing to stores.
   * @throws TemplateNotFoundException if template is unknown type.
   * @throws TemplateImmutabilityException if some template tries to override immutable template config.
   * @throws TemplateValidationException if template after merge does not has some required fields.
   */
  public ClusterTemplate resolveTemplate(Account account, ClusterTemplate clusterTemplate)
    throws TemplateNotFoundException, TemplateImmutabilityException, IOException, TemplateValidationException {
    EntityStoreView entityStore = entityStoreService.getView(account);
    return resolveTemplate(entityStore, clusterTemplate);
  }

  /**
   * Build cluster template from provided includes and parents.
   *
   * @param templateName Cluster template name.
   * @param account Account of the user that is trying to resolve a cluster template.
   * @return Cluster Template with merged body from includes and parents.
   * @throws IOException if there was some error reading from stores.
   * @throws TemplateNotFoundException if template can't be found in store.
   * @throws TemplateImmutabilityException if some template tries to override immutable template config.
   * @throws TemplateValidationException if template after merge does not has some required fields.
   */
  public ClusterTemplate resolveTemplate(Account account, String templateName)
    throws IOException, TemplateNotFoundException, TemplateImmutabilityException, TemplateValidationException {
    EntityStoreView entityStore = entityStoreService.getView(account);
    ClusterTemplate clusterTemplate = entityStore.getClusterTemplate(templateName);
    if  (clusterTemplate == null) {
      throw new TemplateNotFoundException("Cluster template " + templateName + " does not exist");
    }
    return resolveTemplate(entityStore, clusterTemplate);
  }

  private ClusterTemplate resolveTemplate(EntityStoreView entityStore, ClusterTemplate clusterTemplate)
    throws IOException, TemplateImmutabilityException, TemplateNotFoundException, TemplateValidationException {
    Set<AbstractTemplate> mergeSet = getMergeCollection(entityStore, clusterTemplate);
    return templateMerger.merge(mergeSet, clusterTemplate);
  }

  /*
  Merging in order Parent Includes -> Parent -> Child Includes -> Child -> ...
  TODO: merging with mandatory partials and user-level attributes(???)
   */
  private Set<AbstractTemplate> getMergeCollection(EntityStoreView entityStore, ClusterTemplate clusterTemplate)
    throws IOException, TemplateNotFoundException {
    Set<AbstractTemplate> forMerge;
    Parent parent = clusterTemplate.getParent();
    if (parent != null) {
      ClusterTemplate parentTemplate = entityStore.getClusterTemplate(parent.getName());
      if (parentTemplate == null) {
        throw new TemplateNotFoundException(parent.getName() + " parent template not found.");
      }
      forMerge = getMergeCollection(entityStore, parentTemplate);
    } else {
      forMerge = Sets.newLinkedHashSet();
    }
    Set<AbstractTemplate> includes = resolvePartialIncludes(entityStore, clusterTemplate.getIncludes());
    forMerge.addAll(includes);
    forMerge.add(clusterTemplate);

    return forMerge;
  }

  private Set<AbstractTemplate> resolvePartialIncludes(EntityStoreView entityStore, Set<Include> includes)
    throws IOException, TemplateNotFoundException {
    Set<AbstractTemplate> partials = Sets.newLinkedHashSet();
    if (includes != null) {
      for (Include include : includes) {
        PartialTemplate partialTemplate = entityStore.getPartialTemplate(include.getName());
        if (partialTemplate == null) {
          throw new TemplateNotFoundException(include.getName() + " partial template not found.");
        }
        partials.add(partialTemplate);
      }
    }
    return partials;
  }
}
