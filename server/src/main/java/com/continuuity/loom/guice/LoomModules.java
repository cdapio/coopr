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
import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.http.LoomAdminHandler;
import com.continuuity.loom.http.LoomClusterHandler;
import com.continuuity.loom.http.LoomRPCHandler;
import com.continuuity.loom.http.LoomStatusHandler;
import com.continuuity.loom.http.LoomTaskHandler;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.ClusterScheduler;
import com.continuuity.loom.scheduler.JobScheduler;
import com.continuuity.loom.scheduler.Scheduler;
import com.continuuity.loom.scheduler.SolverScheduler;
import com.continuuity.loom.scheduler.callback.ClusterCallback;
import com.continuuity.loom.store.ClusterStore;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.EntityStore;
import com.continuuity.loom.store.SQLClusterStore;
import com.continuuity.loom.store.SQLEntityStore;
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

    final String namespace = conf.get(Constants.Zookeeper.NAMESPACE, Constants.Zookeeper.DEFAULT_NAMESPACE);
    final int port = conf.getInt(Constants.PORT, Constants.DEFAULT_PORT);
    final String host = conf.get(Constants.HOST, Constants.DEFAULT_HOST);
    final int schedulerIntervalSecs = conf.getInt(Constants.SCHEDULER_INTERVAL_SECS,
                                                  Constants.DEFAULT_SCHEDULER_INTERVAL_SECS);
    final long cleanupIntervalSecs = conf.getLong(Constants.CLUSTER_CLEANUP_SECS,
                                                  Constants.DEFAULT_CLUSTER_CLEANUP_SECS);
    final long taskTimeoutSecs = conf.getLong(Constants.TASK_TIMEOUT_SECS,
                                              Constants.DEFAULT_TASK_TIMEOUT_SECS);
    final long queueMsBetweenChecks = TimeUnit.SECONDS.toMillis(100);
    final long queueMsRescheduleTimeout = TimeUnit.SECONDS.toMillis(6000);
    final int nettyExecNumThreads = conf.getInt(Constants.NETTY_EXEC_NUM_THREADS,
                                                Constants.DEFAULT_NETTY_EXEC_NUM_THREADS);
    final int nettyWorkerNumThreads = conf.getInt(Constants.NETTY_WORKER_NUM_THREADS,
                                                  Constants.DEFAULT_NETTY_WORKER_NUM_THREADS);
    final int maxPerNodeLogLength = conf.getInt(Constants.MAX_PER_NODE_LOG_LENGTH,
                                                Constants.DEFAULT_MAX_PER_NODE_LOG_LENGTH);
    final int maxPerNodeNumActions = conf.getInt(Constants.MAX_PER_NODE_NUM_ACTIONS,
                                                 Constants.DEFAULT_MAX_PER_NODE_NUM_ACTIONS);
    final int maxActionRetries = conf.getInt(Constants.MAX_ACTION_RETRIES,
                                             Constants.DEFAULT_MAX_ACTION_RETRIES);
    final int maxClusterSize = conf.getInt(Constants.MAX_CLUSTER_SIZE,
                                           Constants.DEFAULT_MAX_CLUSTER_SIZE);
    // ids will start from this number
    final long idStartNum = conf.getLong(Constants.ID_START_NUM, Constants.DEFAULT_ID_START_NUM);
    // ids will increment by this
    final long idIncrementBy = conf.getLong(Constants.ID_INCREMENT_BY, Constants.DEFAULT_ID_INCREMENT_BY);
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

    final Class callbackClass = Class.forName(conf.get(Constants.CALLBACK_CLASS, Constants.DEFAULT_CALLBACK_CLASS));

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
          bind(EntityStore.class).to(SQLEntityStore.class).in(Scopes.SINGLETON);
          bind(ClusterStore.class).to(SQLClusterStore.class).in(Scopes.SINGLETON);
          bind(ZKClient.class).toInstance(zkClient);
          bind(Integer.class)
            .annotatedWith(Names.named("loom.port")).toInstance(port);
          bind(String.class)
            .annotatedWith(Names.named("loom.host")).toInstance(host);
          bind(String.class)
            .annotatedWith(Names.named("scheduler.id")).toInstance("scheduler-" + host);
          bind(Integer.class)
            .annotatedWith(Names.named(Constants.SCHEDULER_INTERVAL_SECS)).toInstance(schedulerIntervalSecs);

          bind(Long.class)
            .annotatedWith(Names.named("cluster.cleanup.run.interval.seconds"))
            .toInstance(cleanupIntervalSecs);
          bind(Long.class)
            .annotatedWith(Names.named("task.timeout.seconds"))
            .toInstance(taskTimeoutSecs);

          bind(ListeningExecutorService.class)
            .annotatedWith(Names.named("solver.executor.service"))
            .toInstance(executorService);
          bind(ListeningExecutorService.class)
            .annotatedWith(Names.named("callback.executor.service"))
            .toInstance(callbackExecutorService);

          bind(Integer.class)
            .annotatedWith(Names.named(Constants.NETTY_EXEC_NUM_THREADS)).toInstance(nettyExecNumThreads);
          bind(Integer.class)
            .annotatedWith(Names.named(Constants.NETTY_WORKER_NUM_THREADS)).toInstance(nettyWorkerNumThreads);

          bind(Integer.class)
            .annotatedWith(Names.named(Constants.MAX_PER_NODE_LOG_LENGTH)).toInstance(maxPerNodeLogLength);
          bind(Integer.class)
            .annotatedWith(Names.named(Constants.MAX_PER_NODE_NUM_ACTIONS)).toInstance(maxPerNodeNumActions);
          bind(Integer.class)
            .annotatedWith(Names.named(Constants.MAX_ACTION_RETRIES)).toInstance(maxActionRetries);
          bind(Integer.class)
            .annotatedWith(Names.named(Constants.MAX_CLUSTER_SIZE)).toInstance(maxClusterSize);
          bind(Long.class)
            .annotatedWith(Names.named(Constants.ID_START_NUM)).toInstance(idStartNum);
          bind(Long.class)
            .annotatedWith(Names.named(Constants.ID_INCREMENT_BY)).toInstance(idIncrementBy);

          bind(JobScheduler.class).in(Scopes.SINGLETON);
          bind(ClusterScheduler.class).in(Scopes.SINGLETON);
          bind(SolverScheduler.class).in(Scopes.SINGLETON);
          bind(Scheduler.class).in(Scopes.SINGLETON);
          bind(LoomStats.class).in(Scopes.SINGLETON);
          bind(DBConnectionPool.class).in(Scopes.SINGLETON);
          bind(SQLClusterStore.class).in(Scopes.SINGLETON);
          bind(SQLEntityStore.class).in(Scopes.SINGLETON);

          Multibinder<HttpHandler> handlerBinder = Multibinder.newSetBinder(binder(), HttpHandler.class);
          handlerBinder.addBinding().to(LoomAdminHandler.class);
          handlerBinder.addBinding().to(LoomClusterHandler.class);
          handlerBinder.addBinding().to(LoomTaskHandler.class);
          handlerBinder.addBinding().to(LoomStatusHandler.class);
          handlerBinder.addBinding().to(LoomRPCHandler.class);
        }
      };
  }
}
