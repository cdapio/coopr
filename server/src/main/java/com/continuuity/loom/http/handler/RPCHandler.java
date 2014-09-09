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
package com.continuuity.loom.http.handler;

import com.continuuity.http.HttpResponder;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.current.NodePropertiesRequestCodec;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.http.request.NodePropertiesRequest;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

/**
 * Handler for RPCs.
 */
@Path(Constants.API_BASE)
public class RPCHandler extends AbstractAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(RPCHandler.class);
  private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(NodePropertiesRequest.class, new NodePropertiesRequestCodec()).create();
  private final ClusterStoreService clusterStoreService;

  @Inject
  private RPCHandler(TenantStore tenantStore,
                     ClusterStoreService clusterStoreService) {
    super(tenantStore);
    this.clusterStoreService = clusterStoreService;
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
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
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
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
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
    Set<Node> clusterNodes = clusterStoreService.getView(account).getClusterNodes(nodeRequest.getClusterId());
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
        JsonObject nodeProperties = GSON.toJsonTree(node.getProperties()).getAsJsonObject();
        // if the request contains a list of properties, just include those properties
        if (properties.size() > 0) {
          outputProperties = new JsonObject();
          // add all requested node properties
          for (String property : properties) {
            if (nodeProperties.has(property)) {
              outputProperties.add(property, nodeProperties.get(property));
            }
          }
        } else {
          // request did not contain a list of properties, include them all
          outputProperties = nodeProperties;
        }
        output.put(node.getId(), outputProperties);
      }
    }

    responder.sendJson(HttpResponseStatus.OK, output);
  }
}
