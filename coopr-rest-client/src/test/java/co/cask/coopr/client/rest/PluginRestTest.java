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

import co.cask.coopr.Entities;
import co.cask.coopr.client.ClientManager;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.client.rest.handler.PluginTestHandler;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.http.localserver.LocalTestServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginRestTest {

  public static final String TEST_USER_ID = "test";
  public static final String TEST_TENANT_ID = "supertest";

  protected ClientManager clientManager;
  protected String testServerHost;
  protected int testServerPort;
  private LocalTestServer localTestServer;
  PluginClient pluginRestClient;


  @Before
  public void setUp() throws Exception {
    localTestServer = new LocalTestServer(null, null);
    PluginTestHandler pluginHandler = new PluginTestHandler();
    localTestServer.register("*", pluginHandler);
    localTestServer.start();
    testServerHost = localTestServer.getServiceAddress().getHostName();
    testServerPort = localTestServer.getServiceAddress().getPort();
    clientManager = RestClientManager.builder(testServerHost, testServerPort)
      .userId(TEST_USER_ID)
      .tenantId(TEST_TENANT_ID)
      .build();
    pluginRestClient = clientManager.getPluginClient();
  }

  @Test
  public void getAllAutomatorTypesSuccessTest() throws IOException {
    List<AutomatorType> allAutomatorTypes = pluginRestClient.getAllAutomatorTypes();
    Assert.assertEquals(allAutomatorTypes, Lists.newArrayList(Entities.AutomatorTypeExample.CHEF,
                                                              Entities.AutomatorTypeExample.PUPPET));
  }

  @Test
  public void getAutomatorTypesSuccessTest() throws IOException {
    AutomatorType automatorType = pluginRestClient.getAutomatorType("chef");
    Assert.assertEquals(automatorType, Entities.AutomatorTypeExample.CHEF);
  }

  @Test
  public void getAutomatorTypeResourcesSuccessTest() throws IOException {
    Map<String, Set<ResourceMeta>> resourcesMap = pluginRestClient.getAutomatorTypeResources("chef", "reactor",
                                                                                             ResourceStatus.ACTIVE);
    ResourceMeta reactor1 = new ResourceMeta("reactor", 1, ResourceStatus.ACTIVE);
    ResourceMeta reactor2 = new ResourceMeta("reactor", 2, ResourceStatus.ACTIVE);
    Assert.assertEquals(resourcesMap, ImmutableMap.of("reactor", ImmutableSet.of(reactor1, reactor2)));
  }

  @Test
  public void stageAutomatorTypeResourceSuccessTest() throws IOException {
    pluginRestClient.stageAutomatorTypeResource("restRes", "testType", "reactor", "v2");
  }

  @Test
  public void recallAutomatorTypeResourceSuccessTest() throws IOException {
    pluginRestClient.recallAutomatorTypeResource("restRes", "testType", "reactor", "v2");
  }

  @Test
  public void deleteAutomatorTypeResourceSuccessTest() throws IOException {
    pluginRestClient.deleteAutomatorTypeResourceVersion("restRes", "testType", "reactor", "v2");
  }

  @Test
  public void getAllProviderTypesSuccessTest() throws IOException {
    List<ProviderType> allProviderTypes = pluginRestClient.getAllProviderTypes();
    Assert.assertEquals(allProviderTypes, Lists.newArrayList(Entities.ProviderTypeExample.JOYENT,
                                                             Entities.ProviderTypeExample.RACKSPACE));
  }

  @Test
  public void getProviderTypesSuccessTest() throws IOException {
    ProviderType providerType = pluginRestClient.getProviderType("joyent");
    Assert.assertEquals(providerType, Entities.ProviderTypeExample.JOYENT);
  }

  @Test
  public void getProviderTypeResourcesSuccessTest() throws IOException {
    Map<String, Set<ResourceMeta>> resourcesMap = pluginRestClient.getProviderTypeResources("chef", "reactor",
                                                                                            ResourceStatus.ACTIVE);
    ResourceMeta reactor1 = new ResourceMeta("reactor", 1, ResourceStatus.ACTIVE);
    ResourceMeta reactor2 = new ResourceMeta("reactor", 2, ResourceStatus.ACTIVE);
    Assert.assertEquals(resourcesMap, ImmutableMap.of("reactor", ImmutableSet.of(reactor1, reactor2)));
  }

  @Test
  public void stageProviderTypeResourceSuccessTest() throws IOException {
    pluginRestClient.stageProviderTypeResource("restRes", "testType", "reactor", "v2");
  }

  @Test
  public void recallProviderTypeResourceSuccessTest() throws IOException {
    pluginRestClient.recallProviderTypeResource("restRes", "testType", "reactor", "v2");
  }

  @Test
  public void deleteProviderTypeResourceSuccessTest() throws IOException {
    pluginRestClient.deleteProviderTypeResourceVersion("restRes", "testType", "reactor", "v2");
  }
}
