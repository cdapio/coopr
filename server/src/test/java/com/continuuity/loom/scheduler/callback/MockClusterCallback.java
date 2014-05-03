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
import com.google.common.collect.Lists;

import java.util.List;

/**
 *
 */
public class MockClusterCallback implements ClusterCallback {
  private final List<CallbackData> startCallbacks = Lists.newArrayList();
  private final List<CallbackData> successCallbacks = Lists.newArrayList();
  private final List<CallbackData> failureCallbacks = Lists.newArrayList();
  private boolean returnOnStart = true;

  public List<CallbackData> getStartCallbacks() {
    return startCallbacks;
  }

  public List<CallbackData> getSuccessCallbacks() {
    return successCallbacks;
  }

  public List<CallbackData> getFailureCallbacks() {
    return failureCallbacks;
  }

  public void clear() {
    startCallbacks.clear();
    successCallbacks.clear();
    failureCallbacks.clear();
    returnOnStart = true;
  }

  public void setReturnOnStart(boolean val) {
    returnOnStart = val;
  }

  @Override
  public void initialize(Configuration conf, ClusterStore clusterStore) {
  }

  @Override
  public boolean onStart(CallbackData data) {
    startCallbacks.add(data);
    return returnOnStart;
  }

  @Override
  public void onSuccess(CallbackData data) {
    successCallbacks.add(data);
  }

  @Override
  public void onFailure(CallbackData data) {
    failureCallbacks.add(data);
  }
}
