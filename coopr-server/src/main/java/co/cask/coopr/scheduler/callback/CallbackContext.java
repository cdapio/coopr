/*
 * Copyright © 2014 Cask Data, Inc.
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

import co.cask.coopr.account.Account;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.cluster.ClusterStoreView;
import co.cask.coopr.store.cluster.ReadOnlyClusterStoreView;
import co.cask.coopr.store.user.UserStore;

import java.io.IOException;
import java.util.Map;

/**
 * Context that a {@link ClusterCallback} can take place in, giving the callback read access to cluster data and user
 * information that would normally be accessible to the owner of the cluster.
 */
public class CallbackContext {
  private final ClusterStoreView clusterStoreView;
  private final UserStore userStore;
  private final Account account;

  public CallbackContext(ClusterStoreService clusterStoreService, UserStore userStore, Account account) {
    this.clusterStoreView = clusterStoreService.getView(account);
    this.userStore = userStore;
    this.account = account;
  }

  /**
   * Get a read only view of the cluster store as seen by the owner of the cluster.
   *
   * @return read only view of the cluster store as seen by the owner of the cluster
   */
  public ReadOnlyClusterStoreView getClusterStoreView() {
    return clusterStoreView;
  }

  /**
   * Get the profile of the account that owns the cluster, or null if none exists.
   *
   * @return profile of the account that owns the cluster, or null if none exists
   * @throws IOException if there was an exception getting the profile
   */
  public Map<String, Object> getAccountProfile() throws IOException {
    return userStore.getProfile(account);
  }
}
