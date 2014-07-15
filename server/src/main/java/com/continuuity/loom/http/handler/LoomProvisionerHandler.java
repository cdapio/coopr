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

import com.continuuity.http.AbstractHttpHandler;
import com.continuuity.http.HttpResponder;
import com.continuuity.loom.provisioner.Provisioner;
import com.continuuity.loom.provisioner.ProvisionerHeartbeat;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Handles requests from provisioners to register themselves, send heartbeats, and delete themselves.
 */
@Path("/v1/provisioners")
public final class LoomProvisionerHandler extends AbstractHttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger(LoomProvisionerHandler.class);

  private final Gson gson;
  private final TenantProvisionerService tenantProvisionerService;

  @Inject
  private LoomProvisionerHandler(TenantProvisionerService tenantProvisionerService, Gson gson) {
    this.gson = gson;
    this.tenantProvisionerService = tenantProvisionerService;
  }

  /**
   * Write a provisioner.
   *
   * @param request The request to write a provisioner.
   * @param responder Responder to send the response.
   */
  @PUT
  @Path("/{provisioner-id}")
  public void writeProvisioner(HttpRequest request, HttpResponder responder,
                               @PathParam("provisioner-id") String provisionerId) {
    Provisioner provisioner;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      provisioner = gson.fromJson(reader, Provisioner.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request: " + e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request.");
      return;
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.error("Exception closing reader", e);
      }
    }

    if (!provisioner.getId().equals(provisionerId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "id in body and path do not match.");
      return;
    }

    try {
      LOG.debug("Received request to write provisioner {} with {} capacity.",
                provisioner.getId(), provisioner.getCapacityTotal());
      tenantProvisionerService.writeProvisioner(provisioner);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception writing provisioner {}", provisionerId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing provisioner " + provisionerId);
    }
  }

  /**
   * Heartbeat containing provisioner usage information and indicating that it is still alive.
   *
   * @param request The request to write a provisioner.
   * @param responder Responder to send the response.
   */
  @POST
  @Path("/{provisioner-id}/heartbeat")
  public void handleHeartbeat(HttpRequest request, HttpResponder responder,
                             @PathParam("provisioner-id") String provisionerId) {
    ProvisionerHeartbeat heartbeat;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      heartbeat = gson.fromJson(reader, ProvisionerHeartbeat.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request: " + e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request.");
      return;
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.error("Exception closing reader", e);
      }
    }

    try {
      LOG.trace("Received heartbeat for provisioner {}. heartbeat = {}", provisionerId, heartbeat);
      tenantProvisionerService.handleHeartbeat(provisionerId, heartbeat);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception writing provisioner {}", provisionerId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing provisioner " + provisionerId);
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Provisioner " + provisionerId + " not found.");
    }
  }

  /**
   * Delete a provisioner.
   *
   * @param request The request to delete a provisioner.
   * @param responder Responder to send the response.
   */
  @DELETE
  @Path("/{provisioner-id}")
  public void deleteProvisioner(HttpRequest request, HttpResponder responder,
                                @PathParam("provisioner-id") String provisionerId) {
    try {
      LOG.debug("Received request to delete provisioner {}.", provisionerId);
      tenantProvisionerService.deleteProvisioner(provisionerId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception writing tenant info for provisioner {}", provisionerId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing tenant info for provisioner " + provisionerId);
    }
  }
}
