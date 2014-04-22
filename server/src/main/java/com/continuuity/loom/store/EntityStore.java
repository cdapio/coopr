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
package com.continuuity.loom.store;

import com.continuuity.loom.admin.AutomatorType;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProviderType;
import com.continuuity.loom.admin.Service;

import java.util.Collection;

/**
 * EntityStore for adding, modifying, retrieving, and deleting admin definable entities.
 * TODO: Exceptions should be more specific
 * TODO: introduce concept of owner or group acls for these entities
 */
public interface EntityStore {

  /**
   * Get the {@link Provider} associated with the given unique name or null if no such provider exists.
   *
   * @param providerName Unique name of the provider to get.
   * @return Provider matching the given name or null if no such provider exists.
   * @throws Exception
   */
  Provider getProvider(String providerName) throws Exception;

  /**
   * Get all {@link Provider}s.
   *
   * @return Collection of all providers.
   * @throws Exception
   */
  Collection<Provider> getAllProviders() throws Exception;

  /**
   * Write the given {@link Provider} to the store. Will overwrite the existing {@link Provider} if it exists.
   *
   * @param provider Provider to write.
   * @throws Exception
   */
  void writeProvider(Provider provider) throws Exception;

  /**
   * Delete the {@link Provider} associated with the given unique name.
   *
   * @param providerName Name of the provider to delete.
   * @throws Exception
   */
  void deleteProvider(String providerName) throws Exception;

  /**
   * Get the {@link HardwareType} associated with the given unique name or null if no such provider exists.
   *
   * @param hardwareTypeName Unique name of the provider to get.
   * @return Hardware type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  HardwareType getHardwareType(String hardwareTypeName) throws Exception;

  /**
   * Get all {@link HardwareType}s.
   *
   * @return Collection of all hardware types.
   * @throws Exception
   */
  Collection<HardwareType> getAllHardwareTypes() throws Exception;

  /**
   * Write the given {@link HardwareType} to the store. Will overwrite the existing {@link HardwareType} if it exists.
   *
   * @param hardwareType Hardware type to write.
   * @throws Exception
   */
  void writeHardwareType(HardwareType hardwareType) throws Exception;

  /**
   * Delete the {@link HardwareType} associated with the given unique name.
   *
   * @param hardwareTypeName Name of the hardware type to delete.
   * @throws Exception
   */
  void deleteHardwareType(String hardwareTypeName) throws Exception;

  /**
   * Get the {@link ImageType} associated with the given unique name or null if no such provider exists.
   *
   * @param imageTypeName Unique name of the provider to get.
   * @return Image type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ImageType getImageType(String imageTypeName) throws Exception;

  /**
   * Get all {@link ImageType}s.
   *
   * @return Collection of all image types.
   * @throws Exception
   */
  Collection<ImageType> getAllImageTypes() throws Exception;

  /**
   * Write the given {@link ImageType} to the store. Will overwrite the existing {@link ImageType} if it exists.
   *
   * @param imageType Image type to write.
   * @throws Exception
   */
  void writeImageType(ImageType imageType) throws Exception;

  /**
   * Delete the {@link ImageType} associated with the given unique name.
   *
   * @param imageTypeName Name of the image type to delete.
   * @throws Exception
   */
  void deleteImageType(String imageTypeName) throws Exception;

  /**
   * Get the {@link Service} associated with the given unique name or null if no such provider exists.
   *
   * @param serviceName Unique name of the provider to get.
   * @return Service matching the given name or null if no such provider exists.
   * @throws Exception
   */
  Service getService(String serviceName) throws Exception;

  /**
   * Get all {@link Service}s.
   *
   * @return Collection of all services.
   * @throws Exception
   */
  Collection<Service> getAllServices() throws Exception;

  /**
   * Write the given {@link Service} to the store. Will overwrite the existing {@link Service} if it exists.
   *
   * @param service Service to write.
   * @throws Exception
   */
  void writeService(Service service) throws Exception;

  /**
   * Delete the {@link Service} associated with the given unique name.
   *
   * @param serviceName Name of the service to delete.
   * @throws Exception
   */
  void deleteService(String serviceName) throws Exception;

  /**
   * Get the {@link ClusterTemplate} associated with the given unique name or null if no such provider exists.
   *
   * @param clusterTemplateName Unique name of the provider to get.
   * @return Cluster template matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ClusterTemplate getClusterTemplate(String clusterTemplateName) throws Exception;

  /**
   * Get all {@link ClusterTemplate}s.
   *
   * @return Collection of all cluster templates.
   * @throws Exception
   */
  Collection<ClusterTemplate> getAllClusterTemplates() throws Exception;

  /**
   * Write the given {@link ClusterTemplate} to the store.
   * Will overwrite the existing {@link ClusterTemplate} if it exists.
   *
   * @param clusterTemplate Cluster template to write.
   * @throws Exception
   */
  void writeClusterTemplate(ClusterTemplate clusterTemplate) throws Exception;

  /**
   * Delete the {@link ClusterTemplate} associated with the given unique name.
   *
   * @param clusterTemplateName Name of the cluster template to delete.
   * @throws Exception
   */
  void deleteClusterTemplate(String clusterTemplateName) throws Exception;

  /**
   * Get the {@link ProviderType} associated with the given unique name or null if no such provider type exists.
   *
   * @param providerTypeName Unique name of the provider type to get.
   * @return Provider type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ProviderType getProviderType(String providerTypeName) throws Exception;

  /**
   * Get all {@link ProviderType}s.
   *
   * @return Collection of all provider types.
   * @throws Exception
   */
  Collection<ProviderType> getAllProviderTypes() throws Exception;

  /**
   * Write the given {@link ProviderType} to the store.
   * Will overwrite the existing {@link ProviderType} if it exists.
   *
   * @param providerType Provider type to write.
   * @throws Exception
   */
  void writeProviderType(ProviderType providerType) throws Exception;

  /**
   * Delete the {@link ProviderType} associated with the given unique name.
   *
   * @param providerTypeName Name of the provider type to delete.
   * @throws Exception
   */
  void deleteProviderType(String providerTypeName) throws Exception;

  /**
   * Get the {@link AutomatorType} associated with the given unique name or null if no such automator type exists.
   *
   * @param automatorTypeName Unique name of the automator type to get.
   * @return Automator type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  AutomatorType getAutomatorType(String automatorTypeName) throws Exception;

  /**
   * Get all {@link AutomatorType}s.
   *
   * @return Collection of all automator types.
   * @throws Exception
   */
  Collection<AutomatorType> getAllAutomatorTypes() throws Exception;

  /**
   * Write the given {@link AutomatorType} to the store.
   * Will overwrite the existing {@link AutomatorType} if it exists.
   *
   * @param automatorType Automator type to write.
   * @throws Exception
   */
  void writeAutomatorType(AutomatorType automatorType) throws Exception;

  /**
   * Delete the {@link AutomatorType} associated with the given unique name.
   *
   * @param automatorTypeName Name of the automator type to delete.
   * @throws Exception
   */
  void deleteAutomatorType(String automatorTypeName) throws Exception;
}
