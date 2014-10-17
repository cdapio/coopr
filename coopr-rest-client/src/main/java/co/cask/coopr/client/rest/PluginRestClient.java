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

import co.cask.coopr.client.PluginClient;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import com.google.common.base.Charsets;
import com.google.common.reflect.TypeToken;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The {@link co.cask.coopr.client.PluginClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class PluginRestClient extends RestClient implements PluginClient {

  private static final String AUTOMATOR_TYPE_STR = "automatortypes";
  private static final String PROVIDER_TYPE_STR = "providertypes";

  public PluginRestClient(RestClientConnectionConfig config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  @Override
  public List<AutomatorType> getAllAutomatorTypes() throws IOException {
    URI getUri = buildFullURL(String.format("/plugins/%s", AUTOMATOR_TYPE_STR));
    return getAll(getUri, new TypeToken<List<AutomatorType>>() {
    }.getType());
  }

  @Override
  public AutomatorType getAutomatorType(String id) throws IOException {
    URI getUri = buildFullURL(String.format("/plugins/%s/%s", AUTOMATOR_TYPE_STR, id));
    return getSingle(getUri, AutomatorType.class);
  }

  @Override
  public List<ProviderType> getAllProviderTypes() throws IOException {
    URI getUri = buildFullURL(String.format("/plugins/%s", PROVIDER_TYPE_STR));
    return getAll(getUri, new TypeToken<List<ProviderType>>() {
    }.getType());
  }

  @Override
  public ProviderType getProviderType(String id) throws IOException {
    URI getUri = buildFullURL(String.format("/plugins/%s/%s", PROVIDER_TYPE_STR, id));
    return getSingle(getUri, ProviderType.class);
  }

  @Override
  public Map<String, Set<ResourceMeta>> getAutomatorTypeResources(String id, String resourceType, ResourceStatus status)
    throws IOException {
    Type type = new TypeToken<Map<String, Set<ResourceMeta>>>() {
    }.getType();
    return getPluginTypeMap(String.format("/plugins/%s/%s/%s/?status=%s", AUTOMATOR_TYPE_STR, id, resourceType,
                                          status.toString()), type);
  }

  @Override
  public Map<String, Set<ResourceMeta>> getProviderTypeResources(String id, String resourceType, ResourceStatus status)
    throws IOException {
    Type type = new TypeToken<Map<String, Set<ResourceMeta>>>() {
    }.getType();
    return getPluginTypeMap(String.format("/plugins/%s/%s/%s/?status=%s", PROVIDER_TYPE_STR, id, resourceType,
                                          status.toString()), type);
  }

  private <T> Map<String, Set<T>> getPluginTypeMap(String url, Type type) throws IOException {
    String fullUrl = String.format("%s%s", getBaseURL(), url);
    HttpGet getRequest = new HttpGet(fullUrl);
    CloseableHttpResponse httpResponse = execute(getRequest);
    Map<String, Set<T>> resultMap;
    try {
      RestClient.analyzeResponseCode(httpResponse);
      resultMap = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8), type);
    } finally {
      httpResponse.close();
    }
    return resultMap != null ? resultMap : new TreeMap<String, Set<T>>();
  }


  @Override
  public void stageAutomatorTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(buildFullURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/stage",
                                        AUTOMATOR_TYPE_STR, id, resourceType,
                                        resourceName, version)));
  }

  @Override
  public void stageProviderTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(buildFullURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/stage",
                                        PROVIDER_TYPE_STR, id, resourceType,
                                        resourceName, version)));
  }

  @Override
  public void recallAutomatorTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(buildFullURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/recall",
                                        AUTOMATOR_TYPE_STR, id, resourceType,
                                        resourceName, version)));
  }

  @Override
  public void recallProviderTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(buildFullURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/recall",
                                        PROVIDER_TYPE_STR, id, resourceType,
                                        resourceName, version)));
  }

  @Override
  public void deleteAutomatorTypeResourceVersion(String id, String resourceType, String resourceName, String version)
    throws IOException {
    delete(String.format("plugins/%s/%s/%s/%s/versions", PROVIDER_TYPE_STR, id, resourceName,
                         resourceName), version);
  }

  @Override
  public void deleteProviderTypeResourceVersion(String id, String resourceType, String resourceName, String version)
    throws IOException {
    delete(String.format("plugins/%s/%s/%s/%s/versions", PROVIDER_TYPE_STR, id, resourceName,
                         resourceName), version);
  }

  @Override
  public void syncPlugins() throws IOException {
    execPost(buildFullURL("/plugins/sync"));

  }

  private void execPost(URI uri) throws IOException {
    HttpPost postRequest = new HttpPost(uri);
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }
}
