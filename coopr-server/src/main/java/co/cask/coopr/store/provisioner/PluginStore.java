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
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.provisioner.plugin.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Persistent store for plugin data.
 */
public interface PluginStore {

  /**
   * Initialize the store. Guaranteed to be called once before any other calls are made.
   *
   * @param conf Configuration for initializing the store
   */
  void initialize(Configuration conf);

  /**
   * Get an output stream for writing a plugin resource.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource being written
   * @param name Name of the resource being written
   * @param version Version of the resource being written
   * @return Output stream for writing the plugin module
   * @throws IOException if there is an error getting the output stream
   */
  OutputStream getResourceOutputStream(Account account, ResourceType type, String name, int version)
    throws IOException;

  /**
   * Get an input stream for reading a plugin resource.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource to get
   * @param name Name of the resource to get
   * @param version Version of the resource to get
   * @return Input stream for reading the plugin resource, or null if there is no resource
   * @throws IOException if there is an error getting the input stream
   */
  InputStream getResourceInputStream(Account account, ResourceType type, String name, int version)
    throws IOException;

  /**
   * Delete a plugin resource.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource to delete
   * @param name Name of the resource to delete
   * @param version Version of the resource to delete
   * @throws IOException if there was an error deleting the resource
   */
  void deleteResource(Account account, ResourceType type, String name, int version) throws IOException;

}
