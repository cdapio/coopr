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
package com.continuuity.loom.store;

import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.google.common.base.Throwables;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class BaseClusterStoreTest {
  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();
  private static InMemoryZKServer zkServer;
  private static ZKClientService zkClient;

  @BeforeClass
  public static void beforeClass() throws SQLException, ClassNotFoundException, IOException {
    zkServer = InMemoryZKServer.builder().setDataDir(tmpFolder.newFolder()).setTickTime(1000).build();
    zkServer.startAndWait();

    zkClient = ZKClientService.Builder.of(zkServer.getConnectionStr()).build();
    zkClient.startAndWait();
  }

  @AfterClass
  public static void afterClass() {
    zkClient.stopAndWait();
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

  @Test
  public void testIds() throws InterruptedException, SQLException, ClassNotFoundException {
    Configuration conf = Configuration.create();
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    conf.setLong(Constants.ID_START_NUM, 3);
    conf.setLong(Constants.ID_INCREMENT_BY, 10);
    DBConnectionPool dbConnectionPool = new DBConnectionPool(conf);
    final SQLClusterStore store = new SQLClusterStore(zkClient, dbConnectionPool, conf);
    store.initialize();
    store.initDerbyDB();
    store.clearData();

    final int incrementsPerThread = 100;
    final int numThreads = 20;
    final CyclicBarrier barrier = new CyclicBarrier(numThreads);
    final CountDownLatch latch = new CountDownLatch(numThreads);

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    for (int i = 0; i < numThreads; i++) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          for (int j = 0; j < incrementsPerThread; j++) {
            try {
              barrier.await();
              store.getNewClusterId();
            } catch (Exception e) {
              Throwables.propagate(e);
            }
          }
          latch.countDown();
        }
      });
    }

    latch.await();

    // counter starts at 3, goes up by 10 each time
    long expected = 3 + numThreads * incrementsPerThread * 10;
    long actual = Long.valueOf(store.getNewClusterId());
    Assert.assertEquals(expected, actual);
  }

}
