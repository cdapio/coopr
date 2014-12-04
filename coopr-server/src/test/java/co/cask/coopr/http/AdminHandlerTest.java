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
package co.cask.coopr.http;

import co.cask.coopr.Entities;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueueMetrics;
import co.cask.coopr.http.handler.AdminHandler;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class AdminHandlerTest extends ServiceTestBase {

  @Test
  public void testProviders() throws Exception {
    testRestAPIs("providers", gson.toJsonTree(Entities.ProviderExample.JOYENT).getAsJsonObject(),
                 gson.toJsonTree(Entities.ProviderExample.RACKSPACE).getAsJsonObject());
  }

  @Test
  public void testHardwareTypes() throws Exception {
    testRestAPIs("hardwaretypes", gson.toJsonTree(Entities.HardwareTypeExample.SMALL).getAsJsonObject(),
                 gson.toJsonTree(Entities.HardwareTypeExample.MEDIUM).getAsJsonObject());
  }

  @Test
  public void testImageTypes() throws Exception {
    testRestAPIs("imagetypes", gson.toJsonTree(Entities.ImageTypeExample.CENTOS_6).getAsJsonObject(),
                 gson.toJsonTree(Entities.ImageTypeExample.UBUNTU_12).getAsJsonObject());
  }

  @Test
  public void testServices() throws Exception {
    testRestAPIs("services", gson.toJsonTree(Entities.ServiceExample.HOSTS).getAsJsonObject(),
                 gson.toJsonTree(Entities.ServiceExample.NAMENODE).getAsJsonObject());
  }

  @Test
  public void testClusterTemplates() throws Exception {
    testRestAPIs("clustertemplates", gson.toJsonTree(Entities.ClusterTemplateExample.HDFS).getAsJsonObject(),
                 gson.toJsonTree(Entities.ClusterTemplateExample.REACTOR).getAsJsonObject());
  }

  @Test
  public void testNonAdminUserGetsForbiddenStatus() throws Exception {
    String[] resources = { "/providers", "/hardwaretypes", "/imagetypes", "/services", "/clustertemplates" };
    for (String resource : resources) {
      assertResponseStatus(doGetExternalAPI(resource, USER1_HEADERS), HttpResponseStatus.OK);
      assertResponseStatus(doGetExternalAPI(resource + "/id", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
      assertResponseStatus(doPutExternalAPI(resource + "/id", "body", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
      assertResponseStatus(doPostExternalAPI(resource, "body", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    }
    assertResponseStatus(doGetExternalAPI("/export", USER1_HEADERS), HttpResponseStatus.OK);
    assertResponseStatus(doPostExternalAPI("/import", "{}", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testForbiddenIfNonadminGetsQueueMetrics() throws Exception {
    tenantStore.writeTenant(
      new Tenant(UUID.randomUUID().toString(), new TenantSpecification(USER1_ACCOUNT.getTenantId(), 10, 10, 100)));
    assertResponseStatus(doGetExternalAPI("/metrics/queues", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testGetQueueMetrics() throws Exception {
    try {
      tenantStore.writeTenant(new Tenant("idA", new TenantSpecification("tenantA", 10, 10, 100)));
      tenantStore.writeTenant(new Tenant("idB", new TenantSpecification("tenantB", 10, 10, 100)));
      tenantStore.writeTenant(new Tenant("idC", new TenantSpecification("tenantC", 10, 10, 100)));
      tenantStore.writeTenant(new Tenant("idD", new TenantSpecification("tenantD", 10, 10, 100)));
      provisionerQueues.add("idA", new Element("task1"));
      provisionerQueues.add("idA", new Element("task2"));
      provisionerQueues.add("idA", new Element("task3"));
      provisionerQueues.take("idA", "consumer");
      provisionerQueues.add("idB", new Element("task4"));
      provisionerQueues.add("idC", new Element("task5"));
      provisionerQueues.take("idC", "consumer");
      provisionerQueues.add("idD", new Element("task6"));
      provisionerQueues.add("idD", new Element("task7"));
      provisionerQueues.take("idD", "consumer");

      Map<String, QueueMetrics> expected = Maps.newHashMap();
      expected.put("tenantA", new QueueMetrics(2, 1));
      expected.put("tenantB", new QueueMetrics(1, 0));
      expected.put("tenantC", new QueueMetrics(0, 1));
      expected.put("tenantD", new QueueMetrics(1, 1));
      expected.put(TENANT, new QueueMetrics(0, 0));
      expected.put(Constants.SUPERADMIN_TENANT, new QueueMetrics(0, 0));

      assertQueueMetrics(Constants.SUPERADMIN_TENANT, expected);
      assertQueueMetrics("tenantA", ImmutableMap.of("tenantA", new QueueMetrics(2, 1)));
      assertQueueMetrics("tenantB", ImmutableMap.of("tenantB", new QueueMetrics(1, 0)));
      assertQueueMetrics("tenantC", ImmutableMap.of("tenantC", new QueueMetrics(0, 1)));
      assertQueueMetrics("tenantD", ImmutableMap.of("tenantD", new QueueMetrics(1, 1)));
    } finally {
      provisionerQueues.removeAll();
      tenantStore.deleteTenantByName("tenantA");
      tenantStore.deleteTenantByName("tenantB");
      tenantStore.deleteTenantByName("tenantC");
      tenantStore.deleteTenantByName("tenantD");
    }
  }

  @Test
  public void testExportImport() throws Exception {
    // Import some config
    Map<String, JsonElement> import1 = Maps.newHashMap();
    import1.put(AdminHandler.PROVIDERS,
                gson.toJsonTree(Lists.newArrayList(Entities.ProviderExample.JOYENT),
                                new TypeToken<List<Provider>>() {
                                }.getType()));
    import1.put(AdminHandler.HARDWARE_TYPES,
                gson.toJsonTree(Lists.newArrayList(Entities.HardwareTypeExample.LARGE,
                                                   Entities.HardwareTypeExample.MEDIUM),
                                new TypeToken<List<HardwareType>>() {
                                }.getType()));
    import1.put(AdminHandler.IMAGE_TYPES,
                gson.toJsonTree(Lists.newArrayList(Entities.ImageTypeExample.CENTOS_6,
                                                   Entities.ImageTypeExample.UBUNTU_12),
                                new TypeToken<List<ImageType>>() {}.getType()));

    import1.put(AdminHandler.SERVICES,
                gson.toJsonTree(Lists.newArrayList(Entities.ServiceExample.DATANODE,
                                                   Entities.ServiceExample.NAMENODE),
                                new TypeToken<List<Service>>() {
                                }.getType()));

    import1.put(AdminHandler.CLUSTER_TEMPLATES,
                gson.toJsonTree(Lists.newArrayList(Entities.ClusterTemplateExample.HDFS,
                                                   Entities.ClusterTemplateExample.REACTOR),
                                new TypeToken<List<ClusterTemplate>>() {}.getType()));


    // Verify import worked by exporting
    runImportExportTest(import1);

    // Import some other config
    Map<String, JsonElement> import2 = Maps.newHashMap();
    import2.put(AdminHandler.PROVIDERS,
                gson.toJsonTree(Lists.newArrayList(Entities.ProviderExample.RACKSPACE,
                                                   Entities.ProviderExample.JOYENT),
                                new TypeToken<List<Provider>>() {}.getType()));
    import2.put(AdminHandler.HARDWARE_TYPES,
                gson.toJsonTree(Lists.newArrayList(Entities.HardwareTypeExample.MEDIUM,
                                                   Entities.HardwareTypeExample.SMALL),
                                new TypeToken<List<HardwareType>>() {
                                }.getType()));
    import2.put(AdminHandler.IMAGE_TYPES,
                gson.toJsonTree(Lists.newArrayList(Entities.ImageTypeExample.CENTOS_6),
                                new TypeToken<List<ImageType>>() {}.getType()));

    import2.put(AdminHandler.SERVICES,
                gson.toJsonTree(Lists.newArrayList(Entities.ServiceExample.DATANODE,
                                                   Entities.ServiceExample.NAMENODE,
                                                   Entities.ServiceExample.HOSTS),
                                new TypeToken<List<Service>>() {}.getType()));

    import2.put(AdminHandler.CLUSTER_TEMPLATES,
                gson.toJsonTree(Lists.newArrayList(Entities.ClusterTemplateExample.HDFS,
                                                   Entities.ClusterTemplateExample.REACTOR),
                                new TypeToken<List<ClusterTemplate>>() {
                                }.getType()));

    // Verify import worked by exporting
    runImportExportTest(import2);

    // Import empty config
    Map<String, JsonElement> import3 = Maps.newHashMap();
    import3.put(AdminHandler.PROVIDERS, new JsonArray());
    import3.put(AdminHandler.HARDWARE_TYPES, new JsonArray());
    import3.put(AdminHandler.IMAGE_TYPES, new JsonArray());
    import3.put(AdminHandler.SERVICES, new JsonArray());
    import3.put(AdminHandler.CLUSTER_TEMPLATES, new JsonArray());
    // Verify import worked by exporting
    runImportExportTest(import3);
  }

  private void runImportExportTest(Map<String, JsonElement> importJson) throws Exception {
    assertResponseStatus(doPostExternalAPI("/import", gson.toJson(importJson), ADMIN_HEADERS), HttpResponseStatus.OK);

    // verify using export
    HttpResponse response = doGetExternalAPI("/export", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Map<String, JsonElement> exportJson = new Gson().fromJson(reader,
                                                              new TypeToken<Map<String, JsonElement>>() {}.getType());
    Assert.assertEquals(importJson.size(), exportJson.size());
    assertImport(importJson, exportJson, AdminHandler.PROVIDERS);
    assertImport(importJson, exportJson, AdminHandler.HARDWARE_TYPES);
    assertImport(importJson, exportJson, AdminHandler.IMAGE_TYPES);
    assertImport(importJson, exportJson, AdminHandler.SERVICES);
    assertImport(importJson, exportJson, AdminHandler.CLUSTER_TEMPLATES);
  }

  private void assertImport(Map<String, JsonElement> import1, Map<String, JsonElement> export1, String key) {
    if (!import1.containsKey(key)) {
      return;
    }

    Assert.assertEquals(gson.fromJson(import1.get(key),
                                      new TypeToken<Set<?>>() {
                                      }.getType()),
                        gson.fromJson(export1.get(key),
                                      new TypeToken<Set<?>>() {
                                      }.getType()));
  }

  @Test
  public void testInvalidProviderReturns400() throws Exception {
    // test an empty object
    JsonObject provider = new JsonObject();
    assertResponseStatus(doPostExternalAPI("/providers", provider.toString(), ADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // test invalid json
    assertResponseStatus(doPostExternalAPI("/providers", "[dsfmqo", ADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // test an invalid name
    provider.addProperty("name", "?");
    assertResponseStatus(doPostExternalAPI("/providers", provider.toString(), ADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  private void testRestAPIs(String entityType, JsonObject entity1, JsonObject entity2) throws Exception {
    String base = "/" + entityType;
    String entity1Path = base + "/" + entity1.get("name").getAsString();
    String entity2Path = base + "/" + entity2.get("name").getAsString();
    // should start off with no entities
    assertResponseStatus(doGetExternalAPI(entity1Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);

    // add a an entity through post
    Assert.assertEquals(HttpResponseStatus.OK.getCode(),
                        doPostExternalAPI(base, entity1.toString(), ADMIN_HEADERS).getStatusLine().getStatusCode());

    // make sure entity was added correctly
    HttpResponse response = doGetExternalAPI(entity1Path, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals("application/json", response.getEntity().getContentType().getValue());
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);
    // make sure you can't overwrite the entity through post
    assertResponseStatus(doPostExternalAPI(base, entity1.toString(), ADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // delete entity
    assertResponseStatus(doDeleteExternalAPI(entity1Path, ADMIN_HEADERS), HttpResponseStatus.OK);

    // make sure entity was deleted
    assertResponseStatus(doGetExternalAPI(entity1Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);

    // add entity through PUT
    assertResponseStatus(doPutExternalAPI(entity1Path, entity1.toString(), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGetExternalAPI(entity1Path, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);

    // add second entity through POST
    assertResponseStatus(doPostExternalAPI(base, entity2.toString(), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGetExternalAPI(base, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonArray results = new Gson().fromJson(reader, JsonArray.class);

    Assert.assertEquals(2, results.size());
    JsonObject first = results.get(0).getAsJsonObject();
    JsonObject second = results.get(1).getAsJsonObject();
    if (first.get("name").getAsString().equals(entity1.get("name").getAsString())) {
      Assert.assertEquals(entity1, first);
      Assert.assertEquals(entity2, second);
    } else {
      Assert.assertEquals(entity2, first);
      Assert.assertEquals(entity1, second);
    }

    assertResponseStatus(doDeleteExternalAPI(entity1Path, ADMIN_HEADERS), HttpResponseStatus.OK);
    assertResponseStatus(doDeleteExternalAPI(entity2Path, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  private void assertQueueMetrics(String tenant, Map<String, QueueMetrics> expected) throws Exception {
    Header[] headers = {
      new BasicHeader(Constants.USER_HEADER, Constants.ADMIN_USER),
      new BasicHeader(Constants.API_KEY_HEADER, API_KEY),
      new BasicHeader(Constants.TENANT_HEADER, tenant)
    };
    HttpResponse response = doGetExternalAPI("/metrics/queues", headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Map<String, QueueMetrics> result = gson.fromJson(reader, new TypeToken<Map<String, QueueMetrics>>() {}.getType());
    Assert.assertEquals(expected, result);
  }
}
