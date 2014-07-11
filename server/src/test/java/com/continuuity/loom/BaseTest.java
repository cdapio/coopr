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

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.common.queue.guice.QueueModule;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.common.zookeeper.guice.ZookeeperModule;
import com.continuuity.loom.http.guice.HttpModule;
import com.continuuity.loom.scheduler.callback.ClusterCallback;
import com.continuuity.loom.scheduler.callback.MockClusterCallback;
import com.continuuity.loom.scheduler.guice.SchedulerModule;
import com.continuuity.loom.store.DBQueryHelper;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.SQLClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.guice.StoreModule;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.sql.SQLException;

/**
 * Base class with utilities for loading admin entities into a entityStore and starting zookeeper up.
 */
public class BaseTest {
  private static InMemoryZKServer zkServer;
  private static SQLClusterStoreService sqlClusterStoreService;
  protected static final String HOSTNAME = "127.0.0.1";
  protected static Injector injector;
  protected static ZKClientService zkClientService;
  protected static EntityStoreService entityStoreService;
  protected static SQLClusterStoreService clusterStoreService;
  protected static ClusterStore clusterStore;
  protected static TenantStore tenantStore;
  protected static Configuration conf;
  protected static MockClusterCallback mockClusterCallback;
  protected static IdService idService;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupBase() throws Exception {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(5000).build();
    zkServer.startAndWait();

    zkClientService = ZKClientService.Builder.of(zkServer.getConnectionStr()).build();
    zkClientService.startAndWait();

    conf = Configuration.create();
    conf.set(Constants.PORT, "0");
    conf.set(Constants.HOST, HOSTNAME);
    conf.set(Constants.SCHEDULER_INTERVAL_SECS, "1");
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");

    mockClusterCallback = new MockClusterCallback();
    injector = Guice.createInjector(
      Modules.override(
        new ConfigurationModule(conf),
        new ZookeeperModule(zkClientService),
        new StoreModule(),
        new QueueModule(zkClientService),
        new HttpModule(),
        new SchedulerModule(conf, MoreExecutors.sameThreadExecutor(), MoreExecutors.sameThreadExecutor())
      ).with(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(ClusterCallback.class).toInstance(mockClusterCallback);
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
  }

  @AfterClass
  public static void teardownBase() {
    zkClientService.stopAndWait();
    zkServer.stopAndWait();
    DBQueryHelper.dropDerbyDB();
  }

  @Before
  public void setupBaseTest() throws SQLException {
    sqlClusterStoreService.clearData();
  }
}
