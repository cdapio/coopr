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

import com.continuuity.loom.provisioner.PluginResourceMeta;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * A view of the plugin resource metadata store. Each account and resource type pair will see a different view of
 * the store.  Resource metadata essentially serves as a record of what resources have been uploaded and
 * which version of a resource is active.
 */
public interface PluginResourceMetaStoreView {

  /**
   * Checks whether or not the given resource exists.
   *
   * @param meta Metadata to check for
   * @return True if the module version exists, false if not
   * @throws IOException
   */
  public boolean exists(PluginResourceMeta meta) throws IOException;

  /**
   * Write the resource metadata to the store.
   *
   * @param meta Resource metadata to write
   * @throws IOException
   */
  public void write(PluginResourceMeta meta) throws IOException;

  /**
   * Delete the given metadata from the store.
   *
   * @param meta Metadata to delete
   * @throws IOException
   */
  public void delete(PluginResourceMeta meta) throws IOException;

  /**
   * Get an immutable map of resource name to all metadata.
   *
   * @param activeOnly Whether or not to filter out inactive resources
   * @return Immutable map of resource name to resource metadata
   * @throws IOException
   */
  public Map<String, Set<PluginResourceMeta>> getAll(boolean activeOnly) throws IOException;

  /**
   * Get an immutable set of all resource metadata for the given resource name.
   *
   * @param resourceName Name of resource to get metadata for
   * @param activeOnly Whether or not to filter out inactive resources
   * @return Immutable set of all resource metadata for the given resource name
   * @throws IOException
   */
  public Set<PluginResourceMeta> getAll(String resourceName, boolean activeOnly) throws IOException;

  /**
   * Atomically activate a specific resource version and deactivate the previous active version.
   *
   * @param resourceName Name of resource to activate
   * @param version Version of resource to activate
   * @throws IOException
   */
  public void activate(String resourceName, String version) throws IOException;

  /**
   * Deactivate all versions of a resource.
   *
   * @param resourceName Name of resource to deactivate
   * @throws IOException
   */
  public void deactivate(String resourceName) throws IOException;
}
