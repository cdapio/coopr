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

import com.continuuity.http.BodyConsumer;
import com.continuuity.http.HttpResponder;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.provisioner.plugin.ResourceMeta;
import com.continuuity.loom.provisioner.plugin.ResourceService;
import com.continuuity.loom.provisioner.plugin.ResourceStatus;
import com.continuuity.loom.provisioner.plugin.ResourceType;
import com.continuuity.loom.provisioner.plugin.Type;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for plugin resource related operations, such as uploading resources, staging, and unstaging resources,
 * and syncing resources.
 */
@Path("/v1/loom")
public class LoomPluginHandler extends LoomAuthHandler {
  private final Gson gson;
  private final ResourceService resourceService;

  @Inject
  private LoomPluginHandler(TenantStore tenantStore, ResourceService resourceService, Gson gson) {
    super(tenantStore);
    this.resourceService = resourceService;
    this.gson = gson;
  }

  @POST
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}")
  public BodyConsumer uploadAutomatorTypeModule(HttpRequest request, HttpResponder responder,
                                                @PathParam("automatortype-id") String automatortypeId,
                                                @PathParam("resource-type") String resourceType,
                                                @PathParam("resource-name") String resourceName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return null;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return null;
    }

    return uploadResource(responder, account, Type.AUTOMATOR, automatortypeId, resourceType, resourceName);
  }

  @POST
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}")
  public BodyConsumer uploadProviderTypeModule(HttpRequest request, HttpResponder responder,
                                               @PathParam("providertype-id") String providertypeId,
                                               @PathParam("resource-type") String resourceType,
                                               @PathParam("resource-name") String resourceName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return null;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return null;
    }

    return uploadResource(responder, account, Type.PROVIDER, providertypeId, resourceType, resourceName);
  }

  @POST
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}/versions/{version}/stage")
  public void stageAutomatorTypeModule(HttpRequest request, HttpResponder responder,
                                       @PathParam("automatortype-id") String automatortypeId,
                                       @PathParam("resource-type") String resourceType,
                                       @PathParam("resource-name") String resourceName,
                                       @PathParam("version") String version) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }


    stageResource(responder, account, Type.AUTOMATOR,
                  automatortypeId, resourceType, resourceName, version);
  }

  @POST
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}/versions/{version}/stage")
  public void stageProviderTypeModule(HttpRequest request, HttpResponder responder,
                                      @PathParam("providertype-id") String providertypeId,
                                      @PathParam("resource-type") String resourceType,
                                      @PathParam("resource-name") String resourceName,
                                      @PathParam("version") String version) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    stageResource(responder, account, Type.PROVIDER,
                  providertypeId, resourceType, resourceName, version);
  }

  @POST
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}/versions/{version}/unstage")
  public void unstageAutomatorTypeModule(HttpRequest request, HttpResponder responder,
                                         @PathParam("automatortype-id") String automatortypeId,
                                         @PathParam("resource-type") String resourceType,
                                         @PathParam("resource-name") String resourceName,
                                         @PathParam("version") String version) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    unstageResource(responder, account, Type.AUTOMATOR, automatortypeId, resourceType, resourceName, version);
  }

  @POST
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}/versions/{version}/unstage")
  public void unstageProviderTypeModule(HttpRequest request, HttpResponder responder,
                                        @PathParam("providertype-id") String providertypeId,
                                        @PathParam("resource-type") String resourceType,
                                        @PathParam("resource-name") String resourceName,
                                        @PathParam("version") String version) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    unstageResource(responder, account, Type.PROVIDER, providertypeId, resourceType, resourceName, version);
  }

  @GET
  @Path("/automatortypes/{automatortype-id}/{resource-type}")
  public void getAllAutomatorTypeModules(HttpRequest request, HttpResponder responder,
                                         @PathParam("automatortype-id") String automatortypeId,
                                         @PathParam("resource-type") String resourceType) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    getResources(request, responder, account, Type.AUTOMATOR, automatortypeId, resourceType);
  }

  @GET
  @Path("/providertypes/{providertype-id}/{resource-type}")
  public void getAllProviderTypeModules(HttpRequest request, HttpResponder responder,
                                        @PathParam("providertype-id") String providertypeId,
                                        @PathParam("resource-type") String resourceType) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    getResources(request, responder, account, Type.PROVIDER, providertypeId, resourceType);
  }

  @GET
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}")
  public void getAllAutomatorTypeResourceVersions(HttpRequest request, HttpResponder responder,
                                                  @PathParam("automatortype-id") String automatortypeId,
                                                  @PathParam("resource-type") String resourceType,
                                                  @PathParam("resource-name") String resourceName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    getResources(request, responder, account, Type.AUTOMATOR, automatortypeId, resourceType, resourceName);
  }

  @GET
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}")
  public void getAllProviderTypeResourceVersions(HttpRequest request, HttpResponder responder,
                                                 @PathParam("providertype-id") String providertypeId,
                                                 @PathParam("resource-type") String resourceType,
                                                 @PathParam("resource-name") String resourceName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    getResources(request, responder, account, Type.PROVIDER, providertypeId, resourceType, resourceName);
  }

  @DELETE
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}")
  public void deleteAutomatorTypeResource(HttpRequest request, HttpResponder responder,
                                          @PathParam("automatortype-id") String automatortypeId,
                                          @PathParam("resource-type") String resourceType,
                                          @PathParam("resource-name") String resourceName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    deleteResource(responder, account, Type.AUTOMATOR, automatortypeId, resourceType, resourceName);
  }

  @DELETE
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}")
  public void deleteProviderTypeResource(HttpRequest request, HttpResponder responder,
                                         @PathParam("providertype-id") String providertypeId,
                                         @PathParam("resource-type") String resourceType,
                                         @PathParam("resource-name") String resourceName) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    deleteResource(responder, account, Type.PROVIDER, providertypeId, resourceType, resourceName);
  }

  @DELETE
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}/versions/{version}")
  public void deleteAutomatorTypeResourceVersion(HttpRequest request, HttpResponder responder,
                                                 @PathParam("automatortype-id") String automatortypeId,
                                                 @PathParam("resource-type") String resourceType,
                                                 @PathParam("resource-name") String resourceName,
                                                 @PathParam("version") String version) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    deleteResource(responder, account, Type.AUTOMATOR, automatortypeId, resourceType, resourceName, version);
  }

  @DELETE
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}/versions/{version}")
  public void deleteProviderTypeResourceVersion(HttpRequest request, HttpResponder responder,
                                                @PathParam("providertype-id") String providertypeId,
                                                @PathParam("resource-type") String resourceType,
                                                @PathParam("resource-name") String resourceName,
                                                @PathParam("version") String version) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    deleteResource(responder, account, Type.PROVIDER, providertypeId, resourceType, resourceName, version);
  }

  @POST
  @Path("/sync")
  public void syncPlugins(HttpRequest request, HttpResponder responder) {
    // TODO: implement
    responder.sendError(HttpResponseStatus.NOT_IMPLEMENTED, "not implemented yet");
  }

  private BodyConsumer uploadResource(HttpResponder responder, Account account, Type type,
                                      String pluginName, String resourceType,
                                      String resourceName) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      return resourceService.createResourceBodyConsumer(account, pluginResourceType, resourceName, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error uploading module");
      return null;
    }
  }

  private void stageResource(HttpResponder responder, Account account, Type type,
                             String pluginName, String resourceType, String resourceName, String versionStr) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      int version = Integer.parseInt(versionStr);
      resourceService.stage(account, pluginResourceType, resourceName, version);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error activating module version.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Provider type module not found.");
    }
  }

  private void unstageResource(HttpResponder responder, Account account, Type type,
                               String pluginName, String resourceType, String resourceName, String versionStr) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      int version = Integer.parseInt(versionStr);
      resourceService.unstage(account, pluginResourceType, resourceName, version);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error activating module version.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Provider type module not found.");
    }
  }

  private void getResources(HttpRequest request, HttpResponder responder, Account account,
                            Type type, String pluginName, String resourceType) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      ResourceStatus statusFilter = getStatusParam(request);
      responder.sendJson(HttpResponseStatus.OK,
                         resourceService.getAll(account, pluginResourceType, statusFilter),
                         new TypeToken<Map<String, Set<ResourceMeta>>>() {}.getType(),
                         gson);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid status filter.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error getting modules.");
    }
  }

  private void getResources(HttpRequest request, HttpResponder responder, Account account,
                            Type type, String pluginName, String resourceType, String resourceName) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      ResourceStatus statusFilter = getStatusParam(request);
      responder.sendJson(HttpResponseStatus.OK,
                         resourceService.getAll(account, pluginResourceType, resourceName, statusFilter),
                         new TypeToken<Set<ResourceMeta>>() {}.getType(),
                         gson);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid status filter.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error getting modules.");
    }
  }

  private void deleteResource(HttpResponder responder, Account account, Type type,
                              String pluginName, String resourceType, String resourceName, String versionStr) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      int version = Integer.parseInt(versionStr);
      resourceService.delete(account, pluginResourceType, resourceName, version);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, "Resource not in a deletable state.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error activating module version.");
    }
  }

  private void deleteResource(HttpResponder responder, Account account, Type type,
                              String pluginName, String resourceType, String resourceName) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      resourceService.delete(account, pluginResourceType, resourceName);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, "Resource not in a deletable state.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error activating module version.");
    }
  }

  private ResourceStatus getStatusParam(HttpRequest request) throws IllegalArgumentException {
    Map<String, List<String>> queryParams = new QueryStringDecoder(request.getUri()).getParameters();
    return queryParams.containsKey("status") ?
      ResourceStatus.valueOf(queryParams.get("status").get(0).toUpperCase()) : null;
  }
}
