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
package com.continuuity.loom;

import com.continuuity.loom.codec.json.guice.CodecModules;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.common.queue.guice.QueueModule;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.common.zookeeper.guice.ZookeeperModule;
import com.continuuity.loom.http.guice.HttpModule;
import com.continuuity.loom.provisioner.MockProvisionerRequestService;
import com.continuuity.loom.provisioner.ProvisionerRequestService;
import com.continuuity.loom.provisioner.plugin.ResourceService;
import com.continuuity.loom.scheduler.callback.ClusterCallback;
import com.continuuity.loom.scheduler.callback.MockClusterCallback;
import com.continuuity.loom.scheduler.guice.SchedulerModule;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.SQLClusterStoreService;
import com.continuuity.loom.store.credential.CredentialStore;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.guice.TestStoreModule;
import com.continuuity.loom.store.provisioner.MemoryPluginStore;
import com.continuuity.loom.store.provisioner.PluginMetaStoreService;
import com.continuuity.loom.store.provisioner.ProvisionerStore;
import com.continuuity.loom.store.provisioner.SQLPluginMetaStoreService;
import com.continuuity.loom.store.provisioner.SQLProvisionerStore;
import com.continuuity.loom.store.tenant.SQLTenantStore;
import com.continuuity.loom.store.tenant.TenantStore;
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

/**
 * Base class with utilities for loading admin entities into a entityStore and starting zookeeper up.
 */
public class BaseTest {
  private static InMemoryZKServer zkServer;
  private static SQLClusterStoreService sqlClusterStoreService;
  private static SQLProvisionerStore sqlProvisionerStore;
  private static SQLPluginMetaStoreService sqlMetaStoreService;
  private static SQLTenantStore sqlTenantStore;
  protected static final String HOSTNAME = "127.0.0.1";
  protected static Injector injector;
  protected static ZKClientService zkClientService;
  protected static EntityStoreService entityStoreService;
  protected static SQLClusterStoreService clusterStoreService;
  protected static PluginMetaStoreService metaStoreService;
  protected static ResourceService resourceService;
  protected static ClusterStore clusterStore;
  protected static TenantStore tenantStore;
  protected static ProvisionerStore provisionerStore;
  protected static MemoryPluginStore pluginStore;
  protected static Configuration conf;
  protected static MockClusterCallback mockClusterCallback;
  protected static IdService idService;
  protected static CredentialStore credentialStore;
  protected static Gson gson;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  public static Configuration createTestConf() {
    Configuration conf = Configuration.create();
    conf.set(Constants.PORT, "0");
    conf.set(Constants.HOST, HOSTNAME);
    conf.set(Constants.SCHEDULER_INTERVAL_SECS, "1");
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    return conf;
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

    mockClusterCallback = new MockClusterCallback();
    injector = Guice.createInjector(
      Modules.override(
        new ConfigurationModule(conf),
        new ZookeeperModule(zkClientService),
        new TestStoreModule(),
        new QueueModule(zkClientService),
        new HttpModule(),
        new SchedulerModule(conf, MoreExecutors.sameThreadExecutor(), MoreExecutors.sameThreadExecutor()),
        new CodecModules().getModule()
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
      sqlTenantStore.clearData();
      pluginStore.clearData();
      credentialStore.wipe();
    }
  }

  protected boolean shouldClearDataBetweenTests() {
    return true;
  }
}
