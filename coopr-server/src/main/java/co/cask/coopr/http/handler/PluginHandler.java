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
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.provisioner.plugin.PluginType;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceService;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.spec.plugin.AbstractPluginSpecification;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.http.BodyConsumer;
import co.cask.http.HttpResponder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Handler for plugin resource related operations, such as uploading resources, staging, and recalling resources,
 * and syncing resources. Only a tenant admin can access these APIs.
 */
@Path(Constants.API_BASE + "/plugins")
public class PluginHandler extends AbstractAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(PluginHandler.class);
  private final Gson gson;
  private final ResourceService resourceService;
  private final EntityStoreService entityStoreService;
  private final TenantProvisionerService tenantProvisionerService;

  @Inject
  private PluginHandler(TenantStore tenantStore,
                        ResourceService resourceService,
                        EntityStoreService entityStoreService,
                        TenantProvisionerService tenantProvisionerService,
                        Gson gson) {
    super(tenantStore);
    this.resourceService = resourceService;
    this.entityStoreService = entityStoreService;
    this.tenantProvisionerService = tenantProvisionerService;
    this.gson = gson;
  }

  /**
   * Add an automator type resource. For example, uploading a resource named "hadoop" of resource type "cookbook"
   * for the automator type "chef-solo".
   *
   * @param request Request containing the resource contents
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type that is having a resource uploaded
   * @param resourceType Type of resource that is being uploaded for the specified automator type
   * @param resourceName Name of the resource being uploaded
   * @return Body consumer for streaming the contents of the resource to the plugin store
   */
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

    return uploadResource(responder, account, PluginType.AUTOMATOR, automatortypeId, resourceType, resourceName);
  }

  /**
   * Add a provider type resource. For example, uploading a resource named "dev" of resource type "keys"
   * for the provider type "openstack".
   *
   * @param request Request containing the resource contents
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type that is having a resource uploaded
   * @param resourceType Type of resource that is being uploaded for the specified provider type
   * @param resourceName Name of the resource being uploaded
   * @return Body consumer for streaming the contents of the resource to the plugin store
   */
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

    return uploadResource(responder, account, PluginType.PROVIDER, providertypeId, resourceType, resourceName);
  }

  /**
   * Stage a particular resource version, which means that version of the resource will get pushed to provisioners
   * on the next sync call. Staging one version recalls other versions of the resource.
   *
   * @param request Request to stage a resource
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type that has the resource
   * @param resourceType Type of resource to stage
   * @param resourceName Name of the resource to stage
   * @param version Version of the resource to stage
   */
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


    stageResource(responder, account, PluginType.AUTOMATOR,
                  automatortypeId, resourceType, resourceName, version);
  }

  /**
   * Stage a particular resource version, which means that version of the resource will get pushed to provisioners
   * on the next sync call. Staging one version recalls other versions of the resource.
   *
   * @param request Request to stage a resource
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type that has the resource
   * @param resourceType Type of resource to stage
   * @param resourceName Name of the resource to stage
   * @param version Version of the resource to stage
   */
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

    stageResource(responder, account, PluginType.PROVIDER,
                  providertypeId, resourceType, resourceName, version);
  }

  /**
   * Recall a particular resource version, which means that version of the resource will get removed from provisioners
   * on the next sync call.
   *
   * @param request Request to recall a resource
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type that has the resource
   * @param resourceType Type of resource to recall
   * @param resourceName Name of the resource to recall
   * @param version Version of the resource to recall
   */
  @POST
  @Path("/automatortypes/{automatortype-id}/{resource-type}/{resource-name}/versions/{version}/recall")
  public void recallAutomatorTypeModule(HttpRequest request, HttpResponder responder,
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

    recallResource(responder, account, PluginType.AUTOMATOR, automatortypeId, resourceType, resourceName, version);
  }


  /**
   * Recall a particular resource version, which means that version of the resource will get removed from provisioners
   * on the next sync call.
   *
   * @param request Request to recall a resource
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type that has the resource
   * @param resourceType Type of resource to recall
   * @param resourceName Name of the resource to recall
   * @param version Version of the resource to recall
   */
  @POST
  @Path("/providertypes/{providertype-id}/{resource-type}/{resource-name}/versions/{version}/recall")
  public void recallProviderTypeModule(HttpRequest request, HttpResponder responder,
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

    recallResource(responder, account, PluginType.PROVIDER, providertypeId, resourceType, resourceName, version);
  }

  /**
   * Get a mapping of all resources of the given type for the given automator type. Request can optionally contain
   * a 'status' http param whose value is one of 'active', 'inactive', 'staged', or 'recalled' to filter the results
   * to only contain resource that have the given status.
   *
   * @param request Request to get resources of the given type
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type that has the resources
   * @param resourceType Type of resources to get
   */
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

    getResources(request, responder, account, PluginType.AUTOMATOR, automatortypeId, resourceType);
  }

  /**
   * Get a mapping of all resources of the given type for the given provider type. Request can optionally contain
   * a 'status' http param whose value is one of 'active', 'inactive', 'staged', or 'recalled' to filter the results
   * to only contain resource that have the given status.
   *
   * @param request Request to get resources of the given type
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type that has the resources
   * @param resourceType Type of resources to get
   */
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

    getResources(request, responder, account, PluginType.PROVIDER, providertypeId, resourceType);
  }

  /**
   * Get a list of all versions of the given resource of the given type for the given automator type.
   * Request can optionally contain a 'status' http param whose value is one of 'active', 'inactive', 'staged',
   * or 'recalled' to filter the results to only contain resource that have the given status.
   *
   * @param request Request to get resources of the given type
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type that has the resources
   * @param resourceType Type of resources to get
   * @param resourceName Name of the resources to get
   */
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

    getResources(request, responder, account, PluginType.AUTOMATOR, automatortypeId, resourceType, resourceName);
  }

  /**
   * Get a list of all versions of the given resource of the given type for the given provider type.
   * Request can optionally contain a 'status' http param whose value is one of 'active', 'inactive', 'staged',
   * or 'recalled' to filter the results to only contain resource that have the given status.
   *
   * @param request Request to get resources of the given type
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type that has the resources
   * @param resourceType Type of resources to get
   * @param resourceName Name of the resources to get
   */
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

    getResources(request, responder, account, PluginType.PROVIDER, providertypeId, resourceType, resourceName);
  }

  /**
   * Delete all versions of the given resource.
   *
   * @param request Request to delete resources
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type whose resources should be deleted
   * @param resourceType Type of resources to delete
   * @param resourceName Name of resources to delete
   */
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

    deleteResource(responder, account, PluginType.AUTOMATOR, automatortypeId, resourceType, resourceName);
  }

  /**
   * Delete all versions of the given resource.
   *
   * @param request Request to delete resources
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type whose resources should be deleted
   * @param resourceType Type of resources to delete
   * @param resourceName Name of resources to delete
   */
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

    deleteResource(responder, account, PluginType.PROVIDER, providertypeId, resourceType, resourceName);
  }

  /**
   * Delete a specific version of the given resource.
   *
   * @param request Request to delete resource
   * @param responder Responder for responding to the request
   * @param automatortypeId Id of the automator type whose resource should be deleted
   * @param resourceType Type of resource to delete
   * @param resourceName Name of resource to delete
   * @param version Version of resource to delete
   */
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

    deleteResource(responder, account, PluginType.AUTOMATOR, automatortypeId, resourceType, resourceName, version);
  }

  /**
   * Delete a specific version of the given resource.
   *
   * @param request Request to delete resource
   * @param responder Responder for responding to the request
   * @param providertypeId Id of the provider type whose resource should be deleted
   * @param resourceType Type of resource to delete
   * @param resourceName Name of resource to delete
   * @param version Version of resource to delete
   */
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

    deleteResource(responder, account, PluginType.PROVIDER, providertypeId, resourceType, resourceName, version);
  }

  /**
   * Push staged resources to the provisioners, and remove recalled resources from the provisioners.
   *
   * @param request Request to sync resources to the provisioners
   * @param responder Responder for responding to the request
   */
  @POST
  @Path("/sync")
  public void syncPlugins(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    LOG.debug("Plugin sync called for tenant {}.", account.getTenantId());

    try {
      tenantProvisionerService.syncResources(account);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error syncing plugin resources");
    }
  }

  private void validateTypeExists(Account account, ResourceType resourceType)
    throws MissingEntityException, IOException {
    PluginType pluginType = resourceType.getPluginType();
    String pluginName = resourceType.getPluginName();
    String resourceTypeName = resourceType.getTypeName();
    AbstractPluginSpecification plugin;
    if (pluginType == PluginType.AUTOMATOR) {
      plugin = entityStoreService.getView(account).getAutomatorType(pluginName);
    } else if (pluginType == PluginType.PROVIDER) {
      plugin = entityStoreService.getView(account).getProviderType(pluginName);
    } else {
      throw new MissingEntityException("Unknown plugin type " + pluginType);
    }

    if (plugin == null) {
      throw new MissingEntityException(pluginType.name().toLowerCase() + " plugin " + pluginName + " not found.");
    }

    if (!plugin.getResourceTypes().containsKey(resourceTypeName)) {
      throw new MissingEntityException(resourceTypeName + " for " + pluginType.name().toLowerCase() +
                                         " plugin " + pluginName + " not found.");
    }
  }

  private BodyConsumer uploadResource(HttpResponder responder, Account account, PluginType type,
                                      String pluginName, String resourceType,
                                      String resourceName) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      validateTypeExists(account, pluginResourceType);
      return resourceService.createResourceBodyConsumer(account, pluginResourceType, resourceName, responder);
    } catch (IOException e) {
      LOG.error("Exception uploading resource.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error uploading resource");
      return null;
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, e.getMessage());
      return null;
    }
  }

  private void stageResource(HttpResponder responder, Account account, PluginType type,
                             String pluginName, String resourceType, String resourceName, String versionStr) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      int version = Integer.parseInt(versionStr);
      validateTypeExists(account, pluginResourceType);
      resourceService.stage(account, pluginResourceType, resourceName, version);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IOException e) {
      LOG.error("Exception staging resource.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error staging resource version.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, type.name().toLowerCase() + " resource not found.");
    }
  }

  private void recallResource(HttpResponder responder, Account account, PluginType type,
                              String pluginName, String resourceType, String resourceName, String versionStr) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      int version = Integer.parseInt(versionStr);
      validateTypeExists(account, pluginResourceType);
      resourceService.recall(account, pluginResourceType, resourceName, version);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IOException e) {
      LOG.error("Exception recalling resource.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error recalling resource version.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, type.name().toLowerCase() + " resource not found.");
    }
  }

  private void getResources(HttpRequest request, HttpResponder responder, Account account,
                            PluginType type, String pluginName, String resourceType) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      validateTypeExists(account, pluginResourceType);
      ResourceStatus statusFilter = getStatusParam(request);
      responder.sendJson(HttpResponseStatus.OK,
                         resourceService.getAll(account, pluginResourceType, statusFilter),
                         new TypeToken<Map<String, Set<ResourceMeta>>>() { }.getType(),
                         gson);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid status filter.");
    } catch (IOException e) {
      LOG.error("Exception getting resources.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error getting resources.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, e.getMessage());
    }
  }

  private void getResources(HttpRequest request, HttpResponder responder, Account account,
                            PluginType type, String pluginName, String resourceType, String resourceName) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      validateTypeExists(account, pluginResourceType);
      ResourceStatus statusFilter = getStatusParam(request);
      responder.sendJson(HttpResponseStatus.OK,
                         resourceService.getAll(account, pluginResourceType, resourceName, statusFilter),
                         new TypeToken<Set<ResourceMeta>>() { }.getType(),
                         gson);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid status filter.");
    } catch (IOException e) {
      LOG.error("Exception getting resources.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error getting resources.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, e.getMessage());
    }
  }

  private void deleteResource(HttpResponder responder, Account account, PluginType type,
                              String pluginName, String resourceType, String resourceName, String versionStr) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      validateTypeExists(account, pluginResourceType);
      int version = Integer.parseInt(versionStr);
      resourceService.delete(account, pluginResourceType, resourceName, version);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, "Resource not in a deletable state.");
    } catch (IOException e) {
      LOG.error("Exception deleting resource version.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error deleting resource version.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, e.getMessage());
    }
  }

  private void deleteResource(HttpResponder responder, Account account, PluginType type,
                              String pluginName, String resourceType, String resourceName) {
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    try {
      validateTypeExists(account, pluginResourceType);
      resourceService.delete(account, pluginResourceType, resourceName);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.CONFLICT, "Resource not in a deletable state.");
    } catch (IOException e) {
      LOG.error("Exception deleting all versions of resource.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error deleting all versions of resource.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, e.getMessage());
    }
  }

  private ResourceStatus getStatusParam(HttpRequest request) throws IllegalArgumentException {
    Map<String, List<String>> queryParams = new QueryStringDecoder(request.getUri()).getParameters();
    return queryParams.containsKey("status") ?
      ResourceStatus.valueOf(queryParams.get("status").get(0).toUpperCase()) : null;
  }
}
