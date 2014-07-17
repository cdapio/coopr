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
import java.util.List;

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
   * @param resourceMeta Metadata of resource to upload
   * @param responder Responder for responding to the upload request
   * @return BodyConsumer for consuming the resource contents and streaming them to the persistent store
   * @throws IOException if there was an error getting the output stream for writing to the persistent store
   */
  public BodyConsumer createResourceBodyConsumer(final Account account,
                                                 final PluginResourceType resourceType,
                                                 final PluginResourceMeta resourceMeta,
                                                 final HttpResponder responder) throws IOException {
    final OutputStream os = pluginStore.getResourceOutputStream(account, resourceType, resourceMeta);
    final ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceMeta.getName());
    lock.acquire();
    return new BodyConsumer() {
      @Override
      public void chunk(ChannelBuffer request, HttpResponder responder) {
        try {
          request.readBytes(os, request.readableBytes());
        } catch (IOException e) {
          LOG.error("Error during upload of resource {} of type {} for account {}.",
                    resourceMeta, resourceType, account, e);
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

  public InputStream getResourceInputStream(final Account account, PluginResourceType resourceType,
                                            PluginResourceMeta resourceMeta) throws IOException {
    // TODO: implement read, write locks
    final ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceMeta.getName());
    lock.acquire();
    try {
      return pluginStore.getResourceInputStream(account, resourceType, resourceMeta);
    } finally {
      lock.release();
    }
  }

  /**
   * Activate the specified resource version for the given account.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to activate
   * @param resourceMeta Metadata of resource to activate
   * @throws MissingEntityException if there is no such resource version
   * @throws IOException if there was an error activating the resource
   */
  public void activate(Account account, PluginResourceType resourceType, PluginResourceMeta resourceMeta)
    throws MissingEntityException, IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceMeta.getName());
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      if (!view.exists(resourceMeta)) {
        throw new MissingEntityException("Module does not exist.");
      }
      view.activate(resourceMeta.getName(), resourceMeta.getVersion());
    } finally {
      lock.release();
    }
  }

  /**
   * Atomically deactivate all versions of the given resource for the given account.
   *
   * @param account Account that contains the resource
   * @param resourceType Type of resource to deactivate
   * @param resourceName Name of the resource to deactivate
   * @throws MissingEntityException if there is no such module
   * @throws IOException if there was an error deactivating all versions of the module
   */
  public void deactivate(Account account, PluginResourceType resourceType, String resourceName)
    throws MissingEntityException, IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceName);
    lock.acquire();
    try {
      PluginResourceMetaStoreView view = metaStoreService.getView(account, resourceType);
      if (view.getAll(resourceName).isEmpty()) {
        throw new MissingEntityException("Modules do not exist.");
      }
      view.deactivate(resourceName);
    } finally {
      lock.release();
    }
  }

  /**
   * Get all resource metadata of the given type that belong to the given account, optionally filtering out
   * inactive resources.
   *
   * @param account Account containing the resources
   * @param resourceType Type of resource to get
   * @param activeOnly Whether or not to return only active module versions
   * @return Immutable list of metadata for resource of the given type, owned by the given account
   * @throws IOException if there was an error getting the resources
   */
  public List<PluginResourceMeta> getAll(Account account, PluginResourceType resourceType, boolean activeOnly)
    throws IOException {
    if (activeOnly) {
      return metaStoreService.getView(account, resourceType).getAllActive();
    } else {
      return metaStoreService.getView(account, resourceType).getAll();
    }
  }

  /**
   * Get all metadata for versions of the given resource.
   *
   * @param account Account containing the resource
   * @param resourceType Type of resource to get
   * @param resourceName Name of the resource to get
   * @return Immutable list of metadata for versions of the given module
   * @throws IOException if there was an error getting the module versions
   */
  public List<PluginResourceMeta> getVersions(Account account, PluginResourceType resourceType,
                                              String resourceName) throws IOException {
    return metaStoreService.getView(account, resourceType).getAll(resourceName);
  }

  /**
   * Get the metadata for the active version of the given resource, or null if none exists.
   *
   * @param account Account containing the resource
   * @param resourceType Type of resource to get
   * @param resourceName Name of the resource to get
   * @return Metadata for the active version of the given module, or null if none exists
   * @throws IOException if there was an error getting the module versions
   */
  public PluginResourceMeta getActiveVersion(Account account, PluginResourceType resourceType,
                                             String resourceName) throws IOException {
    return metaStoreService.getView(account, resourceType).getActive(resourceName);
  }

  /**
   * Delete the given resource.
   *
   * @param account Account containing the module
   * @param resourceType Type of resource to delete
   * @param resourceMeta Metadata of resource to delete
   * @throws IOException if there was an error deleting the module version
   */
  public void delete(Account account, PluginResourceType resourceType,
                     PluginResourceMeta resourceMeta) throws IOException {
    ZKInterProcessReentrantLock lock = getLock(account, resourceType, resourceMeta.getName());
    lock.acquire();
    try {
      metaStoreService.getView(account, resourceType).delete(resourceMeta);
      pluginStore.deleteResource(account, resourceType, resourceMeta);
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
