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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.common.zookeeper.ElectionHandler;
import com.continuuity.loom.common.zookeeper.LeaderElection;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.common.Threads;
import org.apache.twill.zookeeper.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs the different schedulers for solving cluster layouts and planning and coordinating cluster jobs. Leader election
 * is run so that only a single server in a server cluster will be running these schedulers at any given time,
 * where a server cluster is defined as all servers using the same zookeeper quorum.
 */
public class Scheduler extends AbstractIdleService {
  private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

  private final ScheduledExecutorService executorService;
  private final int schedulerRunInterval;
  private final JobScheduler jobScheduler;
  private final ClusterScheduler clusterScheduler;
  private final SolverScheduler solverScheduler;
  private final CallbackScheduler callbackScheduler;
  private final ClusterCleanup clusterCleanup;
  private final long clusterCleanupRunInterval;
  private final Set<ScheduledFuture<?>> scheduledFutures;
  private final LeaderElection leaderElection;

  @Inject
  public Scheduler(@Named("scheduler.run.interval.seconds") int schedulerRunInterval,
                   @Named("cluster.cleanup.run.interval.seconds") long clusterCleanupRunInterval,
                   JobScheduler jobScheduler,
                   ClusterScheduler clusterScheduler,
                   SolverScheduler solverScheduler,
                   CallbackScheduler callbackScheduler,
                   ClusterCleanup clusterCleanup,
                   ZKClient zkClient) {
    this.executorService = Executors.newScheduledThreadPool(5,
                                                            new ThreadFactoryBuilder()
                                                              .setNameFormat("scheduler-%d")
                                                              .build());
    this.schedulerRunInterval = schedulerRunInterval;
    this.clusterCleanupRunInterval = clusterCleanupRunInterval;
    this.jobScheduler = jobScheduler;
    this.clusterScheduler = clusterScheduler;
    this.solverScheduler = solverScheduler;
    this.callbackScheduler = callbackScheduler;
    this.clusterCleanup = clusterCleanup;
    this.scheduledFutures = Sets.newHashSet();

    this.leaderElection = new LeaderElection(zkClient, "/server-election", new ElectionHandler() {
      private final ExecutorService executor = Executors.newSingleThreadExecutor(
        Threads.createDaemonThreadFactory("scheduler-leader-election"));

      @Override
      public void leader() {
        executor.submit(new Runnable() {
          @Override
          public void run() {
            LOG.info("Became leader...");
            schedule();
          }
        });
      }

      @Override
      public void follower() {
        executor.submit(new Runnable() {
          @Override
          public void run() {
            LOG.info("Became follower...");
            unschedule();
          }
        });
      }
    });
  }

  @Override
  protected void startUp() throws Exception {
    // start up is based on leader election
  }

  @Override
  protected void shutDown() throws Exception {
    LOG.info("Stopping scheduler...");
    leaderElection.cancel();
    executorService.shutdown();
    executorService.awaitTermination(100, TimeUnit.SECONDS);
  }

  private void schedule() {
    LOG.info("Scheduling cluster scheduler every {} secs...", schedulerRunInterval);
    scheduledFutures.add(
      executorService.scheduleAtFixedRate(clusterScheduler, 1, schedulerRunInterval, TimeUnit.SECONDS)
    );

    LOG.info("Scheduling job scheduler every {} secs...", schedulerRunInterval);
    scheduledFutures.add(
      executorService.scheduleAtFixedRate(jobScheduler, 1, schedulerRunInterval, TimeUnit.SECONDS)
    );

    LOG.info("Scheduling solver scheduler every {} secs...", schedulerRunInterval);
    scheduledFutures.add(
      executorService.scheduleAtFixedRate(solverScheduler, 1, schedulerRunInterval, TimeUnit.SECONDS)
    );

    LOG.info("Scheduling callback scheduler every {} secs...", schedulerRunInterval);
    scheduledFutures.add(
      executorService.scheduleAtFixedRate(callbackScheduler, 1, schedulerRunInterval, TimeUnit.SECONDS)
    );

    LOG.info("Scheduling cluster cleanup every {} secs...", clusterCleanupRunInterval);
    scheduledFutures.add(
      executorService.scheduleAtFixedRate(clusterCleanup, 10, clusterCleanupRunInterval, TimeUnit.SECONDS)
    );
  }

  private void unschedule() {
    for (ScheduledFuture<?> future : scheduledFutures) {
      try {
        future.cancel(false);
      } catch (Throwable t) {
        LOG.error("Caught exception while un-scheduling.", t);
      }
    }
    scheduledFutures.clear();
  }
}
