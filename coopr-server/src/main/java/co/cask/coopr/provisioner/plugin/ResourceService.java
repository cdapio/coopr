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
package co.cask.coopr.provisioner.plugin;

import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.utils.ImmutablePair;
import co.cask.coopr.common.zookeeper.LockService;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.entity.EntityStoreView;
import co.cask.coopr.store.provisioner.PluginMetaStoreService;
import co.cask.coopr.store.provisioner.PluginResourceTypeView;
import co.cask.coopr.store.provisioner.PluginStore;
import co.cask.http.BodyConsumer;
import co.cask.http.HttpResponder;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Service for managing plugin modules.
 */
public class ResourceService extends AbstractIdleService {
  private static final Logger LOG  = LoggerFactory.getLogger(ResourceService.class);
  private final Configuration conf;
  private final PluginStore pluginStore;
  private final EntityStoreService entityStoreService;
  private final PluginMetaStoreService metaStoreService;
  private final LockService lockService;
  private final Gson gson;

  @Inject
  private ResourceService(PluginStore pluginStore,
                          EntityStoreService entityStoreService,
                          PluginMetaStoreService metaStoreService,
                          LockService lockService,
                          Configuration conf,
                          Gson gson) {
    this.conf = conf;
    this.pluginStore = pluginStore;
    this.entityStoreService = entityStoreService;
    this.metaStoreService = metaStoreService;
    this.lockService = lockService;
    this.gson = gson;
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
    final Lock lock = getResourceLock(account, resourceType, name);
    lock.lock();
    try {
      PluginResourceTypeView view = metaStoreService.getResourceTypeView(account, resourceType);
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
      metaStoreService.getResourceTypeView(account, resourceType).add(resourceMeta);

      return new BodyConsumer() {
        @Override
        public void chunk(ChannelBuffer request, HttpResponder responder) {
          try {
            request.readBytes(os, request.readableBytes());
          } catch (IOException e) {
            LOG.error("Error during upload of version {} of resource {} for account {}.",
                      version, name, account, e);
            responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
          }
        }

        @Override
        public void finished(HttpResponder responder) {
          try {
            os.close();
            responder.sendJson(HttpResponseStatus.OK, resourceMeta, ResourceMeta.class, gson);
            LOG.debug("finished uploading resource.");
          } catch (Exception e) {
            LOG.error("Error finishing upload of resource {} of type {} for account {}.",
                      resourceMeta, resourceType, account, e);
            responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
          }
        }

        @Override
        public void handleError(Throwable t) {
          LOG.error("Error uploading version {} of resource {} of type {} for account {}.",
                    version, name, resourceType, account, t);
          try {
            os.close();
            // deletion flags the entry in the database as deleted
            metaStoreService.getResourceTypeView(account, resourceType).delete(name, version);
            // dont need the file in the plugin store if there was an error so delete it
            pluginStore.deleteResource(account, resourceType, name, version);
            responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, t.getCause().getMessage());
          } catch (IOException e) {
            LOG.error("Error uploading resource {} of type {} for account {}.", resourceMeta, resourceType, account, e);
          }
        }
      };
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the number of all versions of all resources in the account. Resources can be in any state.
   *
   * @param account Account to get the number of resources for
   * @return Number of resources in the account
   * @throws IOException
   */
  public int numResources(Account account) throws IOException {
    return metaStoreService.getAccountView(account).numResources();
  }

  /**
   * Bootstrap an account's resources by copying what the superadmin has into the account.
   *
   * @param account Account to bootstrap
   */
  public void bootstrapResources(Account account) throws IOException {
    for (ImmutablePair<ResourceType, ResourceTypeSpecification> typePair : getTypesAndFormats(Account.SUPERADMIN)) {
      ResourceType type = typePair.getFirst();
      Map<String, Set<ResourceMeta>> resources =
        metaStoreService.getResourceTypeView(Account.SUPERADMIN, type).getAll();
      for (Set<ResourceMeta> metas : resources.values()) {
        for (ResourceMeta meta : metas) {
          LOG.debug("copying version {} of resource {} of type {} from superadmin account to account {}",
                    meta.getVersion(), meta.getName(), type, account);
          copySuperadminResource(account, type, meta);
        }
      }
    }
  }

  private void copySuperadminResource(Account account, ResourceType type, ResourceMeta meta) throws IOException {
    String name = meta.getName();
    int version = meta.getVersion();
    Lock lock = getResourceLock(account, type, name);
    lock.lock();
    InputStream inStream = null;
    OutputStream outStream = null;
    try {
      inStream = pluginStore.getResourceInputStream(Account.SUPERADMIN, type, name, version);
      outStream = pluginStore.getResourceOutputStream(account, type, name, version);
      if (inStream == null) {
        LOG.error("Could not get input stream for version {} of resource {} of type {} for account {}.",
                  version, name, type, account);
        throw new IOException("Unable to get plugin data.");
      }
      ByteStreams.copy(inStream, outStream);
      PluginResourceTypeView metaView = metaStoreService.getResourceTypeView(account, type);
      if (!metaView.exists(name, version)) {
        metaView.add(meta);
      }
    } finally {
      if (outStream != null) {
        try {
          outStream.close();
        } catch (IOException e) {
          LOG.error("Exception closing output stream while copying superadmin resource.", e);
        }
      }
      if (inStream != null) {
        try {
          inStream.close();
        } catch (IOException e) {
          LOG.error("Exception closing input stream while copying superadmin resource.", e);
        }
      }
      lock.unlock();
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
    ResourceMeta meta = metaStoreService.getResourceTypeView(account, resourceType).get(name, version);
    if (meta == null) {
      throw new MissingEntityException("Resource not found.");
    }
    LOG.debug("getting input stream for version {} of resource {} of type {} for account {}.",
              version, name, resourceType, account);
    return pluginStore.getResourceInputStream(account, resourceType, meta.getName(), meta.getVersion());
  }

  /**
   * Atomically stage the specified resource version for the given account and recall the previous staged version.
   * A staged version will get pushed to provisioners during a sync, and will stay staged unless explicitly recalled.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to stage
   * @param name Name of resource to stage
   * @param version Version of resource to stage
   * @throws MissingEntityException if there is no such resource version
   * @throws IOException if there was an error staging the resource
   */
  public void stage(Account account, ResourceType resourceType, String name, int version)
    throws MissingEntityException, IOException {
    LOG.debug("staging version {} of resource {} of type {} for account {}.",
              version, name, resourceType, account);
    Lock lock = getResourceLock(account, resourceType, name);
    lock.lock();
    try {
      PluginResourceTypeView view = metaStoreService.getResourceTypeView(account, resourceType);
      if (!view.exists(name, version)) {
        throw new MissingEntityException("Resource does not exist.");
      }
      view.stage(name, version);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Recall the given resource for the given account. A no-op if there is no staged version.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to deactivate
   * @param name Name of the resource to deactivate
   * @param version Version of resource to stage
   * @throws MissingEntityException if there is no such module
   * @throws IOException if there was an error deactivating all versions of the module
   */
  public void recall(Account account, ResourceType resourceType, String name, int version)
    throws MissingEntityException, IOException {
    LOG.debug("Recalling version {} of resource {} of type {} for account {}.",
              version, name, resourceType, account);
    Lock lock = getResourceLock(account, resourceType, name);
    lock.lock();
    try {
      PluginResourceTypeView view = metaStoreService.getResourceTypeView(account, resourceType);
      if (!view.exists(name, version)) {
        throw new MissingEntityException("Resource " + name + " does not exist.");
      }
      view.recall(name, version);
    } finally {
      lock.unlock();
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
      return metaStoreService.getResourceTypeView(account, resourceType).getAll();
    }
    return metaStoreService.getResourceTypeView(account, resourceType).getAll(status);
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
      return metaStoreService.getResourceTypeView(account, resourceType).getAll(name);
    }
    return metaStoreService.getResourceTypeView(account, resourceType).getAll(name, status);
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
    Lock lock = getResourceLock(account, resourceType, name);
    lock.lock();
    try {
      PluginResourceTypeView view = metaStoreService.getResourceTypeView(account, resourceType);
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
      lock.unlock();
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
    Lock lock = getResourceLock(account, resourceType, name);
    lock.lock();
    try {
      PluginResourceTypeView view = metaStoreService.getResourceTypeView(account, resourceType);
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
      lock.unlock();
    }
  }

  /**
   * Get the active resources for a given account.
   *
   * @param account Account to get active resources for
   * @return Active resources for the given account
   */
  public ResourceCollection getLiveResources(Account account) throws IOException {
    ResourceCollection resourceCollection = new ResourceCollection();
    for (ImmutablePair<ResourceType, ResourceTypeSpecification> typeSpecs : getTypesAndFormats(account)) {
      ResourceType resourceType = typeSpecs.getFirst();
      ResourceTypeSpecification typeSpec = typeSpecs.getSecond();
      Set<ResourceMeta> resources = metaStoreService.getResourceTypeView(account, resourceType).getLiveResources();
      resourceCollection.addResources(resourceType, typeSpec, resources);
    }
    return resourceCollection;
  }

  /**
   * Get the resources that should be synced for a given account.
   *
   * @param account Account for which to get resources to sync
   * @return Resources that should be synced for the given account
   */
  public ResourceCollection getResourcesToSync(Account account) throws IOException {
    ResourceCollection resourceCollection = new ResourceCollection();

    Set<ImmutablePair<ResourceType, ResourceTypeSpecification>> typeFormats = getTypesAndFormats(account);
    Set<ResourceType> resourceTypes = Sets.newHashSet();
    for (ImmutablePair<ResourceType, ResourceTypeSpecification> typeFormat : typeFormats) {
      ResourceType resourceType = typeFormat.getFirst();
      ResourceTypeSpecification typeSpec = typeFormat.getSecond();
      Set<ResourceMeta> resources = metaStoreService.getResourceTypeView(account, resourceType).getResourcesToSync();
      resourceCollection.addResources(resourceType, typeSpec, resources);
      resourceTypes.add(resourceType);
    }

    return resourceCollection;
  }

  /**
   * Update the metadata store, syncing the resources in the given collection for the given account.
   *
   * @param account Account containing resources to sync
   * @param resourceCollection Collection of synced resources
   * @throws IOException
   */
  public void syncResourceMeta(Account account, ResourceCollection resourceCollection) throws IOException {
    metaStoreService.getAccountView(account).syncResources(resourceCollection);
  }

  // Helper function for getting all the resource types and formats for plugins belonging to an account.
  private Set<ImmutablePair<ResourceType, ResourceTypeSpecification>> getTypesAndFormats(Account account)
    throws IOException {
    Set<ImmutablePair<ResourceType, ResourceTypeSpecification>> typesAndFormats = Sets.newHashSet();
    EntityStoreView entityStoreView = entityStoreService.getView(account);

    for (AutomatorType plugin : entityStoreView.getAllAutomatorTypes()) {
      String pluginName = plugin.getName();
      for (Map.Entry<String, ResourceTypeSpecification> entry : plugin.getResourceTypes().entrySet()) {
        ResourceType resourceType = new ResourceType(PluginType.AUTOMATOR, pluginName, entry.getKey());
        typesAndFormats.add(ImmutablePair.of(resourceType, entry.getValue()));
      }
    }

    for (ProviderType plugin : entityStoreView.getAllProviderTypes()) {
      String pluginName = plugin.getName();
      for (Map.Entry<String, ResourceTypeSpecification> entry : plugin.getResourceTypes().entrySet()) {
        ResourceType resourceType = new ResourceType(PluginType.PROVIDER, pluginName, entry.getKey());
        typesAndFormats.add(ImmutablePair.of(resourceType, entry.getValue()));
      }
    }

    return typesAndFormats;
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
  private Lock getResourceLock(Account account, ResourceType type, String name) {
    return lockService.getResourceLock(account.getTenantId(), type.getPluginType().name().toLowerCase(),
                                       type.getPluginName(), type.getTypeName(), name);
  }
}
