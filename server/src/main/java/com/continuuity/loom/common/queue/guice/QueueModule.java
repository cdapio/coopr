package com.continuuity.loom.common.queue.guice;

import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.continuuity.loom.common.queue.internal.ZKElementsTracking;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.apache.twill.zookeeper.ZKClient;

import java.util.concurrent.TimeUnit;

/**
 * Guice module for binding queue related classes and instances.
 */
public class QueueModule extends AbstractModule {
  private static final String clusterManagerZKBasePath = "/clustermanager";
  // TODO: should not be required to be started
  private final ZKClient zkClient;

  public QueueModule(ZKClient zkClient) {
    this.zkClient = zkClient;
  }

  @Override
  protected void configure() {
    long queueMsBetweenChecks = TimeUnit.SECONDS.toMillis(100);
    long queueMsRescheduleTimeout = TimeUnit.SECONDS.toMillis(6000);

    TimeoutTrackingQueue clusterCreationQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/clustercreate"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    TimeoutTrackingQueue solverQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/solver"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    TimeoutTrackingQueue nodeProvisionTaskQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/nodeprovision"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    TimeoutTrackingQueue jobSchedulerQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/jobscheduler"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    TimeoutTrackingQueue callbackQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/callback"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);
    TimeoutTrackingQueue balancerQueue =
      new TimeoutTrackingQueue(new ZKElementsTracking(zkClient, clusterManagerZKBasePath + "/balancer"),
                               queueMsBetweenChecks,
                               queueMsRescheduleTimeout);

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
    bind(TimeoutTrackingQueue.class)
      .annotatedWith(Names.named(Constants.Queue.WORKER_BALANCE)).toInstance(balancerQueue);
    bind(TrackingQueue.class)
      .annotatedWith(Names.named(Constants.Queue.WORKER_BALANCE)).toInstance(balancerQueue);
  }
}
