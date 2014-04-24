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
package com.continuuity.loom.store;

import com.continuuity.loom.Entities;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Tests for getting and setting admin defined entities.  Test classes for different types of stores must set the
 * protected entityStore field before each test and make sure state is wiped out between tests.
 */
public abstract class EntityStoreTest {
  protected static EntityStore entityStore;

  @Test
  public void testGetStoreDeleteProvider() throws Exception {
    Provider provider = Entities.ProviderExample.JOYENT;
    Assert.assertNull(entityStore.getProvider(provider.getName()));

    // write should work
    entityStore.writeProvider(provider);
    Provider result = entityStore.getProvider(provider.getName());
    Assert.assertEquals(provider, result);

    // overwrite should work
    entityStore.writeProvider(provider);
    result = entityStore.getProvider(provider.getName());
    Assert.assertEquals(provider, result);

    // delete should work
    entityStore.deleteProvider(provider.getName());
    Assert.assertNull(entityStore.getProvider(provider.getName()));
  }

  @Test
  public void testGetStoreDeleteHardwareType() throws Exception {
    HardwareType hardwareType = Entities.HardwareTypeExample.MEDIUM;
    String hardwareTypeName = hardwareType.getName();
    Assert.assertNull(entityStore.getHardwareType(hardwareTypeName));

    // write should work
    entityStore.writeHardwareType(hardwareType);
    HardwareType result = entityStore.getHardwareType(hardwareTypeName);
    Assert.assertEquals(hardwareType, result);

    // overwrite should work
    entityStore.writeHardwareType(hardwareType);
    result = entityStore.getHardwareType(hardwareTypeName);
    Assert.assertEquals(hardwareType, result);

    // delete should work
    entityStore.deleteHardwareType(hardwareTypeName);
    Assert.assertNull(entityStore.getHardwareType(hardwareTypeName));
  }

  @Test
  public void testGetStoreDeleteImageType() throws Exception {
    ImageType imageType = Entities.ImageTypeExample.UBUNTU_12;
    String imageTypeName = imageType.getName();
    Assert.assertNull(entityStore.getImageType(imageTypeName));

    // write should work
    entityStore.writeImageType(imageType);
    ImageType result = entityStore.getImageType(imageTypeName);
    Assert.assertEquals(imageType, result);

    // overwrite should work
    entityStore.writeImageType(imageType);
    result = entityStore.getImageType(imageTypeName);
    Assert.assertEquals(imageType, result);

    // delete should work
    entityStore.deleteImageType(imageTypeName);
    Assert.assertNull(entityStore.getImageType(imageTypeName));
  }

  @Test
  public void testGetStoreDeleteService() throws Exception {
    Service service = Entities.ServiceExample.DATANODE;
    String serviceName = service.getName();
    Assert.assertNull(entityStore.getService(serviceName));

    // write should work
    entityStore.writeService(service);
    Service result = entityStore.getService(serviceName);
    Assert.assertEquals(service, result);

    // overwrite should work
    entityStore.writeService(service);
    result = entityStore.getService(serviceName);
    Assert.assertEquals(service, result);

    // delete should work
    entityStore.deleteService(serviceName);
    Assert.assertNull(entityStore.getService(serviceName));
  }

  @Test
  public void testGetStoreDeleteClusterTemplate() throws Exception {
    ClusterTemplate clusterTemplate = Entities.ClusterTemplateExample.REACTOR;
    String clusterTemplateName = clusterTemplate.getName();
    Assert.assertNull(entityStore.getClusterTemplate(clusterTemplateName));

    // write should work
    entityStore.writeClusterTemplate(clusterTemplate);
    ClusterTemplate result = entityStore.getClusterTemplate(clusterTemplateName);
    Assert.assertEquals(clusterTemplate, result);

    // overwrite should work
    entityStore.writeClusterTemplate(clusterTemplate);
    result = entityStore.getClusterTemplate(clusterTemplateName);
    Assert.assertEquals(clusterTemplate, result);

    // delete should work
    entityStore.deleteClusterTemplate(clusterTemplateName);
    Assert.assertNull(entityStore.getClusterTemplate(clusterTemplateName));
  }

