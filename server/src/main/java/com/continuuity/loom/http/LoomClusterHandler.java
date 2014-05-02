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
package com.continuuity.loom.http;

import com.continuuity.http.HttpResponder;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.layout.ClusterCreateRequest;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.SolverRequest;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterService;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.MissingClusterException;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for performing cluster operations.
 */
@Path("/v1/loom/clusters")
public class LoomClusterHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomClusterHandler.class);
  private static final Gson GSON = new JsonSerde().getGson();

  private final ClusterStore store;
  private final TrackingQueue jobQueue;
  private final JsonSerde codec;
  private final TrackingQueue solverQueue;
  private final ZKClient zkClient;
  private ClusterService clusterService;
  private final int maxClusterSize;
  private final LoomStats loomStats;

  @Inject
  public LoomClusterHandler(ClusterStore store, @Named(Constants.Queue.SOLVER) TrackingQueue solverQueue,
                            @Named(Constants.Queue.JOB) TrackingQueue jobQueue, ZKClient zkClient,
                            ClusterService clusterService, @Named(Constants.MAX_CLUSTER_SIZE) int maxClusterSize,
                            LoomStats loomStats) {
    this.store = store;
    this.jobQueue = jobQueue;
    this.codec = new JsonSerde();
    this.solverQueue = solverQueue;
    this.zkClient = ZKClients.namespace(zkClient, Constants.LOCK_NAMESPACE);
    this.clusterService = clusterService;
    this.maxClusterSize = maxClusterSize;
    this.loomStats = loomStats;
  }

  /**
   * Get all clusters visible to the user.
   *
   * @param request Request for clusters.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  public void getClusters(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    List<Cluster> clusters;
    // TODO: revise endpoints so that admin has its own for this
    if (userId.equals(Constants.ADMIN_USER)) {
      clusters = store.getAllClusters();
    } else {
      clusters = store.getAllClusters(userId);
    }

    JsonArray jsonArray = new JsonArray();
    for (Cluster cluster : clusters) {
      JsonObject obj = new JsonObject();
      obj.addProperty("id", cluster.getId());
      obj.addProperty("name", cluster.getName());
      obj.addProperty("createTime", cluster.getCreateTime());
      obj.addProperty("expireTime", cluster.getExpireTime());
      obj.addProperty("clusterTemplate",
                      cluster.getClusterTemplate() == null ? "..." : cluster.getClusterTemplate().getName());
      obj.addProperty("numNodes", cluster.getNodes().size());
      obj.addProperty("status", cluster.getStatus().name());
      obj.addProperty("ownerId", cluster.getOwnerId());

      jsonArray.add(obj);
    }

    responder.sendJson(HttpResponseStatus.OK, jsonArray);
  }

  /**
   * Get a specific cluster visible to the user.
   *
   * @param request Request for a cluster.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster to get.
   * @throws Exception
   */
  @GET
  @Path("/{cluster-id}")
  public void getCluster(HttpRequest request, HttpResponder responder,
                         @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    Cluster cluster = clusterService.getUserCluster(clusterId, userId);
    if (cluster == null) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
      return;
    }
    
    JsonObject jsonObject = GSON.toJsonTree(cluster).getAsJsonObject();
    
    // Update cluster Json with node information.
    Set<Node> clusterNodes = store.getClusterNodes(clusterId);
    jsonObject.add("nodes", GSON.toJsonTree(clusterNodes));

    // Add last job message if any
    ClusterJob clusterJob = store.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
    if (clusterJob.getStatusMessage() != null) {
      jsonObject.addProperty("message", clusterJob.getStatusMessage());
    }

    responder.sendJson(HttpResponseStatus.OK, jsonObject);
  }

  /**
   * Get the config used by the cluster.
   *
   * @param request Request for config of a cluster.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster containing the config to get.
   * @throws Exception
   */
  @GET
  @Path("/{cluster-id}/config")
  public void getClusterConfig(HttpRequest request, HttpResponder responder,
                               @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    Cluster cluster = clusterService.getUserCluster(clusterId, userId);
    if (cluster == null) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
      return;
    }
    responder.sendJson(HttpResponseStatus.OK, cluster.getConfig());
  }

  /**
   * Get all services on a specific cluster visible to the user.
   *
   * @param request Request for services on a cluster.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster containing the services to get.
   * @throws Exception
   */
  @GET
  @Path("/{cluster-id}/services")
  public void getClusterServices(HttpRequest request, HttpResponder responder,
                                 @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    Cluster cluster = clusterService.getUserCluster(clusterId, userId);
    if (cluster == null) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
      return;
    }
    responder.sendJson(HttpResponseStatus.OK, cluster.getServices());
  }

  /**
   * Get the status of a specific cluster visible to the user.
   *
   * @param request Request for cluster status.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose status to get.
   * @throws Exception
   */
  @GET
  @Path("/{cluster-id}/status")
  public void getClusterStatus(HttpRequest request, HttpResponder responder,
                               @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    Cluster cluster = clusterService.getUserCluster(clusterId, userId);
    if (cluster == null){
      responder.sendError(HttpResponseStatus.NOT_FOUND, String.format("cluster %s not found", clusterId));
      return;
    }

    ClusterJob job = store.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
    if (job == null){
      responder.sendError(HttpResponseStatus.NOT_FOUND,
                          String.format("job %s not found for cluster %s", cluster.getLatestJobId(), clusterId));
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, getClusterResponseJson(cluster, job));
  }

  protected static JsonObject getClusterResponseJson(Cluster cluster, ClusterJob job) {
    Map<String, ClusterTask.Status> taskStatus = job.getTaskStatus();

    int completedTasks = 0;
    for (Map.Entry<String, ClusterTask.Status> entry : taskStatus.entrySet()) {
      if (entry.getValue().equals(ClusterTask.Status.COMPLETE)) {
        completedTasks++;
      }
    }

    JsonObject object = new JsonObject();
    object.addProperty("clusterid", cluster.getId());
    object.addProperty("stepstotal", taskStatus.size());
    object.addProperty("stepscompleted", completedTasks);
    object.addProperty("status", cluster.getStatus().name());
    object.addProperty("actionstatus", job.getJobStatus().toString());
    object.addProperty("action", job.getClusterAction().name());

    return object;
  }

  /**
   * Create a new cluster. Body must include a cluster template and a number of machines.  Optionally it can include any
   * setting that will override the corresponding template default value.
   *
   * @param request Request to add a cluster.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @POST
  public void createCluster(HttpRequest request, HttpResponder responder) throws Exception {
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    try {
      ClusterCreateRequest clusterCreateRequest = codec.getGson().fromJson(reader, ClusterCreateRequest.class);

      if (clusterCreateRequest.getNumMachines() > maxClusterSize) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "numMachines above max cluster size " + maxClusterSize);
        return;
      }

      String name = clusterCreateRequest.getName();
      int numMachines = clusterCreateRequest.getNumMachines();
      String templateName = clusterCreateRequest.getClusterTemplate();
      LOG.debug(String.format("Received a request to create cluster %s with %d machines from template %s", name,
                             numMachines, templateName));
      String clusterId = store.getNewClusterId();
      Cluster cluster = new Cluster(clusterId, userId, name, System.currentTimeMillis(),
                                    clusterCreateRequest.getDescription(), null, null,
                                    ImmutableSet.<String>of(), ImmutableSet.<String>of(),
                                    clusterCreateRequest.getConfig());
      JobId clusterJobId = store.getNewJobId(clusterId);
      ClusterJob clusterJob = new ClusterJob(clusterJobId, ClusterAction.SOLVE_LAYOUT);
      cluster.setLatestJobId(clusterJob.getJobId());

      try {
        LOG.trace("Writing cluster {} to store", cluster);
        store.writeCluster(cluster);
        store.writeClusterJob(clusterJob);
      } catch (Exception e) {
        LOG.error("Exception while trying to add cluster {} to cluster store.", cluster.getName(), e);
        responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error adding cluster.");
        return;
      }

      LOG.debug("adding create cluster element to solverQueue");
      SolverRequest solverRequest = new SolverRequest(SolverRequest.Type.CREATE_CLUSTER,
                                                      GSON.toJson(clusterCreateRequest));
      solverQueue.add(new Element(cluster.getId(), GSON.toJson(solverRequest)));

      loomStats.getClusterStats().incrementStat(ClusterAction.SOLVE_LAYOUT);

      JsonObject response = new JsonObject();
      response.addProperty("id", cluster.getId());
      responder.sendJson(HttpResponseStatus.OK, response);
    } catch (IllegalArgumentException e) {
      LOG.error("Exception trying to create cluster.", e);
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      LOG.error("Exception trying to create cluster.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal error while adding cluster.");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.warn("Exception while closing request reader", e);
      }
    }
  }

  /**
   * Delete a specific cluster that is deletable by the user.
   *
   * @param request Request to delete cluster.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/{cluster-id}")
  public void deleteCluster(HttpRequest request, HttpResponder responder,
                            @PathParam("cluster-id") String clusterId) throws Exception {
    LOG.debug("Received a request to delete cluster {}", clusterId);
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    Cluster cluster = clusterService.getUserCluster(clusterId, userId);
    if (cluster == null) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
      return;
    }

    if (cluster.getStatus() == Cluster.Status.TERMINATED) {
      responder.sendStatus(HttpResponseStatus.OK);
      return;
    }

    ClusterJob clusterJob = store.getClusterJob(JobId.fromString(cluster.getLatestJobId()));
    // If previous job on a cluster is still underway, don't accept new jobs
    if (cluster.getStatus() == Cluster.Status.PENDING) {
      String message = String.format("Job %s is still underway for cluster %s",
                                     clusterJob.getJobId(), clusterId);
      LOG.error(message);
      responder.sendError(HttpResponseStatus.CONFLICT, message);
      return;
    }

    try {
      clusterService.requestClusterDelete(clusterId, userId);
    } catch (Throwable e) {
      LOG.error("Exception while trying to write cluster {} to cluster store.", cluster.getName(), e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error deleting cluster.");
      return;
    }

    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Abort the cluster operation that is currently running for the given cluster.
   *
   * @param request Request to abort the cluster operation.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster to abort.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/abort")
  public void abortClusterJob(HttpRequest request, HttpResponder responder,
                              @PathParam("cluster-id") String clusterId) throws Exception {
    LOG.debug("Received a request to abort job on cluster {}", clusterId);
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    // First read cluster without locking
    Cluster cluster = clusterService.getUserCluster(clusterId, userId);
    if (cluster == null) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
      return;
    }

    if (cluster.getStatus() == Cluster.Status.TERMINATED || cluster.getStatus() != Cluster.Status.PENDING) {
      responder.sendStatus(HttpResponseStatus.OK);
      return;
    }

    // Get latest job
    ClusterJob clusterJob = store.getClusterJob(JobId.fromString(cluster.getLatestJobId()));

    // If job not running, nothing to abort
    if (clusterJob.getJobStatus() == ClusterJob.Status.FAILED ||
      clusterJob.getJobStatus() == ClusterJob.Status.COMPLETE) {
      // Reschedule the job.
      jobQueue.add(new Element(clusterJob.getJobId()));
      responder.sendStatus(HttpResponseStatus.OK);
      return;
    }

    // Job can be aborted only when CLUSTER_CREATE is RUNNING
    if (!(clusterJob.getClusterAction() == ClusterAction.CLUSTER_CREATE &&
      clusterJob.getJobStatus() == ClusterJob.Status.RUNNING)) {
      responder.sendError(HttpResponseStatus.CONFLICT, "Cannot be aborted at this time.");
      return;
    }

    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/" + clusterId);
    lock.acquire();
    try {
      cluster = clusterService.getUserCluster(clusterId, userId);
      if (cluster == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
        return;
      }

      if (cluster.getStatus() == Cluster.Status.TERMINATED || cluster.getStatus() != Cluster.Status.PENDING) {
        responder.sendStatus(HttpResponseStatus.OK);
        return;
      }

      clusterJob = store.getClusterJob(JobId.fromString(cluster.getLatestJobId()));

      // If job already done, return.
      if (clusterJob.getJobStatus() == ClusterJob.Status.COMPLETE ||
        clusterJob.getJobStatus() == ClusterJob.Status.FAILED) {
        responder.sendStatus(HttpResponseStatus.OK);
        return;
      }

      clusterJob.setJobStatus(ClusterJob.Status.FAILED);
      clusterJob.setStatusMessage("Aborted by user.");
      try {
        store.writeClusterJob(clusterJob);
        // Reschedule the job.
        jobQueue.add(new Element(clusterJob.getJobId()));
      } catch (Exception e) {
        LOG.error("Exception while trying to write job {} to cluster store.", clusterJob.getJobId(), e);
        responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error aborting cluster.");
        return;
      }
    } finally {
      lock.release();
    }
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Changes a cluster parameter like lease time.
   *
   * @param request Request to change cluster parameter.
   * @param responder Responder to send the response.
   * @param clusterId Id of the cluster to change.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}")
  public void changeClusterParameter(HttpRequest request, HttpResponder responder,
                                     @PathParam("cluster-id") String clusterId) throws Exception {
    try {
      String userId = getAndAuthenticateUser(request, responder);
      if (userId == null) {
        return;
      }

      JsonObject jsonObject = GSON.fromJson(request.getContent().toString(Charsets.UTF_8), JsonObject.class);
      if (jsonObject == null || !jsonObject.has("expireTime")) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "expire time not specified");
        return;
      }

      long expireTime = jsonObject.get("expireTime").getAsLong();

      clusterService.changeExpireTime(clusterId, userId, expireTime);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (JsonSyntaxException e) {
      LOG.error("Exception while parsing JSON.", e);
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid JSON");
    }
  }

  /**
   * Get the task plan for an operation that has taken place or is currently taking place on a cluster.
   *
   * @param request Request for the plan.
   * @param responder Responder to send the response.
   * @param clusterId Id of the cluster whose plan we want to get.
   * @param planId Id of the plan for the cluster.
   * @throws Exception
   */
  @GET
  @Path("/{cluster-id}/plans/{plan-id}")
  public void getPlanForJob(HttpRequest request, HttpResponder responder,
                            @PathParam("cluster-id") String clusterId,
                            @PathParam("plan-id") String planId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }
    try {
      Cluster cluster = clusterService.getUserCluster(clusterId, userId);
      if (cluster == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "cluster " + clusterId + " not found.");
        return;
      }

      JobId jobId = JobId.fromString(planId);
      ClusterJob clusterJob = store.getClusterJob(jobId);

      if (!clusterJob.getClusterId().equals(clusterId)) {
        throw new IllegalArgumentException(String.format("Job %s does not belong to cluster %s", planId, clusterId));
      }

      responder.sendJson(HttpResponseStatus.OK, formatJobPlan(clusterJob));
    } catch (IllegalArgumentException e) {
      LOG.error("Exception get plan {} for cluster {}.", planId, clusterId, e);
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * Get all plans for cluster operations that have taken place or are currently taking place on a cluster.
   *
   * @param request Request for cluster plans.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose plans we have to fetch.
   * @throws Exception
   */
  @GET
  @Path("/{cluster-id}/plans")
  public void getPlansForCluster(HttpRequest request, HttpResponder responder,
                                 @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }
    try {

      JsonArray jobsJson = new JsonArray();

      List<ClusterJob> jobs = clusterService.getClusterJobs(clusterId, userId);
      if (jobs.isEmpty()) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "Plans for cluster " + clusterId + " not found.");
        return;
      }
      for (ClusterJob clusterJob : clusterService.getClusterJobs(clusterId, userId)) {
        jobsJson.add(formatJobPlan(clusterJob));
      }

      responder.sendJson(HttpResponseStatus.OK, jobsJson);
    } catch (IllegalArgumentException e) {
      LOG.error("Exception getting plans for cluster {}.", clusterId, e);
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * Overwrite the config used by an active cluster. The POST body should contain a "config" key containing the new
   * cluster config. Additionally, the body can contain a "restart" key whose value is true or false, indicating
   * whether or not cluster services should be restarted along with being reconfigured. If restart is not specified,
   * it defaults to true.
   *
   * @param request Request for config of a cluster.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster containing the config to get.
   * @throws Exception
   */
  @PUT
  @Path("/{cluster-id}/config")
  public void putClusterConfig(HttpRequest request, HttpResponder responder,
                               @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    ClusterConfigureRequest configRequest;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      configRequest = GSON.fromJson(reader, ClusterConfigureRequest.class);
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid request body.");
      return;
    }

    try {
      clusterService.requestClusterReconfigure(clusterId, userId,
                                               configRequest.getRestart(), configRequest.getConfig());
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (MissingClusterException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Cluster " + clusterId + " not found.");
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, "Cluster is not in a configurable state.");
    } catch (Exception e) {
      LOG.error("Exception requesting reconfigure on cluster {}.", clusterId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Internal error while requesting cluster reconfigure");
    }
  }

  /**
   * Add specific services to a cluster. The POST body must be a JSON Object with a 'services' key whose value is a
   * JSON Array of service names. Services must be compatible with the template used when the cluster was created,
   * and any dependencies of services to add must either already be on the cluster, or also in the list of services
   * to add. If any of these rules are violated, a BAD_REQUEST status is returned back. Otherwise, the request to add
   * services is queued up.
   *
   * @param request Request to add services to a cluster.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster to add services to.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services")
  public void addClusterServices(HttpRequest request, HttpResponder responder,
                                 @PathParam("cluster-id") String clusterId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    AddServicesRequest addServicesRequest;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      addServicesRequest = GSON.fromJson(reader, AddServicesRequest.class);
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid request body.");
      return;
    }

    try {
      clusterService.requestAddServices(clusterId, userId, addServicesRequest);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (MissingClusterException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Cluster " + clusterId + " not found.");
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT,
                          "Cluster is not in a state where service actions can be performed.");
    } catch (Exception e) {
      LOG.error("Exception requesting to add services to cluster {}.", clusterId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Internal error while requesting service action.");
    }
  }

  /**
   * Starts all services on the cluster, taking into account service dependencies for order of service starts.
   *
   * @param request Request to start cluster services.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose services should be started.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services/start")
  public void startAllClusterServices(HttpRequest request, HttpResponder responder,
                                      @PathParam("cluster-id") String clusterId) throws Exception {
    requestServiceAction(request, responder, clusterId, null, ClusterAction.START_SERVICES);
  }

  /**
   * Stops all services on the cluster, taking into account service dependencies for order of service stops.
   *
   * @param request Request to stop cluster services.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose services should be stopped.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services/stop")
  public void stopAllClusterServices(HttpRequest request, HttpResponder responder,
                                     @PathParam("cluster-id") String clusterId) throws Exception {
    requestServiceAction(request, responder, clusterId, null, ClusterAction.STOP_SERVICES);
  }

  /**
   * Restarts all services on the cluster, taking into account service dependencies for order of service stops
   * and starts.
   *
   * @param request Request to restart cluster services.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose services should be restarted.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services/restart")
  public void restartAllClusterServices(HttpRequest request, HttpResponder responder,
                                        @PathParam("cluster-id") String clusterId) throws Exception {
    requestServiceAction(request, responder, clusterId, null, ClusterAction.RESTART_SERVICES);
  }

  /**
   * Starts the specified service, plus all services it depends on, on the cluster.
   *
   * @param request Request to start cluster service.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose services should be started.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services/{service-id}/start")
  public void startClusterService(HttpRequest request, HttpResponder responder,
                                  @PathParam("cluster-id") String clusterId,
                                  @PathParam("service-id") String serviceId) throws Exception {
    requestServiceAction(request, responder, clusterId, serviceId, ClusterAction.START_SERVICES);
  }

  /**
   * Stops the specified service on the cluster, plus all services that depend on it,
   * taking into account service dependencies for order of service stops.
   *
   * @param request Request to stop cluster services.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose services should be stopped.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services/{service-id}/stop")
  public void stopClusterService(HttpRequest request, HttpResponder responder,
                                 @PathParam("cluster-id") String clusterId,
                                 @PathParam("service-id") String serviceId) throws Exception {
    requestServiceAction(request, responder, clusterId, serviceId, ClusterAction.STOP_SERVICES);
  }

  /**
   * Restarts the specified service on the cluster, plus all services that depend on it,
   * taking into account service dependencies for order of service stops and starts.
   *
   * @param request Request to restart cluster service.
   * @param responder Responder for sending the response.
   * @param clusterId Id of the cluster whose service should be restarted.
   * @throws Exception
   */
  @POST
  @Path("/{cluster-id}/services/{service-id}/restart")
  public void restartClusterService(HttpRequest request, HttpResponder responder,
                                    @PathParam("cluster-id") String clusterId,
                                    @PathParam("service-id") String serviceId) throws Exception {
    requestServiceAction(request, responder, clusterId, serviceId, ClusterAction.RESTART_SERVICES);
  }

  private void requestServiceAction(HttpRequest request, HttpResponder responder, String clusterId,
                                    String service, ClusterAction action) {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    try {
      clusterService.requestServiceRuntimeAction(clusterId, userId, action, service);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (MissingClusterException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Cluster " + clusterId + " not found.");
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT,
                          "Cluster is not in a state where service actions can be performed.");
    } catch (Exception e) {
      LOG.error("Exception requesting {} on service {} for cluster {}.", action, service, clusterId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Internal error while requesting service action.");
    }
  }

  private JsonObject formatJobPlan(ClusterJob job) throws Exception {
    JsonObject jobJson = new JsonObject();
    jobJson.addProperty("id", job.getJobId());
    jobJson.addProperty("clusterId", job.getClusterId());
    jobJson.addProperty("action", job.getClusterAction().name());
    jobJson.addProperty("currentStage", job.getCurrentStageNumber());

    JsonArray stagesJson = new JsonArray();
    for (Set<String> stage : job.getStagedTasks()) {
      JsonArray stageJson = new JsonArray();
      for (String taskId : stage) {
        ClusterTask task = store.getClusterTask(TaskId.fromString(taskId));

        JsonObject taskJson = new JsonObject();
        taskJson.addProperty("id", task.getTaskId());
        taskJson.addProperty("taskName", task.getTaskName().name());
        taskJson.addProperty("nodeId", task.getNodeId());
        taskJson.addProperty("service", task.getService());

        stageJson.add(taskJson);
      }

      stagesJson.add(stageJson);
    }

    jobJson.add("stages", stagesJson);

    return jobJson;
  }
}
