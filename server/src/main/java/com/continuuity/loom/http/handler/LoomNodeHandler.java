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
import com.continuuity.loom.scheduler.task.NodeService;
import com.continuuity.loom.store.node.NodeStore;
import com.continuuity.loom.store.node.NodeStoreService;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
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
import java.util.Set;

/**
 * Handler for performing node operations.
 */
@Path("/v1/loom/nodes")
public class LoomNodeHandler extends LoomAuthHandler {
  private static final Logger LOG = LoggerFactory.getLogger(LoomNodeHandler.class);
  //  private final NodeService clusterService;
  private final NodeStoreService nodeStoreService;
  private final NodeStore nodeStore;
  private final Gson gson;

  /**
   * Initializes a new instance of a LoomNodeHandler.
   */
  @Inject
  public LoomNodeHandler(TenantStore tenantStore, NodeService nodeService, NodeStoreService nodeStoreService,
                         Gson gson) {
    super(tenantStore);
    this.nodeStoreService = nodeStoreService;
    this.nodeStore = this.nodeStoreService.getSystemView();
    this.gson = gson;
  }

  /**
   * Get all noes visible to the user.
   * @param request Request for clusters.
   * @param responder Responder for sending the response.
   */
  @GET
  public void getNodes(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    Set<Node> nodes = null;
    try {
      nodes = nodeStoreService.getView(account).getAllNodes();
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting nodes.");
    }

    JsonArray jsonArray = new JsonArray();
    for (Node node : nodes) {
      JsonObject obj = new JsonObject();
      obj.addProperty("id", node.getId());
      obj.addProperty("clusterId", node.getClusterId());
      //      obj.add("properties", node.getProperties());
      jsonArray.add(obj);
    }

    responder.sendJson(HttpResponseStatus.OK, jsonArray);
  }

  @POST
  public void createNodes(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    Set<Node> nodes = gson.fromJson(reader, new TypeToken<Set<Node>>(){}.getType());
    try {
      nodeStoreService.getView(account).writeNodes(nodes);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception creating nodes.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception creating nodes.");
    }

    responder.sendStatus(HttpResponseStatus.CREATED);
  }

  @PUT
  public void updateNode(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    Node node = gson.fromJson(reader, Node.class);
    try {
      nodeStoreService.getView(account).writeNode(node);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception updating node.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception updating node.");
    }

    responder.sendStatus(HttpResponseStatus.NO_CONTENT);
  }

  @DELETE
  @Path("/{node-id}")
  public void deleteNode(HttpRequest request, HttpResponder responder, @PathParam("node-id") String nodeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    try {
      nodeStoreService.getView(account).deleteNode(nodeId);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception deleting node.");
    }

    responder.sendStatus(HttpResponseStatus.NO_CONTENT);
  }
}
