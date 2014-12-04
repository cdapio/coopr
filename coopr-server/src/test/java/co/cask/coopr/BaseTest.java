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
package co.cask.coopr;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.guice.ConfigModule;
import co.cask.cdap.common.guice.DiscoveryRuntimeModule;
import co.cask.cdap.common.guice.IOModule;
import co.cask.cdap.security.guice.SecurityModules;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.conf.guice.ConfigurationModule;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.guice.QueueModule;
import co.cask.coopr.common.zookeeper.IdService;
import co.cask.coopr.common.zookeeper.guice.ZookeeperModule;
import co.cask.coopr.http.guice.HttpModule;
import co.cask.coopr.provisioner.MockProvisionerRequestService;
import co.cask.coopr.provisioner.ProvisionerRequestService;
import co.cask.coopr.provisioner.plugin.ResourceService;
import co.cask.coopr.scheduler.callback.ClusterCallback;
import co.cask.coopr.scheduler.callback.MockClusterCallback;
import co.cask.coopr.scheduler.guice.SchedulerModule;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.SQLClusterStoreService;
import co.cask.coopr.store.credential.CredentialStore;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.guice.TestStoreModule;
import co.cask.coopr.store.provisioner.MemoryPluginStore;
import co.cask.coopr.store.provisioner.PluginMetaStoreService;
import co.cask.coopr.store.provisioner.ProvisionerStore;
import co.cask.coopr.store.provisioner.SQLPluginMetaStoreService;
import co.cask.coopr.store.provisioner.SQLProvisionerStore;
import co.cask.coopr.store.tenant.SQLTenantStore;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.coopr.store.user.SQLUserStore;
import co.cask.coopr.store.user.UserStore;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.RetryStrategies;
import org.apache.twill.zookeeper.ZKClientService;
import org.apache.twill.zookeeper.ZKClientServices;
import org.apache.twill.zookeeper.ZKClients;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.util.concurrent.TimeUnit;
import java.util.Map.Entry;

/**
 * Base class with utilities for loading admin entities into a entityStore and starting zookeeper up.
 */
public class BaseTest {
  protected static final String HOSTNAME = "127.0.0.1";

  private static InMemoryZKServer zkServer;
  private static SQLClusterStoreService sqlClusterStoreService;
  private static SQLProvisionerStore sqlProvisionerStore;
  private static SQLPluginMetaStoreService sqlMetaStoreService;
  private static SQLTenantStore sqlTenantStore;
  private static SQLUserStore sqlUserStore;
  protected static Injector injector;
  protected static ZKClientService zkClientService;
  protected static EntityStoreService entityStoreService;
  protected static SQLClusterStoreService clusterStoreService;
  protected static PluginMetaStoreService metaStoreService;
  protected static ResourceService resourceService;
  protected static ClusterStore clusterStore;
  protected static TenantStore tenantStore;
  protected static UserStore userStore;
  protected static ProvisionerStore provisionerStore;
  protected static MemoryPluginStore pluginStore;
  protected static Configuration conf;
  protected static MockClusterCallback mockClusterCallback;
  protected static IdService idService;
  protected static CredentialStore credentialStore;
  protected static QueueService queueService;
  protected static Gson gson;
  protected static CConfiguration cConfiguration;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  public static Configuration createTestConf() {
    Configuration conf = Configuration.create();
    conf.setInt(Constants.EXTERNAL_PORT, 0);
    conf.setInt(Constants.INTERNAL_PORT, 0);
    conf.set(Constants.HOST, HOSTNAME);
    conf.setInt(Constants.SCHEDULER_INTERVAL_SECS, 1);
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:coopr;create=true");
    return conf;
  }

  public static CConfiguration createTestCConf() {
    CConfiguration cConf = CConfiguration.create();
    for (Entry<String, String> prop: conf) {
      cConf.set(prop.getKey(), prop.getValue());
    }
    return cConf;
  }

  @BeforeClass
  public static void setupBase() throws Exception {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(5000).build();
    zkServer.startAndWait();

    conf = createTestConf();

    zkClientService = ZKClientServices.delegate(
      ZKClients.reWatchOnExpire(
        ZKClients.retryOnFailure(
          ZKClientService.Builder.of(zkServer.getConnectionStr())
            .setSessionTimeout(conf.getInt(Constants.ZOOKEEPER_SESSION_TIMEOUT_MILLIS))
            .build(),
          RetryStrategies.fixDelay(2, TimeUnit.SECONDS)
        )
      )
    );
    zkClientService.startAndWait();

    cConfiguration = createTestCConf();

    mockClusterCallback = new MockClusterCallback();
    injector = Guice.createInjector(
      Modules.override(
        new ConfigurationModule(conf),
        new ZookeeperModule(zkClientService),
        new TestStoreModule(),
        new QueueModule(zkClientService),
        new HttpModule(),
        new SchedulerModule(conf, MoreExecutors.sameThreadExecutor(), MoreExecutors.sameThreadExecutor()),
        new CodecModules().getModule(),
        new IOModule(),
        new DiscoveryRuntimeModule().getStandaloneModules(),
        new SecurityModules().getStandaloneModules(),
        new ConfigModule(cConfiguration)
      ).with(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(ClusterCallback.class).toInstance(mockClusterCallback);
            bind(ProvisionerRequestService.class).to(MockProvisionerRequestService.class).in(Scopes.SINGLETON);
            bind(MockProvisionerRequestService.class).in(Scopes.SINGLETON);
          }
        }
      )
    );

    idService = injector.getInstance(IdService.class);
    idService.startAndWait();
    entityStoreService = injector.getInstance(EntityStoreService.class);
    entityStoreService.startAndWait();
    sqlClusterStoreService = injector.getInstance(SQLClusterStoreService.class);
    sqlClusterStoreService.startAndWait();
    clusterStoreService = sqlClusterStoreService;
    tenantStore = injector.getInstance(TenantStore.class);
    tenantStore.startAndWait();
    clusterStore = clusterStoreService.getSystemView();
    sqlProvisionerStore = injector.getInstance(SQLProvisionerStore.class);
    provisionerStore = sqlProvisionerStore;
    provisionerStore.startAndWait();
    sqlMetaStoreService = injector.getInstance(SQLPluginMetaStoreService.class);
    metaStoreService = sqlMetaStoreService;
    resourceService = injector.getInstance(ResourceService.class);
    resourceService.startAndWait();
    sqlTenantStore = injector.getInstance(SQLTenantStore.class);
    tenantStore = sqlTenantStore;
    gson = injector.getInstance(Gson.class);
    pluginStore = injector.getInstance(MemoryPluginStore.class);
    credentialStore = injector.getInstance(CredentialStore.class);
    sqlUserStore = injector.getInstance(SQLUserStore.class);
    sqlUserStore.startAndWait();
    userStore = sqlUserStore;
    queueService = injector.getInstance(QueueService.class);
    queueService.startAndWait();
  }

  @AfterClass
  public static void teardownBase() {
    zkClientService.stopAndWait();
    zkServer.stopAndWait();
    DBHelper.dropDerbyDB();
  }

  @After
  public void cleanupBaseTest() throws Exception {
    if (shouldClearDataBetweenTests()) {
      sqlTenantStore.clearData();
      sqlClusterStoreService.clearData();
      sqlProvisionerStore.clearData();
      sqlMetaStoreService.clearData();
      sqlUserStore.clearData();
      pluginStore.clearData();
      credentialStore.wipe();
    }
  }

  protected boolean shouldClearDataBetweenTests() {
    return true;
  }
}
