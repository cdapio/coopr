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
package co.cask.coopr.runtime;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.guice.ConfigModule;
import co.cask.cdap.common.guice.DiscoveryRuntimeModule;
import co.cask.cdap.common.guice.IOModule;
import co.cask.cdap.common.kerberos.SecurityUtil;
import co.cask.cdap.security.guice.SecurityModules;
import co.cask.cdap.security.server.ExternalAuthenticationServer;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.conf.guice.ConfigurationModule;
import co.cask.coopr.common.daemon.DaemonMain;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.guice.QueueModule;
import co.cask.coopr.common.zookeeper.IdService;
import co.cask.coopr.common.zookeeper.guice.ZookeeperModule;
import co.cask.coopr.http.ExternalHandlerServer;
import co.cask.coopr.http.InternalHandlerServer;
import co.cask.coopr.http.guice.HttpModule;
import co.cask.coopr.management.ServerStats;
import co.cask.coopr.management.guice.ManagementModule;
import co.cask.coopr.provisioner.guice.ProvisionerModule;
import co.cask.coopr.provisioner.plugin.ResourceService;
import co.cask.coopr.scheduler.Scheduler;
import co.cask.coopr.scheduler.guice.SchedulerModule;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.credential.CredentialStore;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.guice.StoreModule;
import co.cask.coopr.store.provisioner.ProvisionerStore;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.coopr.store.user.UserStore;
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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Main class that starts up all services.
 */
public final class ServerMain extends DaemonMain {
  private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

  private InMemoryZKServer inMemoryZKServer;
  private ZKClientService zkClientService;
  private Injector injector;
  private ExternalHandlerServer externalHandlerServer;
  private InternalHandlerServer internalHandlerServer;
  private Scheduler scheduler;
  private Configuration conf;
  private CConfiguration cConf;
  private int solverNumThreads;
  private ListeningExecutorService solverExecutorService;
  private ListeningExecutorService callbackExecutorService;
  private ClusterStoreService clusterStoreService;
  private EntityStoreService entityStoreService;
  private ResourceService resourceService;
  private ProvisionerStore provisionerStore;
  private IdService idService;
  private TenantStore tenantStore;
  private UserStore userStore;
  private CredentialStore credentialStore;
  private QueueService queueService;
  // Authentication
  private boolean securityEnabled;
  private ExternalAuthenticationServer externalAuthenticationServer;

  public static void main(final String[] args) throws Exception {
    new ServerMain().doMain(args);
  }

  @Override
  public void init(String[] args) {
    try {
      conf = Configuration.create();

      cConf = CConfiguration.create();
      cConf.addResource("coopr-default.xml");
      cConf.addResource("coopr-site.xml");

      securityEnabled = conf.getBoolean(co.cask.cdap.common.conf.Constants.Security.ENABLED);

      if (securityEnabled) {
        // Enable Kerberos login
        SecurityUtil.enableKerberosLogin(cConf);
      }

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
      LOG.error("Exception initializing server", e);
    }
  }

  @Override
  public void start() {
    LOG.info("Starting server...");
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
      // this is here instead of in init because when it runs with in-process zookeeper, the zk client service
      // cannot be created until the server is started (needs connection string)
      injector = Guice.createInjector(
        new ConfigurationModule(conf),
        new ZookeeperModule(zkClientService),
        new StoreModule(conf),
        new QueueModule(zkClientService),
        new SchedulerModule(conf, callbackExecutorService, solverExecutorService),
        new HttpModule(),
        new ManagementModule(),
        new ProvisionerModule(),
        new CodecModules().getModule(),
        new IOModule(),
        new DiscoveryRuntimeModule().getStandaloneModules(),
        new SecurityModules().getStandaloneModules(),
        new ConfigModule(cConf)
      );

      idService = injector.getInstance(IdService.class);
      idService.startAndWait();
      tenantStore = injector.getInstance(TenantStore.class);
      tenantStore.startAndWait();
      clusterStoreService = injector.getInstance(ClusterStoreService.class);
      clusterStoreService.startAndWait();
      entityStoreService = injector.getInstance(EntityStoreService.class);
      entityStoreService.startAndWait();
      provisionerStore = injector.getInstance(ProvisionerStore.class);
      provisionerStore.startAndWait();
      resourceService = injector.getInstance(ResourceService.class);
      resourceService.startAndWait();
      userStore = injector.getInstance(UserStore.class);
      userStore.startAndWait();
      credentialStore = injector.getInstance(CredentialStore.class);
      credentialStore.startAndWait();
      queueService = injector.getInstance(QueueService.class);
      queueService.startAndWait();
      if (securityEnabled) {
        externalAuthenticationServer = injector.getInstance(ExternalAuthenticationServer.class);
        externalAuthenticationServer.startAndWait();
      }

      // Register MBean
      ServerStats serverStats = injector.getInstance(ServerStats.class);
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("co.cask.coopr:type=ServerStats");
      mbs.registerMBean(serverStats, name);
    } catch (Exception e) {
      LOG.error("Exception starting up.", e);
      System.exit(-1);
    }
    internalHandlerServer = injector.getInstance(InternalHandlerServer.class);
    internalHandlerServer.startAndWait();
    LOG.info("Internal API handler service started on {}", internalHandlerServer.getBindAddress());
    externalHandlerServer = injector.getInstance(ExternalHandlerServer.class);
    externalHandlerServer.startAndWait();
    LOG.info("External API handler service started on {}", externalHandlerServer.getBindAddress());

    scheduler = injector.getInstance(Scheduler.class);
    scheduler.startAndWait();
    LOG.info("Scheduler started successfully.");
  }

  /**
   * Invoked by jsvc to stop the program.
   */
  @Override
  public void stop() {
    LOG.info("Stopping server...");

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

    stopAll(internalHandlerServer, externalHandlerServer, queueService,
            userStore, resourceService, provisionerStore, tenantStore,
            clusterStoreService, entityStoreService, idService, zkClientService, inMemoryZKServer,
            externalAuthenticationServer);
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
