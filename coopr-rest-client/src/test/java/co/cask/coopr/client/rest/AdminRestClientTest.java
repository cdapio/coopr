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

import co.cask.coopr.client.AdminClient;
import co.cask.coopr.client.Entities;
import co.cask.coopr.client.rest.exception.HttpFailureException;
import co.cask.coopr.client.rest.handler.TestStatusUserId;
import co.cask.coopr.spec.template.ClusterTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;


public class AdminRestClientTest extends RestClientTest {

  private AdminClient adminClient;

  public static final String HADOOP_DISTRIBUTED_NAME = "hadoop-distributed";

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

  @Test(expected = HttpFailureException.class)
  public void testGetAllClusterTemplatesBadRequest() throws IOException {
    clientManager = RestClientManager.builder(testServerHost, testServerPort)
      .userId(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue())
      .tenantId(TEST_TENANT_ID)
      .build();
    adminClient = clientManager.getAdminClient();
    adminClient.getAllClusterTemplates();
  }

  @Test
  public void testGetClusterTemplateSuccess() throws IOException {
    ClusterTemplate result = adminClient.getClusterTemplate(HADOOP_DISTRIBUTED_NAME);
    assertTrue(result.equals(Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED));
  }

  @After
  public void shutDown() throws Exception {
    super.shutDown();
  }
}
