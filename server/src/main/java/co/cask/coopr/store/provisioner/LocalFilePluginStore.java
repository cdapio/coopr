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
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.plugin.ResourceType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Plugin store that writes resources to the local file system. Modules are namespaced by the data directory given
 * in the configuration, plugin type, plugin type id, resource type, name, and version.
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
   * 'hadoop' of type 'cookbook'' for automator type 'chef-solo' with data directory '/var/coopr/data/plugins' will be
   * written to '/var/coopr/data/plugins/automatortypes/chef-solo/cookbook/hadoop/2'.
   *
   * @param account Account that owns the plugin resource
   * @param type Type of resource
   * @param name Name of the resource being written
   * @param version Version of the resource being written
   * @return Output stream for the file
   * @throws IOException if there was an error creating the file
   */
  @Override
  public OutputStream getResourceOutputStream(Account account, ResourceType type, String name, int version)
    throws IOException {
    File file = getFile(account, type, name, version);
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
  public InputStream getResourceInputStream(Account account, ResourceType type, String name, int version)
    throws IOException {
    File file = getFile(account, type, name, version);
    if (!file.exists()) {
      return null;
    }
    return new FileInputStream(file);
  }

  @Override
  public void deleteResource(Account account, ResourceType type, String name, int version) throws IOException {
    File file = getFile(account, type, name, version);
    if (file.exists()) {
      // TODO: delete directory structure if empty
      file.delete();
    }
  }

  private File getFile(Account account, ResourceType type,  String name, int version) {
    String path = new StringBuilder()
      .append(baseDir)
      .append(account.getTenantId())
      .append(File.separator)
      .append(type.getPluginType().name().toLowerCase())
      .append(File.separator)
      .append(type.getPluginName())
      .append(File.separator)
      .append(type.getTypeName())
      .append(File.separator)
      .append(name)
      .append(File.separator)
      .append(version)
      .toString();
    return new File(path);
  }
}
