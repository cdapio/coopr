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
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterService;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Map;

/**
 * Handler for RPCs.
 */
@Path("/v1/loom")
public class LoomRPCHandler extends LoomAuthHandler {

  private final ClusterStore store;
  private ClusterService clusterService;

  @Inject
  public LoomRPCHandler(ClusterStore store, ClusterService clusterService) {
    this.store = store;
    this.clusterService = clusterService;
  }

  /**
   * Get the cluster status for all clusters readable by the user making the request.
   *
   * @param request The request for cluster statuses.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @POST
  @Path("/getClusterStatuses")
  public void getClusterStatuses(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    // TODO: Improve this logic by using a table join instead of separate calls for cluster and jobId

    List<Cluster> clusters = clusterService.getAllUserClusters(userId);
    if (clusters.size() == 0) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, String.format("No clusters found"));
      return;
    }

    JsonArray response = new JsonArray();

    Map<JobId, Cluster> clusterMap = Maps.newHashMap();
    for (Cluster cluster : clusters) {
      clusterMap.put(JobId.fromString(cluster.getLatestJobId()), cluster);
    }

    Map<JobId, ClusterJob> jobs = store.getClusterJobs(clusterMap.keySet());

    if (jobs.size() == 0) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, String.format("No jobs found for clusters"));
      return;
    }

    for (JobId jobId : jobs.keySet()) {
      response.add(LoomClusterHandler.getClusterResponseJson(clusterMap.get(jobId), jobs.get(jobId)));
    }

    responder.sendJson(HttpResponseStatus.OK, response);
  }
}
