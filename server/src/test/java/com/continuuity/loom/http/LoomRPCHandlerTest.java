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
package com.continuuity.loom.http;

import com.continuuity.loom.Entities;
import com.continuuity.loom.TestHelper;
import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.layout.ClusterCreateRequest;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.Scheduler;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

/**
 *
 */
public class LoomRPCHandlerTest extends LoomServiceTestBase {
  private static Gson GSON = new JsonSerde().getGson();
  private static ClusterTemplate smallTemplate;

  @BeforeClass
  public static void init() throws Exception {
    // We don't need scheduler to run for these test cases, we'll run them manually due to timing issues.
    Scheduler scheduler = injector.getInstance(Scheduler.class);
    scheduler.stopAndWait();
  }

  @BeforeClass
  public static void initData() throws Exception {
    JsonObject defaultClusterConfig = new JsonObject();
    defaultClusterConfig.addProperty("defaultconfig", "value1");

    smallTemplate =  new ClusterTemplate("one-machine",
                                         "one machine cluster template",
                                         new ClusterDefaults(ImmutableSet.of("zookeeper"), "rackspace", null, null,
                                                             null, defaultClusterConfig),
                                         new Compatibilities(null, null, ImmutableSet.of("zookeeper")),
                                         null, new Administration(new LeaseDuration(10000, 30000, 5000)));

    entityStore.writeClusterTemplate(smallTemplate);
  }

  @After
  public void testCleanup() {
    // cleanup
    solverQueue.removeAll();
    clusterQueue.removeAll();
  }

  @Test
  public void testGetAllStatusesFunction() throws Exception {
    // create the clusters
    ClusterCreateRequest clusterCreateRequest = LoomClusterHandlerTest.createClusterRequest("cluster1", "my 1st cluster",
                                                                                 smallTemplate.getName(), 5);
    HttpResponse creationResponse = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(creationResponse, HttpResponseStatus.OK);
    String cluster1Id = LoomClusterHandlerTest.getIdFromResponse(creationResponse);
    LoomClusterHandlerTest.assertStatus(cluster1Id, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                        ClusterAction.SOLVE_LAYOUT, 0, 0, USER1_HEADERS);

    clusterCreateRequest = LoomClusterHandlerTest.createClusterRequest("cluster2", "my 2nd cluster",
                                                                 smallTemplate.getName(), 6);

    creationResponse = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(creationResponse, HttpResponseStatus.OK);
    String cluster2Id = LoomClusterHandlerTest.getIdFromResponse(creationResponse);
    LoomClusterHandlerTest.assertStatus(cluster2Id, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                        ClusterAction.SOLVE_LAYOUT, 0, 0, USER1_HEADERS);

    clusterCreateRequest = LoomClusterHandlerTest.createClusterRequest("cluster3", "my 3rd cluster",
                                                                 smallTemplate.getName(), 6);

    creationResponse = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER2_HEADERS);
    assertResponseStatus(creationResponse, HttpResponseStatus.OK);
    String cluster3Id = LoomClusterHandlerTest.getIdFromResponse(creationResponse);

    LoomClusterHandlerTest.assertStatus(cluster3Id, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                        ClusterAction.SOLVE_LAYOUT, 0, 0, USER2_HEADERS);

