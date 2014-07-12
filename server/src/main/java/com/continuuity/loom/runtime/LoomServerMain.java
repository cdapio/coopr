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

import com.continuuity.loom.codec.json.guice.CodecModule;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.common.queue.guice.QueueModule;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.common.zookeeper.guice.ZookeeperModule;
import com.continuuity.loom.http.guice.HttpModule;
import com.continuuity.loom.http.handler.LoomService;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.management.guice.ManagementModule;
import com.continuuity.loom.scheduler.Scheduler;
import com.continuuity.loom.scheduler.guice.SchedulerModule;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.guice.StoreModule;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
  private ListeningExecutorService solverExecutorService;
  private ListeningExecutorService callbackExecutorService;
  private ClusterStoreService clusterStoreService;
  private EntityStoreService entityStoreService;
  private IdService idService;
  private TenantStore tenantStore;

  public static void main(final String[] args) throws Exception {
    new LoomServerMain().doMain(args);
  }

  @Override
  public void init(String[] args) {
    try {
      conf = Configuration.create();

      String zkQuorum = conf.get(Constants.ZOOKEEPER_QUORUM);
      if (zkQuorum == null) {
        String dataPath = conf.get(Constants.LOCAL_DATA_DIR) + "/zookeeper";
        inMemoryZKServer = InMemoryZKServer.builder().setDataDir(new File(dataPath)).setTickTime(2000).build();
        LOG.warn(Constants.ZOOKEEPER_QUORUM + " not specified, defaulting to in memory zookeeper with data dir "
                   + dataPath);
      } else {
        zkClientService = getZKService(conf.get(Constants.ZOOKEEPER_QUORUM));
      }

      solverNumThreads = conf.getInt(Constants.SOLVER_NUM_THREADS);
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

    solverExecutorService = MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(solverNumThreads,
                                   new ThreadFactoryBuilder()
                                     .setNameFormat("solver-scheduler-%d")
                                     .setDaemon(true)
                                     .build()));

    callbackExecutorService = MoreExecutors.listeningDecorator(
      Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                      .setNameFormat("callback-%d")
                                      .setDaemon(true)
                                      .build()));

    try {
      // this is here because loom modules does things that need to connect to zookeeper...
      // TODO: move everything that needs zk started out of the module
      injector = Guice.createInjector(
        new ConfigurationModule(conf),
        new ZookeeperModule(zkClientService),
        new StoreModule(),
        new QueueModule(zkClientService),
        new SchedulerModule(conf, callbackExecutorService, solverExecutorService),
        new HttpModule(),
        new ManagementModule(),
        new CodecModule()
      );

      idService = injector.getInstance(IdService.class);
      idService.startAndWait();
      tenantStore = injector.getInstance(TenantStore.class);
      tenantStore.startAndWait();
      clusterStoreService = injector.getInstance(ClusterStoreService.class);
      clusterStoreService.startAndWait();
      entityStoreService = injector.getInstance(EntityStoreService.class);
      entityStoreService.startAndWait();

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
    if (solverExecutorService != null) {
      solverExecutorService.shutdown();
      try {
        solverExecutorService.awaitTermination(100, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        LOG.error("Got Exception: ", e);
      }
    }
    if (callbackExecutorService != null) {
      callbackExecutorService.shutdown();
      try {
        callbackExecutorService.awaitTermination(100, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        LOG.error("Got Exception: ", e);
      }
    }

    stopAll(loomService, tenantStore, clusterStoreService,
            entityStoreService, idService, zkClientService, inMemoryZKServer);
  }

  private void stopAll(Service... services) {
    for (Service service : services) {
      if (service != null) {
        service.stopAndWait();
      }
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
      ZKClients.namespace(
        ZKClients.reWatchOnExpire(
          ZKClients.retryOnFailure(
            ZKClientService.Builder.of(connectString)
              .setSessionTimeout(conf.getInt(Constants.ZOOKEEPER_SESSION_TIMEOUT_MILLIS))
              .build(),
            RetryStrategies.fixDelay(2, TimeUnit.SECONDS)
          )
        ), conf.get(Constants.ZOOKEEPER_NAMESPACE)
      )
    );
  }
}
