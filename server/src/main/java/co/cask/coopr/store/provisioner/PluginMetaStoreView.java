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

import co.cask.coopr.provisioner.plugin.ResourceCollection;
import co.cask.coopr.provisioner.plugin.ResourceType;

import java.io.IOException;

/**
 * View of the plugin metadata persistent store for some account.
 */
public interface PluginMetaStoreView {

  /**
   * Get a view of the metadata store for the given resource type.
   *
   * @param type Type of plugin resource that will be accessed
   * @return View of the metadata store for the given account and resource type
   */
  PluginResourceTypeView getResourceTypeView(ResourceType type);

  /**
   * Atomically sync all resources in the given collection to be live, and any other resource not given to
   * not be live.
   *
   * @param resources Resources to set to the live state
   * @throws IOException
   */
  void syncResources(ResourceCollection resources) throws IOException;

  /**
   * Get the number of resources in the metadata store, including all versions of all resources in any state.
   *
   * @return The number of resources in the metadata store, including all versions of all resources in any state
   * @throws IOException
   */
  int numResources() throws IOException;
}
