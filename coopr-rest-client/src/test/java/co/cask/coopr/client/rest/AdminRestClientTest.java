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

import co.cask.common.http.exception.HttpFailureException;
import co.cask.coopr.Entities;
import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.rest.exception.UnauthorizedAccessTokenException;
import co.cask.coopr.client.rest.handler.TestStatusUserId;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.PartialTemplate;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class AdminRestClientTest extends RestClientTest {

  private AdminClient adminClient;

  public static final String HADOOP_DISTRIBUTED_NAME = "hadoop-distributed";
  public static final String TEST_PARTIAL1_NAME = "partial1";
  public static final String JOYENT_PROVIDER_NAME = "joyent";
  public static final String DATA_NODE_SERVICE_NAME = "datanode";
  public static final String LARGE_HARDWARE_TYPE_NAME = "large";
  public static final String CENTOS_6_IMAGE_TYPE_NAME = "centos6";

  @Before
  public void setUp() throws Exception {
    super.setUp();
    adminClient = clientManager.getAdminClient();
  }

  @Test
  public void testGetAllClusterTemplatesSuccess() throws IOException {
    List<ClusterTemplate> result = adminClient.getAllClusterTemplates();
    assertTrue(result.contains(Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED));
    assertTrue(result.contains(Entities.ClusterTemplateExample.HDFS));
    assertTrue(result.contains(Entities.ClusterTemplateExample.REACTOR));
    assertTrue(result.contains(Entities.ClusterTemplateExample.REACTOR2));
  }

  @Test
  public void testGetAllClusterTemplatesBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getAllClusterTemplates();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetClusterTemplateSuccess() throws IOException {
    ClusterTemplate result = adminClient.getClusterTemplate(HADOOP_DISTRIBUTED_NAME);
    assertTrue(result.equals(Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED));
  }

  @Test
  public void testGetClusterTemplateNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getClusterTemplate(HADOOP_DISTRIBUTED_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteClusterTemplateSuccess() throws IOException {
    adminClient.deleteClusterTemplate(HADOOP_DISTRIBUTED_NAME);
  }

  @Test
  public void testDeleteClusterTemplateNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.deleteClusterTemplate(HADOOP_DISTRIBUTED_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllPartialTemplatesSuccess() throws IOException {
    List<PartialTemplate> result = adminClient.getAllPartialTemplates();
    assertTrue(result.contains(Entities.PartialTemplateExample.TEST_PARTIAL1));
    assertTrue(result.contains(Entities.PartialTemplateExample.TEST_PARTIAL2));
  }

  @Test
  public void testGetAllPartialTemplatesBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getAllPartialTemplates();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetPartialTemplateSuccess() throws IOException {
    PartialTemplate result = adminClient.getPartialTemplate(TEST_PARTIAL1_NAME);
    assertTrue(result.equals(Entities.PartialTemplateExample.TEST_PARTIAL1));
  }

  @Test
  public void testGetPartialTemplateNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getPartialTemplate(TEST_PARTIAL1_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testDeletePartialTemplateSuccess() throws IOException {
    adminClient.deletePartialTemplate(TEST_PARTIAL1_NAME);
  }

  @Test
  public void testDeletePartialTemplateNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.deletePartialTemplate(TEST_PARTIAL1_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllProvidersSuccess() throws IOException {
    List<Provider> result = adminClient.getAllProviders();
    assertTrue(result.contains(Entities.ProviderExample.JOYENT));
    assertTrue(result.contains(Entities.ProviderExample.RACKSPACE));
  }

  @Test
  public void testGetAllProvidersInternalError() throws IOException {
    clientManager = createClientManager(TestStatusUserId.INTERNAL_SERVER_ERROR_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getAllProviders();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
    }
  }

  @Test
  public void testGetProviderSuccess() throws IOException {
    Provider result = adminClient.getProvider(JOYENT_PROVIDER_NAME);
    assertTrue(result.equals(Entities.ProviderExample.JOYENT));
  }

  @Test
  public void testGetProviderConflict() throws IOException {
    clientManager = createClientManager(TestStatusUserId.CONFLICT_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getProvider(JOYENT_PROVIDER_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteProviderSuccess() throws IOException {
    adminClient.deleteProvider(JOYENT_PROVIDER_NAME);
  }

  @Test
  public void testDeleteProviderNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.deleteProvider(JOYENT_PROVIDER_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllServicesSuccess() throws IOException {
    List<Service> result = adminClient.getAllServices();
    assertTrue(result.contains(Entities.ServiceExample.DATANODE));
    assertTrue(result.contains(Entities.ServiceExample.HOSTS));
    assertTrue(result.contains(Entities.ServiceExample.NAMENODE));
  }

  @Test
  public void testGetAllServicesForbidden() throws IOException {
    clientManager = createClientManager(TestStatusUserId.FORBIDDEN_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getAllServices();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, e.getStatusCode());
    }
  }

  @Test
  public void testGetServiceSuccess() throws IOException {
    Service result = adminClient.getService(DATA_NODE_SERVICE_NAME);
    assertTrue(result.equals(Entities.ServiceExample.DATANODE));
  }

  @Test
  public void testGetServiceMethodNotAllowed() throws IOException {
    clientManager = createClientManager(TestStatusUserId.METHOD_NOT_ALLOWED_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getService(DATA_NODE_SERVICE_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteServiceSuccess() throws IOException {
    adminClient.deleteService(DATA_NODE_SERVICE_NAME);
  }

  @Test
  public void testDeleteServiceNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.deleteService(DATA_NODE_SERVICE_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllHardwareTypesSuccess() throws IOException {
    List<HardwareType> result = adminClient.getAllHardwareTypes();
    assertTrue(result.contains(Entities.HardwareTypeExample.LARGE));
    assertTrue(result.contains(Entities.HardwareTypeExample.MEDIUM));
    assertTrue(result.contains(Entities.HardwareTypeExample.SMALL));
  }

  @Test
  public void testGetAllHardwareTypesUnauthorized() throws IOException {
    clientManager = createClientManager(TestStatusUserId.UNAUTHORIZED_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getAllHardwareTypes();
      Assert.fail("Expected UnauthorizedAccessTokenException");
    } catch (UnauthorizedAccessTokenException ignored) {
    }
  }

  @Test
  public void testGetHardwareTypeSuccess() throws IOException {
    HardwareType result = adminClient.getHardwareType(LARGE_HARDWARE_TYPE_NAME);
    assertTrue(result.equals(Entities.HardwareTypeExample.LARGE));
  }

  @Test
  public void testGetHardwareTypeBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getHardwareType(LARGE_HARDWARE_TYPE_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteHardwareTypeSuccess() throws IOException {
    adminClient.deleteHardwareType(LARGE_HARDWARE_TYPE_NAME);
  }

  @Test
  public void testDeleteHardwareTypeNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.deleteHardwareType(LARGE_HARDWARE_TYPE_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllImageTypesSuccess() throws IOException {
    List<ImageType> result = adminClient.getAllImageTypes();
    assertTrue(result.contains(Entities.ImageTypeExample.CENTOS_6));
    assertTrue(result.contains(Entities.ImageTypeExample.UBUNTU_12));
  }

  @Test
  public void testGetAllImageTypesBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.getAllImageTypes();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetImageTypeSuccess() throws IOException {
    ImageType result = adminClient.getImageType(CENTOS_6_IMAGE_TYPE_NAME);
    assertTrue(result.equals(Entities.ImageTypeExample.CENTOS_6));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetImageTypeNotImplemented() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_IMPLEMENTED_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    adminClient.getImageType(CENTOS_6_IMAGE_TYPE_NAME);
  }

  @Test
  public void testDeleteImageTypeSuccess() throws IOException {
    adminClient.deleteImageType(CENTOS_6_IMAGE_TYPE_NAME);
  }

  @Test
  public void testDeleteImageTypeNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    adminClient = clientManager.getAdminClient();
    try {
      adminClient.deleteImageType(CENTOS_6_IMAGE_TYPE_NAME);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @After
  public void shutDown() throws Exception {
    super.shutDown();
  }
}
