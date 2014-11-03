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

package co.cask.coopr.test.client.rest;

import co.cask.common.http.exception.HttpFailureException;
import co.cask.coopr.Entities;
import co.cask.coopr.client.PluginClient;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.test.client.ClientTest;
import co.cask.coopr.test.client.ClientTestEntities;
import com.google.common.collect.Sets;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginRestClientTest extends ClientTest {

  private static final String AUTOMATOR_TYPE_CHEF_NAME = "chef-solo";
  private static final String PROVIDER_TYPE_JOYENT_NAME = "joyent";

  private PluginClient pluginClient;

  @Before
  public void setUpt() throws Exception {
    pluginClient = superadminCientManager.getPluginClient();
  }

  @Test
  public void testGetAllAutomatorTypes() throws IOException {
    List<AutomatorType> result = pluginClient.getAllAutomatorTypes();
    Assert.assertEquals(3, result.size());
    Assert.assertTrue(result.contains(Entities.AutomatorTypeExample.CHEF));
    Assert.assertTrue(result.contains(Entities.AutomatorTypeExample.PUPPET));
    Assert.assertTrue(result.contains(Entities.AutomatorTypeExample.SHELL));
  }

  @Test
  public void testGetAutomatorTypes() throws IOException {
    AutomatorType result = pluginClient.getAutomatorType(AUTOMATOR_TYPE_CHEF_NAME);
    Assert.assertEquals(Entities.AutomatorTypeExample.CHEF, result);
  }

  @Test
  public void testGetAutomatorTypesNotExist() throws IOException {
    try {
      pluginClient.getAutomatorType("test");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAutomatorTypeResources() throws IOException {
    Map<String, Set<ResourceMeta>> result =
      pluginClient.getAutomatorTypeResources(AUTOMATOR_TYPE_CHEF_NAME,
                                             ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                             ResourceStatus.ACTIVE);
    Assert.assertEquals(2, result.size());
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.HADOOP_RESOURCE_META_V1,
                                        ClientTestEntities.HADOOP_RESOURCE_META_V2), result.get("hadoop"));
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.KAFKA_RESOURCE_META), result.get("kafka"));
  }

  @Test
  public void testStageAutomatorTypeResource() throws IOException {
    pluginClient.stageAutomatorTypeResource(AUTOMATOR_TYPE_CHEF_NAME,
                                            ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                            ClientTestEntities.HADOOP_RESOURCE_META_V2.getName(),
                                            String.valueOf(ClientTestEntities.HADOOP_RESOURCE_META_V2.getVersion()));
  }

  @Test
  public void testStageAutomatorTypeResourceUnknownVersion() throws IOException {
    try {
      pluginClient.stageAutomatorTypeResource(AUTOMATOR_TYPE_CHEF_NAME,
                                              ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                              ClientTestEntities.KAFKA_RESOURCE_META.getName(), String.valueOf(5));
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testRecallAutomatorTypeResource() throws IOException {
    pluginClient.recallAutomatorTypeResource(AUTOMATOR_TYPE_CHEF_NAME,
                                             ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                             ClientTestEntities.HADOOP_RESOURCE_META_V1.getName(),
                                             String.valueOf(ClientTestEntities.HADOOP_RESOURCE_META_V1.getVersion()));
  }

  @Test
  public void testRecallAutomatorTypeResourceUnknownResource() throws IOException {
    try {
      pluginClient.recallAutomatorTypeResource(AUTOMATOR_TYPE_CHEF_NAME,
                                               ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(), "test",
                                               String.valueOf(1));
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteAutomatorTypeResource() throws IOException {
    Map<String, Set<ResourceMeta>> result
      = pluginClient.getAutomatorTypeResources(AUTOMATOR_TYPE_CHEF_NAME,
                                               ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                               ResourceStatus.INACTIVE);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.MYSQL_RESOURCE_META), result.get("mysql"));

    pluginClient.deleteAutomatorTypeResourceVersion(AUTOMATOR_TYPE_CHEF_NAME,
                                                    ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                                    ClientTestEntities.MYSQL_RESOURCE_META.getName(),
                                                    String.valueOf(ClientTestEntities.MYSQL_RESOURCE_META
                                                                     .getVersion()));

    result = pluginClient.getAutomatorTypeResources(AUTOMATOR_TYPE_CHEF_NAME,
                                                    ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                                    ResourceStatus.INACTIVE);
    Assert.assertEquals(0, result.size());
  }

  @Test
  public void testDeleteAutomatorTypeResourceActive() throws IOException {
    Map<String, Set<ResourceMeta>> result
      = pluginClient.getAutomatorTypeResources(AUTOMATOR_TYPE_CHEF_NAME,
                                               ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                               ResourceStatus.ACTIVE);
    Assert.assertEquals(2, result.size());
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.HADOOP_RESOURCE_META_V1,
                                        ClientTestEntities.HADOOP_RESOURCE_META_V2), result.get("hadoop"));
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.KAFKA_RESOURCE_META), result.get("kafka"));

    try {
      pluginClient.deleteAutomatorTypeResourceVersion(AUTOMATOR_TYPE_CHEF_NAME,
                                                      ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                                      ClientTestEntities.HADOOP_RESOURCE_META_V1.getName(),
                                                      String.valueOf(ClientTestEntities.HADOOP_RESOURCE_META_V1
                                                                       .getVersion()));
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllProviderTypes() throws IOException {
    List<ProviderType> result = pluginClient.getAllProviderTypes();
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(result.contains(Entities.ProviderTypeExample.JOYENT));
    Assert.assertTrue(result.contains(Entities.ProviderTypeExample.RACKSPACE));
  }

  @Test
  public void testGetProviderTypes() throws IOException {
    ProviderType result = pluginClient.getProviderType(PROVIDER_TYPE_JOYENT_NAME);
    Assert.assertEquals(Entities.ProviderTypeExample.JOYENT, result);
  }

  @Test
  public void testGetProviderTypesNotExist() throws IOException {
    try {
      pluginClient.getProviderType("test");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testProviderTypeResources() throws IOException {
    Map<String, Set<ResourceMeta>> result = pluginClient.getProviderTypeResources(PROVIDER_TYPE_JOYENT_NAME,
                                                ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                                ResourceStatus.ACTIVE);
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.DEV_KEY_RESOURCE_META_V1,
                                        ClientTestEntities.DEV_KEY_RESOURCE_META_V2), result.get("dev"));
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.RESEARCH_KEY_RESOURCE_META), result.get("research"));
  }

  @Test
  public void testGetProviderTypeResourcesUnknownResource() throws IOException {
    try {
      pluginClient.getProviderTypeResources(PROVIDER_TYPE_JOYENT_NAME,
                                                  "test", ResourceStatus.ACTIVE);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void stageProviderTypeResourceSuccessTest() throws IOException {
    pluginClient.stageProviderTypeResource(PROVIDER_TYPE_JOYENT_NAME,
                                           ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                           ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getName(),
                                           String.valueOf(ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getVersion()));
  }

  @Test
  public void recallProviderTypeResourceSuccessTest() throws IOException {
    pluginClient.recallProviderTypeResource(PROVIDER_TYPE_JOYENT_NAME,
                                            ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                            ClientTestEntities.RESEARCH_KEY_RESOURCE_META.getName(),
                                            String.valueOf(ClientTestEntities.RESEARCH_KEY_RESOURCE_META.getVersion()));
  }

  @Test
  public void deleteProviderTypeResourceSuccessTest() throws IOException {
    Map<String, Set<ResourceMeta>> result
      = pluginClient.getProviderTypeResources(PROVIDER_TYPE_JOYENT_NAME,
                                               ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                               ResourceStatus.INACTIVE);
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.VIEW_KEY_RESOURCE_META), result.get("view"));

    pluginClient.deleteProviderTypeResourceVersion(PROVIDER_TYPE_JOYENT_NAME,
                                                    ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                                    ClientTestEntities.VIEW_KEY_RESOURCE_META.getName(),
                                                    String.valueOf(ClientTestEntities.VIEW_KEY_RESOURCE_META
                                                                     .getVersion()));

    result = pluginClient.getProviderTypeResources(PROVIDER_TYPE_JOYENT_NAME,
                                                    ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                                    ResourceStatus.INACTIVE);
    Assert.assertEquals(0, result.size());
  }
}
