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

import co.cask.common.http.exception.HttpFailureException;
import co.cask.coopr.client.ClientManager;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.client.rest.handler.PluginTestConstants;
import co.cask.coopr.client.rest.handler.PluginTestHandler;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.http.NettyHttpService;
import com.google.common.collect.ImmutableSet;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginRestTest {

  protected static ClientManager clientManager;
  static PluginClient pluginRestClient;
  private static NettyHttpService httpService;

  @BeforeClass
  public static void setupTestClass() throws Exception {
    PluginTestHandler handler = new PluginTestHandler();
    NettyHttpService.Builder builder = NettyHttpService.builder();
    builder.addHttpHandlers(ImmutableSet.of(handler));

    builder.setHost("localhost");
    builder.setPort(0);

    builder.setConnectionBacklog(200);
    builder.setExecThreadPoolSize(10);
    builder.setBossThreadPoolSize(1);
    builder.setWorkerThreadPoolSize(1);
    httpService = builder.build();
    httpService.startAndWait();

    int testServerPort = httpService.getBindAddress().getPort();
    String testServerHost = httpService.getBindAddress().getHostName();
    clientManager = new RestClientManager(RestClientConnectionConfig.builder(testServerHost, testServerPort)
                    .userId(PluginTestConstants.TEST_USER_ID)
                    .tenantId(PluginTestConstants.TEST_TENANT_ID).build());
    pluginRestClient = clientManager.getPluginClient();

  }

  @AfterClass
  public static void cleanupTestClass() {
    httpService.stopAndWait();
  }

  @Test
  public void getAllAutomatorTypesSuccessTest() throws IOException {
    List<AutomatorType> allAutomatorTypes = pluginRestClient.getAllAutomatorTypes();
    Assert.assertEquals(allAutomatorTypes, PluginTestConstants.AUTOMATOR_LISTS);
  }

  @Test
  public void getAutomatorTypesSuccessTest() throws IOException {
    AutomatorType automatorType = pluginRestClient.getAutomatorType(PluginTestConstants.CHEF_PLUGIN);
    Assert.assertEquals(automatorType, PluginTestConstants.AUTOMATOR_TYPE);
  }

  @Test
  public void getAutomatorTypesNotExistTest() throws IOException {
    try {
      pluginRestClient.getAutomatorType(PluginTestConstants.NOT_EXISISTING_PLUGIN);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void getAutomatorTypeResourcesSuccessTest() throws IOException {
    Map<String, Set<ResourceMeta>> resourcesMap =
      pluginRestClient.getAutomatorTypeResources(PluginTestConstants.CHEF_PLUGIN,
                                                 PluginTestConstants.REACTOR_RESOURCE, ResourceStatus.ACTIVE);
    Assert.assertEquals(resourcesMap, PluginTestConstants.TYPE_RESOURCES);
  }

  @Test
  public void getAutomatorTypeResourcesNotExistTest() throws IOException {
    try {
      pluginRestClient.getAutomatorTypeResources(PluginTestConstants.CHEF_PLUGIN,
                                                 PluginTestConstants.NOT_EXISISTING_RESOURCE, ResourceStatus.ACTIVE);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void stageAutomatorTypeResourceSuccessTest() throws IOException {
    PluginClient pluginRestClient = clientManager.getPluginClient();
    pluginRestClient.stageAutomatorTypeResource(PluginTestConstants.CHEF_PLUGIN, PluginTestConstants.TEST_RESOURCE_TYPE,
                                                PluginTestConstants.REACTOR_RESOURCE, PluginTestConstants.VERSION);
  }

  @Test
  public void recallAutomatorTypeResourceSuccessTest() throws IOException {
    pluginRestClient.recallAutomatorTypeResource(PluginTestConstants.CHEF_PLUGIN,
                                                 PluginTestConstants.TEST_RESOURCE_TYPE,
                                                 PluginTestConstants.REACTOR_RESOURCE, PluginTestConstants.VERSION);
  }

  @Test
  public void deleteAutomatorTypeResourceSuccessTest() throws IOException {
    pluginRestClient.deleteAutomatorTypeResourceVersion(PluginTestConstants.CHEF_PLUGIN,
                                                        PluginTestConstants.TEST_RESOURCE_TYPE,
                                                        PluginTestConstants.REACTOR_RESOURCE,
                                                        PluginTestConstants.VERSION);
  }

  @Test
  public void getAllProviderTypesSuccessTest() throws IOException {
    List<ProviderType> allProviderTypes = pluginRestClient.getAllProviderTypes();
    Assert.assertEquals(allProviderTypes, PluginTestConstants.PROVIDER_LISTS);
  }

  @Test
  public void getProviderTypesSuccessTest() throws IOException {
    ProviderType providerType = pluginRestClient.getProviderType(PluginTestConstants.JOYENT_PLUGIN);
    Assert.assertEquals(providerType, PluginTestConstants.PROVIDER_TYPE);
  }

  @Test
  public void getProviderTypesNotExistTest() throws IOException {
    try {
      pluginRestClient.getProviderType(PluginTestConstants.NOT_EXISISTING_PLUGIN);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void getProviderTypeResourcesSuccessTest() throws IOException {
    Map<String, Set<ResourceMeta>> resourcesMap =
      pluginRestClient.getProviderTypeResources(PluginTestConstants.JOYENT_PLUGIN,
                                                PluginTestConstants.REACTOR_RESOURCE, ResourceStatus.ACTIVE);
    Assert.assertEquals(resourcesMap, PluginTestConstants.TYPE_RESOURCES);
  }

  @Test
  public void getProviderTypeResourcesNotExistTest() throws IOException {
    try {
      pluginRestClient.getProviderTypeResources(PluginTestConstants.JOYENT_PLUGIN,
                                                PluginTestConstants.NOT_EXISISTING_RESOURCE, ResourceStatus.ACTIVE);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void stageProviderTypeResourceSuccessTest() throws IOException {
    pluginRestClient.stageProviderTypeResource(PluginTestConstants.JOYENT_PLUGIN,
                                               PluginTestConstants.TEST_RESOURCE_TYPE,
                                               PluginTestConstants.REACTOR_RESOURCE, PluginTestConstants.VERSION);
  }

  @Test
  public void recallProviderTypeResourceSuccessTest() throws IOException {
    pluginRestClient.recallProviderTypeResource(PluginTestConstants.JOYENT_PLUGIN,
                                                PluginTestConstants.TEST_RESOURCE_TYPE,
                                                PluginTestConstants.REACTOR_RESOURCE, PluginTestConstants.VERSION);
  }

  @Test
  public void deleteProviderTypeResourceSuccessTest() throws IOException {
    PluginClient pluginRestClient = clientManager.getPluginClient();
    pluginRestClient.deleteProviderTypeResourceVersion(PluginTestConstants.JOYENT_PLUGIN,
                                                       PluginTestConstants.TEST_RESOURCE_TYPE,
                                                       PluginTestConstants.REACTOR_RESOURCE,
                                                       PluginTestConstants.VERSION);
  }
}
