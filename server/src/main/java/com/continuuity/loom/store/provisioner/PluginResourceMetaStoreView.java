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
import com.continuuity.loom.provisioner.PluginResourceStatus;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * A view of the plugin resource metadata store. Each account and resource type pair will see a different view of
 * the store. Resource metadata essentially serves as a record of what resources have been uploaded, what resources
 * are staged, and what resources are in what state.
 */
public interface PluginResourceMetaStoreView {

  /**
   * Checks whether or not any version of the given resource exists.
   *
   * @param resourceName Name of the resource to check for
   * @return True if the module version exists, false if not
   * @throws IOException
   */
  public boolean exists(String resourceName) throws IOException;

  /**
   * Checks whether or not a specific version of the given resource exists.
   *
   * @param resourceName Name of the resource to check for
   * @param resourceVersion Version of the resource to check for
   * @return True if the module version exists, false if not
   * @throws IOException
   */
  public boolean exists(String resourceName, String resourceVersion) throws IOException;

  /**
   * Write the resource metadata to the store.
   *
   * @param meta Metadata to write
   * @throws IOException
   */
  public void write(PluginResourceMeta meta) throws IOException;

  /**
   * Get the metadata for the given resource name and version.
   *
   * @param resourceName Name of the resource to get
   * @param resourceVersion Version of the resource to get
   * @return Metadata of the given resource
   * @throws IOException
   */
  public PluginResourceMeta get(String resourceName, String resourceVersion) throws IOException;

  /**
   * Delete the given version of the given resource from the store.
   *
   * @param resourceName Name of the resource to delete
   * @param resourceVersion Version of the resource to delete
   * @throws IOException
   */
  public void delete(String resourceName, String resourceVersion) throws IOException;

  /**
   * Get an immutable map of resource name to all metadata with that name.
   *
   * @return Immutable map of resource name to resource metadata
   * @throws IOException
   */
  public Map<String, Set<PluginResourceMeta>> getAll() throws IOException;

  /**
   * Get an immutable map of resource name to all metadata with that name with the given status.
   *
   * @param status Status of the resources to get
   * @return Immutable map of resource name to resource metadata
   * @throws IOException
   */
  public Map<String, Set<PluginResourceMeta>> getAll(PluginResourceStatus status) throws IOException;

  /**
   * Get an immutable set of all resource metadata for the given resource name.
   *
   * @param resourceName Name of resource to get metadata for
   * @return Immutable set of all resource metadata for the given resource name
   * @throws IOException
   */
  public Set<PluginResourceMeta> getAll(String resourceName) throws IOException;

  /**
   * Get an immutable set of all resource metadata for the given resource name with the given status.
   *
   * @param resourceName Name of resources to get
   * @param status Status of the resources to get
   * @return Immutable set of all resource metadata for the given resource name with the given status
   * @throws IOException
   */
  public Set<PluginResourceMeta> getAll(String resourceName, PluginResourceStatus status) throws IOException;

  /**
   * Atomically stage an inactive resource version and deactivate the current staged version if there is one.
   * Staging an unstage resource puts it back in an active state. This is a no-op if the given resource is already
   * staged or active.
   *
   * @param resourceName Name of resource to stage
   * @param resourceVersion Version of resource to stage
   * @return True if the resource was staged, false if there was no resource to stage
   * @throws IOException
   */
  public void stage(String resourceName, String resourceVersion) throws IOException;

  /**
   * Unstage the given resource, moving a resource in the staged state into the inactive state, or moving a resource
   * in the active state into the unstaged state. This a no-op if the given resource is already unstaged or inactive.
   *
   * @param resourceName Name of resource to unstage
   * @param resourceVersion Version of resource to unstage
   * @throws IOException
   */
  public void unstage(String resourceName, String resourceVersion) throws IOException;

  /**
   * Atomically promote the staged version of the resource into the active state and the active version of the resource
   * into the inactive state.
   *
   * @param resourceName Name of the resource to activate
   * @throws IOException
   */
  public void activate(String resourceName) throws IOException;
}
