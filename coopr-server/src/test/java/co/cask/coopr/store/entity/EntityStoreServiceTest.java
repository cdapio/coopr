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
package co.cask.coopr.store.entity;

import co.cask.coopr.Entities;
import co.cask.coopr.TestHelper;
import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.service.ServiceDependencies;
import co.cask.coopr.spec.template.ClusterTemplate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for getting and setting admin defined entities.  Test classes for different types of stores must set the
 * protected entityStore field before each test and make sure state is wiped out between tests.
 */
public abstract class EntityStoreServiceTest {
  protected static EntityStoreService entityStoreService;
  private static final Account tenant1Admin = new Account(Constants.ADMIN_USER, "tenant1");
  private static final Account tenant2Admin = new Account(Constants.ADMIN_USER, "tenant2");
  private static final Account superadmin = new Account(Constants.ADMIN_USER, Constants.SUPERADMIN_TENANT);

  public abstract void clearState() throws Exception;

  @Before
  public void setupTest() throws Exception {
    clearState();
  }

  @Test
  public void testGetStoreDeleteProvider() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Provider provider = Entities.ProviderExample.JOYENT;
    Assert.assertNull(entityStore.getProvider(provider.getName()));

    // write should work
    entityStore.writeProvider(provider);
    Provider result = entityStore.getProvider(provider.getName());
    Assert.assertEquals(provider, result);

    // bump version should work
    entityStore.writeProvider(provider);
    entityStore.writeProvider(provider);
    Collection<Provider> providers = entityStore.getAllProviders();
    Assert.assertEquals(1, providers.size());
    Set<Integer> versions = Sets.newHashSet();
    for (Provider provider1 : providers) {
      versions.add(provider1.getVersion());
    }
    Assert.assertEquals(Sets.newHashSet(3), versions);

