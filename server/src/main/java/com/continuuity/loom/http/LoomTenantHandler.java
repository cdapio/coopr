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
import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.store.IdService;
import com.continuuity.loom.store.TenantStore;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
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

/**
 * Handler for performing tenant operations.
 */
@Path("/v1/tenants")
public class LoomTenantHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomTenantHandler.class);
  private static final Gson GSON = new JsonSerde().getGson();

  private final TenantStore store;
  private final IdService idService;

  @Inject
  private LoomTenantHandler(TenantStore store, IdService idService) {
    this.store = store;
    this.idService = idService;
  }

  /**
   * Get all tenants.
   *
   * @param request Request for tenants.
   * @param responder Responder for sending the response.
   */
  @GET
  public void getAllTenants(HttpRequest request, HttpResponder responder) {
    if (!isSuperAdmin(request)) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, store.getAllTenants());
    } catch (IOException e) {
      LOG.error("Exception while getting all tenants.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting all tenants.");
    }
  }

  /**
   * Get a specific tenant.
   *
   * @param request Request for a tenant.
   * @param responder Responder for sending the response.
   * @param tenantId Id of the tenant to get.
   */
  @GET
  @Path("/{tenant-id}")
  public void getTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
    if (!isSuperAdmin(request)) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      Tenant tenant = store.getTenant(Long.parseLong(tenantId));
      if (tenant == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "tenant " + tenantId + " not found");
        return;
      }
      responder.sendJson(HttpResponseStatus.OK, tenant, new TypeToken<Tenant>() {}.getType());
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid tenant id " + tenantId);
    } catch (IOException e) {
      LOG.error("Exception while getting tenant {}." , tenantId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception while getting tenant " + tenantId);
    }
  }

  /**
   * Add a tenant, optionally bootstrapping it with entities from the superadmin.
   *
   * @param request Request for adding a tenant.
   * @param responder Responder for sending the response.
   */
  @POST
  public void createTenant(HttpRequest request, HttpResponder responder) {
    if (!isSuperAdmin(request)) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    Tenant requestedTenant;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      AddTenantRequest addRequest = GSON.fromJson(reader, AddTenantRequest.class);
      requestedTenant = addRequest.getTenant();
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input: " + e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input");
      return;
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.warn("Exception while closing request reader", e);
      }
    }
    if (requestedTenant == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input");
      return;
    }

    try {
      Tenant tenant = new Tenant(requestedTenant.getName(), idService.getNewTenantId(), requestedTenant.getWorkers(),
                                 requestedTenant.getMaxClusters(), requestedTenant.getMaxNodes());
      store.writeTenant(tenant);
      responder.sendJson(HttpResponseStatus.OK, ImmutableMap.<String, Long>of("id", tenant.getId()));
    } catch (IOException e) {
      LOG.error("Exception adding tenant.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding tenant");
    }
  }

  /**
   * Write the specified tenant.
   *
   * @param request Request for writing a tenant.
   * @param responder Responder for sending the response.
   * @param tenantId Id of the tenant to write.
   */
  @PUT
  @Path("/{tenant-id}")
  public void writeTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
    if (!isSuperAdmin(request)) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    long id;
    try {
      id = Long.parseLong(tenantId);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid tenant id " + tenantId);
      return;
    }

    Tenant tenant = null;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      tenant = GSON.fromJson(reader, Tenant.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input: " + e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input");
      return;
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.warn("Exception while closing request reader", e);
      }
    }

    if (tenant.getId() != id) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "tenant id does not match id in path.");
      return;
    }

    try {
      // TODO: validate that there is enough overall capacity to accomodate this request
      store.writeTenant(tenant);
      // TODO: send requests to add/remove tenant workers from provisioners
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception while setting workers for tenant {}", tenantId);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception setting tenant workers.");
    }
  }

  /**
   * Delete the specified tenant.
   *
   * @param request Request to delete a tenant.
   * @param responder Responder for sending the response.
   * @param tenantId Id of the tenant to delete.
   */
  @DELETE
  @Path("/{tenant-id}")
  public void deleteTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-id") String tenantId) {
    if (!isSuperAdmin(request)) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      store.deleteTenant(Long.parseLong(tenantId));
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid tenant id " + tenantId);
    } catch (IOException e) {
      LOG.error("Exception while deleting tenant {}", tenantId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception while deleting tenant " + tenantId);
    }
  }

  private boolean isSuperAdmin(HttpRequest request) {
    String tenant = request.getHeader(Constants.TENANT_HEADER);
    String user = request.getHeader(Constants.USER_HEADER);
    // TODO: authenticate
    String apiKey = request.getHeader(Constants.API_KEY_HEADER);

    return tenant != null && tenant.equals(Constants.SUPERADMIN_TENANT) &&
      user != null && user.equals(Constants.SUPERADMIN_USER);
  }
}
