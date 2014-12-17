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
import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.request.TenantWriteRequest;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.provisioner.plugin.PluginType;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.store.entity.EntityStoreView;
import co.cask.coopr.store.entity.SQLEntityStoreService;
import co.cask.coopr.store.provisioner.SQLProvisionerStore;
import co.cask.coopr.store.tenant.SQLTenantStore;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
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
import java.io.OutputStream;
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
    ((SQLEntityStoreService) entityStoreService).clearData();
    tenantProvisionerService.writeProvisioner(
      new Provisioner("p1", "host", 12345, 100, ImmutableMap.<String, Integer>of(), ImmutableMap.<String, Integer>of())
    );
  }

  @Test
  public void testDeleteSuperadminForbidden() throws Exception {
    assertResponseStatus(doDeleteExternalAPI("/tenants/" + Constants.SUPERADMIN_TENANT, SUPERADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testNonSuperadminReturnsError() throws Exception {
    assertResponseStatus(doGetExternalAPI("/tenants", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGetExternalAPI("/tenants/123", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPostExternalAPI("/tenants", "", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPutExternalAPI("/tenants/123", "", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testCreateTenant() throws Exception {
    String name = "companyX";
    TenantSpecification requestedTenant = new TenantSpecification(name, 10, 100, 1000);
    TenantWriteRequest tenantWriteRequest = new TenantWriteRequest(requestedTenant);
    HttpResponse response = doPostExternalAPI("/tenants", gson.toJson(tenantWriteRequest), SUPERADMIN_HEADERS);

    // perform create request
    assertResponseStatus(response, HttpResponseStatus.OK);

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
    TenantWriteRequest addRequest = new TenantWriteRequest(requestedTenant);
    assertResponseStatus(doPostExternalAPI("/tenants", gson.toJson(addRequest), SUPERADMIN_HEADERS),
                         HttpResponseStatus.OK);
    assertResponseStatus(doPostExternalAPI("/tenants", gson.toJson(addRequest), SUPERADMIN_HEADERS),
                         HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testCreateTenantWithTooManyWorkersReturnsConflict() throws Exception {
    TenantSpecification requestedTenant = new TenantSpecification("companyX", 10000, 100, 1000);
    TenantWriteRequest addRequest = new TenantWriteRequest(requestedTenant);
    HttpResponse response = doPostExternalAPI("/tenants", gson.toJson(addRequest), SUPERADMIN_HEADERS);

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
    TenantWriteRequest writeRequest = new TenantWriteRequest(updatedTenant);
    HttpResponse response =
      doPutExternalAPI("/tenants/" + updatedTenant.getName(), gson.toJson(writeRequest), SUPERADMIN_HEADERS);
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
    TenantWriteRequest writeRequest = new TenantWriteRequest(updatedFields);
    HttpResponse response = doPutExternalAPI("/tenants/" + name, gson.toJson(writeRequest), SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testDeleteTenant() throws Exception {
    String id = UUID.randomUUID().toString();
    tenantProvisionerService.writeTenantSpecification(new TenantSpecification("companyX", 0, 100, 1000));

    // perform request to delete tenant
    HttpResponse response = doDeleteExternalAPI("/tenants/" + id, SUPERADMIN_HEADERS);
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
    assertResponseStatus(doDeleteExternalAPI("/tenants/" + name, SUPERADMIN_HEADERS), HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testGetTenant() throws Exception {
    // write tenant to store
    TenantSpecification spec = new TenantSpecification("companyX", 10, 100, 1000);
    tenantProvisionerService.writeTenantSpecification(spec);

    // perform request to get tenant
    HttpResponse response = doGetExternalAPI("/tenants/" + spec.getName(), SUPERADMIN_HEADERS);
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
    HttpResponse response = doGetExternalAPI("/tenants", SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    Set<Tenant> actualTenants = gson.fromJson(reader, new TypeToken<Set<TenantSpecification>>() {}.getType());
    Assert.assertEquals(ImmutableSet.of(Tenant.DEFAULT_SUPERADMIN.getSpecification(), spec1, spec2), actualTenants);
  }

  @Test
  public void testBadRequestReturns400() throws Exception {
    // test malformed requests
    assertResponseStatus(doPostExternalAPI("/tenants", "{}", SUPERADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);
    assertResponseStatus(doPostExternalAPI("/tenants", "", SUPERADMIN_HEADERS), HttpResponseStatus.BAD_REQUEST);

    // id in object does not match id in path
    TenantSpecification tenantSpecification = new TenantSpecification("name", 10, 10, 10);
    assertResponseStatus(doPutExternalAPI("/tenants/10", gson.toJson(tenantSpecification), SUPERADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // missing id in object
    tenantSpecification = new TenantSpecification("name", 10, 10, 10);
    assertResponseStatus(doPutExternalAPI("/tenants/10", gson.toJson(tenantSpecification), SUPERADMIN_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testMissingObjectReturn404() throws Exception {
    assertResponseStatus(doGetExternalAPI("/tenants/123", SUPERADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testBootstrapTenant() throws Exception {
    // write superadmin entities
    EntityStoreView superadminView = entityStoreService.getView(Account.SUPERADMIN);
    Set<Provider> providers = ImmutableSet.of(Entities.ProviderExample.JOYENT, Entities.ProviderExample.RACKSPACE);
    Set<Service> services = ImmutableSet.of(Entities.ServiceExample.NAMENODE, Entities.ServiceExample.DATANODE);
    Set<HardwareType> hardwareTypes =
      ImmutableSet.of(Entities.HardwareTypeExample.SMALL, Entities.HardwareTypeExample.MEDIUM);
    Set<ImageType> imageTypes =
      ImmutableSet.of(Entities.ImageTypeExample.UBUNTU_12, Entities.ImageTypeExample.CENTOS_6);
    Set<ClusterTemplate> clusterTemplates =
      ImmutableSet.of(Entities.ClusterTemplateExample.HDFS, Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED);
    for (Provider provider : providers) {
      superadminView.writeProvider(provider);
    }
    for (Service service : services) {
      superadminView.writeService(service);
    }
    for (HardwareType hardwareType : hardwareTypes) {
      superadminView.writeHardwareType(hardwareType);
    }
    for (ImageType imageType : imageTypes) {
      superadminView.writeImageType(imageType);
    }
    for (ClusterTemplate template : clusterTemplates) {
      superadminView.writeClusterTemplate(template);
    }
    // write superadmin plugin resources
    superadminView.writeAutomatorType(Entities.AutomatorTypeExample.CHEF);
    ResourceType type1 = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    ResourceMeta meta1 = new ResourceMeta("name1", 3, ResourceStatus.ACTIVE);
    ResourceMeta meta2 = new ResourceMeta("name2", 2, ResourceStatus.INACTIVE);
    metaStoreService.getResourceTypeView(Account.SUPERADMIN, type1).add(meta1);
    metaStoreService.getResourceTypeView(Account.SUPERADMIN, type1).add(meta2);
    writePluginResource(Account.SUPERADMIN, type1, meta1.getName(), meta1.getVersion(), "meta1 contents");
    writePluginResource(Account.SUPERADMIN, type1, meta2.getName(), meta2.getVersion(), "meta2 contents");

    // make a request to create a tenant and bootstrap it
    String name = "company-dev";
    TenantSpecification requestedTenant = new TenantSpecification(name, 10, 100, 1000);
    TenantWriteRequest tenantWriteRequest = new TenantWriteRequest(requestedTenant, true);
    HttpResponse response = doPostExternalAPI("/tenants", gson.toJson(tenantWriteRequest), SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    // make sure tenant account has copied superadmin entities
    Tenant tenant = tenantStore.getTenantByName(name);
    Account account = new Account(Constants.ADMIN_USER, tenant.getId());
    EntityStoreView tenantView = entityStoreService.getView(account);
    Assert.assertEquals(Sets.newHashSet(providers), Sets.newHashSet(tenantView.getAllProviders()));
    Assert.assertEquals(Sets.newHashSet(services), Sets.newHashSet(tenantView.getAllServices()));
    Assert.assertEquals(Sets.newHashSet(hardwareTypes), Sets.newHashSet(tenantView.getAllHardwareTypes()));
    Assert.assertEquals(Sets.newHashSet(imageTypes), Sets.newHashSet(tenantView.getAllImageTypes()));
    Assert.assertEquals(Sets.newHashSet(clusterTemplates), Sets.newHashSet(tenantView.getAllClusterTemplates()));
    // check tenant account has copied superadmin plugin resources
    Assert.assertEquals(meta1,
                        metaStoreService.getResourceTypeView(account, type1).get(meta1.getName(), meta1.getVersion()));
    Assert.assertEquals(meta2,
                        metaStoreService.getResourceTypeView(account, type1).get(meta2.getName(), meta2.getVersion()));
    Assert.assertEquals("meta1 contents", readPluginResource(account, type1, meta1.getName(), meta1.getVersion()));
    Assert.assertEquals("meta2 contents", readPluginResource(account, type1, meta2.getName(), meta2.getVersion()));
  }

  private void writePluginResource(Account account, ResourceType resourceType,
                                   String name, int version, String content) throws IOException {
    OutputStream outputStream = pluginStore.getResourceOutputStream(account, resourceType, name, version);
    try {
      outputStream.write(content.getBytes(Charsets.UTF_8));
    } finally {
      outputStream.close();
    }
  }

  private String readPluginResource(Account account, ResourceType resourceType,
                                    String name, int version) throws IOException {
    Reader reader = new InputStreamReader(
      pluginStore.getResourceInputStream(account, resourceType, name, version), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }
}
