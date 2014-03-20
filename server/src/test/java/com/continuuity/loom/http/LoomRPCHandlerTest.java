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

import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.scheduler.Scheduler;
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

/**
 *
 */
public class LoomRPCHandlerTest extends LoomServiceTestBase {
  private static Gson GSON = new JsonSerde().getGson();
  private static ClusterTemplate smallTemplate;
  private static JsonObject defaultClusterConfig;

  @BeforeClass
  public static void init() throws Exception {
    // We don't need scheduler to run for these test cases, we'll run them manually due to timing issues.
    Scheduler scheduler = injector.getInstance(Scheduler.class);
    scheduler.stopAndWait();
  }

  @BeforeClass
  public static void initData() throws Exception {
    defaultClusterConfig = new JsonObject();
    defaultClusterConfig.addProperty("defaultconfig", "value1");

    smallTemplate =  new ClusterTemplate("one-machine",
                                         "one machine cluster template",
                                         new ClusterDefaults(ImmutableSet.of("zookeeper"), "rackspace", null, null,
                                                             defaultClusterConfig),
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
    JsonObject clusterRequest = LoomClusterHandlerTest.createClusterRequest("cluster1", "my 1st cluster",
                                                                            smallTemplate.getName(), 5);
    HttpResponse creationResponse = doPost("/v1/loom/clusters", clusterRequest.toString(), USER1_HEADERS);
    assertResponseStatus(creationResponse, HttpResponseStatus.OK);
    String cluster1Id = LoomClusterHandlerTest.getIdFromResponse(creationResponse);
    LoomClusterHandlerTest.assertStatus(cluster1Id, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                        ClusterAction.SOLVE_LAYOUT, 0, 0, USER1_HEADERS);

    clusterRequest = LoomClusterHandlerTest.createClusterRequest("cluster2", "my 2nd cluster",
                                                                 smallTemplate.getName(), 6);

    creationResponse = doPost("/v1/loom/clusters", clusterRequest.toString(), USER1_HEADERS);
    assertResponseStatus(creationResponse, HttpResponseStatus.OK);
    String cluster2Id = LoomClusterHandlerTest.getIdFromResponse(creationResponse);
    LoomClusterHandlerTest.assertStatus(cluster2Id, Cluster.Status.PENDING, "NOT_SUBMITTED",
                                        ClusterAction.SOLVE_LAYOUT, 0, 0, USER1_HEADERS);

    clusterRequest = LoomClusterHandlerTest.createClusterRequest("cluster3", "my 3rd cluster",
                                                                 smallTemplate.getName(), 6);

    creationResponse = doPost("/v1/loom/clusters", clusterRequest.toString(), USER2_HEADERS);
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
}
