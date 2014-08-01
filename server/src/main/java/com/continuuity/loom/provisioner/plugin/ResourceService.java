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
package com.continuuity.loom.provisioner.plugin;

import com.continuuity.http.BodyConsumer;
import com.continuuity.http.HttpResponder;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.continuuity.loom.store.provisioner.PluginMetaStoreService;
import com.continuuity.loom.store.provisioner.PluginMetaStoreView;
import com.continuuity.loom.store.provisioner.PluginStore;
import com.google.common.base.Joiner;
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
public class ResourceService extends AbstractIdleService {
  private static final Logger LOG  = LoggerFactory.getLogger(ResourceService.class);
  private final Configuration conf;
  private final PluginStore pluginStore;
  private final PluginMetaStoreService metaStoreService;
  private final ZKClient zkClient;

  @Inject
  private ResourceService(PluginStore pluginStore, PluginMetaStoreService metaStoreService,
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
   * @param name Name of resource to upload
   * @param responder Responder for responding to the upload request
   * @return BodyConsumer for consuming the resource contents and streaming them to the persistent store
   * @throws IOException if there was an error getting the output stream for writing to the persistent store or writing
   *                     the plugin metadata
   */
  public BodyConsumer createResourceBodyConsumer(final Account account,
                                                 final ResourceType resourceType,
                                                 final String name,
                                                 final HttpResponder responder) throws IOException {
    final ZKInterProcessReentrantLock lock = getLock(account, resourceType, name);
    lock.acquire();
    try {
      PluginMetaStoreView view = metaStoreService.getView(account, resourceType);
      // ok to do versioning this way since we have a lock
      final int version = view.getHighestVersion(name) + 1;
      final ResourceMeta resourceMeta = new ResourceMeta(name, version);
      LOG.debug("getting output stream for version {} of resource {} of type {} for account {}",
                version, name, resourceType, account);
      // output stream is used to stream resource contents to the plugin store
      final OutputStream os = pluginStore.getResourceOutputStream(account, resourceType, name, version);
      // we write metadata here before the data completes streaming because we are guaranteed the
      // version will never be used again, and because we need to increment the highest version in case another
      // upload of the same name is started while this one is still going. It is deleted if the upload fails.
      metaStoreService.getView(account, resourceType).add(resourceMeta);

      return new BodyConsumer() {
        @Override
        public void chunk(ChannelBuffer request, HttpResponder responder) {
          try {
            request.readBytes(os, request.readableBytes());
          } catch (IOException e) {
            LOG.error("Error during upload of version {} of resource {} for account {}.",
                      version, name, account, e);
            responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
          }
        }

        @Override
        public void finished(HttpResponder responder) {
          try {
            os.close();
            responder.sendString(HttpResponseStatus.OK, "Upload Complete");
            LOG.debug("finished uploading resource.");
          } catch (Exception e) {
            LOG.error("Error finishing upload of resource {} of type {} for account {}.",
                      resourceMeta, resourceType, account, e);
            responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
          }
        }

        @Override
        public void handleError(Throwable t) {
          LOG.error("Error uploading version {} of resource {} of type {} for account {}.",
                    version, name, resourceType, account, t);
          try {
            os.close();
            // deletion flags the entry in the database as deleted
            metaStoreService.getView(account, resourceType).delete(name, version);
            responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, t.getCause().getMessage());
          } catch (IOException e) {
            LOG.error("Error uploading resource {} of type {} for account {}.", resourceMeta, resourceType, account, e);
          }
        }
      };
    } finally {
      lock.release();
    }
  }

  /**
   * Get an input stream for reading the plugin resource.
   *
   * @param account Account the resource belongs to
   * @param resourceType Type of resource
   * @param name Name of resource to get an input stream for
   * @param version Version of resource to get an input stream for
   * @return Input stream for reading the given plugin resource
   * @throws MissingEntityException if there is no such resource version
   * @throws IOException if there was an error getting the input stream for the resource
   */
  public InputStream getResourceInputStream(final Account account, ResourceType resourceType, String name, int version)
    throws MissingEntityException, IOException {
    // no lock needed since each resource uploaded gets its own id.
    ResourceMeta meta = metaStoreService.getView(account, resourceType).get(name, version);
    if (meta == null) {
      throw new MissingEntityException("Resource not found.");
    }
    LOG.debug("getting input stream for version {} of resource {} of type {} for account {}.",
              version, name, resourceType, account);
    return pluginStore.getResourceInputStream(account, resourceType, meta.getName(), meta.getVersion());
  }

