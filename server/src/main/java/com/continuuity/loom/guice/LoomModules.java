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
package com.continuuity.loom.guice;

import com.continuuity.http.HttpHandler;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.continuuity.loom.common.queue.internal.ZKElementsTracking;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.http.LoomAdminHandler;
import com.continuuity.loom.http.LoomClusterHandler;
import com.continuuity.loom.http.LoomRPCHandler;
import com.continuuity.loom.http.LoomStatusHandler;
import com.continuuity.loom.http.LoomTaskHandler;
import com.continuuity.loom.http.LoomTenantHandler;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.ClusterScheduler;
import com.continuuity.loom.scheduler.JobScheduler;
import com.continuuity.loom.scheduler.Scheduler;
import com.continuuity.loom.scheduler.SolverScheduler;
import com.continuuity.loom.scheduler.callback.ClusterCallback;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.cluster.SQLClusterStoreService;
import com.continuuity.loom.store.entity.EntityStoreService;
import com.continuuity.loom.store.entity.SQLEntityStoreService;
import com.continuuity.loom.store.tenant.SQLTenantStore;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClientService;
import org.apache.twill.zookeeper.ZKClients;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Provides {@link com.google.inject.Guice} configuration modules for Loom component.
 */
public final class LoomModules {
  private static final String clusterManagerZKBasePath = "/clustermanager";

  /**
   * Creates a Guice module for dependency injection.
   *
   * @param zkClientService zookeeper client to use for queues and id generation.
   * @param executorService executor service to use for solving cluster layouts.
   * @param conf Configuration containing relevant settings.
   * @return Guice module for dependency injection.
   * @throws Exception
   */
  public static Module createModule(final ZKClientService zkClientService,
                                    final ListeningExecutorService executorService,
                                    final Configuration conf) throws Exception {

    final String namespace = conf.get(Constants.ZOOKEEPER_NAMESPACE);
    final String host = conf.get(Constants.HOST);
    final int schedulerIntervalSecs = conf.getInt(Constants.SCHEDULER_INTERVAL_SECS);
    final long cleanupIntervalSecs = conf.getLong(Constants.CLUSTER_CLEANUP_SECS);
    final long taskTimeoutSecs = conf.getLong(Constants.TASK_TIMEOUT_SECS);
    final long queueMsBetweenChecks = TimeUnit.SECONDS.toMillis(100);
    final long queueMsRescheduleTimeout = TimeUnit.SECONDS.toMillis(6000);
    // ids will start from this number
    final long idStartNum = conf.getLong(Constants.ID_START_NUM);
    // ids will increment by this
    final long idIncrementBy = conf.getLong(Constants.ID_INCREMENT_BY);
    Preconditions.checkArgument(idStartNum >= 0, Constants.ID_START_NUM + " must not be negative");
    Preconditions.checkArgument(idIncrementBy > 0, Constants.ID_INCREMENT_BY + " must be at least 1");

    final ZKClient zkClient = ZKClients.namespace(zkClientService, namespace);

    final TimeoutTrackingQueue clusterCreationQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/clustercreate"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    final TimeoutTrackingQueue solverQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/solver"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    final TimeoutTrackingQueue nodeProvisionTaskQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/nodeprovision"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    final TimeoutTrackingQueue jobSchedulerQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/jobscheduler"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    final TimeoutTrackingQueue callbackQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/callback"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);

    final ListeningExecutorService callbackExecutorService = MoreExecutors.listeningDecorator(
      Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                      .setNameFormat("callback-%d")
                                      .setDaemon(true)
                                      .build()));

    final Class callbackClass = Class.forName(conf.get(Constants.CALLBACK_CLASS));

    return new AbstractModule() {
        @Override
        protected void configure() {
          bind(Configuration.class).toInstance(conf);

          bind(TimeoutTrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.PROVISIONER)).toInstance(nodeProvisionTaskQueue);
          bind(TrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.PROVISIONER)).toInstance(nodeProvisionTaskQueue);
          bind(TimeoutTrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.CLUSTER)).toInstance(clusterCreationQueue);
          bind(TrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.CLUSTER)).toInstance(clusterCreationQueue);
          bind(TimeoutTrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.SOLVER)).toInstance(solverQueue);
          bind(TrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.SOLVER)).toInstance(solverQueue);
          bind(TimeoutTrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.JOB)).toInstance(jobSchedulerQueue);
          bind(TrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.JOB)).toInstance(jobSchedulerQueue);
          bind(TimeoutTrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.CALLBACK)).toInstance(callbackQueue);
          bind(TrackingQueue.class)
            .annotatedWith(Names.named(Constants.Queue.CALLBACK)).toInstance(callbackQueue);

          bind(ClusterCallback.class).to(callbackClass).in(Scopes.SINGLETON);
          bind(EntityStoreService.class).to(SQLEntityStoreService.class).in(Scopes.SINGLETON);
          bind(ClusterStoreService.class).to(SQLClusterStoreService.class).in(Scopes.SINGLETON);
          bind(TenantStore.class).to(SQLTenantStore.class).in(Scopes.SINGLETON);
          bind(ZKClient.class).toInstance(zkClient);
          bind(String.class)
            .annotatedWith(Names.named("scheduler.id")).toInstance("scheduler-" + host);
          bind(Integer.class)
            .annotatedWith(Names.named(Constants.SCHEDULER_INTERVAL_SECS)).toInstance(schedulerIntervalSecs);

          bind(Long.class)
            .annotatedWith(Names.named(Constants.CLUSTER_CLEANUP_SECS))
            .toInstance(cleanupIntervalSecs);
          bind(Long.class)
            .annotatedWith(Names.named(Constants.TASK_TIMEOUT_SECS))
            .toInstance(taskTimeoutSecs);

          bind(ListeningExecutorService.class)
            .annotatedWith(Names.named("solver.executor.service"))
            .toInstance(executorService);
          bind(ListeningExecutorService.class)
            .annotatedWith(Names.named("callback.executor.service"))
            .toInstance(callbackExecutorService);

          bind(JobScheduler.class).in(Scopes.SINGLETON);
          bind(ClusterScheduler.class).in(Scopes.SINGLETON);
          bind(SolverScheduler.class).in(Scopes.SINGLETON);
          bind(Scheduler.class).in(Scopes.SINGLETON);
          bind(LoomStats.class).in(Scopes.SINGLETON);
          bind(DBConnectionPool.class).in(Scopes.SINGLETON);
          bind(SQLClusterStoreService.class).in(Scopes.SINGLETON);
          bind(SQLEntityStoreService.class).in(Scopes.SINGLETON);
          bind(SQLTenantStore.class).in(Scopes.SINGLETON);
          bind(IdService.class).in(Scopes.SINGLETON);

          Multibinder<HttpHandler> handlerBinder = Multibinder.newSetBinder(binder(), HttpHandler.class);
          handlerBinder.addBinding().to(LoomAdminHandler.class);
          handlerBinder.addBinding().to(LoomClusterHandler.class);
          handlerBinder.addBinding().to(LoomTaskHandler.class);
          handlerBinder.addBinding().to(LoomStatusHandler.class);
          handlerBinder.addBinding().to(LoomRPCHandler.class);
          handlerBinder.addBinding().to(LoomTenantHandler.class);
        }
      };
  }
}