    // test user 1, two clusters
    HttpResponse statusCheckResponse = doPost("/v1/loom/getClusterStatuses", "", USER1_HEADERS);
    assertResponseStatus(statusCheckResponse, HttpResponseStatus.OK);
    String user1StatusResponseStr = EntityUtils.toString(statusCheckResponse.getEntity());
    JsonObject[] jsonList = GSON.fromJson(user1StatusResponseStr, JsonObject[].class);
    Assert.assertEquals(2, jsonList.length);
    for (JsonObject aJsonList : jsonList) {
      LoomClusterHandlerTest.assertStatus(aJsonList, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    }

    // test user 2, one cluster
    statusCheckResponse = doPost("/v1/loom/getClusterStatuses", "", USER2_HEADERS);
    assertResponseStatus(statusCheckResponse, HttpResponseStatus.OK);
    String user2StatusResponseStr = EntityUtils.toString(statusCheckResponse.getEntity());
    jsonList = GSON.fromJson(user2StatusResponseStr, JsonObject[].class);
    Assert.assertEquals(1, jsonList.length);
    for (JsonObject aJsonList : jsonList) {
      LoomClusterHandlerTest.assertStatus(aJsonList, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    }

    // test admin, three clusters
    statusCheckResponse = doPost("/v1/loom/getClusterStatuses", "", ADMIN_HEADERS);
    assertResponseStatus(statusCheckResponse, HttpResponseStatus.OK);
    String adminStatusResponseStr = EntityUtils.toString(statusCheckResponse.getEntity());
    jsonList = GSON.fromJson(adminStatusResponseStr, JsonObject[].class);
    Assert.assertEquals(3, jsonList.length);
    for (JsonObject aJsonList : jsonList) {
      LoomClusterHandlerTest.assertStatus(aJsonList, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    }
  }

  @Test
  public void testInvalidGetNodePropertiesReturns400() throws Exception {
    // not a json object
    assertResponseStatus(doPost("/v1/loom/getNodeProperties", "body", USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // no cluster id
    JsonObject requestBody = new JsonObject();
    assertResponseStatus(doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // bad cluster id
    requestBody = new JsonObject();
    requestBody.add("clusterId", new JsonObject());
    assertResponseStatus(doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // bad properties
    requestBody = new JsonObject();
    requestBody.addProperty("properties", "prop1,prop2");
    assertResponseStatus(doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    // bad services
    requestBody = new JsonObject();
    requestBody.addProperty("services", "service1,service2");
    assertResponseStatus(doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testGetNodeProperties() throws Exception {
    // setup data, 4 node cluster
    Service svcA =
      new Service("svcA", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Service svcB =
      new Service("svcB", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Service svcC =
      new Service("svcC", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of());
    String hostnameProperty = Node.Properties.HOSTNAME.name().toLowerCase();
    String ipProperty = Node.Properties.IPADDRESS.name().toLowerCase();
    Node nodeA = new Node("nodeA", "123", ImmutableSet.of(svcA),
                          ImmutableMap.of(ipProperty, "123.456.0.1",
                                          hostnameProperty, "testcluster-1-1000.local"));
    Node nodeAB = new Node("nodeAB", "123", ImmutableSet.of(svcA, svcB),
                           ImmutableMap.of(ipProperty, "123.456.0.2",
                                           hostnameProperty, "testcluster-1-1001.local"));
    Node nodeABC = new Node("nodeABC", "123", ImmutableSet.of(svcA, svcB, svcC),
                            ImmutableMap.of(ipProperty, "123.456.0.3",
                                            hostnameProperty, "testcluster-1-1002.local"));
    Node nodeBC = new Node("nodeBC", "123", ImmutableSet.of(svcB, svcC),
                           ImmutableMap.of(ipProperty, "123.456.0.4",
                                           hostnameProperty, "testcluster-1-1003.local"));
    Cluster cluster = new Cluster("123", USER1, "testcluster", System.currentTimeMillis(), "description",
                                  Entities.ProviderExample.RACKSPACE, smallTemplate,
                                  ImmutableSet.of(nodeA.getId(), nodeAB.getId(), nodeABC.getId(), nodeBC.getId()),
                                  ImmutableSet.of(svcA.getName(), svcB.getName(), svcC.getName()));
    clusterStore.writeCluster(cluster);
    clusterStore.writeNode(nodeA);
    clusterStore.writeNode(nodeAB);
    clusterStore.writeNode(nodeABC);
    clusterStore.writeNode(nodeBC);

    // test with nonexistant cluster
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("clusterId", "123" + cluster.getId());
    HttpResponse response = doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    JsonObject responseBody = getJsonObjectBodyFromResponse(response);
    Assert.assertTrue(responseBody.entrySet().isEmpty());

    // test with unowned cluster
    requestBody.addProperty("clusterId", cluster.getId());
    response = doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER2_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    Assert.assertTrue(responseBody.entrySet().isEmpty());

    // test without any filters
    response = doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    JsonObject expected = new JsonObject();
    expected.add(nodeA.getId(), nodeA.getProperties());
    expected.add(nodeAB.getId(), nodeAB.getProperties());
    expected.add(nodeABC.getId(), nodeABC.getProperties());
    expected.add(nodeBC.getId(), nodeBC.getProperties());
    Assert.assertEquals(expected, responseBody);

    // test with filter on service A
    requestBody.add("services", TestHelper.jsonArrayOf(svcA.getName()));
    response = doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    expected = new JsonObject();
    Set<Node> expectedNodes = ImmutableSet.of(nodeA, nodeAB, nodeABC);
    for (Node expectedNode : expectedNodes) {
      expected.add(expectedNode.getId(), expectedNode.getProperties());
    }
    Assert.assertEquals(expected, responseBody);

    // test with filter on service A and property list
    requestBody.add("properties", TestHelper.jsonArrayOf(hostnameProperty, ipProperty));
    response = doPost("/v1/loom/getNodeProperties", requestBody.toString(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseBody = getJsonObjectBodyFromResponse(response);
    expected = new JsonObject();
    expectedNodes = ImmutableSet.of(nodeA, nodeAB, nodeABC);
    for (Node expectedNode : expectedNodes) {
      JsonObject value = new JsonObject();
      value.addProperty(hostnameProperty, expectedNode.getProperties().get(hostnameProperty).getAsString());
      value.addProperty(ipProperty, expectedNode.getProperties().get(ipProperty).getAsString());
      expected.add(expectedNode.getId(), value);
    }
    Assert.assertEquals(expected, responseBody);
  }

  private JsonObject getJsonObjectBodyFromResponse(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    return GSON.fromJson(reader, JsonObject.class);
  }
}
