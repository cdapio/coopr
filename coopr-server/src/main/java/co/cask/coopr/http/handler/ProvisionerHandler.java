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
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.ProvisionerHeartbeat;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.provisioner.plugin.PluginType;
import co.cask.coopr.provisioner.plugin.ResourceService;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.http.ChunkResponder;
import co.cask.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Handles requests from provisioners to register themselves, send heartbeats, delete themselves, and get plugin
 * resources.
 */
@Path(Constants.API_BASE)
public final class ProvisionerHandler extends AbstractAuthHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ProvisionerHandler.class);

  private final Gson gson;
  private final ResourceService resourceService;
  private final TenantProvisionerService tenantProvisionerService;
  private final EntityStoreService entityStoreService;

  @Inject
  private ProvisionerHandler(TenantStore tenantStore,
                             TenantProvisionerService tenantProvisionerService,
                             EntityStoreService entityStoreService,
                             ResourceService resourceService,
                             Gson gson) {
    super(tenantStore);
    this.gson = gson;
    this.resourceService = resourceService;
    this.tenantProvisionerService = tenantProvisionerService;
    this.entityStoreService = entityStoreService;
  }

  /**
   * Write a provisioner.
   *
   * @param request The request to write a provisioner.
   * @param responder Responder to send the response.
   */
  @PUT
  @Path("/provisioners/{provisioner-id}")
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
  @Path("/provisioners/{provisioner-id}/heartbeat")
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
  @Path("/provisioners/{provisioner-id}")
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

  /**
   * Get the contents of a specific resource.
   *
   * @param request Request to get an automator type resource
   * @param responder Responder for responding to the request
   * @param tenantId Id of the tenant that owns the resource
   * @param automatortypeId Id of the automator type that owns the resource
   * @param resourceType Type of resource to get
   * @param name Name of the resource to get
   * @param version Version of the resource to get
   */
  @GET
  @Path("/tenants/{tenant-id}/automatortypes/{automatortype-id}/{type}/{name}/versions/{version}")
  public void getAutomatorResource(HttpRequest request, HttpResponder responder,
                                   @PathParam("tenant-id") String tenantId,
                                   @PathParam("automatortype-id") String automatortypeId,
                                   @PathParam("type") String resourceType,
                                   @PathParam("name") String name,
                                   @PathParam("version") String version) {
    Account account = new Account(Constants.ADMIN_USER, tenantId);

    ResourceType resourceTypeObj = new ResourceType(PluginType.AUTOMATOR, automatortypeId, resourceType);
    sendResourceInChunks(responder, account, resourceTypeObj, name, version);
  }

  /**
   * Get the contents of a specific resource.
   *
   * @param request Request to get an provider type resource
   * @param responder Responder for responding to the request
   * @param tenantId Id of the tenant that owns the resource
   * @param providertypeId Id of the provider type that owns the resource
   * @param resourceType Type of resource to get
   * @param name Name of the resource to get
   * @param version Version of the resource to get
   */
  @GET
  @Path("/tenants/{tenant-id}/providertypes/{providertype-id}/{type}/{name}/versions/{version}")
  public void getProviderTypeResource(HttpRequest request, HttpResponder responder,
                                      @PathParam("tenant-id") String tenantId,
                                      @PathParam("providertype-id") String providertypeId,
                                      @PathParam("type") String resourceType,
                                      @PathParam("name") String name,
                                      @PathParam("version") String version) {
    Account account = new Account(Constants.ADMIN_USER, tenantId);

    ResourceType resourceTypeObj = new ResourceType(PluginType.PROVIDER, providertypeId, resourceType);
    sendResourceInChunks(responder, account, resourceTypeObj, name, version);
  }

  /**
   * Writes a {@link co.cask.coopr.spec.plugin.ProviderType}. User must be admin or a 403 is returned.
   * If the name in the path does not match the name in the put body, a 400 is returned.
   *
   * @param request Request to write provider type.
   * @param responder Responder to send response.
   * @param providertypeId Id of the provider type to write.
   */
  @PUT
  @Path("/plugins/providertypes/{providertype-id}")
  public void putProviderType(HttpRequest request, HttpResponder responder,
                              @PathParam("providertype-id") String providertypeId) {
    ProviderType providerType = getEntityFromRequest(request, responder, ProviderType.class);
    if (providerType == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!providerType.getName().equals(providertypeId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between provider type name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(Account.SUPERADMIN).writeProviderType(providerType);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Exception writing provider type " + providertypeId);
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "user unauthorized to write provider type.");
    }
  }

  /**
   * Writes a {@link co.cask.coopr.spec.plugin.AutomatorType}. User must be admin or a 403 is returned.
   * If the name in the path does not match the name in the put body, a 400 is returned.
   *
   * @param request Request to write provider type.
   * @param responder Responder to send response.
   * @param automatortypeId Id of the provider type to write.
   */
  @PUT
  @Path("/plugins/automatortypes/{automatortype-id}")
  public void putAutomatorType(HttpRequest request, HttpResponder responder,
                               @PathParam("automatortype-id") String automatortypeId) {
    AutomatorType automatorType = getEntityFromRequest(request, responder, AutomatorType.class);
    if (automatorType == null) {
      // getEntityFromRequest writes to the responder if there was an issue.
      return;
    } else if (!automatorType.getName().equals(automatortypeId)) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "mismatch between automator type name and name in path.");
      return;
    }

    try {
      entityStoreService.getView(Account.SUPERADMIN).writeAutomatorType(automatorType);
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

  private void sendResourceInChunks(HttpResponder responder, Account account,
                                    ResourceType resourceType, String name, String versionStr) {
    try {
      int version = Integer.parseInt(versionStr);
      InputStream inputStream =
        resourceService.getResourceInputStream(account, resourceType, name, version);
      if (inputStream == null) {
        LOG.error("No input stream available, but metadata exists for version {} of resource {} for tenant {}.",
                  version, name, account.getTenantId());
        responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error getting resource.");
      }
      try {
        ChunkResponder chunkResponder = responder.sendChunkStart(
          HttpResponseStatus.OK, ImmutableMultimap.<String, String>of());
        while (true) {
          byte[] chunkBytes = new byte[Constants.PLUGIN_RESOURCE_CHUNK_SIZE];
          int bytesRead = inputStream.read(chunkBytes, 0, Constants.PLUGIN_RESOURCE_CHUNK_SIZE);
          if (bytesRead == -1) {
            break;
          }
          chunkResponder.sendChunk(ChannelBuffers.wrappedBuffer(chunkBytes, 0, bytesRead));
        }
        chunkResponder.close();
      } finally {
        inputStream.close();
      }
    } catch (NumberFormatException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid version " + versionStr);
    } catch (IOException e) {
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error getting resource.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, "Resource not found.");
    }
  }
}
