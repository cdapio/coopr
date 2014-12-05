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
import co.cask.coopr.client.ClusterClient;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.test.client.ClientTest;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ClusterRestClientTest extends ClientTest {

  public static final String EXPECTED_NEW_CLUSTER_NAME = "new";

  private ClusterClient clusterClient;

  @Before
  public void setUpt() throws Exception {
    clusterClient = adminClientManager.getClusterClient();
  }

  @Test
  public void testGetClusters() throws IOException {
    List<ClusterSummary> result = clusterClient.getClusters();
    Assert.assertEquals(2, result.size());
    for (ClusterSummary clusterSummary : result) {
      if (FIRST_TEST_CLUSTER_ID.equals(clusterSummary.getId())) {
        Assert.assertEquals(BaseEntity.from(FIRST_TEST_CLUSTER.getClusterTemplate()),
                     clusterSummary.getClusterTemplate());
        Assert.assertEquals(FIRST_TEST_CLUSTER.getName(), clusterSummary.getName());
      } else if (SECOND_TEST_CLUSTER_ID.equals(clusterSummary.getId())) {
        Assert.assertEquals(BaseEntity.from(SECOND_TEST_CLUSTER.getClusterTemplate()),
                     clusterSummary.getClusterTemplate());
        Assert.assertEquals(SECOND_TEST_CLUSTER.getName(), clusterSummary.getName());
      } else {
        Assert.fail("Unexpected ClusterSummery object found.");
      }
    }
  }

  @Test
  public void testGetCluster() throws IOException {
    ClusterDetails result = clusterClient.getCluster(FIRST_TEST_CLUSTER_ID);
    Assert.assertNotNull(result);
    Assert.assertEquals(ClusterAction.CLUSTER_CREATE, result.getProgress().getAction());
    for (Node node : result.getNodes()) {
      Assert.assertTrue(FIRST_TEST_CLUSTER.getNodeIDs().contains(node.getId()));
    }
    Assert.assertEquals(FIRST_TEST_CLUSTER, result.getCluster());
  }

  @Test
  public void testDeleteCluster() throws IOException {
    ClusterDetails result = clusterClient.getCluster(SECOND_TEST_CLUSTER_ID);
    Assert.assertNotNull(result);
    Assert.assertEquals(ClusterAction.CLUSTER_CREATE, result.getProgress().getAction());

    clusterClient.deleteCluster(SECOND_TEST_CLUSTER_ID);

    result = clusterClient.getCluster(SECOND_TEST_CLUSTER_ID);
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE, result.getProgress().getAction());
  }

  @Test
  public void testCreateCluster() throws IOException {
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(EXPECTED_NEW_CLUSTER_NAME)
      .setClusterTemplateName(Entities.ClusterTemplateExample.REACTOR.getName())
      .setNumMachines(10)
      .setDNSSuffix("ro.test.com")
      .build();
    String newClusterId = clusterClient.createCluster(createRequest);
    Assert.assertNotNull(newClusterId);

    ClusterDetails createdCluster = clusterClient.getCluster(newClusterId);
    Assert.assertNotNull(createdCluster);
    Assert.assertEquals(ClusterAction.SOLVE_LAYOUT, createdCluster.getProgress().getAction());
  }

  @Test
  public void testCreateClusterInvalidNumMachinesValue() throws IOException {
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(EXPECTED_NEW_CLUSTER_NAME)
      .setClusterTemplateName(Entities.ClusterTemplateExample.REACTOR.getName())
      .setNumMachines(Integer.MAX_VALUE)
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
  public void testGetClusterStatus() throws IOException {
    ClusterStatusResponse result = clusterClient.getClusterStatus(FIRST_TEST_CLUSTER_ID);
    Assert.assertEquals(Cluster.Status.ACTIVE, result.getStatus());
  }

  @Test
  public void testGetClusterConfig() throws IOException {
    JsonObject jsonObject = clusterClient.getClusterConfig(FIRST_TEST_CLUSTER_ID);
    Assert.assertEquals("value1", jsonObject.get("property1").getAsString());
    Assert.assertEquals("value2", jsonObject.get("property2").getAsString());
    Assert.assertEquals("value3", jsonObject.get("property3").getAsString());
  }

  @Test
  public void testSetClusterConfig() throws IOException {
    JsonObject config = clusterClient.getClusterConfig(FIRST_TEST_CLUSTER_ID);
    config.addProperty("property4", "value4");

    ClusterConfigureRequest clusterConfigureRequest = new ClusterConfigureRequest(null, config, false);
    clusterClient.setClusterConfig(FIRST_TEST_CLUSTER_ID, clusterConfigureRequest);

    config = clusterClient.getClusterConfig(FIRST_TEST_CLUSTER_ID);
    Assert.assertEquals("value1", config.get("property1").getAsString());
    Assert.assertEquals("value2", config.get("property2").getAsString());
    Assert.assertEquals("value3", config.get("property3").getAsString());
    Assert.assertEquals("value4", config.get("property4").getAsString());
  }

  @Test
  public void testGetClusterServices() throws IOException {
    List<String> result = clusterClient.getClusterServices(SECOND_TEST_CLUSTER_ID);

    Set<String> expectedResult = Sets.newHashSet(Entities.ServiceExample.DATANODE.getName(),
                                                 Entities.ServiceExample.NAMENODE.getName(),
                                                 Entities.ServiceExample.HOSTS.getName());
    Assert.assertEquals(expectedResult, Sets.newHashSet(result));
  }

  @Test
  public void testSyncClusterTemplate() throws IOException {
    clusterClient.syncClusterTemplate(SECOND_TEST_CLUSTER_ID);
  }

  @Test
  public void testSyncClusterTemplateIncompatibilityServices() throws IOException {
    try {
      clusterClient.syncClusterTemplate(FIRST_TEST_CLUSTER_ID);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testSyncClusterTemplateClusterWithoutNodes() throws IOException {
    try {
      clusterClient.syncClusterTemplate(THIRD_TEST_CLUSTER_ID);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testSetClusterExpireTime() throws IOException {
    clusterClient.setClusterExpireTime(THIRD_TEST_CLUSTER_ID, 10);
  }

  @Test
  public void testSetClusterExpireTimeInvalidValue() throws IOException {
    try {
      clusterClient.setClusterExpireTime(THIRD_TEST_CLUSTER_ID, -1);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testStartServiceOnCluster() throws IOException {
    clusterClient.startServiceOnCluster(SECOND_TEST_CLUSTER_ID,
                                        Entities.ServiceExample.DATANODE.getName());
  }

  @Test
  public void testStartServiceOnClusterUnknownService() throws IOException {
    try {
      clusterClient.startServiceOnCluster(SECOND_TEST_CLUSTER_ID, "test");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testStopServiceOnCluster() throws IOException {
    clusterClient.stopServiceOnCluster(SECOND_TEST_CLUSTER_ID,
                                       Entities.ServiceExample.DATANODE.getName());
  }

  @Test
  public void testRestartServiceOnCluster() throws IOException {
    clusterClient.restartServiceOnCluster(SECOND_TEST_CLUSTER_ID,
                                          Entities.ServiceExample.DATANODE.getName());
  }

  @Test
  public void testAddServicesOnClusterSuccess() throws IOException {
    AddServicesRequest addServicesRequest = new AddServicesRequest(null, Sets.newHashSet("zookeeper"));
    clusterClient.addServicesOnCluster(FIRST_TEST_CLUSTER_ID, addServicesRequest);

    ClusterDetails result = clusterClient.getCluster(FIRST_TEST_CLUSTER_ID);
    Assert.assertEquals(ClusterAction.ADD_SERVICES, result.getProgress().getAction());
  }

  @Test
  public void testAddServicesOnClusterNotExistingService() throws IOException {
    AddServicesRequest addServicesRequest = new AddServicesRequest(null, Sets.newHashSet("test"));
    try {
      clusterClient.addServicesOnCluster(FIRST_TEST_CLUSTER_ID, addServicesRequest);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }
}