  /**
   * Atomically stage the specified resource version for the given account and unstage the previous staged version.
   * A staged version will get pushed to provisioners during a sync, and will stay staged unless explicitly unstaged.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to stage
   * @param name Name of resource to stage
   * @param version Version of resource to stage
   * @throws MissingEntityException if there is no such resource version
   * @throws IOException if there was an error stsaging the resource
   */
  public void stage(Account account, ResourceType resourceType, String name, int version)
    throws MissingEntityException, IOException {
    LOG.debug("staging version {} of resource {} of type {} for account {}.",
              version, name, resourceType, account);
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, name);
    lock.acquire();
    try {
      PluginMetaStoreView view = metaStoreService.getView(account, resourceType);
      if (!view.exists(name)) {
        throw new MissingEntityException("Module does not exist.");
      }
      view.stage(name, version);
    } finally {
      lock.release();
    }
  }

  /**
   * Unstage the given resource for the given account. A no-op if there is no staged version.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to deactivate
   * @param name Name of the resource to deactivate
   * @param version Version of resource to stage
   * @throws MissingEntityException if there is no such module
   * @throws IOException if there was an error deactivating all versions of the module
   */
  public void unstage(Account account, ResourceType resourceType, String name, int version)
    throws MissingEntityException, IOException {
    LOG.debug("unstaging version {} of resource {} of type {} for account {}.",
              version, name, resourceType, account);
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, name);
    lock.acquire();
    try {
      PluginMetaStoreView view = metaStoreService.getView(account, resourceType);
      if (!view.exists(name)) {
        throw new MissingEntityException("Resource " + name + " does not exist.");
      }
      view.unstage(name, version);
    } finally {
      lock.release();
    }
  }

  /**
   * Atomically promote the staged version of the resource into the active state and the unstaged version
   * of the resource into the inactive state.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to activate
   * @param name Name of the resource to activate
   * @throws IOException if there was an error activating the resource
   */
  public void syncStatus(Account account, ResourceType resourceType, String name) throws IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, name);
    LOG.debug("syncing status of resource {} of type {} for account {}.", name, resourceType, account);
    lock.acquire();
    try {
      PluginMetaStoreView view = metaStoreService.getView(account, resourceType);
      view.syncStatus(name);
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
  public Map<String, Set<ResourceMeta>> getAll(Account account, ResourceType resourceType,
                                                     ResourceStatus status) throws IOException {
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
   * @param name Name of the resource to get
   * @param status Status of the resources to get. If null, resources of any status are returned.
   * @return Immutable set of metadata for versions of the given module
   * @throws IOException if there was an error getting the module versions
   */
  public Set<ResourceMeta> getAll(Account account, ResourceType resourceType,
                                        String name, ResourceStatus status) throws IOException {
    if (status == null) {
      return metaStoreService.getView(account, resourceType).getAll(name);
    }
    return metaStoreService.getView(account, resourceType).getAll(name, status);
  }

  /**
   * Delete the given resource. Can only be done if the resource is inactive.
   *
   * @param account Account containing the module
   * @param resourceType Type of resource to delete
   * @param name Name of resource to delete
   * @param version Version of resource to delete
   * @throws IllegalStateException if the resource is not in a deletable state
   * @throws IOException if there was an error deleting the module version
   */
  public void delete(Account account, ResourceType resourceType,
                     String name, int version) throws IllegalStateException, IOException {
    LOG.debug("deleting version {} of resource {} of type {} for account {}.", version, name, resourceType, account);
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, name);
    lock.acquire();
    try {
      PluginMetaStoreView view = metaStoreService.getView(account, resourceType);
      ResourceMeta meta = view.get(name, version);
      if (meta.getStatus() != ResourceStatus.INACTIVE) {
        throw new IllegalStateException("Resource must be inactive before it can be deleted.");
      }
      view.delete(name, version);
      LOG.debug("deleted version {} of resource {} of type {} for account {} from meta store.",
                version, name, resourceType, account);
      pluginStore.deleteResource(account, resourceType, meta.getName(), meta.getVersion());
      LOG.debug("deleted version {} of resource {} of type {} for account {} from plugin store.",
                version, name, resourceType, account);
    } finally {
      lock.release();
    }
  }

  /**
   * Delete all versions of the given resource. Can only be done if all versions are inactive.
   *
   * @param account Account containing the module
   * @param resourceType Type of resource to delete
   * @param name Name of resource to delete
   * @throws IllegalStateException if not all the resources are in a deletable state
   * @throws IOException if there was an error deleting the module version
   */
  public void delete(Account account, ResourceType resourceType,
                     String name) throws IllegalStateException, IOException {
    LOG.debug("deleting all versions of resource {} of type {} for account {}.", name, resourceType, account);
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, name);
    lock.acquire();
    try {
      PluginMetaStoreView view = metaStoreService.getView(account, resourceType);
      Set<ResourceMeta> metas = view.getAll(name);
      for (ResourceMeta meta : metas) {
        if (meta.getStatus() != ResourceStatus.INACTIVE) {
          throw new IllegalStateException("All versions must be inactive before the resource can be deleted.");
        }
      }
      for (ResourceMeta meta : metas) {
        view.delete(name, meta.getVersion());
        pluginStore.deleteResource(account, resourceType, meta.getName(), meta.getVersion());
        LOG.debug("deleted version {} of resource {} of type {} for account {}.",
                  meta.getVersion(), name, resourceType, account);
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

  // locks are namespaced by tenant and resource type and name. for example,
  // /tenant1/automator/chef-solo/cookbooks/reactor
  private ZKInterProcessReentrantLock getLock(Account account, ResourceType type, String name) {
    String path = Joiner.on('/')
      .join(account.getTenantId(),
            type.getPluginType().name().toLowerCase(),
            type.getPluginName(),
            type.getTypeName(),
            name);
    return new ZKInterProcessReentrantLock(zkClient, path);
  }
}
