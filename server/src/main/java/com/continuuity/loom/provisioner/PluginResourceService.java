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
package com.continuuity.loom.provisioner;

import com.continuuity.http.BodyConsumer;
import com.continuuity.http.HttpResponder;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.continuuity.loom.store.provisioner.PluginResourceMetaStoreService;
import com.continuuity.loom.store.provisioner.PluginResourceMetaStoreView;
import com.continuuity.loom.store.provisioner.PluginStore;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing plugin modules.
 */
public class PluginResourceService extends AbstractIdleService {
  private static final Logger LOG  = LoggerFactory.getLogger(PluginResourceService.class);
  private final Configuration conf;
  private final PluginStore pluginStore;
  private final PluginResourceMetaStoreService metaStoreService;
  private final ZKClient zkClient;

  @Inject
  private PluginResourceService(PluginStore pluginStore, PluginResourceMetaStoreService metaStoreService,
                                ZKClient zkClient, Configuration conf) {
    this.conf = conf;
    this.pluginStore = pluginStore;
    this.metaStoreService = metaStoreService;
    this.zkClient = ZKClients.namespace(zkClient, Constants.PLUGIN_LOCK_NAMESPACE);
  }

  /**
   * Create a body consumer for streaming resource contents into the persistent store.
   *
   * @param account Account that is uploading the resource
   * @param resourceType Type of resource to upload
   * @param resourceName Name of resource to upload
   * @param resourceVersion Version of resource to upload
   * @param responder Responder for responding to the upload request
   * @return BodyConsumer for consuming the resource contents and streaming them to the persistent store
   * @throws IOException if there was an error getting the output stream for writing to the persistent store
   */
  public BodyConsumer createResourceBodyConsumer(final Account account,
                                                 final PluginResourceType resourceType,
                                                 final String resourceName,
                                                 final String resourceVersion,
                                                 final HttpResponder responder) throws IOException {
    final PluginResourceMeta resourceMeta = PluginResourceMeta.createNew(resourceName, resourceVersion);
    final OutputStream os = pluginStore.getResourceOutputStream(account, resourceType, resourceMeta);
    final ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    return new BodyConsumer() {
      @Override
      public void chunk(ChannelBuffer request, HttpResponder responder) {
        try {
          request.readBytes(os, request.readableBytes());
        } catch (IOException e) {
          LOG.error("Error during upload of version {} of resource {} for account {}.",
                    resourceVersion, resourceName, account, e);
          responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
      }

      @Override
      public void finished(HttpResponder responder) {
        try {
          os.close();
          metaStoreService.getView(account, resourceType).write(resourceMeta);
          responder.sendString(HttpResponseStatus.OK, "Upload Complete");
        } catch (Exception e) {
          LOG.error("Error finishing upload of resource {} of type {} for account {}.",
                    resourceMeta, resourceType, account, e);
          responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
          lock.release();
        }
      }

      @Override
      public void handleError(Throwable t) {
        try {
          os.close();
          responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, t.getCause().getMessage());
        } catch (IOException e) {
          LOG.error("Error uploading resource {} of type {} for account {}.", resourceMeta, resourceType, account, e);
        } finally {
          lock.release();
        }
      }
    };
  }

  /**
   * Get an input stream for reading the plugin resource.
   *
   * @param account Account the resource belongs to
   * @param resourceType Type of resource
   * @param resourceName Name of resource to get an input stream for
   * @param resourceVersion Version of resource to get an input stream for
   * @return Input stream for reading the given plugin resource
   * @throws MissingEntityException if there is no such resource version
   * @throws IOException if there was an error getting the input stream for the resource
   */
  public InputStream getResourceInputStream(
    final Account account, PluginResourceType resourceType, String resourceName, String resourceVersion)
    throws MissingEntityException, IOException {
    // no lock needed since each resource uploaded gets its own id.
    PluginResourceMeta meta = metaStoreService.getView(account, resourceType).get(resourceName, resourceVersion);
    if (meta == null) {
      throw new MissingEntityException("Resource not found.");
    }
    return pluginStore.getResourceInputStream(account, resourceType, meta);
  }

