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

import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;

import java.util.List;

/**
 * The client API for manage admin defined entities..
 */
public interface AdminClient {

  /**
   * Retrieves the list of all the cluster templates. If no templates exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.template.ClusterTemplate} objects
   */
  List<ClusterTemplate> getAllClusterTemplates();

  /**
   * Retrieves details about a cluster template by the name.
   *
   * @return {co.cask.coopr.spec.template.ClusterTemplate} object
   */
  ClusterTemplate getClusterTemplate(String name);

  /**
   * Deletes a cluster template by the name.
   */
  void deleteClusterTemplate(String name);


  /**
   * Retrieves the list of all configured providers. If no providers exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.Provider} objects
   */
  List<Provider> getAllProviders();

  /**
   * Retrieves details about a provider type by the name.
   *
   * @return {@link co.cask.coopr.spec.Provider} object
   */
  Provider getProvider(String name);

  /**
   * Deletes a provider type by the name.
   */
  void deleteProvider(String name);

  /**
   * Retrieves the list of all services. If no services exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.service.Service} objects
   */
  List<Service> getAllServices();

  /**
   * Retrieves details about a services by the name.
   *
   * @return {@link co.cask.coopr.spec.service.Service} object
   */
  Service getService(String name);

  /**
   * Deletes services by the name.
   */
  void deleteService(String name);

  /**
   * Retrieves the list of all the hardware types. If no hardware types exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.HardwareType} objects
   */
  List<HardwareType> getAllHardwareTypes();

  /**
   * Retrieves details about a hardware type by the name.
   *
   * @return {@link co.cask.coopr.spec.HardwareType} object
   */
  HardwareType getHardwareType(String name);

  /**
   * Deletes a hardware type by the name.
   */
  void deleteHardwareType(String name);

  /**
   * Retrieves the list of all the image types. If no image types exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.ImageType} objects
   */
  List<ImageType> getAllImageTypes();

  /**
   * Retrieves details about a image type by the name.
   *
   * @return {@link co.cask.coopr.spec.ImageType} object
   */
  ImageType getImageType(String name);

  /**
   * Deletes a image type by the name.
   */
  void deleteImageType(String name);
}
