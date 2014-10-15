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

package co.cask.coopr.client;

import java.io.Closeable;

/**
 * The API for managing singletons of all existing clients.
 */
public interface ClientManager extends Closeable {

  /**
   * Creates the instance of the AdminClient.
   *
   * @return {@link co.cask.coopr.client.AdminClient} object
   */
  AdminClient getAdminClient();

  /**
   * Creates the instance of the ClusterClient.
   *
   * @return {@link co.cask.coopr.client.ClusterClient} iobject
   */
  ClusterClient getClusterClient();

  /**
   * Creates the instance of the PluginClient.
   *
   * @return {@link co.cask.coopr.client.PluginClient} object
   */
  PluginClient getPluginClient();

  /**
   * Creates the instance of the ProvisionerClient.
   *
   * @return {@link co.cask.coopr.client.ProvisionerClient} object
   */
  ProvisionerClient getProvisionerClient();

  /**
   * Creates the instance of the TenantClient.
   *
   * @return {@link co.cask.coopr.client.TenantClient} object
   */
  TenantClient getTenantClient();
}
