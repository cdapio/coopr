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

import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The client API for manage provisioner plugins.
 */
public interface PluginClient {

  /**
   * Retrieves all automator types readable by the user. If no automator types exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.plugin.AutomatorType} objects
   */
  List<AutomatorType> getAllAutomatorTypes();

  /**
   * Retrieves a specific automator type if readable by the user.
   *
   * @return {@link co.cask.coopr.spec.plugin.AutomatorType} object
   */
  AutomatorType getAutomatorType(String id);

  /**
   * Retrieves all provider types readable by the user. If no provider types exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.plugin.ProviderType} objects
   */
  List<ProviderType> getAllProviderTypes();

  /**
   * Retrieves a specific provider type if readable by the user.
   *
   * @return {@link co.cask.coopr.spec.plugin.ProviderType} object
   */
  ProviderType getProviderType(String id);

  /**
   * Retrieves a list of all versions of the given resource of the given type for the given automator type.
   * Method can optionally contain a 'status' parameter, whose value is one of 'active', 'inactive', 'staged', or
   * 'recalled' to filter the results to only contain resource that have the given status.
   *
   * @param id Id of the automator type that has the resources
   * @param resourceType Type of resource to get
   * @param status Status of the resources to get {@link co.cask.coopr.provisioner.plugin.ResourceStatus}.
   *               Or null, for resources of any status are returned
   * @return Immutable map of resource name to resource metadata
   */
  Map<String, Set<ResourceMeta>> getAutomatorTypeResources(String id, String resourceType, ResourceStatus status);

  /**
   * Retrieves a mapping of all resources of the given type for the given provider type.
   * Method can optionally contain a 'status' parameter, whose value is one of 'active', 'inactive', 'staged', or
   * 'recalled' to filter the results to only contain resource that have the given status.
   *
   * @param id Id of the provider type that has the resources
   * @param resourceType Type of resource to get
   * @param status Status of the resources to get {@link co.cask.coopr.provisioner.plugin.ResourceStatus}.
   *               Or null, for resources of any status are returned
   * @return Immutable map of resource name to resource metadata
   */
  Map<String, Set<ResourceMeta>> getProviderTypeResources(String id, String resourceType, ResourceStatus status);

  /**
   * Stage a particular resource version, which means that version of the resource will get pushed to provisioners
   * on the next sync call. Staging one version recalls other versions of the resource.
   *
   * @param id Id of the automator type that has the resources
   * @param resourceType Type of resource to stage
   * @param resourceName Name of the resource to stage
   * @param version Version of the resource to stage
   */
  void stageAutomatorTypeResource(String id, String resourceType, String resourceName, String version);

  /**
   * Stage a particular resource version, which means that version of the resource will get pushed to provisioners
   * on the next sync call. Staging one version recalls other versions of the resource.
   *
   * @param id Id of the provider type that has the resources
   * @param resourceType Type of resource to stage
   * @param resourceName Name of the resource to stage
   * @param version Version of the resource to stage
   */
  void stageProviderTypeResource(String id, String resourceType, String resourceName, String version);

  /**
   * Recall a particular resource version, which means that version of the resource will get removed from provisioners
   * on the next sync call.
   *
   * @param id Id of the automator type that has the resources
   * @param resourceType Type of resource to recall
   * @param resourceName Name of the resource to recall
   * @param version Version of the resource to recall
   */
  void recallAutomatorTypeResource(String id, String resourceType, String resourceName, String version);

  /**
   * Recall a particular resource version, which means that version of the resource will get removed from provisioners
   * on the next sync call.
   *
   * @param id Id of the provider type that has the resources
   * @param resourceType Type of resource to recall
   * @param resourceName Name of the resource to recall
   * @param version Version of the resource to recall
   */
  void recallProviderTypeResource(String id, String resourceType, String resourceName, String version);

  /**
   * Delete a specific version of the given resource.
   *
   * @param id Id of the automator type that has the resources
   * @param resourceType Type of resource to delete
   * @param resourceName Name of the resource to delete
   * @param version Version of the resource to delete
   */
  void deleteAutomatorTypeResourceVersion(String id, String resourceType, String resourceName, String version);

  /**
   * Delete a specific version of the given resource.
   *
   * @param id Id of the provider type that has the resources
   * @param resourceType Type of resource to delete
   * @param resourceName Name of the resource to delete
   * @param version Version of the resource to delete
   */
  void deleteProviderTypeResourceVersion(String id, String resourceType, String resourceName, String version);

  /**
   * Push staged resources to the provisioners, and remove recalled resources from the provisioners.
   */
  void syncPlugins();
}