  /**
   * Atomically stage the specified resource version for the given account and unstage the previous staged version.
   * A staged version will get pushed to provisioners during a sync, and will stay staged unless explicitly unstaged.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to stage
   * @param resourceName Name of resource to stage
   * @param resourceVersion Version of resource to stage
   * @throws MissingEntityException if there is no such resource version
   * @throws IOException if there was an error stsaging the resource
   */
  public void stage(Account account, PluginResourceType resourceType, String resourceName, String resourceVersion)
    throws MissingEntityException, IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      if (!view.exists(resourceName)) {
        throw new MissingEntityException("Module does not exist.");
      }
      view.stage(resourceName, resourceVersion);
    } finally {
      lock.release();
    }
  }

  /**
   * Unstage the given resource for the given account. A no-op if there is no staged version.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to deactivate
   * @param resourceName Name of the resource to deactivate
   * @param resourceVersion Version of resource to stage
   * @throws MissingEntityException if there is no such module
   * @throws IOException if there was an error deactivating all versions of the module
   */
  public void unstage(Account account, PluginResourceType resourceType, String resourceName, String resourceVersion)
    throws MissingEntityException, IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      if (!view.exists(resourceName)) {
        throw new MissingEntityException("Resource " + resourceName + " does not exist.");
      }
      view.unstage(resourceName, resourceVersion);
    } finally {
      lock.release();
    }
  }

  /**
   * Atomically promote the staged version of the resource into the active state and the active version of the resource
   * into the inactive state.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to activate
   * @param resourceName Name of the resource to activate
   * @throws IOException if there was an error activating the resource
   */
  public void activate(Account account, PluginResourceType resourceType, String resourceName) throws IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      view.activate(resourceName);
    } finally {
      lock.release();
    }
  }

  /**
   * Get all resource metadata of the given type that belong to the given account that have the given status.
   *
   * @param account Account containing the resources
   * @param resourceType Type of resource to get
   * @param status Status of the resources to get. If null, resources of any status are returned.
   * @return Immutable map of resource name to resource metadata
   * @throws IOException if there was an error getting the resources
   */
  public Map<String, Set<PluginResourceMeta>> getAll(Account account, PluginResourceType resourceType,
                                                     PluginResourceStatus status) throws IOException {
    if (status == null) {
      return metaStoreService.getView(account, resourceType).getAll();
    }
    return metaStoreService.getView(account, resourceType).getAll(status);
  }

  /**
   * Get all metadata for versions of the given resource that have the given status.
   *
   * @param account Account containing the resource
   * @param resourceType Type of resource to get
   * @param resourceName Name of the resource to get
   * @param status Status of the resources to get. If null, resources of any status are returned.
   * @return Immutable set of metadata for versions of the given module
   * @throws IOException if there was an error getting the module versions
   */
  public Set<PluginResourceMeta> getAll(Account account, PluginResourceType resourceType,
                                        String resourceName, PluginResourceStatus status) throws IOException {
    if (status == null) {
      return metaStoreService.getView(account, resourceType).getAll(resourceName);
    }
    return metaStoreService.getView(account, resourceType).getAll(resourceName, status);
  }

  /**
   * Delete the given resource. Can only be done if the resource is inactive.
   *
   * @param account Account containing the module
   * @param resourceType Type of resource to delete
   * @param resourceName Name of resource to delete
   * @param resourceVersion Version of resource to delete
   * @throws IllegalStateException if the resource is not in a deletable state
   * @throws IOException if there was an error deleting the module version
   */
  public void delete(Account account, PluginResourceType resourceType,
                     String resourceName, String resourceVersion) throws IllegalStateException, IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      PluginResourceMeta meta = view.get(resourceName, resourceVersion);
      if (meta.getStatus() != PluginResourceStatus.INACTIVE) {
        throw new IllegalStateException("Resource must be inactive before it can be deleted.");
      }
      view.delete(resourceName, resourceVersion);
      pluginStore.deleteResource(account, resourceType, meta);
    } finally {
      lock.release();
    }
  }

  /**
   * Delete all versions of the given resource. Can only be done if all versions are inactive.
   *
   * @param account Account containing the module
   * @param resourceType Type of resource to delete
   * @param resourceName Name of resource to delete
   * @throws IllegalStateException if not all the resources are in a deletable state
   * @throws IOException if there was an error deleting the module version
   */
  public void delete(Account account, PluginResourceType resourceType,
                     String resourceName) throws IllegalStateException, IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      Set<PluginResourceMeta> metas = view.getAll(resourceName);
      for (PluginResourceMeta meta : metas) {
        if (meta.getStatus() != PluginResourceStatus.INACTIVE) {
          throw new IllegalStateException("All versions must be inactive before the resource can be deleted.");
        }
      }
      for (PluginResourceMeta meta : metas) {
        view.delete(resourceName, meta.getVersion());
        pluginStore.deleteResource(account, resourceType, meta);
      }
    } finally {
      lock.release();
    }
  }

  @Override
  protected void startUp() throws Exception {
    pluginStore.initialize(conf);
    metaStoreService.startAndWait();
  }

  @Override
  protected void shutDown() throws Exception {
    metaStoreService.stopAndWait();
  }

  private ZKInterProcessReentrantLock getLock(Account account, PluginResourceType type, String resourceName) {
    String path = new StringBuilder()
      .append("/")
      .append(account.getTenantId())
      .append("/")
      .append(type.getPluginType().name().toLowerCase())
      .append("s")
      .append("/")
      .append(type.getPluginName())
      .append("/")
      .append(type.getResourceType())
      .append("/")
      .append(resourceName)
      .toString();
    return new ZKInterProcessReentrantLock(zkClient, path);
  }
}
