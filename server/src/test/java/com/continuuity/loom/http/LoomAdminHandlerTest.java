/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.http;

import com.continuuity.loom.Entities;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.codec.json.JsonSerde;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LoomAdminHandlerTest extends LoomServiceTestBase {
  private static final Gson GSON = new JsonSerde().getGson();

  @Test
  public void testProviders() throws Exception {
    testRestAPIs("providers", Entities.ProviderExample.JOYENT_JSON, Entities.ProviderExample.RACKSPACE_JSON);
  }

  @Test
  public void testHardwareTypes() throws Exception {
    testRestAPIs("hardwaretypes", Entities.HardwareTypeExample.SMALL_JSON, Entities.HardwareTypeExample.MEDIUM_JSON);
  }

  @Test
  public void testImageTypes() throws Exception {
    testRestAPIs("imagetypes", Entities.ImageTypeExample.CENTOS_6_JSON, Entities.ImageTypeExample.UBUNTU_12_JSON);
  }

  @Test
  public void testServices() throws Exception {
    testRestAPIs("services", Entities.ServiceExample.HOSTS_JSON, Entities.ServiceExample.NAMENODE_JSON);
  }

  @Test
  public void testClusterTemplates() throws Exception {
    testRestAPIs("clustertemplates", Entities.ClusterTemplateExample.HDFS_JSON,
                 Entities.ClusterTemplateExample.REACTOR_JSON);
  }

  @Test
  public void testProviderTypes() throws Exception {
    testNonPostRestAPIs("providertypes", Entities.ProviderTypeExample.JOYENT_JSON,
                        Entities.ProviderTypeExample.RACKSPACE_JSON);
  }

  @Test
  public void testAutomatorTypes() throws Exception {
    testNonPostRestAPIs("automatortypes", Entities.AutomatorTypeExample.CHEF_JSON,
                        Entities.AutomatorTypeExample.SHELL_JSON);
  }

  @Test
  public void testNonAdminUserGetsForbiddenStatus() throws Exception {
    String base = "/v1/loom/";
    String[] resources = { "providers", "hardwaretypes", "imagetypes", "services", "clustertemplates" };
    for (String resource : resources) {
      assertResponseStatus(doGet(base + resource, USER1_HEADERS), HttpResponseStatus.OK);
      assertResponseStatus(doGet(base + resource + "/id", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
      assertResponseStatus(doPut(base + resource + "/id", "body", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
      assertResponseStatus(doPost(base + resource, "body", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    }
    assertResponseStatus(doGet(base + "export", USER1_HEADERS), HttpResponseStatus.OK);
    assertResponseStatus(doPost(base + "import", "{}", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testExportImport() throws Exception {
    // Import some config
    Map<String, JsonElement> import1 = Maps.newHashMap();
    import1.put(LoomAdminHandler.PROVIDERS,
                GSON.toJsonTree(Lists.newArrayList(Entities.ProviderExample.JOYENT),
                                new TypeToken<List<Provider>>() {}.getType()));
    import1.put(LoomAdminHandler.HARDWARE_TYPES,
                GSON.toJsonTree(Lists.newArrayList(Entities.HardwareTypeExample.LARGE,
                                                   Entities.HardwareTypeExample.MEDIUM),
                                new TypeToken<List<HardwareType>>() {}.getType()));
    import1.put(LoomAdminHandler.IMAGE_TYPES,
                GSON.toJsonTree(Lists.newArrayList(Entities.ImageTypeExample.CENTOS_6,
                                                   Entities.ImageTypeExample.UBUNTU_12),
                                new TypeToken<List<ImageType>>() {}.getType()));

    import1.put(LoomAdminHandler.SERVICES,
                GSON.toJsonTree(Lists.newArrayList(Entities.ServiceExample.DATANODE,
                                                   Entities.ServiceExample.NAMENODE),
                                new TypeToken<List<Service>>() {}.getType()));

    import1.put(LoomAdminHandler.CLUSTER_TEMPLATES,
                GSON.toJsonTree(Lists.newArrayList(Entities.ClusterTemplateExample.HDFS,
                                                   Entities.ClusterTemplateExample.REACTOR),
                                new TypeToken<List<ClusterTemplate>>() {}.getType()));

    // Verify import worked by exporting
    runImportExportTest(import1);

    // Import some other config
    Map<String, JsonElement> import2 = Maps.newHashMap();
    import2.put(LoomAdminHandler.PROVIDERS,
                GSON.toJsonTree(Lists.newArrayList(Entities.ProviderExample.RACKSPACE,
                                                   Entities.ProviderExample.JOYENT),
                                new TypeToken<List<Provider>>() {}.getType()));
    import2.put(LoomAdminHandler.HARDWARE_TYPES,
                GSON.toJsonTree(Lists.newArrayList(Entities.HardwareTypeExample.MEDIUM,
                                                   Entities.HardwareTypeExample.SMALL),
                                new TypeToken<List<HardwareType>>() {}.getType()));
    import2.put(LoomAdminHandler.IMAGE_TYPES,
                GSON.toJsonTree(Lists.newArrayList(Entities.ImageTypeExample.CENTOS_6),
                                new TypeToken<List<ImageType>>() {}.getType()));

    import2.put(LoomAdminHandler.SERVICES,
                GSON.toJsonTree(Lists.newArrayList(Entities.ServiceExample.DATANODE,
                                                   Entities.ServiceExample.NAMENODE,
                                                   Entities.ServiceExample.HOSTS),
                                new TypeToken<List<Service>>() {}.getType()));

    import2.put(LoomAdminHandler.CLUSTER_TEMPLATES,
                GSON.toJsonTree(Lists.newArrayList(Entities.ClusterTemplateExample.HDFS,
                                                   Entities.ClusterTemplateExample.REACTOR),
                                new TypeToken<List<ClusterTemplate>>() {}.getType()));

    // Verify import worked by exporting
    runImportExportTest(import2);

    // Import empty config
    Map<String, JsonElement> import3 = Maps.newHashMap();
    import3.put(LoomAdminHandler.PROVIDERS, new JsonArray());
    import3.put(LoomAdminHandler.HARDWARE_TYPES, new JsonArray());
    import3.put(LoomAdminHandler.IMAGE_TYPES, new JsonArray());
    import3.put(LoomAdminHandler.SERVICES, new JsonArray());
    import3.put(LoomAdminHandler.CLUSTER_TEMPLATES, new JsonArray());

    // Verify import worked by exporting
    runImportExportTest(import3);
  }

  private void runImportExportTest(Map<String, JsonElement> importJson) throws Exception {
    assertResponseStatus(doPost("/v1/loom/import", GSON.toJson(importJson), ADMIN_HEADERS), HttpResponseStatus.OK);

    // verify using export
    HttpResponse response = doGet("/v1/loom/export", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Map<String, JsonElement> exportJson = new Gson().fromJson(reader,
                                                              new TypeToken<Map<String, JsonElement>>() {}.getType());
    Assert.assertEquals(importJson.size(), exportJson.size());
    assertImport(importJson, exportJson, LoomAdminHandler.PROVIDERS);
    assertImport(importJson, exportJson, LoomAdminHandler.HARDWARE_TYPES);
    assertImport(importJson, exportJson, LoomAdminHandler.IMAGE_TYPES);
    assertImport(importJson, exportJson, LoomAdminHandler.SERVICES);
    assertImport(importJson, exportJson, LoomAdminHandler.CLUSTER_TEMPLATES);
  }

  private void assertImport(Map<String, JsonElement> import1, Map<String, JsonElement> export1, String key) {
    if (!import1.containsKey(key)) {
      return;
    }

    Assert.assertEquals(GSON.fromJson(import1.get(key),
                                      new TypeToken<Set<?>>() {}.getType()),
                        GSON.fromJson(export1.get(key),
                                      new TypeToken<Set<?>>() {}.getType()));
  }

  @Test
  public void testInvalidProviderReturns400() throws Exception {
    // test an empty object
    JsonObject provider = new JsonObject();
    assertResponseStatus(doPost("/v1/loom/providers", provider.toString(), ADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // test invalid json
    assertResponseStatus(doPost("/v1/loom/providers", "[dsfmqo", ADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // test an invalid name
    provider.addProperty("name", "?");
    assertResponseStatus(doPost("/v1/loom/providers", provider.toString(), ADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  private void testNonPostRestAPIs(String entityType, JsonObject entity1, JsonObject entity2) throws Exception {
    String base = "/v1/loom/" + entityType;
    String entity1Path = base + "/" + entity1.get("name").getAsString();
    String entity2Path = base + "/" + entity2.get("name").getAsString();
    // should start off with no entities
    assertResponseStatus(doGet(entity1Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);

    // add entity through PUT
    assertResponseStatus(doPut(entity1Path, entity1.toString(), ADMIN_HEADERS), HttpResponseStatus.OK);
    // check we can get it
    HttpResponse response = doGet(entity1Path, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);

    // add second entity through PUT
    assertResponseStatus(doPut(entity2Path, entity2.toString(), ADMIN_HEADERS), HttpResponseStatus.OK);
    // check we can get it
    response = doGet(entity2Path, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity2, result);

    // get both entities
    response = doGet(base, ADMIN_HEADERS);
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

    assertResponseStatus(doDelete(entity1Path, ADMIN_HEADERS), HttpResponseStatus.OK);
    assertResponseStatus(doDelete(entity2Path, ADMIN_HEADERS), HttpResponseStatus.OK);
    // check both were deleted
    assertResponseStatus(doGet(entity1Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGet(entity2Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  private void testRestAPIs(String entityType, JsonObject entity1, JsonObject entity2) throws Exception {
    String base = "/v1/loom/" + entityType;
    String entity1Path = base + "/" + entity1.get("name").getAsString();
    String entity2Path = base + "/" + entity2.get("name").getAsString();
    // should start off with no entities
    assertResponseStatus(doGet(entity1Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);

    // add a an entity through post
    Assert.assertEquals(HttpResponseStatus.OK.getCode(),
                        doPost(base, entity1.toString(), ADMIN_HEADERS).getStatusLine().getStatusCode());

    // make sure entity was added correctly
    HttpResponse response = doGet(entity1Path, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);
    // make sure you can't overwrite the entity through post
    assertResponseStatus(doPost(base, entity1.toString(), ADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // delete entity
    assertResponseStatus(doDelete(entity1Path, ADMIN_HEADERS), HttpResponseStatus.OK);

    // make sure entity was deleted
    assertResponseStatus(doGet(entity1Path, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);

    // add entity through PUT
    assertResponseStatus(doPut(entity1Path, entity1.toString(), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGet(entity1Path, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);

    // add second entity through POST
    assertResponseStatus(doPost(base, entity2.toString(), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGet(base, ADMIN_HEADERS);
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

    assertResponseStatus(doDelete(entity1Path, ADMIN_HEADERS), HttpResponseStatus.OK);
    assertResponseStatus(doDelete(entity2Path, ADMIN_HEADERS), HttpResponseStatus.OK);
  }
}
