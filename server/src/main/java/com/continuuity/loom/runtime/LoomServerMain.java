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
package com.continuuity.loom.runtime;

import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.guice.LoomModules;
import com.continuuity.loom.http.LoomService;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.Scheduler;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.RetryStrategies;
import org.apache.twill.zookeeper.ZKClientService;
import org.apache.twill.zookeeper.ZKClientServices;
import org.apache.twill.zookeeper.ZKClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Loom server.
 */
public final class LoomServerMain extends DaemonMain {
  private static final Logger LOG = LoggerFactory.getLogger(LoomServerMain.class);

  private InMemoryZKServer inMemoryZKServer;
  private ZKClientService zkClientService;
  private Injector injector;
  private LoomService loomService;
  private Scheduler scheduler;
  private Configuration conf;
  private int solverNumThreads;
  private ListeningExecutorService executorService;

  public static void main(final String[] args) throws Exception {
    new LoomServerMain().doMain(args);
  }

  @Override
  public void init(String[] args) {
    try {
      // TODO: switch config class, dont use hadoop.
      conf = new Configuration();
      conf.addResource("loom-site.xml");

      String zkQuorum = conf.get(Constants.Zookeeper.QUORUM);
      if (zkQuorum == null) {
        String dataPath = conf.get(Constants.LOCAL_DATA_DIR, Constants.DEFAULT_LOCAL_DATA_DIR) + "/zookeeper";
        inMemoryZKServer = InMemoryZKServer.builder().setDataDir(new File(dataPath)).setTickTime(2000).build();
        LOG.warn(Constants.Zookeeper.QUORUM + " not specified, defaulting to in memory zookeeper with data dir "
                   + dataPath);
      } else {
        zkClientService = getZKService(conf.get(Constants.Zookeeper.QUORUM));
      }

      solverNumThreads = conf.getInt(Constants.SOLVER_NUM_THREADS, Constants.DEFAULT_SOLVER_NUM_THREADS);
    } catch (Exception e) {
      LOG.error("Exception initializing loom", e);
    }
  }

  @Override
  public void start() {
    LOG.info("Starting Loom...");
    // if no zk quorum is given, use the in memory zk server
    if (inMemoryZKServer != null) {
      inMemoryZKServer.startAndWait();
      zkClientService = getZKService(inMemoryZKServer.getConnectionStr());
    }
    zkClientService.startAndWait();

    executorService = MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(solverNumThreads,
                                   new ThreadFactoryBuilder()
                                     .setNameFormat("solver-scheduler-%d")
                                     .setDaemon(true)
                                     .build()));

    try {
      // this is here because loom modules does things that need to connect to zookeeper...
      // TODO: move everything that needs zk started out of the module
      injector = Guice.createInjector(LoomModules.createModule(zkClientService, executorService, conf));

      ClusterStore clusterStore = injector.getInstance(ClusterStore.class);
      clusterStore.initialize();
      for (String queueName : Constants.Queue.ALL) {
        TimeoutTrackingQueue queue = injector.getInstance(Key.get(TimeoutTrackingQueue.class, Names.named(queueName)));
        queue.start();
      }

      // Register MBean
      LoomStats loomStats = injector.getInstance(LoomStats.class);
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("com.continuuity.loom:type=LoomStats");
      mbs.registerMBean(loomStats, name);
    } catch (Exception e) {
      LOG.error("Exception starting up.", e);
      System.exit(-1);
    }
    loomService = injector.getInstance(LoomService.class);
    loomService.startAndWait();
    LOG.info("Loom service started on {}", loomService.getBindAddress());

    scheduler = injector.getInstance(Scheduler.class);
    scheduler.startAndWait();
    LOG.info("Scheduler started successfully.");
  }

  /**
   * Invoked by jsvc to stop the program.
   */
  @Override
  public void stop() {
    LOG.info("Stopping Loom...");

    if (scheduler != null) {
      scheduler.stopAndWait();
    }
    if (executorService != null) {
      executorService.shutdown();
      try {
        executorService.awaitTermination(100, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        LOG.error("Got Exception: ", e);
      }
    }

    if (loomService != null) {
      loomService.stopAndWait();
    }

    if (zkClientService != null) {
      zkClientService.stopAndWait();
    }

    if (inMemoryZKServer != null) {
      inMemoryZKServer.stopAndWait();
    }
  }

  /**
   * Invoked by jsvc for resource cleanup.
   */
  @Override
  public void destroy() {
  }

  private ZKClientService getZKService(String connectString) {
    return ZKClientServices.delegate(
      ZKClients.reWatchOnExpire(
        ZKClients.retryOnFailure(
          ZKClientService.Builder.of(connectString)
            .setSessionTimeout(conf.getInt(Constants.Zookeeper.CFG_SESSION_TIMEOUT_MILLIS,
                                           Constants.Zookeeper.DEFAULT_SESSION_TIMEOUT_MILLIS)
            ).build(),
          RetryStrategies.fixDelay(2, TimeUnit.SECONDS)
        )
      )
    );
  }
}
