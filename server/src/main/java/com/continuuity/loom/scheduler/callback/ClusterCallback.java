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

import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.store.ClusterStore;
import com.google.inject.Inject;

/**
 * Executes some code before a job starts and after a job completes. Callbacks must be idempotent. There is a
 * possibility they get called more than once if the server goes down at the right time.
 */
public interface ClusterCallback {

  /**
   * Initialize the cluster callback. Guaranteed to be called exactly once before any other methods are called.
   *
   * @param conf Server configuration.
   * @param clusterStore Cluster store for looking up cluster information.
   */
  void initialize(Configuration conf, ClusterStore clusterStore);

  /**
   * Execute some method before a cluster job starts, returning whether or not the job can proceed or whether it should
   * be failed. Is guaranteed to be called and executed before the cluster operation begins.
   *
   * @param data Data available to use while executing callback.
   * @return True if it is ok to proceed with the operation, false if not.
   */
  boolean onStart(CallbackData data);

  /**
   * Execute some method after a cluster completes successfully.
   *
   * @param data Data available to use while executing callback.
   */
  void onSuccess(CallbackData data);

  /**
   * Execute some method after a cluster job fails.
   *
   * @param data Data available to use while executing callback.
   */
  void onFailure(CallbackData data);
}
