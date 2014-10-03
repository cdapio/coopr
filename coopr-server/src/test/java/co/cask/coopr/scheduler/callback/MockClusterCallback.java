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
package co.cask.coopr.scheduler.callback;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.store.cluster.ClusterStoreService;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class MockClusterCallback implements ClusterCallback {
  private final List<CallbackData> receivedCallbacks =
    Collections.synchronizedList(Lists.<CallbackData>newArrayList());
  private boolean returnOnStart = true;

  public void clear() {
    receivedCallbacks.clear();
    returnOnStart = true;
  }

  public List<CallbackData> getReceivedCallbacks() {
    return receivedCallbacks;
  }

  public void setReturnOnStart(boolean val) {
    returnOnStart = val;
  }

  @Override
  public void initialize(Configuration conf) {
  }

  @Override
  public boolean onStart(CallbackData data, CallbackContext context) {
    receivedCallbacks.add(data);
    return returnOnStart;
  }

  @Override
  public void onSuccess(CallbackData data, CallbackContext context) {
    receivedCallbacks.add(data);
  }

  @Override
  public void onFailure(CallbackData data, CallbackContext context) {
    receivedCallbacks.add(data);
  }
}
