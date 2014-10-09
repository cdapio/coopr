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
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * The {@link co.cask.coopr.client.AdminClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class AdminRestClient implements AdminClient {

  private static final Logger LOG = LoggerFactory.getLogger(AdminRestClient.class);

  private static final Gson GSON = new Gson();

  private final RestClient restClient;

  public AdminRestClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public List<ClusterTemplate> getAllClusterTemplates() throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/clustertemplates",
                                                                                   restClient.getVersion())));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    List<ClusterTemplate> clusterTemplates;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      clusterTemplates = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                       new TypeToken<List<ClusterTemplate>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    if (clusterTemplates == null) {
      clusterTemplates = Collections.emptyList();
      LOG.debug("There was no available cluster template found.");
    }
    return clusterTemplates;
  }

  @Override
  public ClusterTemplate getClusterTemplate(String name) throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/clustertemplates/%s",
                                                                                   restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    ClusterTemplate clusterTemplate;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      clusterTemplate = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                       new TypeToken<ClusterTemplate>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return clusterTemplate;
  }

  @Override
  public void deleteClusterTemplate(String name) throws IOException {
    HttpDelete deleteRequest =
      new HttpDelete(restClient.getBaseURL().resolve(String.format("/%s/clustertemplates/%s", restClient.getVersion(),
                                                                   name)));
    CloseableHttpResponse httpResponse = restClient.execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public List<Provider> getAllProviders() throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/providers",
                                                                                   restClient.getVersion())));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    List<Provider> providers;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      providers = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                new TypeToken<List<Provider>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    if (providers == null) {
      providers = Collections.emptyList();
      LOG.debug("There was no available provider found.");
    }
    return providers;
  }

  @Override
  public Provider getProvider(String name) throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/providers/%s",
                                                                                   restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    Provider provider;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      provider = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                               new TypeToken<Provider>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return provider;
  }

  @Override
  public void deleteProvider(String name) throws IOException {
    HttpDelete deleteRequest = new HttpDelete(
      restClient.getBaseURL().resolve(String.format("/%s/providers/%s",  restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public List<Service> getAllServices() throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/services",
                                                                                   restClient.getVersion())));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    List<Service> services;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      services = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                new TypeToken<List<Service>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    if (services == null) {
      services = Collections.emptyList();
      LOG.debug("There was no available services found.");
    }
    return services;
  }

  @Override
  public Service getService(String name) throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/services/%s",
                                                                                   restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    Service service;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      service = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                              new TypeToken<Service>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return service;
  }

  @Override
  public void deleteService(String name) throws IOException {
    HttpDelete deleteRequest = new HttpDelete(
      restClient.getBaseURL().resolve(String.format("/%s/services/%s", restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public List<HardwareType> getAllHardwareTypes() throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/hardwaretypes",
                                                                                   restClient.getVersion())));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    List<HardwareType> hardwareTypes;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      hardwareTypes = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                               new TypeToken<List<HardwareType>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    if (hardwareTypes == null) {
      hardwareTypes = Collections.emptyList();
      LOG.debug("There was no available hardware type found.");
    }
    return hardwareTypes;
  }

  @Override
  public HardwareType getHardwareType(String name) throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/hardwaretypes/%s",
                                                                                   restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    HardwareType hardwareType;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      hardwareType = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                   new TypeToken<HardwareType>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return hardwareType;
  }

  @Override
  public void deleteHardwareType(String name) throws IOException {
    HttpDelete deleteRequest =
      new HttpDelete(restClient.getBaseURL().resolve(String.format("/%s/hardwaretypes/%s", restClient.getVersion(),
                                                                   name)));
    CloseableHttpResponse httpResponse = restClient.execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  @Override
  public List<ImageType> getAllImageTypes() throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/imagetypes",
                                                                                   restClient.getVersion())));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    List<ImageType> imageTypes;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      imageTypes = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                    new TypeToken<List<ImageType>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    if (imageTypes == null) {
      imageTypes = Collections.emptyList();
      LOG.debug("There was no available image type found.");
    }
    return imageTypes;
  }

  @Override
  public ImageType getImageType(String name) throws IOException {
    HttpGet getRequest = new HttpGet(restClient.getBaseURL().resolve(String.format("/%s/imagetypes/%s",
                                                                                   restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(getRequest);
    ImageType imageType;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      imageType = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                   new TypeToken<ImageType>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return imageType;
  }

  @Override
  public void deleteImageType(String name) throws IOException {
    HttpDelete deleteRequest = new HttpDelete(
      restClient.getBaseURL().resolve(String.format("/%s/imagetypes/%s", restClient.getVersion(), name)));
    CloseableHttpResponse httpResponse = restClient.execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }
}
