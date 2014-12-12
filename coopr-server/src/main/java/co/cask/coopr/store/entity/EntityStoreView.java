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
package co.cask.coopr.store.entity;

import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.PartialTemplate;

import java.io.IOException;
import java.util.Collection;

/**
 * A view of the entity store for adding, modifying, retrieving, and deleting entities that are accessible by
 * a tenant admin user.
 * TODO: introduce concept of owner or group acls for these entities
 */
public interface EntityStoreView {

  /**
   * Get the {@link co.cask.coopr.spec.Provider} associated with the given unique name
   * or null if no such provider exists.
   *
   * @param providerName Unique name of the provider to get.
   * @return Provider matching the given name or null if no such provider exists.
   * @throws Exception
   */
  Provider getProvider(String providerName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.Provider} associated with the given unique name and version
   * or null if no such provider exists.
   *
   * @param providerName Unique name of the provider to get.
   * @param version Version of the provider to get.
   * @return Provider matching the given name and version or null if no such provider exists.
   * @throws Exception
   */
  Provider getProvider(String providerName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.Provider}s.
   *
   * @return Collection of all providers.
   * @throws Exception
   */
  Collection<Provider> getAllProviders() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.Provider} to the store. Will overwrite the
   * existing {@link co.cask.coopr.spec.Provider} if it exists.
   *
   * @param provider Provider to write.
   * @throws Exception
   */
  void writeProvider(Provider provider) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.Provider} associated with the given unique name.
   *
   * @param providerName Name of the provider to delete.
   * @throws Exception
   */
  void deleteProvider(String providerName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.Provider} associated with the given unique name and version.
   *
   * @param providerName Name of the provider to delete.
   * @param version Version of the provider to delete.
   * @throws Exception
   */
  void deleteProvider(String providerName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.HardwareType} associated with the
   * given unique name or null if no such hardware type exists.
   *
   * @param hardwareTypeName Unique name of the hardware type to get.
   * @return Hardware type matching the given name or null if no such hardware type exists.
   * @throws Exception
   */
  HardwareType getHardwareType(String hardwareTypeName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.HardwareType} associated with the
   * given unique name and version or null if no such hardware type exists.
   *
   * @param hardwareTypeName Unique name of the hardware type to get.
   * @param version Version of the hardware type to get.
   * @return Hardware type matching the given name and version or null if no such hardware type exists.
   * @throws Exception
   */
  HardwareType getHardwareType(String hardwareTypeName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.HardwareType}s.
   *
   * @return Collection of all hardware types.
   * @throws Exception
   */
  Collection<HardwareType> getAllHardwareTypes() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.HardwareType} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.HardwareType} if it exists.
   *
   * @param hardwareType Hardware type to write.
   * @throws Exception
   */
  void writeHardwareType(HardwareType hardwareType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.HardwareType} associated with the given unique name.
   *
   * @param hardwareTypeName Name of the hardware type to delete.
   * @throws Exception
   */
  void deleteHardwareType(String hardwareTypeName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.HardwareType} associated with the given unique name and version.
   *
   * @param hardwareTypeName Name of the hardware type to delete.
   * @param version Version of the hardware type to delete.
   * @throws Exception
   */
  void deleteHardwareType(String hardwareTypeName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.ImageType} associated with the
   * given unique name or null if no such image type exists.
   *
   * @param imageTypeName Unique name of the image type to get.
   * @return Image type matching the given name or null if no such image type exists.
   * @throws Exception
   */
  ImageType getImageType(String imageTypeName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.ImageType} associated with the
   * given unique name or null if no such image type exists.
   *
   * @param imageTypeName Unique name of the image type to get.
   * @param version Version of the image type to get.
   * @return Image type matching the given name and version or null if no such image type exists.
   * @throws Exception
   */
  ImageType getImageType(String imageTypeName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.ImageType}s.
   *
   * @return Collection of all image types.
   * @throws Exception
   */
  Collection<ImageType> getAllImageTypes() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.ImageType} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.ImageType} if it exists.
   *
   * @param imageType Image type to write.
   * @throws Exception
   */
  void writeImageType(ImageType imageType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.ImageType} associated with the given unique name.
   *
   * @param imageTypeName Name of the image type to delete.
   * @throws Exception
   */
  void deleteImageType(String imageTypeName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.ImageType} associated with the given unique name and version.
   *
   * @param imageTypeName Name of the image type to delete.
   * @param version Version of the image type to delete.
   * @throws Exception
   */
  void deleteImageType(String imageTypeName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.service.Service} associated with the given
   * unique name or null if no such service exists.
   *
   * @param serviceName Unique name of the service to get.
   * @return Service matching the given name or null if no such service exists.
   * @throws Exception
   */
  Service getService(String serviceName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.service.Service} associated with the given
   * unique name and version or null if no such service exists.
   *
   * @param serviceName Unique name of the service to get.
   * @param version Version of the service to get.
   * @return Service matching the given name and version or null if no such service exists.
   * @throws Exception
   */
  Service getService(String serviceName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.service.Service}s.
   *
   * @return Collection of all services.
   * @throws Exception
   */
  Collection<Service> getAllServices() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.service.Service} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.service.Service} if it exists.
   *
   * @param service Service to write.
   * @throws Exception
   */
  void writeService(Service service) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.service.Service} associated with the given unique name.
   *
   * @param serviceName Name of the service to delete.
   * @throws Exception
   */
  void deleteService(String serviceName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.service.Service} associated with the given unique name and version.
   *
   * @param serviceName Name of the service to delete.
   * @param version Version of the service to delete.
   * @throws Exception
   */
  void deleteService(String serviceName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.template.ClusterTemplate} associated with the given unique name
   * or null if no such cluster template exists.
   *
   * @param clusterTemplateName Unique name of the cluster template to get.
   * @return Cluster template matching the given name or null if no such cluster template exists.
   * @throws Exception
   */
  ClusterTemplate getClusterTemplate(String clusterTemplateName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.template.ClusterTemplate} associated with the given unique name and version
   * or null if no such cluster template exists.
   *
   * @param clusterTemplateName Unique name of the cluster template to get.
   * @param version Version of the cluster template to get.
   * @return Cluster template matching the given name or null if no such cluster template exists.
   * @throws Exception
   */
  ClusterTemplate getClusterTemplate(String clusterTemplateName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.template.ClusterTemplate}s.
   *
   * @return Collection of all cluster templates.
   * @throws Exception
   */
  Collection<ClusterTemplate> getAllClusterTemplates() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.template.ClusterTemplate} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.template.ClusterTemplate} if it exists.
   *
   * @param clusterTemplate Cluster template to write.
   * @throws Exception
   */
  void writeClusterTemplate(ClusterTemplate clusterTemplate) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.template.ClusterTemplate} associated with the given unique name.
   *
   * @param clusterTemplateName Name of the cluster template to delete.
   * @throws Exception
   */
  void deleteClusterTemplate(String clusterTemplateName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.template.ClusterTemplate} associated with the given unique name and version.
   *
   * @param clusterTemplateName Name of the cluster template to delete.
   * @param version Version of the cluster template to delete.
   * @throws Exception
   */
  void deleteClusterTemplate(String clusterTemplateName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.template.PartialTemplate} associated with the given unique name
   * or null if no such provider exists.
   *
   * @param partialTemplateName Unique name of the provider to get.
   * @return Partial template matching the given name or null if no such provider exists.
   * @throws Exception
   */
  PartialTemplate getPartialTemplate(String partialTemplateName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.template.PartialTemplate} associated with the given unique name
   * or null if no such provider exists.
   *
   * @param partialTemplateName Unique name of the provider to get.
   * @param version Version of the provider to get.
   * @return Partial template matching the given name or null if no such provider exists.
   * @throws Exception
   */
  PartialTemplate getPartialTemplate(String partialTemplateName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.template.PartialTemplate}s.
   *
   * @return Collection of all partial templates.
   * @throws Exception
   */
  Collection<PartialTemplate> getAllPartialTemplates() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.template.PartialTemplate} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.template.PartialTemplate} if it exists.
   *
   * @param partialTemplate Partial template to write.
   * @throws Exception
   */
  void writePartialTemplate(PartialTemplate partialTemplate) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.template.PartialTemplate} associated with the given unique name.
   *
   * @param partialTemplateName Name of the partial template to delete.
   * @throws Exception
   */
  void deletePartialTemplate(String partialTemplateName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.template.PartialTemplate} associated with the given unique name.
   *
   * @param partialTemplateName Name of the partial template to delete.
   * @param version Version of the partial template to delete.
   * @throws Exception
   */
  void deletePartialTemplate(String partialTemplateName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.plugin.ProviderType} associated with the given unique name
   * or null if no such provider type exists.
   *
   * @param providerTypeName Unique name of the provider type to get.
   * @return Provider type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ProviderType getProviderType(String providerTypeName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.plugin.ProviderType} associated with the given unique name
   * or null if no such provider type exists.
   *
   * @param providerTypeName Unique name of the provider type to get.
   * @param version Version of the provider type to get.
   * @return Provider type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ProviderType getProviderType(String providerTypeName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.plugin.ProviderType}s.
   *
   * @return Collection of all provider types.
   * @throws Exception
   */
  Collection<ProviderType> getAllProviderTypes() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.plugin.ProviderType} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.plugin.ProviderType} if it exists.
   *
   * @param providerType Provider type to write.
   * @throws Exception
   */
  void writeProviderType(ProviderType providerType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.plugin.ProviderType} associated with the given unique name.
   *
   * @param providerTypeName Name of the provider type to delete.
   * @throws Exception
   */
  void deleteProviderType(String providerTypeName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.plugin.ProviderType} associated with the given unique name.
   *
   * @param providerTypeName Name of the provider type to delete.
   * @param version Version of the provider type to delete.
   * @throws Exception
   */
  void deleteProviderType(String providerTypeName, int version) throws IOException, IllegalAccessException;

  /**
   * Get the {@link co.cask.coopr.spec.plugin.AutomatorType} associated with the given unique name or null if
   * no such automator type exists.
   *
   * @param automatorTypeName Unique name of the automator type to get.
   * @return Automator type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  AutomatorType getAutomatorType(String automatorTypeName) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.plugin.AutomatorType} associated with the given unique name or null if
   * no such automator type exists.
   *
   * @param automatorTypeName Unique name of the automator type to get.
   * @param version Version of the automator type to get.
   * @return Automator type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  AutomatorType getAutomatorType(String automatorTypeName, int version) throws IOException;

  /**
   * Get all {@link co.cask.coopr.spec.plugin.AutomatorType}s.
   *
   * @return Collection of all automator types.
   * @throws Exception
   */
  Collection<AutomatorType> getAllAutomatorTypes() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.plugin.AutomatorType} to the store.
   * Will overwrite the existing {@link co.cask.coopr.spec.plugin.AutomatorType} if it exists.
   *
   * @param automatorType Automator type to write.
   * @throws Exception
   */
  void writeAutomatorType(AutomatorType automatorType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.plugin.AutomatorType} associated with the given unique name.
   *
   * @param automatorTypeName Name of the automator type to delete.
   * @throws Exception
   */
  void deleteAutomatorType(String automatorTypeName) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link co.cask.coopr.spec.plugin.AutomatorType} associated with the given unique name.
   *
   * @param automatorTypeName Name of the automator type to delete.
   * @param version Version of the automator type to delete.
   * @throws Exception
   */
  void deleteAutomatorType(String automatorTypeName, int version) throws IOException, IllegalAccessException;
}
