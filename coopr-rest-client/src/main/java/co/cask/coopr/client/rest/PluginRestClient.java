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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link co.cask.coopr.client.PluginClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 *
 * TODO: Implementation
 */
public class PluginRestClient extends RestClient implements PluginClient {

  public PluginRestClient(RestClientConnectionConfig config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  @Override
  public List<AutomatorType> getAllAutomatorTypes() throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public AutomatorType getAutomatorType(String id) throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public List<ProviderType> getAllProviderTypes() throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public ProviderType getProviderType(String id) throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public Map<String, Set<ResourceMeta>> getAutomatorTypeResources(String id, String resourceType, ResourceStatus status)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public Map<String, Set<ResourceMeta>> getProviderTypeResources(String id, String resourceType, ResourceStatus status)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void stageAutomatorTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void stageProviderTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void recallAutomatorTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void recallProviderTypeResource(String id, String resourceType, String resourceName, String version)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void deleteAutomatorTypeResourceVersion(String id, String resourceType, String resourceName, String version)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void deleteProviderTypeResourceVersion(String id, String resourceType, String resourceName, String version)
    throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public void syncPlugins() throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }
}