  @Test
  public void testGetAllProviders() throws Exception {
    Assert.assertEquals(0, entityStore.getAllProviders().size());

    Provider provider1 = createProvider("provider1", "1st provider", Provider.Type.JOYENT, "k1", "v1", "k2", "v2");
    Provider provider2 = createProvider("provider2", "2nd provider", Provider.Type.OPENSTACK, "k2", "v2", "k3", "v3");
    Provider provider3 = createProvider("provider3", "3rd provider", Provider.Type.RACKSPACE, "k4", "v4");
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
    Assert.assertEquals(0, entityStore.getAllHardwareTypes().size());

    HardwareType hw1 = new HardwareType("hw1", "1st hw type", ImmutableMap.<String, Map<String, String>>of(
      "joyent", ImmutableMap.of("flavor", "Medium 4GB"),
      "openstack1", ImmutableMap.of("flavor", "5"),
      "openstack2", ImmutableMap.of("flavor", "3")
    ));
    HardwareType hw2 = new HardwareType("hw2", "2nd hw type", ImmutableMap.<String, Map<String, String>>of(
      "joyent", ImmutableMap.of("flavor", "Medium 2GB"),
      "openstack1", ImmutableMap.of("flavor", "4"),
      "aws", ImmutableMap.of("flavor", "12345")
    ));
    HardwareType hw3 = new HardwareType("hw3", "3rd hw type", ImmutableMap.<String, Map<String, String>>of(
      "joyent", ImmutableMap.of("flavor", "Large 16GB"),
      "openstack1", ImmutableMap.of("flavor", "8"),
      "rackspace", ImmutableMap.of("flavor", "9")
    ));
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
    Assert.assertEquals(0, entityStore.getAllImageTypes().size());

    ImageType it1 =
      new ImageType("centos6.4", "centos 6.4 image", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("image", "4f938eea-9df0-4112-b21f-8cc9cbbf9c71"),
        "openstack1", ImmutableMap.of("image", "325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "openstack2", ImmutableMap.of("image", "f70ed7c7-b42e-4d77-83d8-40fa29825b85")));
    ImageType it2 =
      new ImageType("rhel5", "rhel 5 image", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("image", "3f938eea-9df0-4112-b21f-8cc9cbbf9c71"),
        "openstack1", ImmutableMap.of("image", "225dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "openstack2", ImmutableMap.of("image", "e70ed7c7-b42e-4d77-83d8-40fa29825b85")));
    ImageType it3 =
      new ImageType("ubuntu", "ubuntu image", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.of("image", "2f938eea-9df0-4112-b21f-8cc9cbbf9c71"),
        "openstack1", ImmutableMap.of("image", "125dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "openstack2", ImmutableMap.of("image", "d70ed7c7-b42e-4d77-83d8-40fa29825b85")));
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
    Assert.assertEquals(0, entityStore.getAllServices().size());

    Service s1 = new Service("datanode", "hadoop datanode", ImmutableSet.of("namenode"),
                             ImmutableMap.<ProvisionerAction, ServiceAction>of(
                               ProvisionerAction.INSTALL,
                               new ServiceAction("chef", "install recipe", null),
                               ProvisionerAction.REMOVE,
                               new ServiceAction("chef", "remove recipe", "arbitrary data")
                             )
    );
    Service s2 = new Service("namenode", "hadoop namenode", ImmutableSet.of("hosts"),
                             ImmutableMap.<ProvisionerAction, ServiceAction>of(
                               ProvisionerAction.INSTALL,
                               new ServiceAction("chef", "install recipe", null),
                               ProvisionerAction.REMOVE,
                               new ServiceAction("chef", "remove recipe", "arbitrary data"),
                               ProvisionerAction.CONFIGURE,
                               new ServiceAction("chef", "configure recipe", null)
                             )
    );
    Service s3 = new Service("hosts", "for managing /etc/hosts", ImmutableSet.<String>of(),
                             ImmutableMap.<ProvisionerAction, ServiceAction>of(
                               ProvisionerAction.CONFIGURE,
                               new ServiceAction("chef", "configure recipe", null)
                             )
    );
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

  protected Provider createProvider(String name, String description, Provider.Type type, String... mapKeyVals) {
    Preconditions.checkArgument(mapKeyVals.length % 2 == 0, "each key must have a corresponding value");
    Map<String, String> authMap = Maps.newHashMap();
    for (int i = 0; i < mapKeyVals.length; i += 2) {
      authMap.put(mapKeyVals[i], mapKeyVals[i+1]);
    }

    return new Provider(name, description, type, ImmutableMap.<String, Map<String, String>>of("auth", authMap));
  }
}
