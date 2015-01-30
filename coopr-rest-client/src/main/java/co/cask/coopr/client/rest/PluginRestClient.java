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
import com.google.common.base.Supplier;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link co.cask.coopr.client.PluginClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class PluginRestClient extends RestClient implements PluginClient {

  private static final String AUTOMATOR_TYPE_STR = "automatortypes";
  private static final String PROVIDER_TYPE_STR = "providertypes";
  private static final Type AUTOMATOR_TYPE_LIST = new TypeToken<List<AutomatorType>>() { }.getType();
  private static final Type PROVIDER_TYPE_LIST = new TypeToken<List<ProviderType>>() { }.getType();
  private static final Type RESOURCE_TYPE_MAP = new TypeToken<Map<String, Set<ResourceMeta>>>() { }.getType();

  public PluginRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  public PluginRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient, Gson gson) {
    super(config, httpClient, gson);
  }

  @Override
  public List<AutomatorType> getAllAutomatorTypes() throws IOException {
    URI getUri = resolveURL(String.format("/plugins/%s", AUTOMATOR_TYPE_STR));
    return getAll(getUri, AUTOMATOR_TYPE_LIST);
  }

  @Override
  public AutomatorType getAutomatorType(String id) throws IOException {
    URI getUri = resolveURL(String.format("/plugins/%s/%s", AUTOMATOR_TYPE_STR, id));
    return getSingle(getUri, AutomatorType.class);
  }

  @Override
  public List<ProviderType> getAllProviderTypes() throws IOException {
    URI getUri = resolveURL(String.format("/plugins/%s", PROVIDER_TYPE_STR));
    return getAll(getUri, PROVIDER_TYPE_LIST);
  }

  @Override
  public ProviderType getProviderType(String id) throws IOException {
    URI getUri = resolveURL(String.format("/plugins/%s/%s", PROVIDER_TYPE_STR, id));
    return getSingle(getUri, ProviderType.class);
  }

  @Override
  public Map<String, Set<ResourceMeta>> getAutomatorTypeResources(String id, String resourceType, ResourceStatus status)
    throws IOException {
    String url = String.format("/plugins/%s/%s/%s", AUTOMATOR_TYPE_STR, id, resourceType);
    if (status != null) {
      url =  url + String.format("/?status=%s", status.name());
    }
    return getPluginTypeMap(url, RESOURCE_TYPE_MAP);
  }

  @Override
  public Map<String, Set<ResourceMeta>> getProviderTypeResources(String id, String resourceType, ResourceStatus status)
    throws IOException {
    String url = String.format("/plugins/%s/%s/%s", PROVIDER_TYPE_STR, id, resourceType);
    if (status != null) {
      url =  url + String.format("/?status=%s", status.name());
    }
    return getPluginTypeMap(url, RESOURCE_TYPE_MAP);
  }

  @Override
  public void stageAutomatorTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(resolveURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/stage", AUTOMATOR_TYPE_STR, id, resourceType, resourceName, version)));
  }

  @Override
  public void stageProviderTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(resolveURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/stage", PROVIDER_TYPE_STR, id, resourceType, resourceName, version)));
  }

  @Override
  public void recallAutomatorTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(resolveURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/recall", AUTOMATOR_TYPE_STR, id, resourceType, resourceName, version)));
  }

  @Override
  public void recallProviderTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    execPost(resolveURL(String.format("/plugins/%s/%s/%s/%s/versions/%s/recall", PROVIDER_TYPE_STR, id, resourceType, resourceName, version)));
  }

  @Override
  public void deleteAutomatorTypeResourceVersion(String id, String resourceType, String resourceName, String version)
    throws IOException {
    delete(String.format("plugins/%s/%s/%s/%s/versions", AUTOMATOR_TYPE_STR, id, resourceType,
                         resourceName), version);
  }

  @Override
  public void deleteProviderTypeResourceVersion(String id, String resourceType, String resourceName, String version)
    throws IOException {
    delete(String.format("plugins/%s/%s/%s/%s/versions", PROVIDER_TYPE_STR, id, resourceType,
                         resourceName), version);
  }

  @Override
  public void syncPlugins() throws IOException {
    execPost(resolveURL("/plugins/sync"));

  }
}
