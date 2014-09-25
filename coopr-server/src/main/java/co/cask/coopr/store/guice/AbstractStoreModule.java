package co.cask.coopr.store.guice;

import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.cluster.SQLClusterStoreService;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.entity.SQLEntityStoreService;
import co.cask.coopr.store.node.NodeStoreService;
import co.cask.coopr.store.node.SQLNodeStoreService;
import co.cask.coopr.store.provisioner.PluginMetaStoreService;
import co.cask.coopr.store.provisioner.ProvisionerStore;
import co.cask.coopr.store.provisioner.SQLPluginMetaStoreService;
import co.cask.coopr.store.provisioner.SQLProvisionerStore;
import co.cask.coopr.store.tenant.SQLTenantStore;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.coopr.store.user.SQLUserStore;
import co.cask.coopr.store.user.UserStore;
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
    bind(NodeStoreService.class).to(SQLNodeStoreService.class).in(Scopes.SINGLETON);
    bind(TenantStore.class).to(SQLTenantStore.class).in(Scopes.SINGLETON);
    bind(ProvisionerStore.class).to(SQLProvisionerStore.class).in(Scopes.SINGLETON);
    bind(PluginMetaStoreService.class).to(SQLPluginMetaStoreService.class).in(Scopes.SINGLETON);
    bind(UserStore.class).to(SQLUserStore.class).in(Scopes.SINGLETON);
    bind(DBConnectionPool.class).in(Scopes.SINGLETON);
    bind(SQLClusterStoreService.class).in(Scopes.SINGLETON);
    bind(SQLEntityStoreService.class).in(Scopes.SINGLETON);
    bind(SQLTenantStore.class).in(Scopes.SINGLETON);
    bind(SQLProvisionerStore.class).in(Scopes.SINGLETON);
    bind(SQLPluginMetaStoreService.class).in(Scopes.SINGLETON);
    bind(SQLUserStore.class).in(Scopes.SINGLETON);
  }
}
