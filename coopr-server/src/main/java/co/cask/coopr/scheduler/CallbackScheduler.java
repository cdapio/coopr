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
package co.cask.coopr.scheduler;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.GroupElement;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.scheduler.callback.CallbackContext;
import co.cask.coopr.scheduler.callback.CallbackData;
import co.cask.coopr.scheduler.callback.ClusterCallback;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.TaskService;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.user.UserStore;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Polls a queue which contains {@link co.cask.coopr.scheduler.callback.CallbackData} for performing cluster
 * operation callbacks before starting an operation and upon success or failure of an operation.
 */
public class CallbackScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(CallbackScheduler.class);

  private final String id;
  private final ClusterCallback clusterCallback;
  private final ListeningExecutorService executorService;
  private final TaskService taskService;
  private final ClusterStoreService clusterStoreService;
  private final UserStore userStore;
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
                            UserStore userStore,
                            Gson gson,
                            QueueService queueService) {
    this.id = id;
    this.executorService = executorService;
    this.taskService = taskService;
    this.clusterCallback = clusterCallback;
    this.clusterCallback.initialize(conf);
    this.gson = gson;
    this.callbackQueues = queueService.getQueueGroup(QueueType.CALLBACK);
    this.jobQueues = queueService.getQueueGroup(QueueType.JOB);
    this.clusterStoreService = clusterStoreService;
    this.userStore = userStore;
  }

  @Override
  public void run() {
    try {
      Iterator<GroupElement> callbackIter = callbackQueues.takeIterator(id);
      while (callbackIter.hasNext()) {
        final GroupElement gElement = callbackIter.next();
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
      CallbackContext callbackContext =
        new CallbackContext(clusterStoreService, userStore, callbackData.getCluster().getAccount());
      switch (callbackData.getType()) {
        case START:
          onStart(callbackData, callbackContext);
          break;
        case SUCCESS:
          clusterCallback.onSuccess(callbackData, callbackContext);
          break;
        case FAILURE:
          clusterCallback.onFailure(callbackData, callbackContext);
          break;
        default:
          LOG.error("Unknown callback type {}", callbackData.getType());
      }
    }

    private void onStart(CallbackData callbackData, CallbackContext callbackContext) {
      ClusterJob job = callbackData.getJob();
      Cluster cluster = callbackData.getCluster();
      try {
        if (clusterCallback.onStart(callbackData, callbackContext)) {
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