    // delete should work
    entityStore.deleteProvider(provider.getName());
    Assert.assertNull(entityStore.getProvider(provider.getName()));
  }

  @Test
  public void testGetStoreDeleteHardwareType() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    HardwareType hardwareType = Entities.HardwareTypeExample.MEDIUM;
    String hardwareTypeName = hardwareType.getName();
    Assert.assertNull(entityStore.getHardwareType(hardwareTypeName));

    // write should work
    entityStore.writeHardwareType(hardwareType);
    HardwareType result = entityStore.getHardwareType(hardwareTypeName);
    Assert.assertEquals(hardwareType, result);

    // bump version should work
    entityStore.writeHardwareType(hardwareType);
    result = entityStore.getHardwareType(hardwareTypeName, 2);
    Assert.assertEquals(2, result.getVersion());
    result = entityStore.getHardwareType(hardwareTypeName, 1);
    Assert.assertEquals(hardwareType, result);
    Assert.assertEquals(1, result.getVersion());
    result = entityStore.getHardwareType(hardwareTypeName, 3);
    Assert.assertNull(result);

    // delete should work
    entityStore.deleteHardwareType(hardwareTypeName);
    Assert.assertNull(entityStore.getHardwareType(hardwareTypeName));
  }

  @Test
  public void testGetStoreDeleteImageType() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    ImageType imageType = Entities.ImageTypeExample.UBUNTU_12;
    String imageTypeName = imageType.getName();
    Assert.assertNull(entityStore.getImageType(imageTypeName));

    // write should work
    entityStore.writeImageType(imageType);
    ImageType result = entityStore.getImageType(imageTypeName);
    Assert.assertEquals(imageType, result);

    // bump version should work
    Assert.assertEquals(1, result.getVersion());
    entityStore.writeImageType(imageType);
    result = entityStore.getImageType(imageTypeName);
    Assert.assertEquals(2, result.getVersion());

    // delete should work
    entityStore.deleteImageType(imageTypeName);
    Assert.assertNull(entityStore.getImageType(imageTypeName));
  }

  @Test
  public void testGetStoreDeleteService() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Service service = Entities.ServiceExample.DATANODE;
    String serviceName = service.getName();
    Assert.assertNull(entityStore.getService(serviceName));

    // write should work
    entityStore.writeService(service);
    Service result = entityStore.getService(serviceName);
    Assert.assertEquals(service, result);

    // bump version should work
    entityStore.writeService(service);
    result = entityStore.getService(serviceName);
    Assert.assertEquals(2, result.getVersion());

    // delete should work
    entityStore.deleteService(serviceName, 2);
    Assert.assertNotNull(entityStore.getService(serviceName));
    entityStore.deleteService(serviceName, 1);
    Assert.assertNull(entityStore.getService(serviceName));
  }

  @Test
  public void testGetStoreDeleteClusterTemplate() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    ClusterTemplate clusterTemplate = Entities.ClusterTemplateExample.REACTOR;
    String clusterTemplateName = clusterTemplate.getName();
    Assert.assertNull(entityStore.getClusterTemplate(clusterTemplateName));

    // write should work
    entityStore.writeClusterTemplate(clusterTemplate);
    ClusterTemplate result = entityStore.getClusterTemplate(clusterTemplateName);
    Assert.assertEquals(clusterTemplate, result);

    // bump version should work
    entityStore.writeClusterTemplate(clusterTemplate);
    entityStore.writeClusterTemplate(clusterTemplate);
    Collection<ClusterTemplate> clusterTemplates = entityStore.getAllClusterTemplates();
    Assert.assertEquals(1, clusterTemplates.size());
    Set<Integer> versions = Sets.newHashSet();
    for (ClusterTemplate clusterTemplate1 : clusterTemplates) {
      versions.add(clusterTemplate1.getVersion());
    }
    Assert.assertEquals(Sets.newHashSet(3), versions);

    // delete should work
    entityStore.deleteClusterTemplate(clusterTemplateName);
    Assert.assertNull(entityStore.getClusterTemplate(clusterTemplateName));
  }

  @Test
  public void testGetStoreDeleteProviderType() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(superadmin);
    ProviderType providerType = Entities.ProviderTypeExample.JOYENT;
    String providerTypeName = providerType.getName();
    Assert.assertNull(entityStore.getProviderType(providerTypeName));

    // write should work
    entityStore.writeProviderType(providerType);
    ProviderType result = entityStore.getProviderType(providerTypeName);
    Assert.assertEquals(providerType, result);

    // overwrite should work
    entityStore.writeProviderType(providerType);
    result = entityStore.getProviderType(providerTypeName);
    Assert.assertEquals(providerType, result);

    // delete should work
    entityStore.deleteProviderType(providerTypeName);
    Assert.assertNull(entityStore.getProviderType(providerTypeName));
  }

  @Test
  public void testGetStoreDeleteAutomatorType() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(superadmin);
    AutomatorType automatorType = Entities.AutomatorTypeExample.CHEF;
    String automatorTypeName = automatorType.getName();
    Assert.assertNull(entityStore.getAutomatorType(automatorTypeName));

    // write should work
    entityStore.writeAutomatorType(automatorType);
    AutomatorType result = entityStore.getAutomatorType(automatorTypeName);
    Assert.assertEquals(automatorType, result);

    // overwrite should work
    entityStore.writeAutomatorType(automatorType);
    result = entityStore.getAutomatorType(automatorTypeName);
    Assert.assertEquals(automatorType, result);

    // delete should work
    entityStore.deleteAutomatorType(automatorTypeName);
    Assert.assertNull(entityStore.getAutomatorType(automatorTypeName));
  }

  @Test
  public void testGetAllProviderTypes() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(superadmin);
    Assert.assertEquals(0, entityStore.getAllProviderTypes().size());

    ProviderType type1 = Entities.ProviderTypeExample.JOYENT;
    ProviderType type2 = Entities.ProviderTypeExample.RACKSPACE;
    ProviderType type3 = Entities.ProviderTypeExample.USER_RACKSPACE;
    List<ProviderType> types = ImmutableList.of(type1, type2, type3);

    for (ProviderType type : types) {
      entityStore.writeProviderType(type);
    }
    Collection<ProviderType> result = entityStore.getAllProviderTypes();
    Assert.assertEquals("provider types written and fetched are not equal in size",
                        types.size(), result.size());
    Assert.assertTrue("not all provider types written were found in the results", result.containsAll(result));

    // check we get all the providers after one of them is deleted
    entityStore.deleteProviderType(type1.getName());
    types = ImmutableList.of(type2, type3);
    result = entityStore.getAllProviderTypes();
    Assert.assertEquals("provider types written and fetched are not equal in size",
                        types.size(), result.size());
    Assert.assertTrue("not all provider types written were found in the results", result.containsAll(result));
  }

  @Test
  public void testGetAllAutomatorTypes() throws Exception {
    EntityStoreView superadminView = entityStoreService.getView(superadmin);
    EntityStoreView adminView = entityStoreService.getView(tenant1Admin);
    Assert.assertEquals(0, superadminView.getAllAutomatorTypes().size());
    Assert.assertEquals(0, adminView.getAllAutomatorTypes().size());

    AutomatorType type1 = Entities.AutomatorTypeExample.SHELL;
    AutomatorType type2 = Entities.AutomatorTypeExample.CHEF;
    AutomatorType type3 = Entities.AutomatorTypeExample.PUPPET;
    List<AutomatorType> types = ImmutableList.of(type1, type2, type3);

    for (AutomatorType type : types) {
      superadminView.writeAutomatorType(type);
    }
    Collection<AutomatorType> result = superadminView.getAllAutomatorTypes();
    Assert.assertEquals("automator types written and fetched are not equal in size",
                        types.size(), result.size());
    Assert.assertTrue("not all automator types written were found in the results", result.containsAll(result));
    result = adminView.getAllAutomatorTypes();
    Assert.assertEquals("automator types written and fetched are not equal in size",
                        types.size(), result.size());
    Assert.assertTrue("not all automator types written were found in the results", result.containsAll(result));

    // check we get all the providers after one of them is deleted
    superadminView.deleteAutomatorType(type1.getName());
    types = ImmutableList.of(type2, type3);
    result = superadminView.getAllAutomatorTypes();
    Assert.assertEquals("automator types written and fetched are not equal in size",
                        types.size(), result.size());
    Assert.assertTrue("not all automator types written were found in the results", result.containsAll(result));
    result = adminView.getAllAutomatorTypes();
    Assert.assertEquals("automator types written and fetched are not equal in size",
                        types.size(), result.size());
    Assert.assertTrue("not all automator types written were found in the results", result.containsAll(result));
  }

  @Test
  public void testGetAllProviders() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Assert.assertEquals(0, entityStore.getAllProviders().size());

    Provider provider1 = createProvider("provider1", "1st provider", Entities.JOYENT, "k1", "v1", "k2", "v2");
    Provider provider2 = createProvider("provider2", "2nd provider", Entities.OPENSTACK, "k2", "v2", "k3", "v3");
    Provider provider3 = createProvider("provider3", "3rd provider", Entities.RACKSPACE, "k4", "v4");
    List<Provider> providers = ImmutableList.of(provider1, provider2, provider3);

    for (Provider provider : providers) {
      entityStore.writeProvider(provider);
    }
    Collection<Provider> result = entityStore.getAllProviders();
    Assert.assertEquals("providers written and fetched are not equal in size",
                        providers.size(), result.size());
    Assert.assertTrue("not all providers written were found in the results", result.containsAll(result));

    // check we get all the providers after one of them is deleted
    entityStore.deleteProvider(provider1.getName());
    providers = ImmutableList.of(provider2, provider3);
    result = entityStore.getAllProviders();
    Assert.assertEquals("providers written and fetched are not equal in size",
                        providers.size(), result.size());
    Assert.assertTrue("not all providers written were found in the results", result.containsAll(result));
  }

  @Test
  public void testGetAllHardwareTypes() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Assert.assertEquals(0, entityStore.getAllHardwareTypes().size());

    HardwareType hw1 = HardwareType.builder()
      .setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("flavor", "Medium 4GB"),
        "openstack1", ImmutableMap.of("flavor", "5"),
        "openstack2", ImmutableMap.of("flavor", "3")
      ))
      .setName("hw1")
      .setDescription("1st hw type")
      .build();
    HardwareType hw2 = HardwareType.builder()
      .setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("flavor", "Medium 2GB"),
        "openstack1", ImmutableMap.of("flavor", "4"),
        "aws", ImmutableMap.of("flavor", "12345")
      ))
      .setName("hw2")
      .setDescription("2nd hw type")
      .build();
    HardwareType hw3 = HardwareType.builder()
      .setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("flavor", "Large 16GB"),
        "openstack1", ImmutableMap.of("flavor", "8"),
        "rackspace", ImmutableMap.of("flavor", "9")
      ))
      .setName("hw3")
      .setDescription("3rd hw type")
      .build();
    List<HardwareType> hardwareTypes = ImmutableList.of(hw1, hw2, hw3);

    for (HardwareType hardwareType : hardwareTypes) {
      entityStore.writeHardwareType(hardwareType);
    }

    Collection<HardwareType> result = entityStore.getAllHardwareTypes();
    Assert.assertEquals("hardware types written and fetched are not equal in size",
                        hardwareTypes.size(), result.size());
    Assert.assertTrue("not all written hardware types were found in the results", result.containsAll(hardwareTypes));

    hardwareTypes = ImmutableList.of(hw1, hw3);
    entityStore.deleteHardwareType(hw2.getName());
    result = entityStore.getAllHardwareTypes();
    Assert.assertEquals("hardware types written and fetched are not equal in size",
                        hardwareTypes.size(), result.size());
    Assert.assertTrue("not all written hardware types were found in the results", result.containsAll(hardwareTypes));
  }

  @Test
  public void testGetAllImageTypes() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Assert.assertEquals(0, entityStore.getAllImageTypes().size());

    ImageType it1 =ImageType.builder()
      .setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("image", "4f938eea-9df0-4112-b21f-8cc9cbbf9c71"),
        "openstack1", ImmutableMap.of("image", "325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "openstack2", ImmutableMap.of("image", "f70ed7c7-b42e-4d77-83d8-40fa29825b85")))
      .setName("centos6.4")
      .setDescription("centos 6.4 image")
      .build();
    ImageType it2 =ImageType.builder()
      .setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("image", "3f938eea-9df0-4112-b21f-8cc9cbbf9c71"),
        "openstack1", ImmutableMap.of("image", "225dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "openstack2", ImmutableMap.of("image", "e70ed7c7-b42e-4d77-83d8-40fa29825b85")))
      .setName("rhel5")
      .setDescription("rhel 5 image")
      .build();
    ImageType it3 =ImageType.builder()
      .setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("image", "2f938eea-9df0-4112-b21f-8cc9cbbf9c71"),
        "openstack1", ImmutableMap.of("image", "125dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "openstack2", ImmutableMap.of("image", "d70ed7c7-b42e-4d77-83d8-40fa29825b85")))
      .setName("ubuntu")
      .setDescription("ubuntu image")
      .build();
    List<ImageType> imageTypes = ImmutableList.of(it1, it2, it3);

    for (ImageType imageType : imageTypes) {
      entityStore.writeImageType(imageType);
    }

    Collection<ImageType> result = entityStore.getAllImageTypes();
    Assert.assertEquals("image types written and fetched are not equal in size",
                        imageTypes.size(), result.size());
    Assert.assertTrue("not all written image types were found in the results",
                      result.containsAll(imageTypes));

    imageTypes = ImmutableList.of(it2, it3);
    entityStore.deleteImageType(it1.getName());
    result = entityStore.getAllImageTypes();
    Assert.assertEquals("image types written and fetched are not equal in size",
                        imageTypes.size(), result.size());
    Assert.assertTrue("not all written image types were found in the results",
                      result.containsAll(imageTypes));
  }

  @Test
  public void testGetAllServices() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Assert.assertEquals(0, entityStore.getAllServices().size());

    Service s1 = Service.builder()
      .setName("datanode")
      .setDependencies(ServiceDependencies.runtimeRequires("namenode"))
      .setProvisionerActions(
        ImmutableMap.<ProvisionerAction, ServiceAction>of(
          ProvisionerAction.INSTALL,
          new ServiceAction("chef-solo", TestHelper.actionMapOf("install recipe", null)),
          ProvisionerAction.REMOVE,
          new ServiceAction("chef-solo", TestHelper.actionMapOf("remove recipe", "arbitrary data"))
        ))
      .build();
    Service s2 = Service.builder()
      .setName("namenode")
      .setDependencies(ServiceDependencies.runtimeRequires("hosts"))
      .setProvisionerActions(ImmutableMap.<ProvisionerAction, ServiceAction>of(
        ProvisionerAction.INSTALL,
        new ServiceAction("chef-solo", TestHelper.actionMapOf("install recipe", null)),
        ProvisionerAction.REMOVE,
        new ServiceAction("chef-solo", TestHelper.actionMapOf("remove recipe", "arbitrary data")),
        ProvisionerAction.CONFIGURE,
        new ServiceAction("chef-solo", TestHelper.actionMapOf("configure recipe", null))
      ))
      .build();
    Service s3 = Service.builder()
      .setName("hosts")
      .setProvisionerActions(ImmutableMap.<ProvisionerAction, ServiceAction>of(
        ProvisionerAction.CONFIGURE,
        new ServiceAction("chef-solo", TestHelper.actionMapOf("configure recipe", null))
      ))
      .build();
    List<Service> services = ImmutableList.of(s1, s2, s3);

    for (Service service : services) {
      entityStore.writeService(service);
    }

    Collection<Service> result = entityStore.getAllServices();
    Assert.assertEquals("services written and fetched are not equal in size",
                        services.size(), result.size());
    Assert.assertTrue("not all written services were found in the results",
                      result.containsAll(services));

    services = ImmutableList.of(s2, s3);
    entityStore.deleteService(s1.getName());
    result = entityStore.getAllServices();
    Assert.assertEquals("services written and fetched are not equal in size",
                        services.size(), result.size());
    Assert.assertTrue("not all written services were found in the results",
                      result.containsAll(services));
  }

  @Test
  public void testGetAllClusterTemplates() throws Exception {
    EntityStoreView entityStore = entityStoreService.getView(tenant1Admin);
    Assert.assertEquals(0, entityStore.getAllClusterTemplates().size());

    ClusterTemplate c1 = Entities.ClusterTemplateExample.HDFS;
    ClusterTemplate c2 = Entities.ClusterTemplateExample.REACTOR;
    List<ClusterTemplate> clusterTemplates = ImmutableList.of(c1, c2);

    for (ClusterTemplate clusterTemplate : clusterTemplates) {
      entityStore.writeClusterTemplate(clusterTemplate);
    }

    Collection<ClusterTemplate> result = entityStore.getAllClusterTemplates();
    Assert.assertEquals("cluster templates written and fetched are not equal in size",
                        clusterTemplates.size(), result.size());
    Assert.assertTrue("not all written cluster templates were found in the results",
                      result.containsAll(clusterTemplates));

    clusterTemplates = ImmutableList.of(c1);
    entityStore.deleteClusterTemplate(c2.getName());
    result = entityStore.getAllClusterTemplates();
    Assert.assertEquals("cluster templates written and fetched are not equal in size",
                        clusterTemplates.size(), result.size());
    Assert.assertTrue("not all written cluster templates were found in the results",
                      result.containsAll(clusterTemplates));
  }

  @Test
  public void testProvidersDoNotOverlapAcrossTenants() throws Exception {
    Provider provider1 = Entities.ProviderExample.RACKSPACE;
    Provider provider2 = Entities.ProviderExample.JOYENT;
    EntityStoreView account1View = entityStoreService.getView(tenant1Admin);
    EntityStoreView account2View = entityStoreService.getView(tenant2Admin);
    account1View.writeProvider(provider1);

    Assert.assertNull(account2View.getProvider(provider1.getName()));
    Assert.assertEquals(provider1, account1View.getProvider(provider1.getName()));

    account1View.deleteProvider(provider1.getName());
    account2View.writeProvider(provider1);

    Assert.assertNull(account1View.getProvider(provider1.getName()));
    Assert.assertEquals(provider1, account2View.getProvider(provider1.getName()));

    account2View.writeProvider(provider2);

    Assert.assertTrue(account1View.getAllProviders().isEmpty());
    Assert.assertEquals(ImmutableSet.<Provider>of(provider1, provider2),
                        ImmutableSet.copyOf(account2View.getAllProviders()));
  }

  @Test
  public void testHardwaretypesDoNotOverlapAcrossTenants() throws Exception {
    HardwareType entity1 = Entities.HardwareTypeExample.MEDIUM;
    HardwareType entity2 = Entities.HardwareTypeExample.LARGE;
    EntityStoreView account1View = entityStoreService.getView(tenant1Admin);
    EntityStoreView account2View = entityStoreService.getView(tenant2Admin);
    account1View.writeHardwareType(entity1);

    Assert.assertNull(account2View.getHardwareType(entity1.getName()));
    Assert.assertEquals(entity1, account1View.getHardwareType(entity1.getName()));

    account1View.deleteHardwareType(entity1.getName());
    account2View.writeHardwareType(entity1);

    Assert.assertNull(account1View.getHardwareType(entity1.getName()));
    Assert.assertEquals(entity1, account2View.getHardwareType(entity1.getName()));

    account2View.writeHardwareType(entity2);

    Assert.assertTrue(account1View.getAllHardwareTypes().isEmpty());
    Assert.assertEquals(ImmutableSet.<HardwareType>of(entity1, entity2),
                        ImmutableSet.copyOf(account2View.getAllHardwareTypes()));
  }

  @Test
  public void testImagetypesDoNotOverlapAcrossTenants() throws Exception {
    ImageType entity1 = Entities.ImageTypeExample.CENTOS_6;
    ImageType entity2 = Entities.ImageTypeExample.UBUNTU_12;
    EntityStoreView account1View = entityStoreService.getView(tenant1Admin);
    EntityStoreView account2View = entityStoreService.getView(tenant2Admin);
    account1View.writeImageType(entity1);

    Assert.assertNull(account2View.getImageType(entity1.getName()));
    Assert.assertEquals(entity1, account1View.getImageType(entity1.getName()));

    account1View.deleteImageType(entity1.getName());
    account2View.writeImageType(entity1);

    Assert.assertNull(account1View.getImageType(entity1.getName()));
    Assert.assertEquals(entity1, account2View.getImageType(entity1.getName()));

    account2View.writeImageType(entity2);

    Assert.assertTrue(account1View.getAllImageTypes().isEmpty());
    Assert.assertEquals(ImmutableSet.<ImageType>of(entity1, entity2),
                        ImmutableSet.copyOf(account2View.getAllImageTypes()));
  }

  @Test
  public void testServicesDoNotOverlapAcrossTenants() throws Exception {
    Service entity1 = Entities.ServiceExample.DATANODE;
    Service entity2 = Entities.ServiceExample.NAMENODE;
    EntityStoreView account1View = entityStoreService.getView(tenant1Admin);
    EntityStoreView account2View = entityStoreService.getView(tenant2Admin);
    account1View.writeService(entity1);

    Assert.assertNull(account2View.getService(entity1.getName()));
    Assert.assertEquals(entity1, account1View.getService(entity1.getName()));

    account1View.deleteService(entity1.getName());
    account2View.writeService(entity1);

    Assert.assertNull(account1View.getService(entity1.getName()));
    Assert.assertEquals(entity1, account2View.getService(entity1.getName()));

    account2View.writeService(entity2);

    Assert.assertTrue(account1View.getAllServices().isEmpty());
    Assert.assertEquals(ImmutableSet.<Service>of(entity1, entity2),
                        ImmutableSet.copyOf(account2View.getAllServices()));
  }

  @Test
  public void testTemplatesDoNotOverlapAcrossTenants() throws Exception {
    ClusterTemplate entity1 = Entities.ClusterTemplateExample.HDFS;
    ClusterTemplate entity2 = Entities.ClusterTemplateExample.REACTOR;
    EntityStoreView account1View = entityStoreService.getView(tenant1Admin);
    EntityStoreView account2View = entityStoreService.getView(tenant2Admin);
    account1View.writeClusterTemplate(entity1);

    Assert.assertNull(account2View.getClusterTemplate(entity1.getName()));
    Assert.assertEquals(entity1, account1View.getClusterTemplate(entity1.getName()));

    account1View.deleteClusterTemplate(entity1.getName());
    account2View.writeClusterTemplate(entity1);

    Assert.assertNull(account1View.getClusterTemplate(entity1.getName()));
    Assert.assertEquals(entity1, account2View.getClusterTemplate(entity1.getName()));

    account2View.writeClusterTemplate(entity2);

    Assert.assertTrue(account1View.getAllClusterTemplates().isEmpty());
    Assert.assertEquals(ImmutableSet.<ClusterTemplate>of(entity1, entity2),
                        ImmutableSet.copyOf(account2View.getAllClusterTemplates()));
  }

  protected Provider createProvider(String name, String description, String type, String... mapKeyVals) {
    Preconditions.checkArgument(mapKeyVals.length % 2 == 0, "each key must have a corresponding value");
    Map<String, Object> authMap = Maps.newHashMap();
    for (int i = 0; i < mapKeyVals.length; i += 2) {
      authMap.put(mapKeyVals[i], mapKeyVals[i+1]);
    }
    return Provider.builder()
      .setProviderType(type)
      .setProvisionerFields(authMap)
      .setName(name)
      .setDescription(description)
      .build();
  }
}
