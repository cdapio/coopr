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
package co.cask.coopr.store.node;

import co.cask.coopr.account.Account;

/**
 * /**
 * Service for getting a {@link NodeStoreView} for different an account that will restrict what parts of the
 * actual store can be viewed or edited.
 */
public interface NodeStoreService {
  /**
   * Get a view of the node store as seen by the given account.
   * @param account Account of the user that is accessing the store.
   * @return View of the node store as seen by the account.
   */
  NodeStoreView getView(Account account);

  /**
   * Get the full view of the node store for system operations.
   * @return Full view of the node store.
   */
  NodeStore getSystemView();
}
