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
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.provisioner.plugin.PluginResourceMeta;
import com.continuuity.loom.provisioner.plugin.PluginResourceType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Plugin store that writes modules to the local file system. Modules are namespaced by the data directory given
 * in the configuration, plugin type, plugin type id, module type, module name, and module version.
 */
public class LocalFilePluginStore implements PluginStore {
  private String baseDir;

  @Override
  public void initialize(Configuration conf) {
    baseDir = conf.get(Constants.LocalFilePluginStore.DATA_DIR);
    if (!baseDir.endsWith(File.separator)) {
      baseDir = baseDir + File.separator;
    }
  }

  /**
   * Get an output stream for the given resource. Files are namespaced by base data directory,
   * plugin type, plugin name, resource type, resource name, and resource version. For example, version 2 of resource
   * 'hadoop' of type 'cookbook'' for automator type 'chef-solo' with data directory '/var/loom/data/plugins' will be
   * written to '/var/loom/data/plugins/automatortypes/chef-solo/cookbook/hadoop/2'.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource
   * @param meta Metadata of the resource
   * @return Output stream for the file
   * @throws IOException if there was an error creating the file
   */
  @Override
  public OutputStream getResourceOutputStream(Account account, PluginResourceType type,
                                              PluginResourceMeta meta) throws IOException {
    File file = getFile(account, type, meta);
    File parent = file.getParentFile();
    if (!parent.exists()) {
      if (!parent.mkdirs()) {
        throw new IOException("Unable to create directory " + parent.getAbsolutePath());
      }
    }
    if (!file.exists()) {
      if (!file.createNewFile()) {
        throw new IOException("Unable to create file " + file.getAbsolutePath());
      }
    }
    return new FileOutputStream(file);
  }

  @Override
  public InputStream getResourceInputStream(Account account, PluginResourceType type,
                                            PluginResourceMeta meta) throws IOException {
    File file = getFile(account, type, meta);
    if (!file.exists()) {
      return null;
    }
    return new FileInputStream(file);
  }

  @Override
  public void deleteResource(Account account, PluginResourceType type, PluginResourceMeta meta) throws IOException {
    File file = getFile(account, type, meta);
    if (file.exists()) {
      file.delete();
    }
  }

  private File getFile(Account account, PluginResourceType type, PluginResourceMeta meta) {
    String path = new StringBuilder()
      .append(baseDir)
      .append(account.getTenantId())
      .append(File.separator)
      .append(type.getPluginType().name().toLowerCase())
      .append(File.separator)
      .append(type.getPluginName())
      .append(File.separator)
      .append(type.getResourceType())
      .append(File.separator)
      .append(meta.getName())
      .append(File.separator)
      .append(meta.getVersion())
      .toString();
    return new File(path);
  }
}
