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
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.layout.ClusterCreateRequest;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.ClusterScheduler;
import com.continuuity.loom.scheduler.JobScheduler;
import com.continuuity.loom.scheduler.Scheduler;
import com.continuuity.loom.scheduler.SchedulerTest;
import com.continuuity.loom.scheduler.SolverRequest;
import com.continuuity.loom.scheduler.SolverScheduler;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.utils.ImmutablePair;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.http.Header;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LoomClusterHandlerTest extends LoomServiceTestBase {
  private static ClusterTemplate reactorTemplate;
  private static Gson GSON = new JsonSerde().getGson();
  private static ClusterTemplate smallTemplate;
  private static JsonObject defaultClusterConfig;
  private static JobScheduler jobScheduler;
  private static ClusterScheduler clusterScheduler;
  private static SolverScheduler solverScheduler;
  private static TrackingQueue jobQueue;

  @BeforeClass
  public static void init() {
    jobScheduler = injector.getInstance(JobScheduler.class);
    clusterScheduler = injector.getInstance(ClusterScheduler.class);
    solverScheduler = injector.getInstance(SolverScheduler.class);
    jobQueue = injector.getInstance(
      Key.get(TrackingQueue.class, Names.named("internal.job.queue")));

    // We don't need scheduler to run for these test cases, we'll run them manually due to timing issues.
    Scheduler scheduler = injector.getInstance(Scheduler.class);
    scheduler.stopAndWait();
  }

  @After
  public void testCleanup() {
    // cleanup
    solverQueue.removeAll();
    clusterQueue.removeAll();
  }

  @Test
  public void testAddCluster() throws Exception {
    String clusterName = "my-cluster";
    ClusterCreateRequest clusterCreateRequest = createClusterRequest(clusterName, "my cluster", reactorTemplate.getName(), 5);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    // check there was an element added to the cluster queue for creating this cluster
    Element element = solverQueue.take("0");
    Assert.assertEquals(clusterId, element.getId());
    ClusterCreateRequest expected =
      new ClusterCreateRequest(clusterName, "my cluster", reactorTemplate.getName(),
                               5, null, null, null, null, -1L, null, null);
    SolverRequest expectedSolverRequest = new SolverRequest(SolverRequest.Type.CREATE_CLUSTER, GSON.toJson(expected));
    Assert.assertEquals(expectedSolverRequest, GSON.fromJson(element.getValue(), SolverRequest.class));
  }

  @Test
  public void testAddClusterWithOptionalArgs() throws Exception {
    String clusterName = "my-cluster";
    ClusterCreateRequest clusterCreateRequest =
      new ClusterCreateRequest(clusterName, "my cluster", reactorTemplate.getName(), 5, "providerA",
                         ImmutableSet.of("service1", "service2"), "hardwareC", "imageB", -1L, null, null);

    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    // check there was an element added to the cluster queue for creating this cluster
    Element element = solverQueue.take("0");
    SolverRequest request = GSON.fromJson(element.getValue(), SolverRequest.class);
    ClusterCreateRequest createRequest = GSON.fromJson(request.getJsonRequest(), ClusterCreateRequest.class);
    Assert.assertEquals("providerA", createRequest.getProvider());
    Assert.assertEquals("imageB", createRequest.getImageType());
    Assert.assertEquals("hardwareC", createRequest.getHardwareType());
    Assert.assertEquals(ImmutableSet.of("service1", "service2"), createRequest.getServices());
  }

  @Test
  public void testUserConfig() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-with-user-config";

    JsonObject userConfig = new JsonObject();
    userConfig.addProperty("userconfig1", "value1");
    userConfig.addProperty("userconfig2", "value1");

    ClusterCreateRequest clusterCreateRequest = createClusterRequest(clusterName, "test cluster", smallTemplate
      .getName(), 1, userConfig);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();  // run scheduler put in queue
    jobScheduler.run();  // run scheduler take from queue

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");

    // Only user config should be present, not default cluster config.
    Assert.assertEquals(userConfig, object.get("config").getAsJsonObject().get("cluster").getAsJsonObject());
  }

  @Test
  public void testClusterStatus() throws Exception {
    List<String> actions = Lists.newArrayList();
    List<String> statuses = Lists.newArrayList();

    //Create cluster
    String clusterName = "test-cluster-should-be-provisioned";
    ClusterCreateRequest clusterCreateRequest = createClusterRequest(clusterName, "test cluster", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    //Take 3 tasks and finish them
    for (int i = 0; i < 3; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, i);

      JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");
      Assert.assertEquals(defaultClusterConfig,
                          object.get("config").getAsJsonObject().get("cluster").getAsJsonObject());
      if (i > 0) {
        Assert.assertEquals("111.222.333." + (i - 1),
                            object.getAsJsonObject("config").get("ipaddress").getAsString());
      }

      actions.add(object.get("taskName").getAsString());
      statuses.add(Node.Status.IN_PROGRESS.name());
      verifyNode(object.get("nodeId").getAsString(), actions, statuses);

      JsonObject returnJson = new JsonObject();
      returnJson.addProperty("status", 0);
      returnJson.addProperty("workerId", "workerX");
      returnJson.addProperty("taskId", object.get("taskId").getAsString());
      returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "111.222.333." + i)));

      TestHelper.finishTask(getBaseUrl(), returnJson);
      assertResponseStatus(response, HttpResponseStatus.OK);

      statuses.remove(statuses.size() - 1);
      statuses.add(Node.Status.COMPLETE.name());
      verifyNode(object.get("nodeId").getAsString(), actions, statuses);
    }

    jobScheduler.run();
    jobScheduler.run();

    //All tasks completed and status complete
    assertStatusWithUser1(clusterId, Cluster.Status.ACTIVE, "COMPLETE", ClusterAction.CLUSTER_CREATE, 3, 3);

    // Assert cluster object returned from REST call has real Node objects in it.
    JsonObject restCluster = GSON.fromJson(
      EntityUtils.toString(doGet("/v1/loom/clusters/" + clusterId, USER1_HEADERS).getEntity()), JsonObject.class);
    Assert.assertNotNull(restCluster.get("nodes").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString());

    //Test invalid cluster's status
    response = doGet(String.format("/v1/loom/clusters/%s/status","567"), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testDeleteCluster() throws Exception {
    //Create cluster
    String clusterId = "2";
    Cluster cluster = new JsonSerde().getGson().fromJson(SchedulerTest.TEST_CLUSTER, Cluster.class);
    cluster.setStatus(Cluster.Status.ACTIVE);
    ClusterJob clusterJob = new ClusterJob(new JobId(clusterId, 1), ClusterAction.CLUSTER_DELETE);
    clusterJob.setJobStatus(ClusterJob.Status.COMPLETE);
    cluster.setLatestJobId(clusterJob.getJobId());
    clusterStore.writeCluster(cluster);
    clusterStore.writeClusterJob(clusterJob);

    Node node1 = new JsonSerde().getGson().fromJson(SchedulerTest.NODE1, Node.class);
    Node node2 = new JsonSerde().getGson().fromJson(SchedulerTest.NODE2, Node.class);
    clusterStore.writeNode(node1);
    clusterStore.writeNode(node2);

    HttpResponse response = doDelete("/v1/loom/clusters/" + clusterId, USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_DELETE, 2, 0);

    //Take 2 delete tasks and finish them
    for (int i = 0; i < 2; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");
      verifyNode(object.get("nodeId").getAsString(), ImmutableList.of("DELETE"),
                 ImmutableList.of(Node.Status.IN_PROGRESS.name()));

      JsonObject returnJson = new JsonObject();
      returnJson.addProperty("status", 0);
      returnJson.addProperty("workerId", "workerX");
      returnJson.addProperty("taskId", object.get("taskId").getAsString());

      TestHelper.finishTask(getBaseUrl(), returnJson);
      assertResponseStatus(response, HttpResponseStatus.OK);

      verifyNode(object.get("nodeId").getAsString(), ImmutableList.of("DELETE"),
                 ImmutableList.of(Node.Status.COMPLETE.name()));
    }

    jobScheduler.run();
    jobScheduler.run();

    //All tasks completed and status complete
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, "COMPLETE", ClusterAction.CLUSTER_DELETE, 2, 2);

    //Test invalid cluster's status
    response = doGet(String.format("/v1/loom/clusters/%s/status","567"), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.NOT_FOUND);
  }

  private void verifyNode(String nodeId, List<String> actions,
                          List<String> statuses) throws Exception {
    Assert.assertEquals(actions.size(), statuses.size());

    Node node = clusterStore.getNode(nodeId);
    Assert.assertNotNull(node);

    List<Node.Action> nodeActions = node.getActions();
    Assert.assertEquals(actions.size(), nodeActions.size());

    for (int i = 0; i < actions.size(); ++i) {
      Assert.assertEquals(actions.get(i), nodeActions.get(i).getAction());
      Assert.assertEquals(statuses.get(i), nodeActions.get(i).getStatus().name());
    }
  }

  @Test
  public void testFailedClusterStatus() throws Exception {
    //Test Failed Status
    String clusterName = "test-cluster-should-be-failed";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "test cluster", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    solverScheduler.run();
    clusterScheduler.run();

    jobScheduler.run();
    jobScheduler.run();

    //take a job and fail it. Failed tasks are retried 3 times.
    for (int i = 0; i < 3; ++i) {
      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

      jobScheduler.run();
      jobScheduler.run();

      JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");

      JsonObject returnJson = new JsonObject();
      returnJson.addProperty("status", 1);
      returnJson.addProperty("workerId", "workerX");
      returnJson.addProperty("taskId", object.get("taskId").getAsString());
      returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "111.222.333." + i)));

      TestHelper.finishTask(getBaseUrl(), returnJson);
      assertResponseStatus(response, HttpResponseStatus.OK);
    }

    jobScheduler.run();
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);
  }

  @Test
  public void testFailedRetryClusterStatus() throws Exception {
    //Test Failed Status
    String clusterName = "test-cluster-retry-failed";
    ClusterCreateRequest clusterCreateRequest = createClusterRequest(clusterName, "test cluster", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    solverScheduler.run();
    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);
    for (int i = 0; i < 3; ++i) {
      // Let create complete successfully
      jobScheduler.run();
      jobScheduler.run();

      JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");
      Assert.assertEquals("CREATE", object.get("taskName").getAsString());

      JsonObject returnJson = new JsonObject();
      returnJson.addProperty("status", 0);
      returnJson.addProperty("workerId", "workerX");
      returnJson.addProperty("taskId", object.get("taskId").getAsString());
      returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "111.222.333." + i)));

      TestHelper.finishTask(getBaseUrl(), returnJson);
      assertResponseStatus(response, HttpResponseStatus.OK);


      jobScheduler.run();
      jobScheduler.run();

      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3 + i, 1 + i);

      //Fail confirm.
      object = TestHelper.takeTask(getBaseUrl(), "workerX");
      Assert.assertEquals("CONFIRM", object.get("taskName").getAsString());

      returnJson = new JsonObject();
      returnJson.addProperty("status", 1);
      returnJson.addProperty("workerId", "workerX");
      returnJson.addProperty("taskId", object.get("taskId").getAsString());
      returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "111.222.333." + i)));

      TestHelper.finishTask(getBaseUrl(), returnJson);
      assertResponseStatus(response, HttpResponseStatus.OK);

      jobScheduler.run();
      jobScheduler.run();

      if (i < 2) {
        // Should get DELETE task now
        object = TestHelper.takeTask(getBaseUrl(), "workerX");
        Assert.assertEquals("DELETE", object.get("taskName").getAsString());

        returnJson = new JsonObject();
        returnJson.addProperty("status", 0);
        returnJson.addProperty("workerId", "workerX");
        returnJson.addProperty("taskId", object.get("taskId").getAsString());
        returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "111.222.333." + i)));

        TestHelper.finishTask(getBaseUrl(), returnJson);
        assertResponseStatus(response, HttpResponseStatus.OK);
      }
    }

    jobScheduler.run();
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, "FAILED", ClusterAction.CLUSTER_CREATE, 5, 3);
  }

  @Test
  public void testUnsolvableCluster() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-unsolvable";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "unsolvable cluster", reactorTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();
    // Get the status - 0 tasks completed and TERMINATED
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, "FAILED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    clusterScheduler.run();
    jobScheduler.run();  // run scheduler put in queue
    jobScheduler.run();  // run scheduler take from queue

    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, "FAILED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    // Delete the cluster now.
    response = doDelete("/v1/loom/clusters/" + clusterId, USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, "FAILED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    jobScheduler.run();  // run scheduler put in queue
    jobScheduler.run();  // run scheduler take from queue

    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, "FAILED", ClusterAction.SOLVE_LAYOUT, 0, 0);
  }

  @Test
  public void testCancelClusterJobWithTasks() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-with-tasks";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "test cancel cluster", smallTemplate.getName(), 2);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 6, 0);

    //Take 3 tasks and finish them
    for (int i = 0; i < 3; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 6, i);

      JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");
      JsonObject returnJson = new JsonObject();
      returnJson.addProperty("status", 0);
      returnJson.addProperty("workerId", "workerX");
      returnJson.addProperty("taskId", object.get("taskId").getAsString());
      returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "111.222.333." + i)));

      TestHelper.finishTask(getBaseUrl(), returnJson);
      assertResponseStatus(response, HttpResponseStatus.OK);
    }

    // 3 tasks are done, 3 more to go. We are also done with 1 task in a stage, with 1 remaining.
    // Now cancel the job
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    jobScheduler.run();
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "FAILED", ClusterAction.CLUSTER_CREATE, 6, 3);

    // We should be not be able to take any more tasks once the job has been failed.
    JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");
    Assert.assertTrue(object.entrySet().isEmpty());
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, "FAILED", ClusterAction.CLUSTER_CREATE, 6, 3);
  }

  @Test
  public void testCancelClusterJobNotAllowed1() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-after-solving";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "test cancel cluster", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    // Not possible to cancel the job before solving is done.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    // Not possible to cancel the job after solving is done.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();

    // Can cancel after job scheduler is run.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    jobScheduler.run();
    jobScheduler.run();

    // We should not be able to take any tasks after the job has been failed.
    JsonObject object = TestHelper.takeTask(getBaseUrl(), "workerX");
    Assert.assertTrue(object.entrySet().isEmpty());
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    response = doGet("/v1/loom/clusters/" + clusterId, USER1_HEADERS);
    JsonObject clusterJson = GSON.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertEquals("Aborted by user.", clusterJson.get("message").getAsString());
  }

  @Test
  public void testCancelClusterJobNotAllowed2() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-after-solving";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "test cancel cluster", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    // Not possible to cancel the job before solving is done.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    // Not possible to cancel the job after solving is done.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    // Cancel the job after cluster scheduler is done.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    response = doGet("/v1/loom/clusters/" + clusterId, USER1_HEADERS);
    JsonObject clusterJson = GSON.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertEquals("Aborted by user.", clusterJson.get("message").getAsString());
  }

  @Test
  public void testCancelClusterNotRunningJob() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-after-solving";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "test cancel cluster", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "NOT_SUBMITTED", ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    // Cancel the job after cluster scheduler is done.
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    jobQueue.removeAll();
    Assert.assertEquals(0, jobQueue.size());

    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    // Should reschedule the job, even though it is FAILED
    response = doPost("/v1/loom/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, "FAILED", ClusterAction.CLUSTER_CREATE, 3, 0);

    response = doGet("/v1/loom/clusters/" + clusterId, USER1_HEADERS);
    JsonObject clusterJson = GSON.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertEquals("Aborted by user.", clusterJson.get("message").getAsString());
  }

  protected static void assertStatusWithUser1(String clusterId, Cluster.Status status, String actionStatus,
                                     ClusterAction action, int totalSteps, int completeSteps) throws Exception {
    // by default uses user1
    assertStatus(clusterId, status, actionStatus, action, totalSteps, completeSteps, USER1_HEADERS);
  }

  protected static void assertStatus(String clusterId, Cluster.Status status, String actionStatus,
      ClusterAction action, int totalSteps, int completeSteps, Header[] userHeaders) throws Exception {
    HttpResponse response = doGet(String.format("/v1/loom/clusters/%s/status", clusterId), userHeaders);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject statusJson = GSON.fromJson(reader, JsonObject.class);

    Assert.assertEquals(clusterId, statusJson.get("clusterid").getAsString());
    assertStatus(statusJson, status, actionStatus, action, totalSteps, completeSteps);
  }

  protected static void assertStatus(JsonObject statusJson, Cluster.Status status, String actionStatus,
      ClusterAction action, int totalSteps, int completeSteps) throws Exception {

    Assert.assertEquals(totalSteps, statusJson.get("stepstotal").getAsInt());
    Assert.assertEquals(completeSteps, statusJson.get("stepscompleted").getAsInt());
    Assert.assertEquals(actionStatus, statusJson.get("actionstatus").getAsString());
    Assert.assertEquals(action.name(), statusJson.get("action").getAsString());
    Assert.assertEquals(status.name(), statusJson.get("status").getAsString());
  }

  @Test
  public void testGetAllClusters() throws Exception {
    // First delete all clusters
    for (Cluster cluster : clusterStore.getAllClusters()) {
      clusterStore.deleteCluster(cluster.getId());
    }

    ClusterCreateRequest clusterCreateRequest = createClusterRequest("cluster1", "my 1st cluster", reactorTemplate.getName(), 5);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String cluster1 = getIdFromResponse(response);

    solverScheduler.run();

    clusterCreateRequest = createClusterRequest("cluster2", "my 2nd cluster", reactorTemplate.getName(), 6);
    response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String cluster2 = getIdFromResponse(response);

    solverScheduler.run();

    response = doGet("/v1/loom/clusters", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String responseStr = EntityUtils.toString(response.getEntity());
    List<Map<String, String>> clusterInfos =
      GSON.fromJson(responseStr, new TypeToken<List<Map<String, String>>>() {}.getType());

    Assert.assertNotNull(clusterInfos.get(0).get("id"));
    Assert.assertNotNull(clusterInfos.get(0).get("createTime"));
    Assert.assertEquals("cluster2", clusterInfos.get(0).get("name"));
    Assert.assertEquals("reactor-medium", clusterInfos.get(0).get("clusterTemplate"));
    Assert.assertEquals("6", clusterInfos.get(0).get("numNodes"));

    Assert.assertNotNull(clusterInfos.get(1).get("id"));
    Assert.assertNotNull(clusterInfos.get(1).get("createTime"));
    Assert.assertEquals("cluster1", clusterInfos.get(1).get("name"));
    Assert.assertEquals("reactor-medium", clusterInfos.get(1).get("clusterTemplate"));
    Assert.assertEquals("5", clusterInfos.get(1).get("numNodes"));

    // cleanup
    clusterQueue.removeAll();
    clusterStore.deleteCluster(cluster1);
    clusterStore.deleteCluster(cluster2);
  }

  @Test
  public void testGetNonexistantClusterReturns404() throws Exception {
    assertResponseStatus(doGet("/v1/loom/clusters/567", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testGetServicesFromNonexistantClusterReturns404() throws Exception {
    assertResponseStatus(doGet("/v1/loom/clusters/567/services", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testGetClusterNotOwnedByUserReturns404() throws Exception {
    ClusterCreateRequest clusterCreateRequest = createClusterRequest("cluster1", "my 1st cluster",
                                                                     reactorTemplate.getName(), 5);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    assertResponseStatus(doGet("/v1/loom/clusters/" + clusterId, USER2_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAdminCanGetClustersOwnedByOthers() throws Exception {
    ClusterCreateRequest clusterCreateRequest = createClusterRequest("cluster1", "my 1st cluster", reactorTemplate.getName(), 5);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    assertResponseStatus(doGet("/v1/loom/clusters/" + clusterId, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  @Test
  public void testDeleteOnClusterNotOwnedByUserReturns404() throws Exception {
    ClusterCreateRequest clusterCreateRequest = createClusterRequest("cluster1", "my 1st cluster", reactorTemplate.getName(), 5);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    assertResponseStatus(doDelete("/v1/loom/clusters/" + clusterId, USER2_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAdminCanDeleteClustersOwnedByOthers() throws Exception {
    String clusterId = "2";
    Cluster cluster = new JsonSerde().getGson().fromJson(SchedulerTest.TEST_CLUSTER, Cluster.class);
    cluster.setStatus(Cluster.Status.ACTIVE);
    ClusterJob clusterJob = new ClusterJob(new JobId(clusterId, 1), ClusterAction.CLUSTER_DELETE);
    clusterJob.setJobStatus(ClusterJob.Status.COMPLETE);
    cluster.setLatestJobId(clusterJob.getJobId());
    clusterStore.writeCluster(cluster);
    clusterStore.writeClusterJob(clusterJob);

    assertResponseStatus(doDelete("/v1/loom/clusters/" + clusterId, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  @Test
  public void testGetAllClustersReturnsOnlyThoseOwnedByUser() throws Exception {
    ClusterCreateRequest clusterCreateRequest = createClusterRequest("cluster1", "my 1st cluster", reactorTemplate.getName(), 5);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String cluster1 = getIdFromResponse(response);

    clusterCreateRequest = createClusterRequest("cluster2", "my 2nd cluster", reactorTemplate.getName(), 6);
    response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER2_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String cluster2 = getIdFromResponse(response);

    // check get call from user1 only gets back cluster1
    response = doGet("/v1/loom/clusters", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String responseStr = EntityUtils.toString(response.getEntity());
    List<Map<String, String>> clusterInfos =
      GSON.fromJson(responseStr, new TypeToken<List<Map<String, String>>>() {}.getType());
    Assert.assertEquals(1, clusterInfos.size());
    Assert.assertEquals(cluster1, clusterInfos.get(0).get("id"));

    // check get call from user2 only gets back cluster2
    response = doGet("/v1/loom/clusters", USER2_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseStr = EntityUtils.toString(response.getEntity());
    clusterInfos = GSON.fromJson(responseStr, new TypeToken<List<Map<String, String>>>() {}.getType());
    Assert.assertEquals(1, clusterInfos.size());
    Assert.assertEquals(cluster2, clusterInfos.get(0).get("id"));

    // check admin get all clusters
    response = doGet("/v1/loom/clusters", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    responseStr = EntityUtils.toString(response.getEntity());
    clusterInfos = GSON.fromJson(responseStr, new TypeToken<List<Map<String, String>>>() {}.getType());
    Assert.assertEquals(2, clusterInfos.size());
    Set<String> ids = Sets.newHashSet();
    for (Map<String, String> clusterInfo : clusterInfos) {
      ids.add(clusterInfo.get("id"));
    }
    Assert.assertEquals(ImmutableSet.of(cluster1, cluster2), ids);
  }

  @Test
  public void testGetClusterServices() throws Exception {
    Map<String, Node> nodes = ImmutableMap.of(
      "node1",
      new Node("node1", "123",
               ImmutableSet.<Service>of(
                 new Service("s1", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of()),
                 new Service("s2", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of())),
               ImmutableMap.<String, String>of()),
      "node2",
      new Node("node2", "123",
               ImmutableSet.<Service>of(
                 new Service("s2", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of()),
                 new Service("s3", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of())),
               ImmutableMap.<String, String>of()));
    Cluster cluster = new Cluster("123", USER1, "my-cluster", System.currentTimeMillis(), "my cluster", null, null,
                                  nodes.keySet(), ImmutableSet.of("s1", "s2", "s3"));
    clusterStore.writeCluster(cluster);

    // check services
    HttpResponse response = doGet("/v1/loom/clusters/" + cluster.getId() + "/services", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Set<String> services = GSON.fromJson(reader, new TypeToken<Set<String>>() {}.getType());
    Assert.assertEquals(ImmutableSet.of("s1", "s2", "s3"), services);

    // cleanup
    clusterStore.deleteCluster(cluster.getId());
  }

  @Test
  public void testGetPlanForJob() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-for-plan-job";
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest(clusterName, "test cluster plan job", smallTemplate.getName(), 1);
    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    solverScheduler.run();
    clusterScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, "RUNNING", ClusterAction.CLUSTER_CREATE, 3, 0);

    // Setup expected plan
    JsonObject expected = GSON.fromJson(SAMPLE_PLAN, JsonObject.class);
    JsonArray expectedAllPlans = GSON.fromJson(ALL_SAMPLE_PLANS, JsonArray.class);

    // Verify plan for job
    Cluster cluster = clusterStore.getCluster(clusterId);
    response = doGet("/v1/loom/clusters/" + clusterId + "/plans/" + cluster.getLatestJobId(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject actual = GSON.fromJson(reader, JsonObject.class);

    verifyPlanJson(expected, actual);

    // Verify all plans for cluster
    response = doGet("/v1/loom/clusters/" + clusterId + "/plans", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonArray actualAllPlans = GSON.fromJson(reader, JsonArray.class);
    Assert.assertEquals(expectedAllPlans.size(), actualAllPlans.size());

    for (int i = 0; i < expectedAllPlans.size(); ++i) {
      verifyPlanJson(expectedAllPlans.get(i).getAsJsonObject(), actualAllPlans.get(i).getAsJsonObject());
    }
  }

  @Test
  public void testMaxClusterSize() throws Exception {
    ClusterCreateRequest clusterCreateRequest =
      createClusterRequest("cluster", "desc", smallTemplate.getName(), Constants.DEFAULT_MAX_CLUSTER_SIZE + 1);
    assertResponseStatus(doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testClusterCreateLeaseDuration() throws Exception {
    // Reactor template has forever initial lease duration
    verifyInitialLeaseDuration(200, Cluster.Status.PENDING, 200, reactorTemplate.getName());
    verifyInitialLeaseDuration(0, Cluster.Status.PENDING, 0, reactorTemplate.getName());
    verifyInitialLeaseDuration(0, Cluster.Status.PENDING, -1, reactorTemplate.getName());

    // Small template has 10000 initial lease duration
    verifyInitialLeaseDuration(10000, Cluster.Status.TERMINATED, 20000, smallTemplate.getName());
    verifyInitialLeaseDuration(10000, Cluster.Status.PENDING, 10000, smallTemplate.getName());
    verifyInitialLeaseDuration(500, Cluster.Status.PENDING, 500, smallTemplate.getName());
    verifyInitialLeaseDuration(10000, Cluster.Status.TERMINATED, 0, smallTemplate.getName());
    verifyInitialLeaseDuration(10000, Cluster.Status.PENDING, -1, smallTemplate.getName());
  }

  private void verifyInitialLeaseDuration(long expectedExpireTime, Cluster.Status expectedStatus,
                                          long requestedLeaseDuration,
                                          String clusterTemplate) throws Exception {
    ClusterCreateRequest clusterCreateRequest = new ClusterCreateRequest("test-lease", "test cluster initial lease", clusterTemplate, 4,
                                                       null, null, null, null, requestedLeaseDuration, null, null);

    HttpResponse response = doPost("/v1/loom/clusters", GSON.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    solverScheduler.run();

    String clusterId = getIdFromResponse(response);
    Cluster cluster = clusterStore.getCluster(clusterId);

    Assert.assertEquals(expectedStatus, cluster.getStatus());

    if (cluster.getStatus() == Cluster.Status.TERMINATED) {
      return;
    }

    if (expectedExpireTime == 0) {
      Assert.assertEquals(expectedExpireTime, cluster.getExpireTime());
    } else {
      Assert.assertEquals(expectedExpireTime,
                          cluster.getExpireTime() == 0 ? 0 : cluster.getExpireTime() - cluster.getCreateTime());
    }
  }

  @Test
  public void testClusterProlongForever() throws Exception {
    ClusterTemplate foreverTemplate =
      new ClusterTemplate("forever-template", "",
                          new ClusterDefaults(ImmutableSet.<String>of(), "", "", "", null, new JsonObject()),
                          Compatibilities.EMPTY_COMPATIBILITIES,
                          Constraints.EMPTY_CONSTRAINTS, Administration.EMPTY_ADMINISTRATION);

    long currentTime = 10000;
    Cluster foreverCluster = new Cluster("1002", "", "prolong-test", currentTime, "", null,
                                  foreverTemplate, ImmutableSet.<String>of(), ImmutableSet.<String>of(),
                                  new JsonObject());
    foreverCluster.setExpireTime(currentTime + 10000);
    foreverCluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(foreverCluster);

    HttpResponse response = doPost("/v1/loom/clusters/" + foreverCluster.getId(),
                                   "{'expireTime' : 90000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    foreverCluster = clusterStore.getCluster(foreverCluster.getId());
    Assert.assertEquals(90000, foreverCluster.getExpireTime());
  }

  @Test
  public void testClusterProlong() throws Exception {
    ClusterTemplate template =
      new ClusterTemplate("limited-template", "",
                          new ClusterDefaults(ImmutableSet.<String>of(), "", "", "", null, new JsonObject()),
                          Compatibilities.EMPTY_COMPATIBILITIES,
                          Constraints.EMPTY_CONSTRAINTS, new Administration(new LeaseDuration(1000, 12000, 1000)));

    long currentTime = 10000;
    Cluster cluster = new Cluster("1002", "", "prolong-test", currentTime, "", null,
                                         template, ImmutableSet.<String>of(), ImmutableSet.<String>of(),
                                         new JsonObject());
    long expireTime = currentTime + 10000;
    cluster.setExpireTime(expireTime);
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to step size > specified
    HttpResponse response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to no expire time
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to no expire time
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'foo' : 'bar'}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to invalid expire size, since it is less than create time
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 9000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Reduction should succeed
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 19000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(19000, cluster.getExpireTime());

    // Should succeed
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 20000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should succeed again
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 21000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());

    // Should succeed again
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(22000, cluster.getExpireTime());

    // Try again should fail since it exceeds max
    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 23000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(22000, cluster.getExpireTime());

    // Expire time of incomplete cluster can be changed
    // Put cluster in incomplete state
    cluster.setStatus(Cluster.Status.INCOMPLETE);
    clusterStore.writeCluster(cluster);

    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 21000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());

    // Any attempt to change cluster expire time should fail on pending cluster
    // Put cluster in incomplete state
    cluster.setStatus(Cluster.Status.PENDING);
    clusterStore.writeCluster(cluster);

    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());

    // Any attempt to change cluster expire time should fail on terminated cluster
    // Terminate cluster
    cluster.setStatus(Cluster.Status.TERMINATED);
    clusterStore.writeCluster(cluster);

    response = doPost("/v1/loom/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());
  }

  @Test
  public void testInvalidGetClusterConfigRequests() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "get-config-test", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), new JsonObject());
    clusterStore.writeCluster(cluster);

    assertResponseStatus(doGet("/v1/loom/clusters/" + cluster.getId() + "9/config", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGet("/v1/loom/clusters/" + cluster.getId() + "/config", USER2_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testGetClusterConfig() throws Exception {
    JsonObject config = new JsonObject();
    config.addProperty("key1", "val1");
    JsonArray arrayVal = new JsonArray();
    arrayVal.add(new JsonPrimitive("arrayval1"));
    arrayVal.add(new JsonPrimitive("arrayval2"));
    config.add("key2", arrayVal);
    JsonObject objVal = new JsonObject();
    objVal.addProperty("okey1", "oval1");
    objVal.addProperty("okey2", "oval2");
    config.add("key3", objVal);
    Cluster cluster = new Cluster("123", USER1, "get-config-test", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), config);
    clusterStore.writeCluster(cluster);
    HttpResponse response = doGet("/v1/loom/clusters/" + cluster.getId() + "/config", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject actual = GSON.fromJson(reader, JsonObject.class);
    Assert.assertEquals(config, actual);
  }

  @Test
  public void testInvalidClusterConfigRequests() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "get-config-test", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), new JsonObject());
    clusterStore.writeCluster(cluster);
    String requestStr = GSON.toJson(new ClusterConfigureRequest(new JsonObject(), false));

    assertResponseStatus(doPut("/v1/loom/clusters/" + cluster.getId() + "/config", "{}", USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    assertResponseStatus(doPut("/v1/loom/clusters/" + cluster.getId() + "9/config", requestStr, USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPut("/v1/loom/clusters/" + cluster.getId() + "/config", requestStr, USER2_HEADERS),
                         HttpResponseStatus.NOT_FOUND);

    cluster.setStatus(Cluster.Status.INCOMPLETE);
    clusterStore.writeCluster(cluster);

    assertResponseStatus(doPut("/v1/loom/clusters/" + cluster.getId() + "/config", requestStr, USER1_HEADERS),
                         HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testPutClusterConfigCanRunOnInconsistentClusters() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "get-config-test", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), new JsonObject());
    cluster.setStatus(Cluster.Status.INCONSISTENT);
    clusterStore.writeCluster(cluster);
    String requestStr = GSON.toJson(new ClusterConfigureRequest(new JsonObject(), false));

    assertResponseStatus(doPut("/v1/loom/clusters/" + cluster.getId() + "/config", requestStr, USER1_HEADERS),
                         HttpResponseStatus.OK);
  }

  @Test
  public void testPutClusterConfig() throws Exception {
    JsonObject originalConfig = new JsonObject();
    originalConfig.addProperty("key1", "val1");
    Cluster cluster = new Cluster("123", USER1, "get-config-test", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), originalConfig);
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);

    HttpResponse response = doGet("/v1/loom/clusters/123/config", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject actual = GSON.fromJson(reader, JsonObject.class);
    Assert.assertEquals(originalConfig, actual);

    JsonObject newConfig = new JsonObject();
    newConfig.addProperty("key2", "val2");
    ClusterConfigureRequest configRequest = new ClusterConfigureRequest(newConfig, false);
    assertResponseStatus(doPut("/v1/loom/clusters/123/config", GSON.toJson(configRequest), USER1_HEADERS),
                         HttpResponseStatus.OK);

    response = doGet("/v1/loom/clusters/123/config", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    actual = GSON.fromJson(reader, JsonObject.class);
    Assert.assertEquals(newConfig, actual);
  }

  @Test
  public void testClusterServiceActions() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "service-actions", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of("namenode", "datanode"));
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);
    Map<String, ClusterAction> actions = Maps.newHashMap();
    actions.put("/stop", ClusterAction.STOP_SERVICES);
    actions.put("/start", ClusterAction.START_SERVICES);
    actions.put("/restart", ClusterAction.RESTART_SERVICES);
    actions.put("/namenode/stop", ClusterAction.STOP_SERVICES);
    actions.put("/namenode/start", ClusterAction.START_SERVICES);
    actions.put("/namenode/restart", ClusterAction.RESTART_SERVICES);

    try {
      for (Map.Entry<String, ClusterAction> entry : actions.entrySet()) {
        assertResponseStatus(doPost("/v1/loom/clusters/123/services" + entry.getKey(), "", USER1_HEADERS),
                             HttpResponseStatus.OK);
        Assert.assertEquals(entry.getValue().name(), clusterQueue.take("0").getValue());
        cluster.setStatus(Cluster.Status.ACTIVE);
        clusterStore.writeCluster(cluster);
      }
    } finally {
      clusterQueue.removeAll();
    }
  }

  @Test
  public void testServiceActionsOnNonexistantClusterReturn404() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "test-cluster", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of());
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);
    Set<String> actions = ImmutableSet.of(
      "/stop",
      "/start",
      "/restart",
      "/namenode/stop",
      "/namenode/start",
      "/namenode/restart"
    );
    for (String action : actions) {
      // no cluster 1123
      assertResponseStatus(doPost("/v1/loom/clusters/1123/services" + action, "", USER1_HEADERS),
                           HttpResponseStatus.NOT_FOUND);
      // no cluster for user2
      assertResponseStatus(doPost("/v1/loom/clusters/123/services" + action, "", USER2_HEADERS),
                           HttpResponseStatus.NOT_FOUND);
    }
  }

  @Test
  public void testServiceActionsOnNonexistantClusterServiceReturn404() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "test-cluster", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of());
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);
    assertResponseStatus(doPost("/v1/loom/clusters/123/services/fake/stop", "", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPost("/v1/loom/clusters/123/services/fake/start", "", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPost("/v1/loom/clusters/123/services/fake/restart", "", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testServiceActionsCanOnlyRunOnActiveCluster() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "test-cluster", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of("namenode", "datanode"));
    Set<Cluster.Status> badStatuses = ImmutableSet.of(
      Cluster.Status.INCOMPLETE, Cluster.Status.PENDING, Cluster.Status.TERMINATED, Cluster.Status.INCONSISTENT);
    Set<String> resources = ImmutableSet.of(
      "/v1/loom/clusters/123/services/stop",
      "/v1/loom/clusters/123/services/start",
      "/v1/loom/clusters/123/services/restart",
      "/v1/loom/clusters/123/services/namenode/stop",
      "/v1/loom/clusters/123/services/namenode/start",
      "/v1/loom/clusters/123/services/namenode/restart"
    );
    for (Cluster.Status status : badStatuses) {
      cluster.setStatus(status);
      clusterStore.writeCluster(cluster);
      for (String resource : resources) {
        assertResponseStatus(doPost(resource, "", USER1_HEADERS), HttpResponseStatus.CONFLICT);
      }
    }
  }

  @Test
  public void testAddInvalidServicesReturns400() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "test-cluster", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of("namenode", "datanode"));
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);
    // can't add nodemanager without resourcemanager
    AddServicesRequest body = new AddServicesRequest(ImmutableSet.of("nodemanager"));
    assertResponseStatus(doPost("/v1/loom/clusters/123/services", GSON.toJson(body), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
    // can't add nonexistant service
    body = new AddServicesRequest(ImmutableSet.of("fakeservice"));
    assertResponseStatus(doPost("/v1/loom/clusters/123/services", GSON.toJson(body), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testAddServicesOnNonexistantClusterReturns404() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "test-cluster", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of("namenode", "datanode"));
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStore.writeCluster(cluster);
    AddServicesRequest body = new AddServicesRequest(ImmutableSet.of("resourcemanager", "nodemanager"));
    assertResponseStatus(doPost("/v1/loom/clusters/1123/services", GSON.toJson(body), USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPost("/v1/loom/clusters/123/services", GSON.toJson(body), USER2_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAddServicesCanOnlyRunOnActiveCluster() throws Exception {
    Cluster cluster = new Cluster("123", USER1, "test-cluster", 0, "", null, Entities.ClusterTemplateExample.HDFS,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of("namenode", "datanode"));
    Set<Cluster.Status> badStatuses = ImmutableSet.of(
      Cluster.Status.INCOMPLETE, Cluster.Status.PENDING, Cluster.Status.TERMINATED, Cluster.Status.INCONSISTENT);
    AddServicesRequest body = new AddServicesRequest(ImmutableSet.of("resourcemanager", "nodemanager"));
    for (Cluster.Status status : badStatuses) {
      cluster.setStatus(status);
      clusterStore.writeCluster(cluster);
      assertResponseStatus(doPost("/v1/loom/clusters/123/services", GSON.toJson(body), USER1_HEADERS),
                           HttpResponseStatus.CONFLICT);
    }
  }

  private void verifyPlanJson(JsonObject expected, JsonObject actual) {
    // Fix ids before comparing
    int nodeId = 0;
    actual.getAsJsonObject().addProperty("id", String.valueOf(++nodeId));
    actual.getAsJsonObject().addProperty("clusterId", String.valueOf(++nodeId));
    for (JsonElement stage : actual.get("stages").getAsJsonArray()) {
      for (JsonElement plan : stage.getAsJsonArray()) {
        plan.getAsJsonObject().addProperty("id", String.valueOf(++nodeId));
        plan.getAsJsonObject().addProperty("nodeId", String.valueOf(++nodeId));
      }
    }

    Assert.assertEquals(expected, actual);
  }

  protected static String getIdFromResponse(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    return GSON.fromJson(reader, JsonObject.class).get("id").getAsString();
  }

  protected static ClusterCreateRequest createClusterRequest(String name, String description,
                                                       String template, int numMachines) {
    return new ClusterCreateRequest(name, description, template, numMachines,
                                    null, null, null, null, -1L, null, null);
  }

  protected static ClusterCreateRequest createClusterRequest(String name, String description, String template,
                                                             int numMachines, JsonObject userConfig) {
    return new ClusterCreateRequest(name, description, template, numMachines,
                                    null, null, null, null, -1L, null, userConfig);
  }

  @BeforeClass
  public static void initData() throws Exception {
    Set<String> services = ImmutableSet.of("namenode", "datanode", "resourcemanager", "nodemanager",
                                           "hbasemaster", "regionserver", "zookeeper", "reactor");
    reactorTemplate = new ClusterTemplate(
      "reactor-medium",
      "medium reactor cluster template",
      new ClusterDefaults(services, "joyent", null, null, null, new JsonObject()),
      new Compatibilities(
        ImmutableSet.<String>of("large-mem", "large-cpu", "large", "medium", "small"),
        null,
        null
      ),
      new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          "namenode",
          new ServiceConstraint(
            ImmutableSet.of("large-mem"),
            ImmutableSet.of("centos6", "ubuntu12"), 1, 1, 1, null),
          "datanode",
          new ServiceConstraint(
            ImmutableSet.of("medium", "large-cpu"),
            ImmutableSet.of("centos6", "ubuntu12"), 1, 50, 1, null),
          "zookeeper",
          new ServiceConstraint(
            ImmutableSet.of("small", "medium"),
            ImmutableSet.of("centos6"), 1, 5, 2, ImmutablePair.of(1, 20)),
          "reactor",
          new ServiceConstraint(
            ImmutableSet.of("medium", "large"),
            null, 1, 5, 1, ImmutablePair.of(1, 10))
        ),
        new LayoutConstraint(
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("datanode", "nodemanager", "regionserver"),
            ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("datanode", "namenode"),
            ImmutableSet.of("datanode", "zookeeper"),
            ImmutableSet.of("namenode", "zookeeper"),
            ImmutableSet.of("datanode", "reactor"),
            ImmutableSet.of("namenode", "reactor")
          )
        )
      ),
      Administration.EMPTY_ADMINISTRATION
    );

    defaultClusterConfig = new JsonObject();
    defaultClusterConfig.addProperty("defaultconfig", "value1");

    smallTemplate =  new ClusterTemplate("one-machine",
                                         "one machine cluster template",
                                         new ClusterDefaults(ImmutableSet.of("zookeeper"), "joyent", null, null,
                                                             null, defaultClusterConfig),
                                         new Compatibilities(null, null, ImmutableSet.of("zookeeper")),
                                         null, new Administration(new LeaseDuration(10000, 30000, 5000)));

    // create providers
    entityStore.writeProvider(new Provider("joyent", "joyent provider", Provider.Type.JOYENT,
                                     Collections.<String, Map<String, String>>emptyMap()));
    // create hardware types
    entityStore.writeHardwareType(
      new HardwareType(
        "medium",
        "medium hardware",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Medium 4GB"))
      )
    );
    entityStore.writeHardwareType(
      new HardwareType(
        "large-mem",
        "hardware with a lot of memory",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 32GB"))
      )
    );
    entityStore.writeHardwareType(
      new HardwareType(
        "large-cpu",
        "hardware with a lot of cpu",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 16GB"))
      )
    );
    // create image types
    entityStore.writeImageType(
      new ImageType(
        "centos6",
        "CentOs 6.4 image",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("image", "joyent-hash-of-centos6.4"))
      )
    );
    // create services
    for (String serviceName : services) {
      entityStore.writeService(new Service(
        serviceName, serviceName + " description", Collections.<String>emptySet(),
        Collections.<ProvisionerAction, ServiceAction>emptyMap()));
    }
    entityStore.writeClusterTemplate(reactorTemplate);
    entityStore.writeClusterTemplate(smallTemplate);
  }

  private static final String SAMPLE_PLAN =
    "{\n" +
      "   \"id\":\"1\",\n" +
      "   \"clusterId\":\"2\",\n" +
      "   \"action\":\"CLUSTER_CREATE\",\n" +
      "   \"currentStage\":0,\n" +
      "   \"stages\":[\n" +
      "      [\n" +
      "         {\n" +
      "            \"id\":\"3\",\n" +
      "            \"taskName\":\"CREATE\",\n" +
      "            \"nodeId\":\"4\",\n" +
      "            \"service\":\"\"\n" +
      "         }\n" +
      "      ],\n" +
      "      [\n" +
      "         {\n" +
      "            \"id\":\"5\",\n" +
      "            \"taskName\":\"CONFIRM\",\n" +
      "            \"nodeId\":\"6\",\n" +
      "            \"service\":\"\"\n" +
      "         }\n" +
      "      ],\n" +
      "      [\n" +
      "         {\n" +
      "            \"id\":\"7\",\n" +
      "            \"taskName\":\"BOOTSTRAP\",\n" +
      "            \"nodeId\":\"8\",\n" +
      "            \"service\":\"\"\n" +
      "         }\n" +
      "      ]\n" +
      "   ]\n" +
      "}";

  private static final String SOLVER_PLAN =
    "{\"id\":\"1\",\"clusterId\":\"2\",\"action\":\"SOLVE_LAYOUT\",\"currentStage\":0,\"stages\":[]}";

  private static final String ALL_SAMPLE_PLANS =
    "[\n" +
      SAMPLE_PLAN + ",\n" +
      SOLVER_PLAN +
      "\n]";
}
