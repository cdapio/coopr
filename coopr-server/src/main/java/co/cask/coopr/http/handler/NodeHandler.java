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
package co.cask.coopr.http.handler;

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.scheduler.task.NodeService;
import co.cask.coopr.store.node.NodeStore;
import co.cask.coopr.store.node.NodeStoreService;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Handler for performing node operations.
 */
@Path(Constants.API_BASE + "/nodes")
public class NodeHandler extends AbstractAuthHandler {
  private static final Logger LOG = LoggerFactory.getLogger(NodeHandler.class);
  private final NodeStoreService nodeStoreService;
  private final NodeStore nodeStore;
  private final Gson gson;

  /**
   * Initializes a new instance of a NodeHandler.
   */
  @Inject
  private NodeHandler(TenantStore tenantStore, NodeService nodeService,
                      NodeStoreService nodeStoreService, Gson gson) {
    super(tenantStore);
    this.nodeStoreService = nodeStoreService;
    this.nodeStore = this.nodeStoreService.getSystemView();
    this.gson = gson;
  }

  /**
   * Get all nodes visible to the user.
   * @param request Request for clusters.
   * @param responder Responder for sending the response.
   */
  @GET
  public void getNodes(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    Set<Node> nodes;
    try {
      nodes = nodeStoreService.getView(account).getAllNodes();
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting nodes.");
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, nodes);
  }

  /**
   * Get a node visible to the user.
   * @param request Request for clusters.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/{node-id}")
  public void getNode(HttpRequest request, HttpResponder responder, @PathParam("node-id") String nodeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    Node node;
    try {
      node = nodeStoreService.getView(account).getNode(nodeId);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting nodes.");
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, node);
  }

  @POST
  public void createNode(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);

    try {
      Node node = gson.fromJson(reader, Node.class);

      if (node.getId() == null || node.getId().isEmpty()) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "Node ID is not set");
        return;
      }
      nodeStoreService.getView(account).writeNode(node);
      responder.sendStatus(HttpResponseStatus.CREATED);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "Exception creating node.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception creating node.");
    } catch (JsonIOException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Exception reading node from body.");
    } catch (JsonSyntaxException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Exception reading node from body.");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.warn("Exception while closing request reader", e);
      }
    }
  }

  @PUT
  @Path("/{node-id}")
  public void updateNode(HttpRequest request, HttpResponder responder, @PathParam("node-id") String nodeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);

    try {
      Node node = gson.fromJson(reader, Node.class);
      if (node.getId() == null || node.getId().isEmpty()) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "Node ID is not set");
        return;
      }

      if (node.getId().equals(nodeId)) {
        nodeStoreService.getView(account).writeNode(node);
        responder.sendStatus(HttpResponseStatus.NO_CONTENT);
      } else {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "Node ID in body does not match Node ID in URI path");
      }
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "Exception updating node.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception updating node.");
    } catch (JsonIOException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Exception reading node from body.");
    } catch (JsonSyntaxException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Exception reading node from body.");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.warn("Exception while closing request reader", e);
      }
    }
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
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "Exception deleting node.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception deleting node.");
    }

    responder.sendStatus(HttpResponseStatus.NO_CONTENT);
  }
}
