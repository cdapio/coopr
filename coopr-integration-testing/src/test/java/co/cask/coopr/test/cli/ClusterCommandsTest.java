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

package co.cask.coopr.test.cli;

import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.Entities;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.test.Constants;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Set;

public class ClusterCommandsTest extends AbstractTest {

  public static final String EXPECTED_NEW_CLUSTER_NAME = "new";
  public static final String NEW_CLUSTER_ID = "00000003";

  @BeforeClass
  public static void beforeClass()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException {
    createCli(ADMIN_ACCOUNT);
  }

  @Test
  public void testListClusters() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_CLUSTERS_COMMAND);
    Set<ClusterSummary> result = getSetFromOutput(new TypeToken<Set<ClusterSummary>>() {}.getType());
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
  public void testGetCluster() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_CLUSTER_COMMAND, FIRST_TEST_CLUSTER_ID);
    execute(command);
    ClusterDetails result = getObjectFromOutput(ClusterDetails.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(ClusterAction.CLUSTER_CREATE, result.getProgress().getAction());
    for (Node node : result.getNodes()) {
      Assert.assertTrue(FIRST_TEST_CLUSTER.getNodeIDs().contains(node.getId()));
    }
    Assert.assertEquals(FIRST_TEST_CLUSTER, result.getCluster());
  }

  @Test
  public void testDeleteCluster() throws IOException, InvalidCommandException {
    String getCommand = String.format(Constants.GET_CLUSTER_COMMAND, SECOND_TEST_CLUSTER_ID);
    String deleteCommand = String.format(Constants.DELETE_CLUSTER_COMMAND, SECOND_TEST_CLUSTER_ID);

    execute(getCommand);
    ClusterDetails result = getObjectFromOutput(ClusterDetails.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(ClusterAction.CLUSTER_CREATE, result.getProgress().getAction());

    OUTPUT_STREAM.reset();

    execute(deleteCommand);
    execute(getCommand);
    result = getObjectFromOutput(ClusterDetails.class);
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE, result.getProgress().getAction());
  }

  @Test
  public void testCreateCluster() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_CLUSTER_COMMAND, NEW_CLUSTER_ID);
    String command = String.format(Constants.CREATE_CLUSTER_COMMAND,
                                   EXPECTED_NEW_CLUSTER_NAME, Entities.ClusterTemplateExample.REACTOR.getName(), 10);
    execute(command);

    execute(getCommand);
    ClusterDetails createdCluster = getObjectFromOutput(ClusterDetails.class);
    Assert.assertNotNull(createdCluster);
    Assert.assertEquals(ClusterAction.CLUSTER_CREATE, createdCluster.getProgress().getAction());
  }

  @Test
  public void testCreateClusterInvalidNumMachinesValue() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.CREATE_CLUSTER_COMMAND,
                                   EXPECTED_NEW_CLUSTER_NAME, Entities.ClusterTemplateExample.REACTOR.getName(),
                                   Integer.MAX_VALUE);
    execute(command);
    checkError();
  }

  @Test
  public void testGetClusterStatus() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_CLUSTER_STATUS_COMMAND, FIRST_TEST_CLUSTER_ID);
    execute(command);
    ClusterStatusResponse result = getObjectFromOutput(ClusterStatusResponse.class);
    Assert.assertEquals(Cluster.Status.ACTIVE, result.getStatus());
  }

  @Test
  public void testGetClusterConfig() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_CLUSTER_CONFIG_COMMAND, FIRST_TEST_CLUSTER_ID);
    execute(command);
    JsonObject jsonObject = getObjectFromOutput(JsonObject.class);
    Assert.assertEquals("value1", jsonObject.get("property1").getAsString());
    Assert.assertEquals("value2", jsonObject.get("property2").getAsString());
    Assert.assertEquals("value3", jsonObject.get("property3").getAsString());
  }

  @Test
  public void testSetClusterConfig() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_CLUSTER_CONFIG_COMMAND, FIRST_TEST_CLUSTER_ID);

    execute(getCommand);
    JsonObject config = getObjectFromOutput(JsonObject.class);
    config.addProperty("property4", "value4");

    OUTPUT_STREAM.reset();

    ClusterConfigureRequest clusterConfigureRequest = new ClusterConfigureRequest(null, config, false);
    String setCommand = String.format(Constants.SET_CLUSTER_CONFIG_COMMAND, getJsonFromObject(clusterConfigureRequest),
                                      FIRST_TEST_CLUSTER_ID);
    execute(setCommand);
    execute(getCommand);

    config = getObjectFromOutput(JsonObject.class);
    Assert.assertEquals("value1", config.get("property1").getAsString());
    Assert.assertEquals("value2", config.get("property2").getAsString());
    Assert.assertEquals("value3", config.get("property3").getAsString());
    Assert.assertEquals("value4", config.get("property4").getAsString());
  }

  @Test
  public void testGetClusterServices() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.LIST_CLUSTER_SERVICES_COMMAND, SECOND_TEST_CLUSTER_ID);
    execute(command);
    Set<String> result = getSetFromOutput(new TypeToken<Set<String>>() {}.getType());

    Set<String> expectedResult = Sets.newHashSet(Entities.ServiceExample.DATANODE.getName(),
                                                 Entities.ServiceExample.NAMENODE.getName(),
                                                 Entities.ServiceExample.HOSTS.getName());
    Assert.assertEquals(expectedResult, result);
  }

  @Test
  public void testSyncClusterTemplate() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format("sync cluster template \"%s\"", SECOND_TEST_CLUSTER_ID);
    execute(command);
    checkEmptyOutput();
  }

  @Test
  public void testSyncClusterTemplateIncompatibilityServices()
    throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.SYNC_CLUSTER_TEMPLATE_COMMAND, FIRST_TEST_CLUSTER_ID);
    execute(command);
    checkError();
  }

  @Test
  public void testSyncClusterTemplateClusterWithoutNodes()
    throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.SYNC_CLUSTER_TEMPLATE_COMMAND, THIRD_TEST_CLUSTER_ID);
    execute(command);
    checkError();
  }

  @Test
  public void testSetClusterExpireTime() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.SET_CLUSTER_EXPIRE_TIME_COMMAND, 10, THIRD_TEST_CLUSTER_ID);
    execute(command);
    checkEmptyOutput();
  }

  @Test
  public void testSetClusterExpireTimeInvalidValue() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.SET_CLUSTER_EXPIRE_TIME_COMMAND, -1, THIRD_TEST_CLUSTER_ID);
    execute(command);
    checkError();
  }

  @Test
  public void testStartServiceOnCluster() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.START_SERVICE_ON_CLUSTER_COMMAND,
                                   Entities.ServiceExample.DATANODE.getName(), SECOND_TEST_CLUSTER_ID);
    execute(command);
    checkEmptyOutput();
  }

  @Test
  public void testStartServiceOnClusterUnknownService() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.START_SERVICE_ON_CLUSTER_COMMAND, "test", SECOND_TEST_CLUSTER_ID);
    execute(command);
    checkError();
  }

  @Test
  public void testStopServiceOnCluster() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.STOP_SERVICE_ON_CLUSTER_COMMAND,
                                   Entities.ServiceExample.DATANODE.getName(), SECOND_TEST_CLUSTER_ID);
    execute(command);
    checkEmptyOutput();
  }

  @Test
  public void testRestartServiceOnCluster() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.RESTART_SERVICE_ON_CLUSTER_COMMAND,
                                   Entities.ServiceExample.DATANODE.getName(), SECOND_TEST_CLUSTER_ID);
    execute(command);
    checkEmptyOutput();
  }

  @Test
  public void testAddServicesOnClusterSuccess() throws InvalidCommandException, UnsupportedEncodingException {
    String getClusterCommand = String.format(Constants.GET_CLUSTER_COMMAND, FIRST_TEST_CLUSTER_ID);
    AddServicesRequest addServicesRequest = new AddServicesRequest(null, Sets.newHashSet("zookeeper"));
    String command = String.format(Constants.ADD_SERVICES_ON_CLUSTER_COMMAND,
                                   getJsonFromObject(addServicesRequest), FIRST_TEST_CLUSTER_ID);
    execute(command);

    OUTPUT_STREAM.reset();

    execute(getClusterCommand);
    ClusterDetails result = getObjectFromOutput(ClusterDetails.class);
    Assert.assertEquals(ClusterAction.ADD_SERVICES, result.getProgress().getAction());
  }

  @Test
  public void testAddServicesOnClusterNotExistingService()
    throws InvalidCommandException, UnsupportedEncodingException {
    AddServicesRequest addServicesRequest = new AddServicesRequest(null, Sets.newHashSet("test"));
    String command = String.format(Constants.ADD_SERVICES_ON_CLUSTER_COMMAND,
                                   getJsonFromObject(addServicesRequest), FIRST_TEST_CLUSTER_ID);
    execute(command);
    checkError();
  }
}
