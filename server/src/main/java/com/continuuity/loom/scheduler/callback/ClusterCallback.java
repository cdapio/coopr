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
 * Executes some code before a job starts and after a job completes.
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
   * Execute some method before a cluster job starts.
   *
   * @param data Data available to use while executing callback.
   */
  void onStart(CallbackData data);

  /**
   * Execute some method after a cluster completes succesfully.
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
