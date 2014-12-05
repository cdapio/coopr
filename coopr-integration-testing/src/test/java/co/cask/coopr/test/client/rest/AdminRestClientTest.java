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
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.test.client.ClientTest;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class AdminRestClientTest extends ClientTest {

  private AdminClient adminClient;

  @Before
  public void setUp() {
    adminClient = adminClientManager.getAdminClient();
  }

  @Test
  public void testGetAllClusterTemplates() throws IOException {
    List<ClusterTemplate> result = adminClient.getAllClusterTemplates();
    Assert.assertTrue(result.contains(REACTOR_CLUSTER_TEMPLATE));
    Assert.assertTrue(result.contains(HDFS_CLUSTER_TEMPLATE));
    Assert.assertTrue(result.contains(HADOOP_DISTRIBUTED_CLUSTER_TEMPLATE));
    Assert.assertEquals(3, result.size());
  }

  @Test
  public void testGetClusterTemplate() throws IOException {
    ClusterTemplate result = adminClient.getClusterTemplate(REACTOR_CLUSTER_TEMPLATE.getName());
    Assert.assertEquals(REACTOR_CLUSTER_TEMPLATE, result);
  }

  @Test
  public void testGetClusterTemplateNotFound() throws IOException {
    try {
      adminClient.getClusterTemplate("cassandra");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteClusterTemplate() throws IOException {
    ClusterTemplate result = adminClient.getClusterTemplate(REACTOR_CLUSTER_TEMPLATE.getName());
    Assert.assertNotNull(result);
    Assert.assertEquals(REACTOR_CLUSTER_TEMPLATE, result);
    adminClient.deleteClusterTemplate(REACTOR_CLUSTER_TEMPLATE.getName());
    try {
      adminClient.getClusterTemplate(REACTOR_CLUSTER_TEMPLATE.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }


  @Test
  public void testGetAllProviders() throws IOException {
    List<Provider> result = adminClient.getAllProviders();
    Assert.assertTrue(result.contains(Entities.ProviderExample.JOYENT));
    Assert.assertTrue(result.contains(Entities.ProviderExample.RACKSPACE));
    Assert.assertEquals(2, result.size());
  }

  @Test
  public void testGetProvider() throws IOException {
    Provider result = adminClient.getProvider(Entities.JOYENT);
    Assert.assertEquals(Entities.ProviderExample.JOYENT, result);
  }

  @Test
  public void testGetProviderNotFound() throws IOException {
    try {
      adminClient.getProvider("test");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteProvider() throws IOException {
    Provider result = adminClient.getProvider(Entities.JOYENT);
    Assert.assertNotNull(result);
    Assert.assertEquals(Entities.ProviderExample.JOYENT, result);

    adminClient.deleteProvider(Entities.JOYENT);
    try {
      adminClient.getProvider(Entities.JOYENT);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllServices() throws IOException {
    List<Service> result = adminClient.getAllServices();
    Assert.assertEquals(4, result.size());
    Assert.assertTrue(result.contains(Entities.ServiceExample.DATANODE));
    Assert.assertTrue(result.contains(Entities.ServiceExample.NAMENODE));
    Assert.assertTrue(result.contains(Entities.ServiceExample.HOSTS));
    Assert.assertTrue(result.contains(ZOOKEEPER));
  }


  @Test
  public void testGetService() throws IOException {
    Service result = adminClient.getService(Entities.ServiceExample.NAMENODE.getName());
    Assert.assertEquals(Entities.ServiceExample.NAMENODE, result);
  }

  @Test
  public void testDeleteService() throws IOException {
    Service result = adminClient.getService(Entities.ServiceExample.HOSTS.getName());
    Assert.assertNotNull(result);
    Assert.assertEquals(Entities.ServiceExample.HOSTS, result);

    adminClient.deleteService(Entities.ServiceExample.HOSTS.getName());
    try {
      adminClient.getService(Entities.ServiceExample.HOSTS.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllHardwareTypes() throws IOException {
    List<HardwareType> result = adminClient.getAllHardwareTypes();
    Assert.assertEquals(3, result.size());
    Assert.assertTrue(result.contains(Entities.HardwareTypeExample.LARGE));
    Assert.assertTrue(result.contains(Entities.HardwareTypeExample.MEDIUM));
    Assert.assertTrue(result.contains(Entities.HardwareTypeExample.SMALL));
  }


  @Test
  public void testGetHardwareType() throws IOException {
    HardwareType result = adminClient.getHardwareType(Entities.HardwareTypeExample.LARGE.getName());
    Assert.assertEquals(Entities.HardwareTypeExample.LARGE, result);
  }

  @Test
  public void testDeleteHardwareType() throws IOException {
    HardwareType result = adminClient.getHardwareType(Entities.HardwareTypeExample.SMALL.getName());
    Assert.assertNotNull(result);
    Assert.assertEquals(Entities.HardwareTypeExample.SMALL, result);

    adminClient.deleteService(Entities.HardwareTypeExample.SMALL.getName());
    try {
      adminClient.getService(Entities.HardwareTypeExample.SMALL.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllImageTypes() throws IOException {
    List<ImageType> result = adminClient.getAllImageTypes();
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(result.contains(Entities.ImageTypeExample.CENTOS_6));
    Assert.assertTrue(result.contains(Entities.ImageTypeExample.UBUNTU_12));
  }

  @Test
  public void testGetImageType() throws IOException {
    ImageType result = adminClient.getImageType(Entities.ImageTypeExample.CENTOS_6.getName());
    Assert.assertEquals(Entities.ImageTypeExample.CENTOS_6, result);
  }

  @Test
  public void testDeleteImageType() throws IOException {
    ImageType result = adminClient.getImageType(Entities.ImageTypeExample.CENTOS_6.getName());
    Assert.assertNotNull(result);
    Assert.assertEquals(Entities.ImageTypeExample.CENTOS_6, result);

    adminClient.deleteService(Entities.ImageTypeExample.CENTOS_6.getName());
    try {
      adminClient.getService(Entities.ImageTypeExample.CENTOS_6.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }
}
