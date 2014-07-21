package com.continuuity.loom.store.guice;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.cluster.SQLClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.entity.SQLEntityStoreService;
import com.continuuity.loom.store.provisioner.MemoryPluginStore;
import com.continuuity.loom.store.provisioner.PluginResourceMetaStoreService;
import com.continuuity.loom.store.provisioner.PluginStore;
import com.continuuity.loom.store.provisioner.ProvisionerStore;
import com.continuuity.loom.store.provisioner.SQLPluginResourceMetaStoreService;
import com.continuuity.loom.store.provisioner.SQLProvisionerStore;
import com.continuuity.loom.store.tenant.SQLTenantStore;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Creates guice modules for binding store related classes.
 */
public class StoreModules {
  private final Class pluginStoreClass;

  public StoreModules(Configuration conf) throws ClassNotFoundException {
    this.pluginStoreClass = Class.forName(conf.get(Constants.PLUGIN_STORE_CLASS));
  }

  /**
   * Get the default module, which binds store classes to their default sql backed implementation.
   *
   * @return module that binds classes to SQL backed implementations.
   */
  public AbstractModule getDefaultModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityStoreService.class).to(SQLEntityStoreService.class).in(Scopes.SINGLETON);
        bind(ClusterStoreService.class).to(SQLClusterStoreService.class).in(Scopes.SINGLETON);
        bind(TenantStore.class).to(SQLTenantStore.class).in(Scopes.SINGLETON);
        bind(ProvisionerStore.class).to(SQLProvisionerStore.class).in(Scopes.SINGLETON);
        bind(PluginResourceMetaStoreService.class).to(SQLPluginResourceMetaStoreService.class).in(Scopes.SINGLETON);
        bind(PluginStore.class).to(pluginStoreClass).in(Scopes.SINGLETON);
        bind(DBConnectionPool.class).in(Scopes.SINGLETON);
        bind(SQLClusterStoreService.class).in(Scopes.SINGLETON);
        bind(SQLEntityStoreService.class).in(Scopes.SINGLETON);
        bind(SQLTenantStore.class).in(Scopes.SINGLETON);
        bind(SQLProvisionerStore.class).in(Scopes.SINGLETON);
        bind(SQLPluginResourceMetaStoreService.class).in(Scopes.SINGLETON);
      }
    };
  }

  /**
   * Get the test module, which binds store classes to their default sql backed implementations where applicable, and
   * memory backed implementations where not.
   *
   * @return module that binds some classes to memory backed implementations
   */
  public AbstractModule getTestModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityStoreService.class).to(SQLEntityStoreService.class).in(Scopes.SINGLETON);
        bind(ClusterStoreService.class).to(SQLClusterStoreService.class).in(Scopes.SINGLETON);
        bind(TenantStore.class).to(SQLTenantStore.class).in(Scopes.SINGLETON);
        bind(ProvisionerStore.class).to(SQLProvisionerStore.class).in(Scopes.SINGLETON);
        bind(PluginResourceMetaStoreService.class).to(SQLPluginResourceMetaStoreService.class).in(Scopes.SINGLETON);
        bind(PluginStore.class).to(MemoryPluginStore.class).in(Scopes.SINGLETON);
        bind(DBConnectionPool.class).in(Scopes.SINGLETON);
        bind(SQLClusterStoreService.class).in(Scopes.SINGLETON);
        bind(SQLEntityStoreService.class).in(Scopes.SINGLETON);
        bind(SQLTenantStore.class).in(Scopes.SINGLETON);
        bind(SQLProvisionerStore.class).in(Scopes.SINGLETON);
        bind(SQLPluginResourceMetaStoreService.class).in(Scopes.SINGLETON);
      }
    };
  }
}
