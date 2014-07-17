package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.continuuity.loom.provisioner.PluginResourceType;

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
