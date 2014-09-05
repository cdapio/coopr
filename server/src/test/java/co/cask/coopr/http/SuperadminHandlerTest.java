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
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.store.provisioner.SQLProvisionerStore;
import co.cask.coopr.store.tenant.SQLTenantStore;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class SuperadminHandlerTest extends ServiceTestBase {
  private static TenantProvisionerService tenantProvisionerService;

  @BeforeClass
  public static void setupTestClass() {
    tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
  }

  @Before
  public void setupSuperadminHandlerTest() throws SQLException, IOException {
    // base tests will write some tenants that we don't want.
    ((SQLTenantStore) tenantStore).clearData();
    ((SQLProvisionerStore) provisionerStore).clearData();
    tenantProvisionerService.writeProvisioner(
      new Provisioner("p1", "host", 12345, 100, ImmutableMap.<String, Integer>of(), ImmutableMap.<String, Integer>of())
    );
  }

  @Test
  public void testDeleteSuperadminForbidden() throws Exception {
    assertResponseStatus(doDelete("/tenants/" + Constants.SUPERADMIN_TENANT, SUPERADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testNonSuperadminReturnsError() throws Exception {
    assertResponseStatus(doGet("/tenants", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGet("/tenants/123", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPost("/tenants", "", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPut("/tenants/123", "", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testCreateTenant() throws Exception {
    String name = "companyX";
    TenantSpecification requestedTenant = new TenantSpecification(name, 10, 100, 1000);
    HttpResponse response = doPost("/tenants", gson.toJson(requestedTenant), SUPERADMIN_HEADERS);

    // perform create request
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());

    // make sure tenant was actually written
    TenantSpecification actualTenant = tenantStore.getTenantByName(name).getSpecification();
    Assert.assertEquals(requestedTenant.getName(), actualTenant.getName());
    Assert.assertEquals(requestedTenant.getWorkers(), actualTenant.getWorkers());
    Assert.assertEquals(requestedTenant.getMaxClusters(), actualTenant.getMaxClusters());
    Assert.assertEquals(requestedTenant.getMaxNodes(), actualTenant.getMaxNodes());
  }

  @Test
  public void testDuplicateTenantNameNotAllowed() throws Exception {
    String name = "companyX";
    TenantSpecification requestedTenant = new TenantSpecification(name, 10, 100, 1000);
    assertResponseStatus(doPost("/tenants", gson.toJson(requestedTenant), SUPERADMIN_HEADERS),
                         HttpResponseStatus.OK);
    assertResponseStatus(doPost("/tenants", gson.toJson(requestedTenant), SUPERADMIN_HEADERS),
                         HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testCreateTenantWithTooManyWorkersReturnsConflict() throws Exception {
    TenantSpecification requestedTenant = new TenantSpecification("companyX", 10000, 100, 1000);
    HttpResponse response = doPost("/tenants", gson.toJson(requestedTenant), SUPERADMIN_HEADERS);

    // perform create request
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testWriteTenant() throws Exception {
    // write tenant to store
    String id = UUID.randomUUID().toString();
    Tenant actualTenant = new Tenant(id, new TenantSpecification("companyX", 10, 100, 1000));
    tenantStore.writeTenant(actualTenant);

    // perform request to write tenant
    TenantSpecification updatedTenant = new TenantSpecification("companyX", 10, 100, 500);
    HttpResponse response =
      doPut("/tenants/" + updatedTenant.getName(), gson.toJson(updatedTenant), SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    Assert.assertEquals(updatedTenant, tenantStore.getTenantByID(actualTenant.getId()).getSpecification());
  }

  @Test
  public void testWriteTenantWithTooManyWorkersReturnsConflict() throws Exception {
    // write tenant to store
    String id = UUID.randomUUID().toString();
    String name = "companyX";
    Tenant actualTenant = new Tenant(id, new TenantSpecification(name, 10, 100, 1000));
    tenantStore.writeTenant(actualTenant);

    // perform request to write tenant
    TenantSpecification updatedFields = new TenantSpecification(name, 100000, 100, 500);
    HttpResponse response = doPut("/tenants/" + name, gson.toJson(updatedFields), SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testDeleteTenant() throws Exception {
    String id = UUID.randomUUID().toString();
    tenantProvisionerService.writeTenantSpecification(new TenantSpecification("companyX", 0, 100, 1000));

    // perform request to delete tenant
    HttpResponse response = doDelete("/tenants/" + id, SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    Assert.assertNull(tenantStore.getTenantByID(id));
  }

  @Test
  public void testDeleteTenantWithNonzeroWorkersFails() throws Exception {
    String id = UUID.randomUUID().toString();
    String name = "companyX";
    Tenant tenant = new Tenant(id, new TenantSpecification(name, 10, 100, 1000));
    tenantStore.writeTenant(tenant);
    tenantProvisionerService.rebalanceTenantWorkers(id);

    // perform request to delete tenant
    assertResponseStatus(doDelete("/tenants/" + name, SUPERADMIN_HEADERS), HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testGetTenant() throws Exception {
    // write tenant to store
    TenantSpecification spec = new TenantSpecification("companyX", 10, 100, 1000);
    tenantProvisionerService.writeTenantSpecification(spec);

    // perform request to get tenant
    HttpResponse response = doGet("/tenants/" + spec.getName(), SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    Assert.assertEquals(spec, gson.fromJson(reader, TenantSpecification.class));
  }

  @Test
  public void testGetAllTenants() throws Exception {
    // write tenants to store
    TenantSpecification spec1 = new TenantSpecification("companyX", 2, 100, 1000);
    TenantSpecification spec2 = new TenantSpecification("companyY", 3, 1000, 10000);
    tenantProvisionerService.writeTenantSpecification(spec1);
    tenantProvisionerService.writeTenantSpecification(spec2);

    // perform request to get tenant
    HttpResponse response = doGet("/tenants", SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    Set<Tenant> actualTenants = gson.fromJson(reader, new TypeToken<Set<TenantSpecification>>() {}.getType());
    Assert.assertEquals(ImmutableSet.of(Tenant.DEFAULT_SUPERADMIN.getSpecification(), spec1, spec2), actualTenants);
  }

  @Test
  public void testBadRequestReturns400() throws Exception {
    // test malformed requests
    assertResponseStatus(doPost("/tenants", "{}", SUPERADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);
    assertResponseStatus(doPost("/tenants", "", SUPERADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // id in object does not match id in path
    TenantSpecification tenantSpecification = new TenantSpecification("name", 10, 10, 10);
    assertResponseStatus(doPut("/tenants/10", gson.toJson(tenantSpecification), SUPERADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // missing id in object
    tenantSpecification = new TenantSpecification("name", 10, 10, 10);
    assertResponseStatus(doPut("/tenants/10", gson.toJson(tenantSpecification), SUPERADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testMissingObjectReturn404() throws Exception {
    assertResponseStatus(doGet("/tenants/123", SUPERADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAddTenantWithDefaults() {
    // TODO: implement once bootstrapping with defaults is implemented
  }

  @Test
  public void testProviderTypes() throws Exception {
    testNonPostRestAPIs("providertypes", gson.toJsonTree(Entities.ProviderTypeExample.JOYENT).getAsJsonObject(),
                        gson.toJsonTree(Entities.ProviderTypeExample.RACKSPACE).getAsJsonObject(), SUPERADMIN_HEADERS);
  }

  @Test
  public void testAutomatorTypes() throws Exception {
    testNonPostRestAPIs("automatortypes", gson.toJsonTree(Entities.AutomatorTypeExample.CHEF).getAsJsonObject(),
                        gson.toJsonTree(Entities.AutomatorTypeExample.SHELL).getAsJsonObject(), SUPERADMIN_HEADERS);
  }

  @Test
  public void testEditProviderTypesMustBeSuperadmin() throws Exception {
    tenantStore.writeTenant(
      new Tenant(ADMIN_ACCOUNT.getTenantId(), new TenantSpecification(TENANT, 500, 1000, 10000)));
    ProviderType type = Entities.ProviderTypeExample.RACKSPACE;
    assertResponseStatus(doPut("/plugins/providertypes/" + type.getName(), gson.toJson(type), ADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete("/plugins/providertypes/" + type.getName(), ADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testEditAutomatorTypesMustBeSuperadmin() throws Exception {
    tenantStore.writeTenant(
      new Tenant(ADMIN_ACCOUNT.getTenantId(), new TenantSpecification(TENANT, 500, 1000, 10000)));
    AutomatorType type = Entities.AutomatorTypeExample.CHEF;
    assertResponseStatus(doPut("/plugins/automatortypes/" + type.getName(), gson.toJson(type), ADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete("/plugins/automatortypes/" + type.getName(), ADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
  }

  private void testNonPostRestAPIs(String entityType, JsonObject entity1, JsonObject entity2,
                                   Header[] headers) throws Exception {
    String base = "/plugins/" + entityType;
    String entity1Path = base + "/" + entity1.get("name").getAsString();
    String entity2Path = base + "/" + entity2.get("name").getAsString();
    // should start off with no entities
    assertResponseStatus(doGet(entity1Path, headers), HttpResponseStatus.NOT_FOUND);

    // add entity through PUT
    assertResponseStatus(doPut(entity1Path, entity1.toString(), headers), HttpResponseStatus.OK);
    // check we can get it
    HttpResponse response = doGet(entity1Path, headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);

    // add second entity through PUT
    assertResponseStatus(doPut(entity2Path, entity2.toString(), headers), HttpResponseStatus.OK);
    // check we can get it
    response = doGet(entity2Path, headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity2, result);

    // get both entities
    response = doGet(base, headers);
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

    assertResponseStatus(doDelete(entity1Path, headers), HttpResponseStatus.OK);
    assertResponseStatus(doDelete(entity2Path, headers), HttpResponseStatus.OK);
    // check both were deleted
    assertResponseStatus(doGet(entity1Path, headers), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGet(entity2Path, headers), HttpResponseStatus.NOT_FOUND);
  }
}
