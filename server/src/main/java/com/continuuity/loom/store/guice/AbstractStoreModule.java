package com.continuuity.loom.store.guice;

import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.cluster.SQLClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.entity.SQLEntityStoreService;
import com.continuuity.loom.store.provisioner.PluginMetaStoreService;
import com.continuuity.loom.store.provisioner.ProvisionerStore;
import com.continuuity.loom.store.provisioner.SQLPluginMetaStoreService;
import com.continuuity.loom.store.provisioner.SQLProvisionerStore;
import com.continuuity.loom.store.tenant.SQLTenantStore;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 *
 */
public abstract class AbstractStoreModule extends AbstractModule {

  abstract void bindPluginStore();

  @Override
  protected void configure() {
    bindCommon();
    bindPluginStore();
  }

  protected void bindCommon() {
    bind(EntityStoreService.class).to(SQLEntityStoreService.class).in(Scopes.SINGLETON);
    bind(ClusterStoreService.class).to(SQLClusterStoreService.class).in(Scopes.SINGLETON);
    bind(TenantStore.class).to(SQLTenantStore.class).in(Scopes.SINGLETON);
    bind(ProvisionerStore.class).to(SQLProvisionerStore.class).in(Scopes.SINGLETON);
    bind(PluginMetaStoreService.class).to(SQLPluginMetaStoreService.class).in(Scopes.SINGLETON);
    bind(DBConnectionPool.class).in(Scopes.SINGLETON);
    bind(SQLClusterStoreService.class).in(Scopes.SINGLETON);
    bind(SQLEntityStoreService.class).in(Scopes.SINGLETON);
    bind(SQLTenantStore.class).in(Scopes.SINGLETON);
    bind(SQLProvisionerStore.class).in(Scopes.SINGLETON);
    bind(SQLPluginMetaStoreService.class).in(Scopes.SINGLETON);
  }
}
