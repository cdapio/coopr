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

package co.cask.coopr.client.rest;

import co.cask.coopr.client.AdminClient;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.PartialTemplate;
import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * The {@link co.cask.coopr.client.AdminClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class AdminRestClient extends RestClient implements AdminClient {

  private static final String CLUSTER_TEMPLATES_URL_SUFFIX = "clustertemplates";
  private static final String PARTIAL_TEMPLATES_URL_SUFFIX = "partialtemplates";
  private static final String PROVIDERS_URL_SUFFIX = "providers";
  private static final String SERVICES_URL_SUFFIX = "services";
  private static final String HARDWARE_TYPES_URL_SUFFIX = "hardwaretypes";
  private static final String IMAGE_TYPES_URL_SUFFIX = "imagetypes";
  // Constant types
  private static final Type CLUSTER_TEMPLATES_LIST_TYPE = new TypeToken<List<ClusterTemplate>>() { }.getType();
  private static final Type PARTIAL_TEMPLATES_LIST_TYPE = new TypeToken<List<PartialTemplate>>() { }.getType();
  private static final Type PROVIDERS_LIST_TYPE = new TypeToken<List<Provider>>() { }.getType();
  private static final Type SERVICES_LIST_TYPE = new TypeToken<List<Service>>() { }.getType();
  private static final Type HARDWARE_TYPES_LIST_TYPE = new TypeToken<List<HardwareType>>() { }.getType();
  private static final Type IMAGE_TYPES_LIST_TYPE = new TypeToken<List<ImageType>>() { }.getType();

  public AdminRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  public AdminRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient, Gson gson) {
    super(config, httpClient, gson);
  }

  @Override
  public List<ClusterTemplate> getAllClusterTemplates() throws IOException {
    return getAll(CLUSTER_TEMPLATES_URL_SUFFIX, CLUSTER_TEMPLATES_LIST_TYPE);
  }

  @Override
  public ClusterTemplate getClusterTemplate(String name) throws IOException {
    return getSingle(CLUSTER_TEMPLATES_URL_SUFFIX, name, ClusterTemplate.class);
  }

  @Override
  public void deleteClusterTemplate(String name) throws IOException {
    delete(CLUSTER_TEMPLATES_URL_SUFFIX, name);
  }

  @Override
  public List<PartialTemplate> getAllPartialTemplates() throws IOException {
    return getAll(PARTIAL_TEMPLATES_URL_SUFFIX, PARTIAL_TEMPLATES_LIST_TYPE);
  }

  @Override
  public PartialTemplate getPartialTemplate(String name) throws IOException {
    return getSingle(PARTIAL_TEMPLATES_URL_SUFFIX, name, PartialTemplate.class);
  }

  @Override
  public void deletePartialTemplate(String name) throws IOException {
    delete(PARTIAL_TEMPLATES_URL_SUFFIX, name);
  }

  @Override
  public List<Provider> getAllProviders() throws IOException {
    return getAll(PROVIDERS_URL_SUFFIX, PROVIDERS_LIST_TYPE);
  }

  @Override
  public Provider getProvider(String name) throws IOException {
    return getSingle(PROVIDERS_URL_SUFFIX, name, Provider.class);
  }

  @Override
  public void deleteProvider(String name) throws IOException {
    delete(PROVIDERS_URL_SUFFIX, name);
  }

  @Override
  public List<Service> getAllServices() throws IOException {
    return getAll(SERVICES_URL_SUFFIX, SERVICES_LIST_TYPE);
  }

  @Override
  public Service getService(String name) throws IOException {
    return getSingle(SERVICES_URL_SUFFIX, name, Service.class);
  }

  @Override
  public void deleteService(String name) throws IOException {
    delete(SERVICES_URL_SUFFIX, name);
  }

  @Override
  public List<HardwareType> getAllHardwareTypes() throws IOException {
    return getAll(HARDWARE_TYPES_URL_SUFFIX, HARDWARE_TYPES_LIST_TYPE);
  }

  @Override
  public HardwareType getHardwareType(String name) throws IOException {
    return getSingle(HARDWARE_TYPES_URL_SUFFIX, name, HardwareType.class);
  }

  @Override
  public void deleteHardwareType(String name) throws IOException {
    delete(HARDWARE_TYPES_URL_SUFFIX, name);
  }

  @Override
  public List<ImageType> getAllImageTypes() throws IOException {
    return getAll(IMAGE_TYPES_URL_SUFFIX, IMAGE_TYPES_LIST_TYPE);
  }

  @Override
  public ImageType getImageType(String name) throws IOException {
    return getSingle(IMAGE_TYPES_URL_SUFFIX, name, ImageType.class);
  }

  @Override
  public void deleteImageType(String name) throws IOException {
    delete(IMAGE_TYPES_URL_SUFFIX, name);
  }
}
