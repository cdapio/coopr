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
import com.continuuity.loom.admin.AutomatorType;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProviderType;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.store.EntityStore;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
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
 * Handler for getting, adding, modifying, and deleting admin defined Loom entities.
 * GET calls work for any user, non-GET calls work only for admin.
 */
@Path("/v1/loom")
public class LoomAdminHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomAdminHandler.class);

  public static final String PROVIDERS = "providers";
  public static final String HARDWARE_TYPES = "hardwaretypes";
  public static final String IMAGE_TYPES = "imagetypes";
  public static final String CLUSTER_TEMPLATES = "clustertemplates";
  public static final String SERVICES = "services";

  private final EntityStore entityStore;
  private final JsonSerde codec;

  @Inject
  public LoomAdminHandler(EntityStore entityStore) {
    this.entityStore = entityStore;
    this.codec = new JsonSerde();
  }

  /**
   * Get a specific {@link Provider} if readable by the user.
   *
   * @param request The request for the provider.
   * @param responder Responder for sending the response.
   * @param providerId Id of the provider to get.
   * @throws Exception
   */
  @GET
  @Path("/providers/{provider-id}")
  public void getProvider(HttpRequest request, HttpResponder responder,
                          @PathParam("provider-id") String providerId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getProvider(providerId), "provider", providerId, Provider.class, responder);
  }

  /**
   * Get a specific {@link HardwareType} if readable by the user.
   *
   * @param request The request for the hardware type.
   * @param responder Responder for sending the response.
   * @param hardwaretypeId Id of the hardware type to get.
   * @throws Exception
   */
  @GET
  @Path("/hardwaretypes/{hardwaretype-id}")
  public void getHardwareType(HttpRequest request, HttpResponder responder,
                              @PathParam("hardwaretype-id") String hardwaretypeId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getHardwareType(hardwaretypeId), "hardware type",
                       hardwaretypeId, HardwareType.class, responder);
  }

  /**
   * Get a specific {@link ImageType} if readable by the user.
   *
   * @param request The request for the image type.
   * @param responder Responder for sending the response.
   * @param imagetypeId Id of the image type to get.
   * @throws Exception
   */
  @GET
  @Path("/imagetypes/{imagetype-id}")
  public void getImageType(HttpRequest request, HttpResponder responder,
                           @PathParam("imagetype-id") String imagetypeId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getImageType(imagetypeId), "image type", imagetypeId, ImageType.class, responder);
  }

  /**
   * Get a specific {@link Service} if readable by the user.
   *
   * @param request The request for the service.
   * @param responder Responder for sending the response.
   * @param serviceId Id of the service to get.
   * @throws Exception
   */
  @GET
  @Path("/services/{service-id}")
  public void getService(HttpRequest request, HttpResponder responder,
                              @PathParam("service-id") String serviceId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getService(serviceId), "service", serviceId, Service.class, responder);
  }

  /**
   * Get a specific {@link ClusterTemplate} if readable by the user.
   *
   * @param request The request for the cluster template.
   * @param responder Responder for sending the response.
   * @param clustertemplateId Id of the cluster template to get.
   * @throws Exception
   */
  @GET
  @Path("/clustertemplates/{clustertemplate-id}")
  public void getClusterTemplate(HttpRequest request, HttpResponder responder,
                                 @PathParam("clustertemplate-id") String clustertemplateId) throws Exception {
    // TODO: clustertemplates should be associated with a user group
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getClusterTemplate(clustertemplateId), "cluster template",
                       clustertemplateId, ClusterTemplate.class, responder);
  }

  /**
   * Get a specific {@link ProviderType} if readable by the user.
   *
   * @param request The request for the provider type.
   * @param responder Responder for sending the response.
   * @param providertypeId Id of the provider type to get.
   * @throws Exception
   */
  @GET
  @Path("/providertypes/{providertype-id}")
  public void getProviderType(HttpRequest request, HttpResponder responder,
                              @PathParam("providertype-id") String providertypeId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getProviderType(providertypeId), "provider type",
                       providertypeId, ProviderType.class, responder);
  }

  /**
   * Get a specific {@link AutomatorType} if readable by the user.
   *
   * @param request The request for the automator type.
   * @param responder Responder for sending the response.
   * @param automatortypeId Id of the automator type to get.
   * @throws Exception
   */
  @GET
  @Path("/automatortypes/{automatortype-id}")
  public void getAutomatorType(HttpRequest request, HttpResponder responder,
                              @PathParam("automatortype-id") String automatortypeId) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    respondToGetEntity(entityStore.getAutomatorType(automatortypeId), "automator type",
                       automatortypeId, AutomatorType.class, responder);
  }

  /**
   * Get all {@link Provider}s readable by the user.
   *
   * @param request The request for providers.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/providers")
  public void getProviders(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllProviders(),
                       new TypeToken<Collection<Provider>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Get all {@link HardwareType}s readable by the user.
   *
   * @param request The request for hardware types.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/hardwaretypes")
  public void getHardwareType(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllHardwareTypes(),
                       new TypeToken<Collection<HardwareType>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Get all {@link ImageType}s readable by the user.
   *
   * @param request The request for image types.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/imagetypes")
  public void getImageType(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllImageTypes(),
                       new TypeToken<Collection<ImageType>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Get all {@link Service}s readable by the user.
   *
   * @param request The request for services.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/services")
  public void getServices(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllServices(),
                       new TypeToken<Collection<Service>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Get all {@link ProviderType}s readable by the user.
   *
   * @param request The request for services.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/providertypes")
  public void getProviderTypes(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllProviderTypes(),
                       new TypeToken<Collection<ProviderType>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Get all {@link AutomatorType}s readable by the user.
   *
   * @param request The request for services.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/automatortypes")
  public void getAutomatorTypes(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllAutomatorTypes(),
                       new TypeToken<Collection<AutomatorType>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Get all {@link ClusterTemplate}s readable by the user.
   *
   * @param request The request for cluster templates.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @GET
  @Path("/clustertemplates")
  public void getClusterTemplate(HttpRequest request, HttpResponder responder) throws Exception {
    // TODO: clustertemplates should be associated with a user group
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    responder.sendJson(HttpResponseStatus.OK, entityStore.getAllClusterTemplates(),
                       new TypeToken<Collection<ClusterTemplate>>() {}.getType(),
                       codec.getGson());
  }

  /**
   * Delete a specific {@link Provider}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a provider.
   * @param responder Responder for sending the response.
   * @param providerId Id of the provider to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/providers/{provider-id}")
  public void deleteProvider(HttpRequest request, HttpResponder responder,
                             @PathParam("provider-id") String providerId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteProvider(providerId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Delete a specific {@link HardwareType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a hardware type.
   * @param responder Responder for sending the response.
   * @param hardwaretypeId Id of the hardware type to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/hardwaretypes/{hardwaretype-id}")
  public void deleteHardwareType(HttpRequest request, HttpResponder responder,
                                 @PathParam("hardwaretype-id") String hardwaretypeId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteHardwareType(hardwaretypeId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Delete a specific {@link ImageType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a image type.
   * @param responder Responder for sending the response.
   * @param imagetypeId Id of the image type to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/imagetypes/{imagetype-id}")
  public void deleteImageType(HttpRequest request, HttpResponder responder,
                              @PathParam("imagetype-id") String imagetypeId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteImageType(imagetypeId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Delete a specific {@link Service}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a service.
   * @param responder Responder for sending the response.
   * @param serviceId Id of the service to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/services/{service-id}")
  public void deleteService(HttpRequest request, HttpResponder responder,
                            @PathParam("service-id") String serviceId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteService(serviceId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Delete a specific {@link ClusterTemplate}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a cluster template.
   * @param responder Responder for sending the response.
   * @param clustertemplateId Id of the cluster template to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/clustertemplates/{clustertemplate-id}")
  public void deleteClusterTemplate(HttpRequest request, HttpResponder responder,
                                    @PathParam("clustertemplate-id") String clustertemplateId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteClusterTemplate(clustertemplateId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Delete a specific {@link ProviderType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete a provider type.
   * @param responder Responder for sending the response.
   * @param providertypeId Id of the provider type to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/providertypes/{providertype-id}")
  public void deleteProviderType(HttpRequest request, HttpResponder responder,
                                 @PathParam("providertype-id") String providertypeId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteProviderType(providertypeId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Delete a specific {@link AutomatorType}. User must be admin or a 403 is returned.
   *
   * @param request The request to delete an automator type.
   * @param responder Responder for sending the response.
   * @param automatortypeId Id of the automator type to delete.
   * @throws Exception
   */
  @DELETE
  @Path("/automatortypes/{automatortype-id}")
  public void deleteAutomatorType(HttpRequest request, HttpResponder responder,
                                 @PathParam("automatortype-id") String automatortypeId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    entityStore.deleteAutomatorType(automatortypeId);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link Provider}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write provider.
   * @param responder Responder to send response.
   * @param providerId Id of the provider to write.
   * @throws Exception
   */
  @PUT
  @Path("/providers/{provider-id}")
  public void putProvider(HttpRequest request, HttpResponder responder,
                          @PathParam("provider-id") String providerId) throws Exception {
    if (!isAdminRequest(request)) {
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

    entityStore.writeProvider(provider);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link HardwareType}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write hardware type.
   * @param responder Responder to send response.
   * @param hardwaretypeId Id of the hardware type to write.
   * @throws Exception
   */
  @PUT
  @Path("/hardwaretypes/{hardwaretype-id}")
  public void putHardwareType(HttpRequest request, HttpResponder responder,
                              @PathParam("hardwaretype-id") String hardwaretypeId) throws Exception {
    if (!isAdminRequest(request)) {
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

    entityStore.writeHardwareType(hardwareType);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link ImageType}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write image type.
   * @param responder Responder to send response.
   * @param imagetypeId Id of the image type to write.
   * @throws Exception
   */
  @PUT
  @Path("/imagetypes/{imagetype-id}")
  public void putImageType(HttpRequest request, HttpResponder responder,
                           @PathParam("imagetype-id") String imagetypeId) throws Exception {
    if (!isAdminRequest(request)) {
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

    entityStore.writeImageType(imageType);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link Service}. User must be admin or a 403 is returned. If the name in the path does not match the
   * name in the put body, a 400 is returned.
   *
   * @param request Request to write service.
   * @param responder Responder to send response.
   * @param serviceId Id of the service to write.
   * @throws Exception
   */
  @PUT
  @Path("/services/{service-id}")
  public void putService(HttpRequest request, HttpResponder responder,
                         @PathParam("service-id") String serviceId) throws Exception {
    if (!isAdminRequest(request)) {
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

    entityStore.writeService(service);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link ClusterTemplate}. User must be admin or a 403 is returned. If the name in the path does not match
   * the name in the put body, a 400 is returned.
   *
   * @param request Request to write cluster template.
   * @param responder Responder to send response.
   * @param clustertemplateId Id of the cluster template to write.
   * @throws Exception
   */
  @PUT
  @Path("/clustertemplates/{clustertemplate-id}")
  public void putClusterTemplate(HttpRequest request, HttpResponder responder,
                                 @PathParam("clustertemplate-id") String clustertemplateId) throws Exception {
    if (!isAdminRequest(request)) {
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

    entityStore.writeClusterTemplate(clusterTemplate);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link ProviderType}. User must be admin or a 403 is returned.
   * If the name in the path does not match the name in the put body, a 400 is returned.
   *
   * @param request Request to write provider type.
   * @param responder Responder to send response.
   * @param providertypeId Id of the provider type to write.
   * @throws Exception
   */
  @PUT
  @Path("/providertypes/{providertype-id}")
  public void putProviderType(HttpRequest request, HttpResponder responder,
                              @PathParam("providertype-id") String providertypeId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
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

    entityStore.writeProviderType(providerType);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Writes a {@link AutomatorType}. User must be admin or a 403 is returned.
   * If the name in the path does not match the name in the put body, a 400 is returned.
   *
   * @param request Request to write provider type.
   * @param responder Responder to send response.
   * @param automatortypeId Id of the provider type to write.
   * @throws Exception
   */
  @PUT
  @Path("/automatortypes/{automatortype-id}")
  public void putAutomatorType(HttpRequest request, HttpResponder responder,
                               @PathParam("automatortype-id") String automatortypeId) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
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

    entityStore.writeAutomatorType(automatorType);
    responder.sendStatus(HttpResponseStatus.OK);
  }

  /**
   * Add the specified {@link Provider}. User must be admin or a 403 is returned. Returns a 400 if the
   * provider already exists.
   *
   * @param request Request to add provider.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @POST
  @Path("/providers")
  public void postProvider(HttpRequest request, HttpResponder responder) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    Provider provider = getEntityFromRequest(request, responder, Provider.class);
    if (provider == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    }

    if (entityStore.getProvider(provider.getName()) != null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST,
                          "provider " + provider.getName() + " already exists");
    } else {
      entityStore.writeProvider(provider);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }

  /**
   * Add the specified {@link HardwareType}. User must be admin or a 403 is returned. Returns a 400 if the
   * hardware type already exists.
   *
   * @param request Request to add hardware type.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @POST
  @Path("/hardwaretypes")
  public void postHardwareType(HttpRequest request, HttpResponder responder) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    HardwareType hardwareType = getEntityFromRequest(request, responder, HardwareType.class);
    if (hardwareType == null) {
      return;
    }

    if (entityStore.getHardwareType(hardwareType.getName()) != null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST,
                          "hardware type " + hardwareType.getName() + " already exists");
    } else {
      entityStore.writeHardwareType(hardwareType);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }

  /**
   * Add the specified {@link ImageType}. User must be admin or a 403 is returned. Returns a 400 if the
   * image type already exists.
   *
   * @param request Request to add image type.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @POST
  @Path("/imagetypes")
  public void postImageType(HttpRequest request, HttpResponder responder) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    ImageType imageType = getEntityFromRequest(request, responder, ImageType.class);
    if (imageType == null) {
      return;
    }

    if (entityStore.getImageType(imageType.getName()) != null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "image type " + imageType.getName() + " already exists");
    } else {
      entityStore.writeImageType(imageType);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }

  /**
   * Add the specified {@link Service}. User must be admin or a 403 is returned. Returns a 400 if the
   * service already exists.
   *
   * @param request Request to add service.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @POST
  @Path("/services")
  public void postService(HttpRequest request, HttpResponder responder) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    Service service = getEntityFromRequest(request, responder, Service.class);
    if (service == null) {
      return;
    }

    if (entityStore.getService(service.getName()) != null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "service " + service.getName() + " already exists");
    } else {
      entityStore.writeService(service);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }

  /**
   * Add the specified {@link ClusterTemplate}. User must be admin or a 403 is returned. Returns a 400 if the
   * cluster template already exists.
   *
   * @param request Request to add cluster template.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @POST
  @Path("/clustertemplates")
  public void postClusterTemplate(HttpRequest request, HttpResponder responder) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    ClusterTemplate clusterTemplate = getEntityFromRequest(request, responder, ClusterTemplate.class);
    if (clusterTemplate == null) {
      return;
    }

    if (entityStore.getClusterTemplate(clusterTemplate.getName()) != null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST,
                          "cluster template " + clusterTemplate.getName() + " already exists");
    } else {
      if (!validateClusterTemplate(clusterTemplate, responder)) {
        return;
      }

      entityStore.writeClusterTemplate(clusterTemplate);
      responder.sendStatus(HttpResponseStatus.OK);
    }
  }

  /**
   * Export all providers, hardware types, image types, services, and cluster templates.
   *
   * @param request Request to export admin definable entities.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @GET
  @Path("/export")
  public void exportConfig(HttpRequest request, HttpResponder responder) throws Exception {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId == null) {
      return;
    }

    Map<String, JsonElement> outJson = Maps.newHashMap();

    Collection<Provider> providers = entityStore.getAllProviders();
    LOG.debug("Exporting {} providers", providers.size());
    outJson.put(PROVIDERS, codec.getGson().toJsonTree(providers,
                                                        new TypeToken<Collection<Provider>>() {}.getType()));

    Collection<HardwareType> hardwareTypes = entityStore.getAllHardwareTypes();
    LOG.debug("Exporting {} hardware types", hardwareTypes.size());
    outJson.put(HARDWARE_TYPES, codec.getGson().toJsonTree(hardwareTypes,
                                                            new TypeToken<Collection<HardwareType>>() {}.getType()));

    Collection<ImageType> imageTypes = entityStore.getAllImageTypes();
    LOG.debug("Exporting {} image types", imageTypes.size());
    outJson.put(IMAGE_TYPES, codec.getGson().toJsonTree(imageTypes,
                                                        new TypeToken<Collection<ImageType>>() {}.getType()));

    Collection<Service> services = entityStore.getAllServices();
    LOG.debug("Exporting {} services", services.size());
    outJson.put(SERVICES, codec.getGson().toJsonTree(services,
                                                       new TypeToken<Collection<Service>>() {}.getType()));

    Collection<ClusterTemplate> clusterTemplates = entityStore.getAllClusterTemplates();
    LOG.debug("Exporting {} cluster templates", clusterTemplates.size());
    outJson.put(CLUSTER_TEMPLATES, codec.getGson().toJsonTree(clusterTemplates,
                                                               new TypeToken<Collection<Service>>() {}.getType()));

    LOG.trace("Exporting {}", outJson);

    responder.sendJson(HttpResponseStatus.OK, outJson);
  }

  /**
   * Imports all providers, image types, hardware types, services, and cluster templates from a file. All existing
   * providers, image types, hardware types, services, and cluster templates will be deleted.
   *
   * @param request Request to import admin definable entities.
   * @param responder Responder for sending response.
   * @throws Exception
   */
  @POST
  @Path("/import")
  public void importConfig(HttpRequest request, HttpResponder responder) throws Exception {
    if (!isAdminRequest(request)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized, must be admin.");
      return;
    }
    // Parse incoming json
    List<Provider> newProviders;
    List<HardwareType> newHardwareTypes;
    List<ImageType> newImageTypes;
    List<Service> newServices;
    List<ClusterTemplate> newClusterTemplates;

    try {
      Map<String, JsonElement> inJson = getEntityFromRequest(request, responder,
                                                             new TypeToken<Map<String, JsonElement>>() {}.getType());
      if (inJson == null) {
        return;
      }

      LOG.trace("Importing {}", inJson);

      newProviders = !inJson.containsKey(PROVIDERS) ? ImmutableList.<Provider>of() :
        codec.getGson().<List<Provider>>fromJson(inJson.get(PROVIDERS), new TypeToken<List<Provider>>() {}.getType());

      newHardwareTypes = !inJson.containsKey(HARDWARE_TYPES) ? ImmutableList.<HardwareType>of() :
        codec.getGson().<List<HardwareType>>fromJson(inJson.get(HARDWARE_TYPES),
                                                     new TypeToken<List<HardwareType>>() {}.getType());

      newImageTypes = !inJson.containsKey(IMAGE_TYPES) ? ImmutableList.<ImageType>of() :
        codec.getGson().<List<ImageType>>fromJson(inJson.get(IMAGE_TYPES),
                                                  new TypeToken<List<ImageType>>() {}.getType());

      newServices = !inJson.containsKey(SERVICES) ? ImmutableList.<Service>of() :
        codec.getGson().<List<Service>>fromJson(inJson.get(SERVICES), new TypeToken<List<Service>>() {}.getType());

      newClusterTemplates = !inJson.containsKey(CLUSTER_TEMPLATES) ?
        ImmutableList.<ClusterTemplate>of() :
        codec.getGson().<List<ClusterTemplate>>fromJson(inJson.get(CLUSTER_TEMPLATES),
                                                        new TypeToken<List<ClusterTemplate>>() {}.getType());
    } catch (JsonSyntaxException e) {
      LOG.error("Got exception while importing config", e);
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Json syntax error");
      return;
    }

    // Delete all existing config data
    for (Provider provider : entityStore.getAllProviders()) {
      entityStore.deleteProvider(provider.getName());
    }

    for (HardwareType hardwareType : entityStore.getAllHardwareTypes()) {
      entityStore.deleteHardwareType(hardwareType.getName());
    }

    for (ImageType imageType : entityStore.getAllImageTypes()) {
      entityStore.deleteImageType(imageType.getName());
    }

    for (Service service : entityStore.getAllServices()) {
      entityStore.deleteService(service.getName());
    }

    for (ClusterTemplate clusterTemplate : entityStore.getAllClusterTemplates()) {
      entityStore.deleteClusterTemplate(clusterTemplate.getName());
    }

    // Add new config data
    LOG.debug("Importing {} providers", newProviders.size());
    for (Provider provider : newProviders) {
      entityStore.writeProvider(provider);
    }

    LOG.debug("Importing {} hardware types", newHardwareTypes.size());
    for (HardwareType hardwareType : newHardwareTypes) {
      entityStore.writeHardwareType(hardwareType);
    }

    LOG.debug("Importing {} image types", newImageTypes.size());
    for (ImageType imageType : newImageTypes) {
      entityStore.writeImageType(imageType);
    }

    LOG.debug("Importing {} services", newServices.size());
    for (Service service : newServices) {
      entityStore.writeService(service);
    }

    LOG.debug("Importing {} cluster types", newClusterTemplates.size());
    for (ClusterTemplate clusterTemplate : newClusterTemplates) {
      entityStore.writeClusterTemplate(clusterTemplate);
    }

    responder.sendStatus(HttpResponseStatus.OK);
  }

  private <T> T getEntityFromRequest(HttpRequest request, HttpResponder responder, Type tClass) {
    T result = null;
    try {
      Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
      try {
        result = codec.deserialize(reader, tClass);
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
      responder.sendString(HttpResponseStatus.OK, codec.getGson().toJson(entity, entityClass));
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
