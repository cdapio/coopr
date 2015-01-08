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
import co.cask.coopr.spec.template.PartialTemplate;

import java.io.IOException;
import java.util.List;

/**
 * The client API for manage admin defined entities.
 */
public interface AdminClient {

  /**
   * Retrieves the list of all the cluster templates. If no templates exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.template.ClusterTemplate} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<ClusterTemplate> getAllClusterTemplates() throws IOException;

  /**
   * Retrieves details about a cluster template by the name.
   *
   * @return {co.cask.coopr.spec.template.ClusterTemplate} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  ClusterTemplate getClusterTemplate(String name) throws IOException;

  /**
   * Deletes a cluster template by the name.
   *
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteClusterTemplate(String name) throws IOException;

  /**
   * Retrieves the list of all the partial templates. If no templates exist, returns an empty list.
   *
   * @return List of {@link PartialTemplate} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<PartialTemplate> getAllPartialTemplates() throws IOException;

  /**
   * Retrieves details about a partial template by the name.
   *
   * @return {@link PartialTemplate} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  PartialTemplate getPartialTemplate(String name) throws IOException;

  /**
   * Deletes a partial template by the name.
   *
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deletePartialTemplate(String name) throws IOException;


  /**
   * Retrieves the list of all configured providers. If no providers exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.Provider} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<Provider> getAllProviders() throws IOException;

  /**
   * Retrieves details about a provider type by the name.
   *
   * @return {@link co.cask.coopr.spec.Provider} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  Provider getProvider(String name) throws IOException;

  /**
   * Deletes a provider type by the name.
   *
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteProvider(String name) throws IOException;

  /**
   * Retrieves the list of all services. If no services exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.service.Service} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<Service> getAllServices() throws IOException;

  /**
   * Retrieves details about a services by the name.
   *
   * @return {@link co.cask.coopr.spec.service.Service} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  Service getService(String name) throws IOException;

  /**
   * Deletes services by the name.
   *
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteService(String name) throws IOException;

  /**
   * Retrieves the list of all the hardware types. If no hardware types exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.HardwareType} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<HardwareType> getAllHardwareTypes() throws IOException;

  /**
   * Retrieves details about a hardware type by the name.
   *
   * @return {@link co.cask.coopr.spec.HardwareType} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  HardwareType getHardwareType(String name) throws IOException;

  /**
   * Deletes a hardware type by the name.
   *
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteHardwareType(String name) throws IOException;

  /**
   * Retrieves the list of all the image types. If no image types exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.ImageType} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<ImageType> getAllImageTypes() throws IOException;

  /**
   * Retrieves details about a image type by the name.
   *
   * @return {@link co.cask.coopr.spec.ImageType} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  ImageType getImageType(String name) throws IOException;

  /**
   * Deletes a image type by the name.
   *
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteImageType(String name) throws IOException;
}
