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
package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.provisioner.plugin.PluginResourceMeta;
import com.continuuity.loom.provisioner.plugin.PluginResourceType;

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
   * @param meta Metadata of resource being written
   * @return Output stream for writing the plugin module
   * @throws IOException if there is an error getting the output stream
   */
  OutputStream getResourceOutputStream(Account account, PluginResourceType type,
                                       PluginResourceMeta meta) throws IOException;

  /**
   * Get an input stream for reading a plugin resource.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource being written
   * @param meta Metadata of resource being read
   * @return Input stream for reading the plugin resource, or null if there is no resource
   * @throws IOException if there is an error getting the input stream
   */
  InputStream getResourceInputStream(Account account, PluginResourceType type,
                                     PluginResourceMeta meta) throws IOException;

  /**
   * Delete a plugin resource.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource being deleted
   * @param meta Metadata of the resource being deleted
   * @throws IOException if there was an error deleting the module
   */
  void deleteResource(Account account, PluginResourceType type,
                      PluginResourceMeta meta) throws IOException;

}
