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
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterService;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for RPCs.
 */
@Path("/v1/loom")
public class LoomRPCHandler extends LoomAuthHandler {
  private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(NodePropertiesRequest.class, new NodePropertiesRequestCodec()).create();
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

  /**
   * Get properties of nodes from a specific cluster visible to the user. POST body is a JSON object that
   * must contain "clusterId", and may contain "properties" and "services". The "properties" key maps to an array
   * of node properties like "ipaddress" and "hostname" to return in the response. The "services" key maps to an
   * array of service names, indicating that all nodes returned by have all services given in the array. The response
   * is a JSON object with node ids as keys and JSON objects as values, where the value contains the properties passed
   * in, or all properties if none were passed in.
   *
   * @param request Request for node properties in a cluster.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @POST
  @Path("/getNodeProperties")
  public void getNodeProperties(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    NodePropertiesRequest nodeRequest;
    try {
      nodeRequest = GSON.fromJson(reader, NodePropertiesRequest.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid request body. Must be a valid JSON Object.");
      return;
    }

    Map<String, JsonObject> output = Maps.newHashMap();
    Set<Node> clusterNodes = clusterService.getClusterNodes(nodeRequest.getClusterId(), userId);
    Set<String> properties = nodeRequest.getProperties();
    Set<String> requiredServices = nodeRequest.getServices();

    for (Node node : clusterNodes) {
      Set<String> nodeServices = Sets.newHashSet();
      for (Service service : node.getServices()) {
        nodeServices.add(service.getName());
      }

      // if the node has all services needed
      if (nodeServices.containsAll(requiredServices)) {
        JsonObject outputProperties;
        // if the request contains a list of properties, just include those properties
        if (properties.size() > 0) {
          outputProperties = new JsonObject();
          JsonObject nodeProperties = node.getProperties();
          // add all requested node properties
          for (String property : properties) {
            if (nodeProperties.has(property)) {
              outputProperties.add(property, nodeProperties.get(property));
            }
          }
        } else {
          // request did not contain a list of properties, include them all
          outputProperties = node.getProperties();
        }
        output.put(node.getId(), outputProperties);
      }
    }

    responder.sendJson(HttpResponseStatus.OK, output);
  }
}
