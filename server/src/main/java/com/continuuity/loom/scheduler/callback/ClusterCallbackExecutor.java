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
package com.continuuity.loom.scheduler.callback;

import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Executes callbacks asynchronously, with the onStart calls happening before any tasks for the job are queued.
 */
public class ClusterCallbackExecutor {
  private static final Logger LOG  = LoggerFactory.getLogger(ClusterCallbackExecutor.class);
  private final ListeningExecutorService executorService;
  private final ClusterCallback clusterCallback;
  private final TrackingQueue jobQueue;

  @Inject
  protected ClusterCallbackExecutor(ClusterCallback clusterCallback, Configuration conf, ClusterStore clusterStore,
                                    @Named("callback.executor.service") ListeningExecutorService executorService,
                                    @Named("internal.job.queue") TrackingQueue jobQueue) {
    this.executorService = executorService;
    this.jobQueue = jobQueue;
    this.clusterCallback = clusterCallback;
    this.clusterCallback.initialize(conf, clusterStore);
  }

  /**
   * Execute some method before a cluster job starts.
   *
   * @param data Data available to use while executing callback.
   */
  public void onStart(final CallbackData data) {
    ListenableFuture future = executorService.submit(new Runnable() {
      @Override
      public void run() {
        clusterCallback.onStart(data);
      }
    });
    future.addListener(new Runnable() {
      @Override
      public void run() {
        String jobId = data.getJob().getJobId();
        jobQueue.add(new Element(jobId));
        LOG.debug("added job {} to job queue", jobId);
      }
    }, executorService);
  }

  /**
   * Execute some method after a cluster completes succesfully.
   *
   * @param data Data available to use while executing callback.
   */
  public void onSuccess(final CallbackData data) {
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        clusterCallback.onSuccess(data);
      }
    });
  }

  /**
   * Execute some method after a cluster job fails.
   *
   * @param data Data available to use while executing callback.
   */
  public void onFailure(final CallbackData data) {
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        clusterCallback.onFailure(data);
      }
    });
  }
}
