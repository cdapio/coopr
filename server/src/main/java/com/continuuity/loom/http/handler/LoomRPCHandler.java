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
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.current.NodePropertiesRequestCodec;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.http.request.BootstrapRequest;
import com.continuuity.loom.http.request.NodePropertiesRequest;
import com.continuuity.loom.provisioner.TenantProvisionerService;
import com.continuuity.loom.provisioner.plugin.ResourceService;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.entity.EntityStoreView;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for RPCs.
 */
@Path("/v1/loom")
public class LoomRPCHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomRPCHandler.class);
  private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(NodePropertiesRequest.class, new NodePropertiesRequestCodec()).create();
  private final EntityStoreService entityStoreService;
  private final ClusterStoreService clusterStoreService;
  private final ClusterStore clusterStore;
  private final ResourceService resourceService;
  private final TenantProvisionerService tenantProvisionerService;

  @Inject
  private LoomRPCHandler(TenantStore tenantStore,
                         EntityStoreService entityStoreService,
                         ClusterStoreService clusterStoreService,
                         ResourceService resourceService,
                         TenantProvisionerService tenantProvisionerService,
                         Configuration conf) {
    super(tenantStore, conf);
    this.entityStoreService = entityStoreService;
    this.clusterStoreService = clusterStoreService;
    this.clusterStore = clusterStoreService.getSystemView();
    this.resourceService = resourceService;
    this.tenantProvisionerService = tenantProvisionerService;
  }

  /**
   * Bootstraps a tenant by copying all entities and plugin resources from the superadmin into the tenant. Will not
   * overwrite existing content unless the body contains overwrite=true.
   *
   * @param request Request to bootstrap the tenant
   * @param responder Responder for responding to the request
   * @throws Exception
   */
  @POST
  @Path("/bootstrap")
  public void bootstrapTenant(HttpRequest request, HttpResponder responder) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (account.isSuperadmin()) {
      responder.sendString(HttpResponseStatus.OK, "Nothing to bootstrap for superadmin.");
      return;
    }
    if (!account.isAdmin()) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "User is not allowed to bootstrap.");
      return;
    }

    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    BootstrapRequest bootstrapRequest = null;
    try {
      bootstrapRequest = GSON.fromJson(reader, BootstrapRequest.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid request body. Must be a valid JSON Object.");
      return;
    }

    boolean forced = bootstrapRequest == null ? false : bootstrapRequest.isForced();
    EntityStoreView superadminView = entityStoreService.getView(Account.SUPERADMIN);
    EntityStoreView tenantView = entityStoreService.getView(account);

    try {
      if (!forced && !canBootstrap(account)) {
        responder.sendError(HttpResponseStatus.CONFLICT, "Cannot bootstrap unless the tenant is empty.");
        return;
      }

      // copy entities
      LOG.debug("Bootstrapping entities");
      for (HardwareType hardwareType : superadminView.getAllHardwareTypes()) {
        if (!forced && tenantView.getHardwareType(hardwareType.getName()) != null) {
          continue;
        }
        tenantView.writeHardwareType(hardwareType);
      }
      for (ImageType imageType : superadminView.getAllImageTypes()) {
        if (!forced && tenantView.getImageType(imageType.getName()) != null) {
          continue;
        }
        tenantView.writeImageType(imageType);
      }
      for (Service service : superadminView.getAllServices()) {
        if (!forced && tenantView.getService(service.getName()) != null) {
          continue;
        }
        tenantView.writeService(service);
      }
      for (ClusterTemplate template : superadminView.getAllClusterTemplates()) {
        if (!forced && tenantView.getClusterTemplate(template.getName()) != null) {
          continue;
        }
        tenantView.writeClusterTemplate(template);
      }
      for (Provider provider : superadminView.getAllProviders()) {
        if (!forced && tenantView.getProvider(provider.getName()) != null) {
          continue;
        }
        tenantView.writeProvider(provider);
      }

      // bootstrap plugin resources
      LOG.debug("Bootstrapping plugin resources");
      resourceService.bootstrapResources(account);
      LOG.debug("Syncing plugin resources");
      tenantProvisionerService.syncResources(account);

      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IOException e) {
      LOG.error("Exception bootstrapping account {}", account);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error boostrapping account.");
    } catch (IllegalAccessException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "User not allowed to write entities.");
    }
  }

  // can bootstrap if there is nothing in the account
  private boolean canBootstrap(Account account) throws IOException {
    EntityStoreView tenantView = entityStoreService.getView(account);
    return tenantView.getAllProviders().isEmpty() &&
      tenantView.getAllHardwareTypes().isEmpty() &&
      tenantView.getAllImageTypes().isEmpty() &&
      tenantView.getAllServices().isEmpty() &&
      tenantView.getAllClusterTemplates().isEmpty() &&
      resourceService.numResources(account) == 0;
  }

  /**
   * Get the cluster status for all clusters readable by the user making the request.
   *
   * @param request The request for cluster statuses.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @POST
  @Path("/getClusterStatuses")
  public void getClusterStatuses(HttpRequest request, HttpResponder responder) throws Exception {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }

    // TODO: Improve this logic by using a table join instead of separate calls for cluster and jobId

    List<Cluster> clusters = clusterStoreService.getView(account).getAllClusters();
    if (clusters.size() == 0) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, String.format("No clusters found"));
      return;
    }

    JsonArray response = new JsonArray();

    Map<JobId, Cluster> clusterMap = Maps.newHashMap();
    for (Cluster cluster : clusters) {
      clusterMap.put(JobId.fromString(cluster.getLatestJobId()), cluster);
    }

    Map<JobId, ClusterJob> jobs = clusterStore.getClusterJobs(clusterMap.keySet(), account.getTenantId());

    if (jobs.size() == 0) {
      responder.sendError(HttpResponseStatus.NOT_FOUND, String.format("No jobs found for clusters"));
      return;
    }

    for (JobId jobId : jobs.keySet()) {
      response.add(LoomClusterHandler.getClusterResponseJson(clusterMap.get(jobId), jobs.get(jobId)));
    }

    responder.sendJson(HttpResponseStatus.OK, response);
  }

  /**
   * Get properties of nodes from a specific cluster visible to the user. POST body is a JSON object that
   * must contain "clusterId", and may contain "properties" and "services". The "properties" key maps to an array
   * of node properties like "ipaddress" and "hostname" to return in the response. The "services" key maps to an
   * array of service names, indicating that all nodes returned by have all services given in the array. The response
   * is a JSON object with node ids as keys and JSON objects as values, where the value contains the properties passed
   * in, or all properties if none were passed in.
   *
   * @param request Request for node properties in a cluster.
   * @param responder Responder for sending the response.
   * @throws Exception
   */
  @POST
  @Path("/getNodeProperties")
  public void getNodeProperties(HttpRequest request, HttpResponder responder) throws Exception {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    NodePropertiesRequest nodeRequest;
    try {
      nodeRequest = GSON.fromJson(reader, NodePropertiesRequest.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid request body. Must be a valid JSON Object.");
      return;
    }

    Map<String, JsonObject> output = Maps.newHashMap();
    Set<Node> clusterNodes = clusterStoreService.getView(account).getClusterNodes(nodeRequest.getClusterId());
    Set<String> properties = nodeRequest.getProperties();
    Set<String> requiredServices = nodeRequest.getServices();

    for (Node node : clusterNodes) {
      Set<String> nodeServices = Sets.newHashSet();
      for (Service service : node.getServices()) {
        nodeServices.add(service.getName());
      }

      // if the node has all services needed
      if (nodeServices.containsAll(requiredServices)) {
        JsonObject outputProperties;
        JsonObject nodeProperties = GSON.toJsonTree(node.getProperties()).getAsJsonObject();
        // if the request contains a list of properties, just include those properties
        if (properties.size() > 0) {
          outputProperties = new JsonObject();
          // add all requested node properties
          for (String property : properties) {
            if (nodeProperties.has(property)) {
              outputProperties.add(property, nodeProperties.get(property));
            }
          }
        } else {
          // request did not contain a list of properties, include them all
          outputProperties = nodeProperties;
        }
        output.put(node.getId(), outputProperties);
      }
    }

    responder.sendJson(HttpResponseStatus.OK, output);
  }
}
