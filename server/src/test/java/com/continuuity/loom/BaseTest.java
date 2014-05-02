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

import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.scheduler.callback.ClusterCallback;
import com.continuuity.loom.scheduler.callback.MockClusterCallback;
import com.continuuity.loom.store.EntityStore;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.guice.LoomModules;
import com.continuuity.loom.store.ClusterStore;
import com.continuuity.loom.store.SQLClusterStore;
import com.google.inject.AbstractModule;
import com.google.inject.util.Modules;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Base class with utilities for loading admin entities into a entityStore and starting zookeeper up.
 */
public class BaseTest {
  private static InMemoryZKServer zkServer;
  private static SQLClusterStore sqlClusterStore;
  protected static final String HOSTNAME = "127.0.0.1";
  protected static Injector injector;
  protected static ZKClientService zkClientService;
  protected static EntityStore entityStore;
  protected static ClusterStore clusterStore;
  protected static Configuration conf;
  protected static MockClusterCallback mockClusterCallback;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupBase() throws Exception {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(5000).build();
    zkServer.startAndWait();

    zkClientService = ZKClientService.Builder.of(zkServer.getConnectionStr()).build();
    zkClientService.startAndWait();

    conf = new Configuration();
    conf.set(Constants.PORT, "0");
    conf.set(Constants.HOST, HOSTNAME);
    conf.set(Constants.SCHEDULER_INTERVAL_SECS, "1");
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");

    mockClusterCallback = new MockClusterCallback();
    injector = Guice.createInjector(
      Modules.override(
        LoomModules.createModule(zkClientService, MoreExecutors.sameThreadExecutor(), conf)
      ).with(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(ClusterCallback.class).toInstance(mockClusterCallback);
          }
        }
      )
    );

    entityStore = injector.getInstance(EntityStore.class);
    sqlClusterStore = injector.getInstance(SQLClusterStore.class);
    sqlClusterStore.initialize();
    sqlClusterStore.initDerbyDB();
    clusterStore = sqlClusterStore;
  }

  @AfterClass
  public static void teardownBase() {
    zkClientService.stopAndWait();
    zkServer.stopAndWait();
    try {
      DriverManager.getConnection("jdbc:derby:memory:loom;drop=true");
    } catch (SQLException e) {
      // this is normal when a drop happens
      if (!e.getSQLState().equals("08006") ) {
        Throwables.propagate(e);
      }
    }
  }

  @Before
  public void setupBaseTest() throws SQLException {
    sqlClusterStore.clearData();
  }
}
