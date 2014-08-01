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
import com.continuuity.loom.admin.AutomatorType;
import com.continuuity.loom.admin.ProviderType;
import com.continuuity.loom.admin.TenantSpecification;
import com.continuuity.loom.provisioner.CapacityException;
import com.continuuity.loom.provisioner.Provisioner;
import com.continuuity.loom.provisioner.QuotaException;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
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
import java.lang.reflect.Type;

/**
 * Handler for performing tenant operations.
 */
@Path("/v1")
public class LoomSuperadminHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomSuperadminHandler.class);

  private final Gson gson;
  private final EntityStoreService entityStoreService;
  private final TenantProvisionerService tenantProvisionerService;

  @Inject
  private LoomSuperadminHandler(TenantStore store, TenantProvisionerService tenantProvisionerService,
                                EntityStoreService entityStoreService, Gson gson) {
    super(store);
    this.gson = gson;
    this.entityStoreService = entityStoreService;
    this.tenantProvisionerService = tenantProvisionerService;
  }

  /**
   * Get all tenants.
   *
   * @param request Request for tenants.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/tenants")
  public void getAllTenants(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, tenantProvisionerService.getAllTenantSpecifications());
    } catch (IOException e) {
      LOG.error("Exception while getting all tenants.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting all tenants.");
    }
  }

  /**
   * Get a specific tenant by name.
   *
   * @param request Request for a tenant.
   * @param responder Responder for sending the response.
   * @param tenantName Name of the tenant to get.
   */
  @GET
  @Path("/tenants/{tenant-name}")
  public void getTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-name") String tenantName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      TenantSpecification tenantSpec = tenantProvisionerService.getTenantSpecification(tenantName);
      if (tenantSpec == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "tenant " + tenantName + " not found");
        return;
      }
      responder.sendJson(HttpResponseStatus.OK, tenantSpec);
    } catch (IOException e) {
      LOG.error("Exception while getting tenant {}." , tenantName, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception while getting tenant " + tenantName);
    }
  }

  /**
   * Add a tenant, optionally bootstrapping it with entities from the superadmin.
   *
   * @param request Request for adding a tenant.
   * @param responder Responder for sending the response.
   */
  @POST
  @Path("/tenants")
  public void createTenant(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    TenantSpecification requestedFields;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      requestedFields = gson.fromJson(reader, TenantSpecification.class);
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
    if (requestedFields == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input");
      return;
    }

    try {
      String name = requestedFields.getName();
      if (tenantProvisionerService.getTenantSpecification(name) != null) {
        responder.sendError(HttpResponseStatus.CONFLICT, "Tenant " + name + " already exists.");
        return;
      }
      tenantProvisionerService.writeTenantSpecification(requestedFields);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception adding tenant.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding tenant.");
    } catch (CapacityException e) {
      LOG.info("Could not add tenant due to lack of free workers.");
      responder.sendError(HttpResponseStatus.CONFLICT, "Not enough capacity to add tenant.");
    } catch (QuotaException e) {
      // should not happen
      responder.sendError(HttpResponseStatus.CONFLICT, e.getMessage());
    }
  }

  /**
   * Write the specified tenant.
   *
   * @param request Request for writing a tenant.
   * @param responder Responder for sending the response.
   * @param tenantName Name of the tenant to write.
   */
  @PUT
  @Path("/tenants/{tenant-name}")
  public void writeTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-name") String tenantName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    TenantSpecification tenantSpecification = null;
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      tenantSpecification = gson.fromJson(reader, TenantSpecification.class);
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

    if (tenantSpecification.getName() == null || !tenantSpecification.getName().equals(tenantName)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "tenant name in body does not match name in path.");
      return;
    }

    try {
      tenantProvisionerService.writeTenantSpecification(tenantSpecification);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception while writing tenant {}", tenantName);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception setting tenant workers.");
    } catch (CapacityException e) {
      LOG.error("Could not edit tenant {} due to lack of free workers.", tenantName);
      responder.sendError(HttpResponseStatus.CONFLICT, "Not enough capacity to update tenant.");
    } catch (QuotaException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, e.getMessage());
    }
  }

  /**
   * Delete the specified tenant.
   *
   * @param request Request to delete a tenant.
   * @param responder Responder for sending the response.
   * @param tenantName Name of the tenant to delete.
   */
  @DELETE
  @Path("/tenants/{tenant-name}")
  public void deleteTenant(HttpRequest request, HttpResponder responder, @PathParam("tenant-name") String tenantName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      tenantProvisionerService.deleteTenantByName(tenantName);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, e.getMessage());
    } catch (IOException e) {
      LOG.error("Exception while deleting tenant {}", tenantName, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception while deleting tenant " + tenantName);
    }
  }

  /**
   * Get the specified provisioner.
   *
   * @param request Request to get a provisioner.
   * @param responder Responder for sending the response.
   * @param provisionerId Id of the provisioner to get.
   */
  @GET
  @Path("/provisioners/{provisioner-id}")
  public void getProvisioner(HttpRequest request, HttpResponder responder,
                             @PathParam("provisioner-id") String provisionerId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      Provisioner provisioner = tenantProvisionerService.getProvisioner(provisionerId);
      if (provisioner == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "Provisioner " + provisionerId + " not found.");
      } else {
        responder.sendJson(HttpResponseStatus.OK, provisioner);
      }
    } catch (IOException e) {
      LOG.error("Exception while getting provisioners", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception while getting provisioners");
    }
  }

  /**
   * Get all provisioners in the system.
   *
   * @param request Request to get all provisioners.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/provisioners")
  public void getProvisioners(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendStatus(HttpResponseStatus.FORBIDDEN);
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, tenantProvisionerService.getAllProvisioners());
    } catch (IOException e) {
      LOG.error("Exception while getting provisioners", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception while getting provisioners");
    }
  }
  /**
   * Delete a specific {@link com.continuuity.loom.admin.ProviderType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a provider type.
   * @param responder Responder for sending the response.
   * @param providertypeId Id of the provider type to delete.
   */
  @DELETE
  @Path("/loom/providertypes/{providertype-id}")
  public void deleteProviderType(HttpRequest request, HttpResponder responder,
                                 @PathParam("providertype-id") String providertypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be superadmin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteProviderType(providertypeId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception deleting provider type " + providertypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete provider type.");
    }
  }

  /**
   * Delete a specific {@link com.continuuity.loom.admin.AutomatorType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete an automator type.
   * @param responder Responder for sending the response.
   * @param automatortypeId Id of the automator type to delete.
   */
  @DELETE
  @Path("/loom/automatortypes/{automatortype-id}")
  public void deleteAutomatorType(HttpRequest request, HttpResponder responder,
                                  @PathParam("automatortype-id") String automatortypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be superadmin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteAutomatorType(automatortypeId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception deleting automator type " + automatortypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete automator type.");
    }
  }

  /**
   * Writes a {@link com.continuuity.loom.admin.ProviderType}. User must be admin or a 403 is returned.
   * If the name in the path does not match the name in the put body, a 400 is returned.
   *
   * @param request Request to write provider type.
   * @param responder Responder to send response.
   * @param providertypeId Id of the provider type to write.
   */
  @PUT
  @Path("/loom/providertypes/{providertype-id}")
  public void putProviderType(HttpRequest request, HttpResponder responder,
                              @PathParam("providertype-id") String providertypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be superadmin.");
      return;
    }

    ProviderType providerType = getEntityFromRequest(request, responder, ProviderType.class);
    if (providerType == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!providerType.getName().equals(providertypeId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between provider type name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(account).writeProviderType(providerType);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing provider type " + providertypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write provider type.");
    }
  }

  /**
   * Writes a {@link com.continuuity.loom.admin.AutomatorType}. User must be admin or a 403 is returned.
   * If the name in the path does not match the name in the put body, a 400 is returned.
   *
   * @param request Request to write provider type.
   * @param responder Responder to send response.
   * @param automatortypeId Id of the provider type to write.
   */
  @PUT
  @Path("/loom/automatortypes/{automatortype-id}")
  public void putAutomatorType(HttpRequest request, HttpResponder responder,
                               @PathParam("automatortype-id") String automatortypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be superadmin.");
      return;
    }

    AutomatorType automatorType = getEntityFromRequest(request, responder, AutomatorType.class);
    if (automatorType == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!automatorType.getName().equals(automatortypeId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between automator type name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(account).writeAutomatorType(automatorType);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing automator type " + automatortypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write automator type.");
    }
  }

  private <T> T getEntityFromRequest(HttpRequest request, HttpResponder responder, Type tClass) {
    T result = null;
    try {
      Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
      try {
        result = gson.fromJson(reader, tClass);
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          LOG.warn("Exception while closing request reader", e);
        }
      }
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input: " + e.getMessage());
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid input");
    }
    return result;
  }
}
