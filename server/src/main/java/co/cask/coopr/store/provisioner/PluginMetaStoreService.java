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
package co.cask.coopr.store.provisioner;

import co.cask.coopr.account.Account;
import co.cask.coopr.provisioner.plugin.ResourceType;
import com.google.common.util.concurrent.Service;

/**
 * Service that provides views for reading and writing to and from the plugin resource metadata store.
 */
public interface PluginMetaStoreService extends Service {

  /**
   * Get a view of the metadata store for the given account.
   *
   * @param account Account that will be accessing the store
   * @return View of the metadata store for the given account
   */
  PluginMetaStoreView getAccountView(Account account);

  /**
   * Get a view of the metadata store for the given account and resource type.
   *
   * @param account Account that will be accessing the store
   * @param type Type of resource that will be accessed
   * @return View of the metadata store for the given account and resource type
   */
  PluginResourceTypeView getResourceTypeView(Account account, ResourceType type);
}
