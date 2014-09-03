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
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.QueueMetrics;
import com.continuuity.loom.scheduler.task.TaskQueueService;
import com.continuuity.loom.spec.HardwareType;
import com.continuuity.loom.spec.ImageType;
import com.continuuity.loom.spec.Provider;
import com.continuuity.loom.spec.plugin.AutomatorType;
import com.continuuity.loom.spec.plugin.ProviderType;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.spec.template.ClusterTemplate;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.entity.EntityStoreView;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
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
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Handler for getting, adding, modifying, and deleting admin defined entities.
 * GET calls work for any user, non-GET calls work only for admin.
 */
@Path(Constants.API_BASE)
public class LoomAdminHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomAdminHandler.class);

  public static final String PROVIDERS = "providers";
  public static final String HARDWARE_TYPES = "hardwaretypes";
  public static final String IMAGE_TYPES = "imagetypes";
  public static final String CLUSTER_TEMPLATES = "clustertemplates";
  public static final String SERVICES = "services";

  private final EntityStoreService entityStoreService;
  private final TaskQueueService taskQueueService;
  private final Gson gson;

  @Inject
  private LoomAdminHandler(TenantStore tenantStore, EntityStoreService entityStoreService,
                           TaskQueueService taskQueueService, Gson gson) {
    super(tenantStore);
    this.taskQueueService = taskQueueService;
    this.entityStoreService = entityStoreService;
    this.gson = gson;
  }

  /**
   * Get a mapping of tenant to provisioner queue metrics for that tenant. User requesting the metrics must be a
   * tenant, with tenant admins getting back only the queue metrics for their own tenant and with superadmins getting
   * the metrics across all tenants.
   *
   * @param request Request for queue metrics.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/metrics/queues")
  public void getQueueMetrics(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      if (account.isSuperadmin()) {
        responder.sendJson(HttpResponseStatus.OK,
                           taskQueueService.getTaskQueueMetricsSnapshot());
      } else if (account.isAdmin()) {
        String tenantName = request.getHeader(Constants.TENANT_HEADER);
        Map<String, QueueMetrics> responseBody =
          ImmutableMap.of(tenantName, taskQueueService.getTaskQueueMetricsSnapshot(account.getTenantId()));
        responder.sendJson(HttpResponseStatus.OK, responseBody);
      } else {
        responder.sendError(HttpResponseStatus.FORBIDDEN, "Forbidden to get queue metrics.");
      }
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal error getting queue metrics.");
    }
  }

  /**
   * Get a specific {@link Provider} if readable by the user.
   *
   * @param request The request for the provider.
   * @param responder Responder for sending the response.
   * @param providerId Id of the provider to get.
   */
  @GET
  @Path("/providers/{provider-id}")
  public void getProvider(HttpRequest request, HttpResponder responder, @PathParam("provider-id") String providerId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getProvider(providerId), "provider",
                         providerId, Provider.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting provider " + providerId);
    }
  }

  /**
   * Get a specific {@link HardwareType} if readable by the user.
   *
   * @param request The request for the hardware type.
   * @param responder Responder for sending the response.
   * @param hardwaretypeId Id of the hardware type to get.
   */
  @GET
  @Path("/hardwaretypes/{hardwaretype-id}")
  public void getHardwareType(HttpRequest request, HttpResponder responder,
                              @PathParam("hardwaretype-id") String hardwaretypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getHardwareType(hardwaretypeId), "hardware type",
                         hardwaretypeId, HardwareType.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception getting hardware type " + hardwaretypeId);
    }
  }

  /**
   * Get a specific {@link ImageType} if readable by the user.
   *
   * @param request The request for the image type.
   * @param responder Responder for sending the response.
   * @param imagetypeId Id of the image type to get.
   */
  @GET
  @Path("/imagetypes/{imagetype-id}")
  public void getImageType(HttpRequest request, HttpResponder responder,
                           @PathParam("imagetype-id") String imagetypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getImageType(imagetypeId),
                         "image type", imagetypeId, ImageType.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting image type " + imagetypeId);
    }
  }

  /**
   * Get a specific {@link Service} if readable by the user.
   *
   * @param request The request for the service.
   * @param responder Responder for sending the response.
   * @param serviceId Id of the service to get.
   */
  @GET
  @Path("/services/{service-id}")
  public void getService(HttpRequest request, HttpResponder responder, @PathParam("service-id") String serviceId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getService(serviceId), "service",
                         serviceId, Service.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting service " + serviceId);
    }
  }

  /**
   * Get a specific {@link ClusterTemplate} if readable by the user.
   *
   * @param request The request for the cluster template.
   * @param responder Responder for sending the response.
   * @param clustertemplateId Id of the cluster template to get.
   */
  @GET
  @Path("/clustertemplates/{clustertemplate-id}")
  public void getClusterTemplate(HttpRequest request, HttpResponder responder,
                                 @PathParam("clustertemplate-id") String clustertemplateId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getClusterTemplate(clustertemplateId), "cluster template",
                         clustertemplateId, ClusterTemplate.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception getting cluster template " + clustertemplateId);
    }
  }

  /**
   * Get a specific {@link com.continuuity.loom.spec.plugin.ProviderType} if readable by the user.
   *
   * @param request The request for the provider type.
   * @param responder Responder for sending the response.
   * @param providertypeId Id of the provider type to get.
   */
  @GET
  @Path("/plugins/providertypes/{providertype-id}")
  public void getProviderType(HttpRequest request, HttpResponder responder,
                              @PathParam("providertype-id") String providertypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getProviderType(providertypeId), "provider type",
                         providertypeId, ProviderType.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception getting provider type " + providertypeId);
    }
  }

  /**
   * Get a specific {@link com.continuuity.loom.spec.plugin.AutomatorType} if readable by the user.
   *
   * @param request The request for the automator type.
   * @param responder Responder for sending the response.
   * @param automatortypeId Id of the automator type to get.
   */
  @GET
  @Path("/plugins/automatortypes/{automatortype-id}")
  public void getAutomatorType(HttpRequest request, HttpResponder responder,
                              @PathParam("automatortype-id") String automatortypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      respondToGetEntity(entityStoreService.getView(account).getAutomatorType(automatortypeId), "automator type",
                         automatortypeId, AutomatorType.class, responder);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception getting automator type " + automatortypeId);
    }
  }

  /**
   * Get all {@link Provider}s readable by the user.
   *
   * @param request The request for providers.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/providers")
  public void getProviders(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllProviders(),
                         new TypeToken<Collection<Provider>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting providers");
    }
  }

  /**
   * Get all {@link HardwareType}s readable by the user.
   *
   * @param request The request for hardware types.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/hardwaretypes")
  public void getHardwareType(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllHardwareTypes(),
                         new TypeToken<Collection<HardwareType>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting hardware types");
    }
  }

  /**
   * Get all {@link ImageType}s readable by the user.
   *
   * @param request The request for image types.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/imagetypes")
  public void getImageType(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllImageTypes(),
                         new TypeToken<Collection<ImageType>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting image types");
    }
  }

  /**
   * Get all {@link Service}s readable by the user.
   *
   * @param request The request for services.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/services")
  public void getServices(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllServices(),
                         new TypeToken<Collection<Service>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting services");
    }
  }

  /**
   * Get all {@link com.continuuity.loom.spec.plugin.ProviderType}s readable by the user.
   *
   * @param request The request for services.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/plugins/providertypes")
  public void getProviderTypes(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllProviderTypes(),
                         new TypeToken<Collection<ProviderType>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting provider types");
    }
  }

  /**
   * Get all {@link com.continuuity.loom.spec.plugin.AutomatorType}s readable by the user.
   *
   * @param request The request for services.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/plugins/automatortypes")
  public void getAutomatorTypes(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllAutomatorTypes(),
                         new TypeToken<Collection<AutomatorType>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting automator types");
    }
  }

  /**
   * Get all {@link ClusterTemplate}s readable by the user.
   *
   * @param request The request for cluster templates.
   * @param responder Responder for sending the response.
   */
  @GET
  @Path("/clustertemplates")
  public void getClusterTemplate(HttpRequest request, HttpResponder responder){
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    try {
      responder.sendJson(HttpResponseStatus.OK, entityStoreService.getView(account).getAllClusterTemplates(),
                         new TypeToken<Collection<ClusterTemplate>>() {}.getType(), gson);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception getting cluster templates");
    }
  }

  /**
   * Delete a specific {@link Provider}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a provider.
   * @param responder Responder for sending the response.
   * @param providerId Id of the provider to delete.
   */
  @DELETE
  @Path("/providers/{provider-id}")
  public void deleteProvider(HttpRequest request, HttpResponder responder,
                             @PathParam("provider-id") String providerId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteProvider(providerId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception deleting provider " + providerId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete provider.");
    }
  }

  /**
   * Delete a specific {@link HardwareType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a hardware type.
   * @param responder Responder for sending the response.
   * @param hardwaretypeId Id of the hardware type to delete.
   */
  @DELETE
  @Path("/hardwaretypes/{hardwaretype-id}")
  public void deleteHardwareType(HttpRequest request, HttpResponder responder,
                                 @PathParam("hardwaretype-id") String hardwaretypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteHardwareType(hardwaretypeId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception deleting hardware type " + hardwaretypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete hardware type.");
    }
  }

  /**
   * Delete a specific {@link ImageType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a image type.
   * @param responder Responder for sending the response.
   * @param imagetypeId Id of the image type to delete.
   */
  @DELETE
  @Path("/imagetypes/{imagetype-id}")
  public void deleteImageType(HttpRequest request, HttpResponder responder,
                              @PathParam("imagetype-id") String imagetypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteImageType(imagetypeId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception deleting image type " + imagetypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete image type.");
    }
  }

  /**
   * Delete a specific {@link Service}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a service.
   * @param responder Responder for sending the response.
   * @param serviceId Id of the service to delete.
   */
  @DELETE
  @Path("/services/{service-id}")
  public void deleteService(HttpRequest request, HttpResponder responder, @PathParam("service-id") String serviceId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteService(serviceId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception deleting service " + serviceId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete service.");
    }
  }

  /**
   * Delete a specific {@link ClusterTemplate}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a cluster template.
   * @param responder Responder for sending the response.
   * @param clustertemplateId Id of the cluster template to delete.
   */
  @DELETE
  @Path("/clustertemplates/{clustertemplate-id}")
  public void deleteClusterTemplate(HttpRequest request, HttpResponder responder,
                                    @PathParam("clustertemplate-id") String clustertemplateId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    try {
      entityStoreService.getView(account).deleteClusterTemplate(clustertemplateId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception deleting cluster template " + clustertemplateId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to delete cluster template.");
    }
  }

  /**
   * Writes a {@link Provider}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write provider.
   * @param responder Responder to send response.
   * @param providerId Id of the provider to write.
   */
  @PUT
  @Path("/providers/{provider-id}")
  public void putProvider(HttpRequest request, HttpResponder responder, @PathParam("provider-id") String providerId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    Provider provider = getEntityFromRequest(request, responder, Provider.class);
    if (provider == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!provider.getName().equals(providerId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between provider name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(account).writeProvider(provider);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception writing provider " + providerId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write provider.");
    }
  }

  /**
   * Writes a {@link HardwareType}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write hardware type.
   * @param responder Responder to send response.
   * @param hardwaretypeId Id of the hardware type to write.
   */
  @PUT
  @Path("/hardwaretypes/{hardwaretype-id}")
  public void putHardwareType(HttpRequest request, HttpResponder responder,
                              @PathParam("hardwaretype-id") String hardwaretypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    HardwareType hardwareType = getEntityFromRequest(request, responder, HardwareType.class);
    if (hardwareType == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!hardwareType.getName().equals(hardwaretypeId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between hardware type name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(account).writeHardwareType(hardwareType);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing hardware type " + hardwaretypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write hardware type.");
    }
  }

  /**
   * Writes a {@link ImageType}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write image type.
   * @param responder Responder to send response.
   * @param imagetypeId Id of the image type to write.
   */
  @PUT
  @Path("/imagetypes/{imagetype-id}")
  public void putImageType(HttpRequest request, HttpResponder responder,
                           @PathParam("imagetype-id") String imagetypeId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    ImageType imageType = getEntityFromRequest(request, responder, ImageType.class);
    if (imageType == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!imageType.getName().equals(imagetypeId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between image type name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(account).writeImageType(imageType);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception writing image type " + imagetypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write image type.");
    }
  }

  /**
   * Writes a {@link Service}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write service.
   * @param responder Responder to send response.
   * @param serviceId Id of the service to write.
   */
  @PUT
  @Path("/services/{service-id}")
  public void putService(HttpRequest request, HttpResponder responder, @PathParam("service-id") String serviceId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    Service service = getEntityFromRequest(request, responder, Service.class);
    if (service == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!service.getName().equals(serviceId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between service name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(account).writeService(service);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception writing service " + serviceId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write service.");
    }
  }

  /**
   * Writes a {@link ClusterTemplate}. User must be admin or a 403 is returned. If the name in the path does not match
   * the name in the put body, a 400 is returned.
   *
   * @param request Request to write cluster template.
   * @param responder Responder to send response.
   * @param clustertemplateId Id of the cluster template to write.
   */
  @PUT
  @Path("/clustertemplates/{clustertemplate-id}")
  public void putClusterTemplate(HttpRequest request, HttpResponder responder,
                                 @PathParam("clustertemplate-id") String clustertemplateId) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }

    ClusterTemplate clusterTemplate = getEntityFromRequest(request, responder, ClusterTemplate.class);
    if (clusterTemplate == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!clusterTemplate.getName().equals(clustertemplateId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between cluster template name and name in path.");
      return;
    }

    if (!validateClusterTemplate(clusterTemplate, responder)) {
      return;
    }

    try {
      entityStoreService.getView(account).writeClusterTemplate(clusterTemplate);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing cluster template " + clustertemplateId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write cluster template.");
    }
  }

  /**
   * Add the specified {@link Provider}. User must be admin or a 403 is returned. Returns a 400 if the
   * provider already exists.
   *
   * @param request Request to add provider.
   * @param responder Responder for sending response.
   */
  @POST
  @Path("/providers")
  public void postProvider(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    Provider provider = getEntityFromRequest(request, responder, Provider.class);
    if (provider == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    }

    try {
      EntityStoreView view = entityStoreService.getView(account);
      if (view.getProvider(provider.getName()) != null) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST,
                            "provider " + provider.getName() + " already exists");
      } else {
        view.writeProvider(provider);
        responder.sendStatus(HttpResponseStatus.OK);
      }
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to add provider.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding provider.");
    }
  }

  /**
   * Add the specified {@link HardwareType}. User must be admin or a 403 is returned. Returns a 400 if the
   * hardware type already exists.
   *
   * @param request Request to add hardware type.
   * @param responder Responder for sending response.
   */
  @POST
  @Path("/hardwaretypes")
  public void postHardwareType(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    HardwareType hardwareType = getEntityFromRequest(request, responder, HardwareType.class);
    if (hardwareType == null) {
      return;
    }

    try {
      EntityStoreView view = entityStoreService.getView(account);
      if (view.getHardwareType(hardwareType.getName()) != null) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST,
                            "hardware type " + hardwareType.getName() + " already exists");
      } else {
        view.writeHardwareType(hardwareType);
        responder.sendStatus(HttpResponseStatus.OK);
      }
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to add hardware type.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding hardware type.");
    }
  }

  /**
   * Add the specified {@link ImageType}. User must be admin or a 403 is returned. Returns a 400 if the
   * image type already exists.
   *
   * @param request Request to add image type.
   * @param responder Responder for sending response.
   */
  @POST
  @Path("/imagetypes")
  public void postImageType(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    ImageType imageType = getEntityFromRequest(request, responder, ImageType.class);
    if (imageType == null) {
      return;
    }

    try {
      EntityStoreView view = entityStoreService.getView(account);
      if (view.getImageType(imageType.getName()) != null) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "image type " + imageType.getName() + " already exists");
      } else {
        view.writeImageType(imageType);
        responder.sendStatus(HttpResponseStatus.OK);
      }
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to add image type.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding image type.");
    }
  }

  /**
   * Add the specified {@link Service}. User must be admin or a 403 is returned. Returns a 400 if the
   * service already exists.
   *
   * @param request Request to add service.
   * @param responder Responder for sending response.
   */
  @POST
  @Path("/services")
  public void postService(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    Service service = getEntityFromRequest(request, responder, Service.class);
    if (service == null) {
      return;
    }

    try {
      EntityStoreView view = entityStoreService.getView(account);
      if (view.getService(service.getName()) != null) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST, "service " + service.getName() + " already exists");
      } else {
        view.writeService(service);
        responder.sendStatus(HttpResponseStatus.OK);
      }
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to add service.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding service.");
    }
  }

  /**
   * Add the specified {@link ClusterTemplate}. User must be admin or a 403 is returned. Returns a 400 if the
   * cluster template already exists.
   *
   * @param request Request to add cluster template.
   * @param responder Responder for sending response.
   */
  @POST
  @Path("/clustertemplates")
  public void postClusterTemplate(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    ClusterTemplate clusterTemplate = getEntityFromRequest(request, responder, ClusterTemplate.class);
    if (clusterTemplate == null) {
      return;
    }

    try {
      EntityStoreView view = entityStoreService.getView(account);
      if (view.getClusterTemplate(clusterTemplate.getName()) != null) {
        responder.sendError(HttpResponseStatus.BAD_REQUEST,
                            "cluster template " + clusterTemplate.getName() + " already exists");
      } else {
        if (!validateClusterTemplate(clusterTemplate, responder)) {
          return;
        }

        view.writeClusterTemplate(clusterTemplate);
        responder.sendStatus(HttpResponseStatus.OK);
      }
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to add cluster template.");
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception adding cluster template.");
    }
  }

  /**
   * Export all providers, hardware types, image types, services, and cluster templates.
   *
   * @param request Request to export admin definable entities.
   * @param responder Responder for sending response.
   */
  @GET
  @Path("/export")
  public void exportConfig(HttpRequest request, HttpResponder responder) throws Exception {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    Map<String, JsonElement> outJson = Maps.newHashMap();

    EntityStoreView view = entityStoreService.getView(account);
    Collection<Provider> providers = view.getAllProviders();
    LOG.debug("Exporting {} providers", providers.size());
    outJson.put(PROVIDERS, gson.toJsonTree(providers));

    Collection<HardwareType> hardwareTypes = view.getAllHardwareTypes();
    LOG.debug("Exporting {} hardware types", hardwareTypes.size());
    outJson.put(HARDWARE_TYPES, gson.toJsonTree(hardwareTypes));

    Collection<ImageType> imageTypes = view.getAllImageTypes();
    LOG.debug("Exporting {} image types", imageTypes.size());
    outJson.put(IMAGE_TYPES, gson.toJsonTree(imageTypes));

    Collection<Service> services = view.getAllServices();
    LOG.debug("Exporting {} services", services.size());
    outJson.put(SERVICES, gson.toJsonTree(services));

    Collection<ClusterTemplate> clusterTemplates = view.getAllClusterTemplates();
    LOG.debug("Exporting {} cluster templates", clusterTemplates.size());
    outJson.put(CLUSTER_TEMPLATES, gson.toJsonTree(clusterTemplates));

    LOG.trace("Exporting {}", outJson);

    responder.sendJson(HttpResponseStatus.OK, outJson);
  }

  /**
   * Imports all providers, image types, hardware types, services, and cluster templates from a file. All existing
   * providers, image types, hardware types, services, and cluster templates will be deleted.
   *
   * @param request Request to import admin definable entities.
   * @param responder Responder for sending response.
   */
  @POST
  @Path("/import")
  public void importConfig(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    // Parse incoming json
    List<Provider> newProviders;
    List<HardwareType> newHardwareTypes;
    List<ImageType> newImageTypes;
    List<Service> newServices;
    List<ClusterTemplate> newClusterTemplates;
    List<AutomatorType> newAutomatorTypes;
    List<ProviderType> newProviderTypes;

    try {
      Map<String, JsonElement> inJson = getEntityFromRequest(request, responder,
                                                             new TypeToken<Map<String, JsonElement>>() {}.getType());
      if (inJson == null) {
        return;
      }

      LOG.trace("Importing {}", inJson);

      newProviders = !inJson.containsKey(PROVIDERS) ? ImmutableList.<Provider>of() :
        gson.<List<Provider>>fromJson(inJson.get(PROVIDERS), new TypeToken<List<Provider>>() {}.getType());

      newHardwareTypes = !inJson.containsKey(HARDWARE_TYPES) ? ImmutableList.<HardwareType>of() :
        gson.<List<HardwareType>>fromJson(inJson.get(HARDWARE_TYPES),
                                                     new TypeToken<List<HardwareType>>() {}.getType());

      newImageTypes = !inJson.containsKey(IMAGE_TYPES) ? ImmutableList.<ImageType>of() :
        gson.<List<ImageType>>fromJson(inJson.get(IMAGE_TYPES),
                                                  new TypeToken<List<ImageType>>() {}.getType());

      newServices = !inJson.containsKey(SERVICES) ? ImmutableList.<Service>of() :
        gson.<List<Service>>fromJson(inJson.get(SERVICES), new TypeToken<List<Service>>() {}.getType());

      newClusterTemplates = !inJson.containsKey(CLUSTER_TEMPLATES) ?
        ImmutableList.<ClusterTemplate>of() :
        gson.<List<ClusterTemplate>>fromJson(inJson.get(CLUSTER_TEMPLATES),
                                                        new TypeToken<List<ClusterTemplate>>() {}.getType());


    } catch (JsonSyntaxException e) {
      LOG.error("Got exception while importing config", e);
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Json syntax error");
      return;
    }

    try {
      EntityStoreView view = entityStoreService.getView(account);
      // Delete all existing config data
      for (Provider provider : view.getAllProviders()) {
        view.deleteProvider(provider.getName());
      }

      for (HardwareType hardwareType : view.getAllHardwareTypes()) {
        view.deleteHardwareType(hardwareType.getName());
      }

      for (ImageType imageType : view.getAllImageTypes()) {
        view.deleteImageType(imageType.getName());
      }

      for (Service service : view.getAllServices()) {
        view.deleteService(service.getName());
      }

      for (ClusterTemplate clusterTemplate : view.getAllClusterTemplates()) {
        view.deleteClusterTemplate(clusterTemplate.getName());
      }

      // Add new config data
      LOG.debug("Importing {} providers", newProviders.size());
      for (Provider provider : newProviders) {
        view.writeProvider(provider);
      }

      LOG.debug("Importing {} hardware types", newHardwareTypes.size());
      for (HardwareType hardwareType : newHardwareTypes) {
        view.writeHardwareType(hardwareType);
      }

      LOG.debug("Importing {} image types", newImageTypes.size());
      for (ImageType imageType : newImageTypes) {
        view.writeImageType(imageType);
      }

      LOG.debug("Importing {} services", newServices.size());
      for (Service service : newServices) {
        view.writeService(service);
      }

      LOG.debug("Importing {} cluster templates", newClusterTemplates.size());
      for (ClusterTemplate clusterTemplate : newClusterTemplates) {
        view.writeClusterTemplate(clusterTemplate);
      }

      LOG.debug("Importing {} cluster templates", newClusterTemplates.size());
      for (ClusterTemplate clusterTemplate : newClusterTemplates) {
        view.writeClusterTemplate(clusterTemplate);
      }
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, e.getMessage());
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception importing entities.");
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

  private <T> void respondToGetEntity(Object entity, String entityType, String entityId,
                                      Class<T> entityClass, HttpResponder responder) {
    if (entity == null) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, Joiner.on(" ").join(entityType, entityId, " not found."));
    } else {
      responder.sendJson(HttpResponseStatus.OK, entity, entityClass, gson);
    }
  }

  private boolean validateClusterTemplate(ClusterTemplate clusterTemplate, HttpResponder responder) {
    long initial = clusterTemplate.getAdministration().getLeaseDuration().getInitial();
    initial = initial == 0 ? Long.MAX_VALUE : initial;

    long max = clusterTemplate.getAdministration().getLeaseDuration().getMax();
    max = max == 0 ? Long.MAX_VALUE : max;

    if (max < initial) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST,
                          "Initial lease duration cannot be more than max lease duration for cluster template " +
                            clusterTemplate.getName());
      return false;
    }

    return true;
  }
}
