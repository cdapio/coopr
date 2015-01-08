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
import co.cask.coopr.TestHelper;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.LeaseDuration;
import co.cask.coopr.store.entity.SQLEntityStoreService;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

/**
 *
 */
public class RPCHandlerTest extends ServiceTestBase {
  private static ClusterTemplate smallTemplate;

  @BeforeClass
  public static void initData() throws Exception {
    JsonObject defaultClusterConfig = new JsonObject();
    defaultClusterConfig.addProperty("defaultconfig", "value1");

    smallTemplate = ClusterTemplate.builder()
      .setName("one-machine")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("zookeeper")
          .setProvider("rackspace")
          .setConfig(defaultClusterConfig)
          .build())
      .setCompatibilities(Compatibilities.builder().setServices("zookeeper").build())
      .setAdministration(new Administration(LeaseDuration.of("10s", "30s", "5s")))
      .build();
  }

  @Before
  public void setupTest() throws Exception {
    entityStoreService.getView(ADMIN_ACCOUNT).writeClusterTemplate(smallTemplate);
  }

  @After
  public void testCleanup() throws Exception {
    // cleanup
    solverQueues.removeAll();
    clusterQueues.removeAll();
    ((SQLEntityStoreService) entityStoreService).clearData();
  }

  @Test
  public void testInvalidGetNodePropertiesReturns400() throws Exception {
    // not a json object
    assertResponseStatus(doPostExternalAPI("/getNodeProperties", "body", USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // no cluster id
    JsonObject requestBody = new JsonObject();
    assertResponseStatus(doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // bad cluster id
    requestBody = new JsonObject();
    requestBody.add("clusterId", new JsonObject());
    assertResponseStatus(doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // bad properties
    requestBody = new JsonObject();
    requestBody.addProperty("properties", "prop1,prop2");
    assertResponseStatus(doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // bad services
    requestBody = new JsonObject();
    requestBody.addProperty("services", "service1,service2");
    assertResponseStatus(doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testGetNodeProperties() throws Exception {
    // setup data, 4 node cluster
    Service svcA = Service.builder().setName("svcA").build();
    Service svcB = Service.builder().setName("svcB").build();
    Service svcC = Service.builder().setName("svcC").build();

    Node nodeA = new Node("nodeA", "123", ImmutableSet.of(svcA),
                          NodeProperties.builder()
                            .setHostname("testcluster-1-1000.local")
                            .addIPAddress("access_v4", "123.456.0.1").build());
    Node nodeAB = new Node("nodeAB", "123", ImmutableSet.of(svcA, svcB),
                           NodeProperties.builder()
                             .setHostname("testcluster-1-1001.local")
                             .addIPAddress("access_v4", "123.456.0.2").build());
    Node nodeABC = new Node("nodeABC", "123", ImmutableSet.of(svcA, svcB, svcC),
                            NodeProperties.builder()
                              .setHostname("testcluster-1-1002.local")
                              .addIPAddress("access_v4", "123.456.0.3").build());
    Node nodeBC = new Node("nodeBC", "123", ImmutableSet.of(svcB, svcC),
                           NodeProperties.builder()
                             .setHostname("testcluster-1-1003.local")
                             .addIPAddress("access_v4", "123.456.0.4").build());
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("testcluster")
      .setProvider(Entities.ProviderExample.RACKSPACE)
      .setClusterTemplate(smallTemplate)
      .setNodes(ImmutableSet.of(nodeA.getId(), nodeAB.getId(), nodeABC.getId(), nodeBC.getId()))
      .setServices(ImmutableSet.of(svcA.getName(), svcB.getName(), svcC.getName()))
      .build();
    clusterStoreService.getView(USER1_ACCOUNT).writeCluster(cluster);
    clusterStore.writeNode(nodeA);
    clusterStore.writeNode(nodeAB);
    clusterStore.writeNode(nodeABC);
    clusterStore.writeNode(nodeBC);

    // test with nonexistant cluster
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("clusterId", "123" + cluster.getId());
    HttpResponse response = doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    JsonObject responseBody = getJsonObjectBodyFromResponse(response);
    Assert.assertTrue(responseBody.entrySet().isEmpty());

    // test with unowned cluster
    requestBody.addProperty("clusterId", cluster.getId());
    response = doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER2_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    Assert.assertTrue(responseBody.entrySet().isEmpty());

    // test without any filters
    response = doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    JsonObject expected = new JsonObject();
    expected.add(nodeA.getId(), gson.toJsonTree(nodeA.getProperties()));
    expected.add(nodeAB.getId(), gson.toJsonTree(nodeAB.getProperties()));
    expected.add(nodeABC.getId(), gson.toJsonTree(nodeABC.getProperties()));
    expected.add(nodeBC.getId(), gson.toJsonTree(nodeBC.getProperties()));
    Assert.assertEquals(expected, responseBody);

    // test with filter on service A
    requestBody.add("services", TestHelper.jsonArrayOf(svcA.getName()));
    response = doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    expected = new JsonObject();
    Set<Node> expectedNodes = ImmutableSet.of(nodeA, nodeAB, nodeABC);
    for (Node expectedNode : expectedNodes) {
      expected.add(expectedNode.getId(), gson.toJsonTree(expectedNode.getProperties()));
    }
    Assert.assertEquals(expected, responseBody);

    // test with filter on service A and property list
    requestBody.add("properties", TestHelper.jsonArrayOf("hostname", "ipaddresses"));
    response = doPostExternalAPI("/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    expected = new JsonObject();
    expectedNodes = ImmutableSet.of(nodeA, nodeAB, nodeABC);
    for (Node expectedNode : expectedNodes) {
      JsonObject value = new JsonObject();
      value.addProperty("hostname", expectedNode.getProperties().getHostname());
      value.add("ipaddresses", gson.toJsonTree(expectedNode.getProperties().getIPAddresses()));
      expected.add(expectedNode.getId(), value);
    }
    Assert.assertEquals(expected, responseBody);
  }

  private JsonObject getJsonObjectBodyFromResponse(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    return gson.fromJson(reader, JsonObject.class);
  }
}
