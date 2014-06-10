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

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.codec.json.JsonSerde;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class LoomTenantHandlerTest extends LoomServiceTestBase {
  private static final Gson GSON = new JsonSerde().getGson();

  @Test
  public void testNonSuperadminReturnsError() throws Exception {
    assertResponseStatus(doGet("/v1/tenants", ADMIN_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet("/v1/tenants/123", ADMIN_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doPost("/v1/tenants", "", ADMIN_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doPut("/v1/tenants/123", "", ADMIN_HEADERS), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testCreateTenant() throws Exception {
    Tenant requestedTenant = new Tenant("companyX", null, 10, 100, 1000);
    HttpResponse response = doPost("/v1/tenants", GSON.toJson(requestedTenant), SUPERADMIN_HEADERS);

    // perform create request
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    JsonObject responseObj = GSON.fromJson(reader, JsonObject.class);
    String id = responseObj.get("id").getAsString();

    // make sure tenant was actually written
    Tenant tenant = tenantStore.getTenant(id);
    Assert.assertEquals(requestedTenant.getName(), tenant.getName());
    Assert.assertEquals(requestedTenant.getWorkers(), tenant.getWorkers());
    Assert.assertEquals(requestedTenant.getMaxClusters(), tenant.getMaxClusters());
    Assert.assertEquals(requestedTenant.getMaxNodes(), tenant.getMaxNodes());
  }

  @Test
  public void testWriteTenant() throws Exception {
    // write tenant to store
    String id = UUID.randomUUID().toString();
    Tenant actualTenant = new Tenant("companyX", id, 10, 100, 1000);
    tenantStore.writeTenant(actualTenant);

    // perform request to delete tenant
    Tenant updatedTenant = new Tenant("companyX", id, 10, 100, 500);
    HttpResponse response = doPut("/v1/tenants/" + id, GSON.toJson(updatedTenant), SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    Assert.assertEquals(updatedTenant, tenantStore.getTenant(updatedTenant.getId()));
  }

  @Test
  public void testDeleteTenant() throws Exception {
    // write tenant to store
    String id = UUID.randomUUID().toString();
    Tenant actualTenant = new Tenant("companyX", id, 10, 100, 1000);
    tenantStore.writeTenant(actualTenant);

    // perform request to delete tenant
    HttpResponse response = doDelete("/v1/tenants/" + id, SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    Assert.assertNull(tenantStore.getTenant(actualTenant.getId()));
  }

  @Test
  public void testGetTenant() throws Exception {
    // write tenant to store
    String id = UUID.randomUUID().toString();
    Tenant actualTenant = new Tenant("companyX", id, 10, 100, 1000);
    tenantStore.writeTenant(actualTenant);

    // perform request to get tenant
    HttpResponse response = doGet("/v1/tenants/" + id, SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    Assert.assertEquals(actualTenant, GSON.fromJson(reader, Tenant.class));
  }

  @Test
  public void testGetAllTenants() throws Exception {
    // write tenants to store
    String id1 = UUID.randomUUID().toString();
    String id2 = UUID.randomUUID().toString();
    Tenant expectedTenant1 = new Tenant("companyX", id1, 10, 100, 1000);
    Tenant expectedTenant2 = new Tenant("companyY", id2, 500, 1000, 10000);
    tenantStore.writeTenant(expectedTenant1);
    tenantStore.writeTenant(expectedTenant2);

    // perform request to get tenant
    HttpResponse response = doGet("/v1/tenants", SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    Set<Tenant> actualTenants = GSON.fromJson(reader, new TypeToken<Set<Tenant>>() {}.getType());
    Assert.assertEquals(ImmutableSet.of(expectedTenant1, expectedTenant2), actualTenants);
  }

  @Test
  public void testBadRequestReturns400() throws Exception {
    // test malformed requests
    assertResponseStatus(doPost("/v1/tenants", "{}", SUPERADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);
    assertResponseStatus(doPost("/v1/tenants", "", SUPERADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // id in object does not match id in path
    Tenant tenant = new Tenant("name", "id123", 10, 10, 10);
    assertResponseStatus(doPut("/v1/tenants/10", GSON.toJson(tenant), SUPERADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // missing id in object
    tenant = new Tenant("name", null, 10, 10, 10);
    assertResponseStatus(doPut("/v1/tenants/10", GSON.toJson(tenant), SUPERADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testMissingObjectReturn404() throws Exception {
    assertResponseStatus(doGet("/v1/tenants/123", SUPERADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAddTenantWithDefaults() {
    // TODO: implement once bootstrapping with defaults is implemented
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
}
