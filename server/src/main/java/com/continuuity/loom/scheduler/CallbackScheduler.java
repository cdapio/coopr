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

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.GroupElement;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.scheduler.callback.CallbackData;
import com.continuuity.loom.scheduler.callback.ClusterCallback;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls a queue which contains {@link com.continuuity.loom.scheduler.callback.CallbackData} for performing cluster
 * operation callbacks before starting an operation and upon success or failure of an operation.
 */
public class CallbackScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(CallbackScheduler.class);

  private final String id;
  private final ClusterCallback clusterCallback;
  private final ListeningExecutorService executorService;
  private final TaskService taskService;
  private final Gson gson;
  private final QueueGroup callbackQueues;
  private final QueueGroup jobQueues;

  @Inject
  private CallbackScheduler(@Named("scheduler.id") String id,
                            @Named("callback.executor.service") ListeningExecutorService executorService,
                            TaskService taskService,
                            ClusterCallback clusterCallback,
                            Configuration conf,
                            ClusterStoreService clusterStoreService,
                            Gson gson,
                            @Named(Constants.Queue.CALLBACK) QueueGroup callbackQueues,
                            @Named(Constants.Queue.JOB) QueueGroup jobQueues) {
    this.id = id;
    this.executorService = executorService;
    this.taskService = taskService;
    this.clusterCallback = clusterCallback;
    this.clusterCallback.initialize(conf, clusterStoreService);
    this.gson = gson;
    this.callbackQueues = callbackQueues;
    this.jobQueues = jobQueues;
  }

  @Override
  public void run() {
    try {
      while (true) {
        final GroupElement gElement = callbackQueues.take(id);
        if (gElement == null) {
          return;
        }

        final Element element = gElement.getElement();
        final ListenableFuture future = executorService.submit(new CallbackRunner(gElement));
        future.addListener(new Runnable() {
          @Override
          public void run() {
            try {
              callbackQueues.recordProgress(id, gElement.getQueueName(), element.getId(),
                                            TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "Executed");
            } catch (Exception e) {
              LOG.error("Exception processing callback", e);
            }
          }
        }, executorService);
      }
    } catch (Exception e) {
      LOG.error("Got exception:", e);
    }
  }

  private class CallbackRunner implements Runnable {
    private final GroupElement gElement;

    private CallbackRunner(GroupElement gElement) {
      this.gElement = gElement;
    }

    @Override
    public void run() {
      CallbackData callbackData = gson.fromJson(gElement.getElement().getValue(), CallbackData.class);
      switch (callbackData.getType()) {
        case START:
          onStart(callbackData);
          break;
        case SUCCESS:
          clusterCallback.onSuccess(callbackData);
          break;
        case FAILURE:
          clusterCallback.onFailure(callbackData);
          break;
        default:
          LOG.error("Unknown callback type {}", callbackData.getType());
      }
    }

    private void onStart(CallbackData callbackData) {
      ClusterJob job = callbackData.getJob();
      Cluster cluster = callbackData.getCluster();
      try {
        if (clusterCallback.onStart(callbackData)) {
          String jobId = callbackData.getJob().getJobId();
          jobQueues.add(gElement.getQueueName(), new Element(jobId));
          LOG.debug("added job {} to job queue", jobId);
        } else {
          switch (job.getClusterAction()) {
            case CLUSTER_CREATE:
              taskService.failJobAndTerminateCluster(job, cluster,
                                                     "Cluster creation stopped by failed start callback.");
              break;
            default:
              // failed to plan means the job should fail, but state has already been changed so the cluster
              // state in the db is inconsistent with reality.
              // TODO: Should revert it here but need versioning or cluster history or something to that effect.
              taskService.failJobAndSetClusterStatus(
                job, cluster, Cluster.Status.INCONSISTENT,
                "Failed to schedule the " + job.getClusterAction() + " operation.");
              break;
          }
        }
      } catch (Exception e) {
        LOG.error("Exception failing job {} for cluster {}", job.getJobId(), cluster.getId(), e);
      }
    }
  }
}
