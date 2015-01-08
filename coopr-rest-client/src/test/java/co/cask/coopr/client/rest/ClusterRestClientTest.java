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
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.client.rest.exception.UnauthorizedAccessTokenException;
import co.cask.coopr.client.rest.handler.TestStatusUserId;
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import co.cask.coopr.scheduler.ClusterAction;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClusterRestClientTest extends RestClientTest {

  public static final long EXPECTED_TEST_EXPIRE_TIME = 10000;
  public static final String UPDATED_CONFIG_CLUSTER_NAME = "updated";
  public static final String EXPECTED_NEW_CLUSTER_NAME = "new";
  public static final String TEST_CLUSTER_ID = "test";
  public static final String TEST_SERVICE_ID = "testService";

  private ClusterClient clusterClient;

  @Before
  public void setUpt() throws Exception {
    super.setUp();
    clusterClient = clientManager.getClusterClient();
  }

  @Test
  public void testGetClustersSuccess() throws IOException {
    List<ClusterSummary> result = clusterClient.getClusters();
    assertTrue(result.size() != 0);
    assertEquals(Entities.ClusterExample.createCluster().getName(), result.get(0).getName());
  }

  @Test
  public void testGetClustersBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    clusterClient = clientManager.getClusterClient();
    try {
      clusterClient.getClusters();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetClusterSuccess() throws IOException {
    ClusterDetails result = clusterClient.getCluster(TEST_CLUSTER_ID);
    assertEquals(Entities.ClusterExample.createCluster(), result.getCluster());
  }

  @Test
  public void testGetClusterNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    clusterClient = clientManager.getClusterClient();
    try {
      clusterClient.getCluster(TEST_CLUSTER_ID);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteClusterSuccess() throws IOException {
    clusterClient.deleteCluster(TEST_CLUSTER_ID);
  }

  @Test
  public void testCreateClusterSuccess() throws IOException {
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(EXPECTED_NEW_CLUSTER_NAME)
      .setClusterTemplateName(Entities.ClusterTemplateExample.REACTOR.toString())
      .setNumMachines(10)
      .setDNSSuffix("ro.test.com")
      .build();
    String newClusterId = clusterClient.createCluster(createRequest);
    assertEquals("testCluster", newClusterId);
  }

  @Test
  public void testCreateClusterFailed() throws IOException {
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName("Unexpected")
      .setClusterTemplateName(Entities.ClusterTemplateExample.REACTOR.toString())
      .setNumMachines(10)
      .setDNSSuffix("ro.test.com")
      .build();
    try {
      clusterClient.createCluster(createRequest);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetClusterStatusSuccess() throws IOException {
    ClusterStatusResponse result = clusterClient.getClusterStatus(TEST_CLUSTER_ID);
    assertEquals(Entities.ClusterExample.createCluster().getId(), result.getClusterid());
    assertEquals(ClusterAction.SOLVE_LAYOUT, result.getAction());
  }

  @Test
  public void testGetClusterStatusUnauthorized() throws IOException {
    clientManager = createClientManager(TestStatusUserId.UNAUTHORIZED_STATUS_USER_ID.getValue());
    clusterClient = clientManager.getClusterClient();
    try {
      clusterClient.getClusterStatus(TEST_CLUSTER_ID);
      Assert.fail("Expected UnauthorizedAccessTokenException");
    } catch (UnauthorizedAccessTokenException ignored) {
    }
  }

  @Test
  public void testGetClusterConfigSuccess() throws IOException {
    JsonObject jsonObject = clusterClient.getClusterConfig(TEST_CLUSTER_ID);
    assertEquals("test", jsonObject.get("name").getAsString());
    assertEquals(10000, jsonObject.get("expireTime").getAsLong());
    assertEquals(5, jsonObject.get("nodes").getAsInt());
  }

  @Test
  public void testSetClusterConfigSuccess() throws IOException {
    JsonObject testConfig = new JsonObject();
    testConfig.addProperty("name", UPDATED_CONFIG_CLUSTER_NAME);
    ClusterConfigureRequest clusterConfigureRequest = new ClusterConfigureRequest(null, testConfig, false);
    clusterClient.setClusterConfig(TEST_CLUSTER_ID, clusterConfigureRequest);
  }

  @Test
  public void testSetClusterConfigBadRequest() throws IOException {
    JsonObject testConfig = new JsonObject();
    testConfig.addProperty("name", "Unexpected");
    ClusterConfigureRequest clusterConfigureRequest = new ClusterConfigureRequest(null, testConfig, false);
    try {
      clusterClient.setClusterConfig(TEST_CLUSTER_ID, clusterConfigureRequest);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetClusterServicesSuccess() throws IOException {
    List<String> result = clusterClient.getClusterServices(TEST_CLUSTER_ID);
    assertEquals(Lists.newArrayList(Entities.ClusterExample.createCluster().getServices()), result);
  }

  @Test
  public void testSyncClusterTemplateSuccess() throws IOException {
    clusterClient.syncClusterTemplate(TEST_CLUSTER_ID);
  }

  @Test
  public void testSyncClusterTemplateNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    clusterClient = clientManager.getClusterClient();
    try {
      clusterClient.syncClusterTemplate(TEST_CLUSTER_ID);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testSetClusterExpireTimeSuccess() throws IOException {
    clusterClient.setClusterExpireTime(TEST_CLUSTER_ID, EXPECTED_TEST_EXPIRE_TIME);
  }

 @Test
  public void testSetClusterExpireTimeBadRequest() throws IOException {
   try {
     clusterClient.setClusterExpireTime(TEST_CLUSTER_ID, 10);
     Assert.fail("Expected HttpFailureException");
   } catch (HttpFailureException e) {
     Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
   }
  }

  @Test
  public void testStartServiceOnClusterSuccess() throws IOException {
    clusterClient.startServiceOnCluster(TEST_CLUSTER_ID, TEST_SERVICE_ID);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testStartServiceOnClusterNotImplemented() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_IMPLEMENTED_STATUS_USER_ID.getValue());
    clusterClient = clientManager.getClusterClient();
    clusterClient.startServiceOnCluster(TEST_CLUSTER_ID, TEST_SERVICE_ID);
  }

  @Test
  public void testStopServiceOnClusterSuccess() throws IOException {
    clusterClient.stopServiceOnCluster(TEST_CLUSTER_ID, TEST_SERVICE_ID);
  }

  @Test
  public void testRestartServiceOnClusterSuccess() throws IOException {
    clusterClient.restartServiceOnCluster(TEST_CLUSTER_ID, TEST_SERVICE_ID);
  }

  @Test
  public void testAddServicesOnClusterSuccess() throws IOException {
    AddServicesRequest addServicesRequest =
      new AddServicesRequest(null, Entities.ClusterExample.createCluster().getServices());
    clusterClient.addServicesOnCluster(TEST_CLUSTER_ID, addServicesRequest);
  }

  @Test
  public void testAddServicesOnClusterFailed() throws IOException {
    try {
      clusterClient.addServicesOnCluster(TEST_CLUSTER_ID, null);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @After
  public void shutDown() throws Exception {
    super.shutDown();
  }
}
