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
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterStatusResponse;
import co.cask.coopr.http.request.FinishTaskRequest;
import co.cask.coopr.http.request.TakeTaskRequest;
import co.cask.coopr.scheduler.CallbackScheduler;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.ClusterScheduler;
import co.cask.coopr.scheduler.JobScheduler;
import co.cask.coopr.scheduler.SolverRequest;
import co.cask.coopr.scheduler.SolverScheduler;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.SchedulableTask;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.LayoutConstraint;
import co.cask.coopr.spec.template.LeaseDuration;
import co.cask.coopr.spec.template.ServiceConstraint;
import co.cask.coopr.spec.template.SizeConstraint;
import co.cask.coopr.store.cluster.ClusterStoreView;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ClusterHandlerTest extends ServiceTestBase {
  private static ClusterTemplate reactorTemplate;
  private static ClusterTemplate smallTemplate;
  private static JsonObject defaultClusterConfig;
  private static JobScheduler jobScheduler;
  private static ClusterScheduler clusterScheduler;
  private static SolverScheduler solverScheduler;
  private static CallbackScheduler callbackScheduler;

  @BeforeClass
  public static void init() {
    jobScheduler = injector.getInstance(JobScheduler.class);
    clusterScheduler = injector.getInstance(ClusterScheduler.class);
    solverScheduler = injector.getInstance(SolverScheduler.class);
    callbackScheduler = injector.getInstance(CallbackScheduler.class);
  }

  @After
  public void testCleanup() {
    // cleanup
    solverQueues.removeAll();
    clusterQueues.removeAll();
    callbackQueues.removeAll();
    jobQueues.removeAll();
    provisionerQueues.removeAll();
  }

  @Test
  public void testAddCluster() throws Exception {
    String clusterName = "my-cluster";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    // check there was an element added to the cluster queue for creating this cluster
    Element element = solverQueues.take(tenantId, "0");
    Assert.assertEquals(clusterId, element.getId());
    ClusterCreateRequest expected = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .setInitialLeaseDuration(-1L)
      .build();
    SolverRequest expectedSolverRequest = new SolverRequest(SolverRequest.Type.CREATE_CLUSTER, gson.toJson(expected));
    Assert.assertEquals(expectedSolverRequest, gson.fromJson(element.getValue(), SolverRequest.class));
  }

  @Test
  public void testPauseResumeCluster() throws Exception {
    String clusterName = "cluster-for-pause-resume";
    //Create cluster
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(2)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();
    callbackScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 6, 0);

    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId);
    //Take 2 tasks and finish them
    for (int i = 0; i < 2; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                            ClusterAction.CLUSTER_CREATE, 6, i);

      SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
      JsonObject result = new JsonObject();
      result.addProperty("ipaddress", "111.222.333." + i);
      FinishTaskRequest finishRequest =
        new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId,
                              task.getTaskId(), null, null, 0, null, null, result);

      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);
    }
    jobScheduler.run();
    jobScheduler.run();

    // Pause the job
    response = doPostExternalAPI("/clusters/" + clusterId + "/pause", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    // Check that the job status is PAUSED
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.PAUSED,
                          ClusterAction.CLUSTER_CREATE, 6, 2);

    // Resume the job
    response = doPostExternalAPI("/clusters/" + clusterId + "/resume", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    // Check that the job status is again RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 6, 2);
  }

  @Test
  public void testAddClusterWithOptionalArgs() throws Exception {
    String clusterName = "my-cluster";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .setProviderName("joyent")
      .setServiceNames(ImmutableSet.of("namenode", "datanode"))
      .setHardwareTypeName("large")
      .setImageTypeName("centos6")
      .setInitialLeaseDuration(-1L)
      .build();

    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    // check there was an element added to the cluster queue for creating this cluster
    Element element = solverQueues.take(tenantId, "0");
    SolverRequest request = gson.fromJson(element.getValue(), SolverRequest.class);
    ClusterCreateRequest createRequest = gson.fromJson(request.getJsonRequest(), ClusterCreateRequest.class);
    Assert.assertEquals("joyent", createRequest.getProvider());
    Assert.assertEquals("centos6", createRequest.getImageType());
    Assert.assertEquals("large", createRequest.getHardwareType());
    Assert.assertEquals(ImmutableSet.of("namenode", "datanode"), createRequest.getServices());
  }

  @Test
  public void testInvalidNumMachines() throws Exception {
    // when its below the min
    ClusterCreateRequest clusterCreateRequest =ClusterCreateRequest.builder()
      .setName("my-cluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setProviderName(reactorTemplate.getClusterDefaults().getProvider())
      .setNumMachines(1)
      .build();
    assertResponseStatus(doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
    // when its above the max
    clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("my-cluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setProviderName(reactorTemplate.getClusterDefaults().getProvider())
      .setNumMachines(500)
      .build();
    assertResponseStatus(doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testUserConfig() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-with-user-config";
    String tenantId = USER1_ACCOUNT.getTenantId();

    JsonObject userConfig = new JsonObject();
    userConfig.addProperty("userconfig1", "value1");
    userConfig.addProperty("userconfig2", "value1");

    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .setConfig(userConfig)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();
    callbackScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();  // run scheduler put in queue
    jobScheduler.run();  // run scheduler take from queue

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId));

    // Only user config should be present, not default cluster config.
    Assert.assertEquals(userConfig, task.getConfig().getClusterConfig());
  }

  @Test
  public void testClusterStatus() throws Exception {
    List<String> actions = Lists.newArrayList();
    List<String> statuses = Lists.newArrayList();

    //Create cluster
    String clusterName = "test-cluster-should-be-provisioned";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();
    callbackScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId);

    //Take 3 tasks and finish them
    for (int i = 0; i < 3; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                            ClusterAction.CLUSTER_CREATE, 3, i);

      SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
      Assert.assertEquals(defaultClusterConfig,
                          task.getConfig().getClusterConfig());
      if (i > 0) {
        Assert.assertEquals("111.222.333." + (i - 1),
                            task.getConfig().getNodeProperties().getIPAddress("access"));
        Assert.assertEquals("444.555.666." + (i - 1),
                            task.getConfig().getNodeProperties().getIPAddress("bind"));
      }

      actions.add(task.getTaskName());
      statuses.add(Node.Status.IN_PROGRESS.name());
      verifyNode(task.getNodeId(), actions, statuses);

      JsonObject result = new JsonObject();
      Map<String, String> ipAddresses = ImmutableMap.of(
        "access", "111.222.333." + i,
        "bind", "444.555.666." + i
      );
      FinishTaskRequest finishRequest =
        new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId, task.getTaskId(),
                              null, null, 0, null, ipAddresses, result);

      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);

      statuses.remove(statuses.size() - 1);
      statuses.add(Node.Status.COMPLETE.name());
      verifyNode(task.getNodeId(), actions, statuses);
    }

    jobScheduler.run();
    jobScheduler.run();

    //All tasks completed and status complete
    assertStatusWithUser1(clusterId, Cluster.Status.ACTIVE, ClusterJob.Status.COMPLETE,
                          ClusterAction.CLUSTER_CREATE, 3, 3);

    // Assert cluster object returned from REST call has real Node objects in it.
    JsonObject restCluster = gson.fromJson(
      EntityUtils.toString(doGetExternalAPI("/clusters/" + clusterId, USER1_HEADERS).getEntity()), JsonObject.class);
    Assert.assertNotNull(restCluster.get("nodes").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString());

    //Test invalid cluster's status
    response = doGetExternalAPI(String.format("/clusters/%s/status","567"), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testDeleteCluster() throws Exception {
    //Create cluster
    String clusterId = "2";
    Cluster cluster = Entities.ClusterExample.createCluster();
    String tenantId = cluster.getAccount().getTenantId();
    cluster.setStatus(Cluster.Status.ACTIVE);
    ClusterJob clusterJob = new ClusterJob(new JobId(clusterId, 1), ClusterAction.CLUSTER_DELETE);
    clusterJob.setJobStatus(ClusterJob.Status.COMPLETE);
    cluster.setLatestJobId(clusterJob.getJobId());
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    clusterStore.writeClusterJob(clusterJob);

    Node node1 = Entities.ClusterExample.NODE1;
    Node node2 = Entities.ClusterExample.NODE2;
    clusterStore.writeNode(node1);
    clusterStore.writeNode(node2);

    HttpResponse response = doDeleteExternalAPI("/clusters/" + clusterId, USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    clusterScheduler.run();
    callbackScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_DELETE, 2, 0);

    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId);
    //Take 2 delete tasks and finish them
    for (int i = 0; i < 2; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
      verifyNode(task.getNodeId(), ImmutableList.of("DELETE"),
                 ImmutableList.of(Node.Status.IN_PROGRESS.name()));

      FinishTaskRequest finishRequest =
        new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId, task.getTaskId(), null, null, 0, null, null, null);

      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);

      verifyNode(task.getNodeId(), ImmutableList.of("DELETE"),
                 ImmutableList.of(Node.Status.COMPLETE.name()));
    }

    jobScheduler.run();
    jobScheduler.run();

    //All tasks completed and status complete
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.COMPLETE,
                          ClusterAction.CLUSTER_DELETE, 2, 2);

    //Test invalid cluster's status
    response = doGetExternalAPI(String.format("/clusters/%s/status","567"), USER1_HEADERS);
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
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    solverScheduler.run();
    clusterScheduler.run();
    callbackScheduler.run();

    jobScheduler.run();
    jobScheduler.run();

    //take a job and fail it. Failed tasks are retried 3 times.
    for (int i = 0; i < 3; ++i) {
      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                            ClusterAction.CLUSTER_CREATE, 3, 0);

      jobScheduler.run();
      jobScheduler.run();

      SchedulableTask task =
        TestHelper.takeTask(getBaseUrlInternalAPI(), new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId));

      JsonObject result = new JsonObject();
      result.addProperty("ipaddress", "111.222.333." + i);
      FinishTaskRequest finishRequest =
        new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId, task.getTaskId(),
                              null, null, 1, null, null, result);

      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);
    }

    jobScheduler.run();
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);
  }

  @Test
  public void testFailedRetryClusterStatus() throws Exception {
    //Test Failed Status
    String clusterName = "test-cluster-retry-failed";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    solverScheduler.run();
    clusterScheduler.run();
    callbackScheduler.run();

    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId);
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);
    // 3 loops for 3 retries.  We will simulate a situation where the confirm step always fails, which results
    // in a delete task for rollback, then a create task for retry.
    // So we should expect the following task chain 3 times: create success -> confirm failure -> delete success
    for (int i = 0; i < 3; ++i) {
      // Let create complete successfully
      jobScheduler.run();
      jobScheduler.run();

      SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
      Assert.assertEquals("CREATE", task.getTaskName());

      JsonObject result = new JsonObject();
      result.addProperty("ipaddress", "111.222.333." + i);
      FinishTaskRequest finishRequest =
        new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId, task.getTaskId(),
                              null, null, 0, null, null, result);

      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);

      jobScheduler.run();
      jobScheduler.run();

      // total steps is 3 + 2 * i because there are 3 steps in the plan (create, confirm, boostrap),
      // plus 2 steps (delete success + create success) added for each confirm retry.
      // steps completed is 1 + 2 * i because there is 1 complete (create),
      // plus 2 steps (confirm failure + delete success) for each confirm retry.
      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                            ClusterAction.CLUSTER_CREATE, 3 + 2 * i, 1 + 2 * i);

      //Fail confirm.
      task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
      Assert.assertEquals("CONFIRM", task.getTaskName());

      finishRequest = new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId,
                                            task.getTaskId(), null, null, 1, null, null, result);
      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);

      jobScheduler.run();
      jobScheduler.run();

      if (i < 2) {
        // Should get DELETE task now
        task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
        Assert.assertEquals("DELETE", task.getTaskName());

        result = new JsonObject();
        result.addProperty("ipaddress", "111.222.333." + i);
        finishRequest = new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId,
                                              task.getTaskId(), null, null, 0, null, null, result);

        TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
        assertResponseStatus(response, HttpResponseStatus.OK);
      }
    }

    jobScheduler.run();
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 7, 5);
  }

  @Test
  public void testUnsolvableCluster() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-unsolvable";
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setDescription("unsolvable cluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(2)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();
    // Get the status - 0 tasks completed and TERMINATED
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    clusterScheduler.run();
    jobScheduler.run();  // run scheduler put in queue
    jobScheduler.run();  // run scheduler take from queue

    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    // Delete the cluster now.
    response = doDeleteExternalAPI("/clusters/" + clusterId, USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    jobScheduler.run();  // run scheduler put in queue
    jobScheduler.run();  // run scheduler take from queue

    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);
  }

  @Test
  public void testCancelClusterJobWithTasks() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-with-tasks";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(2)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();
    callbackScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 6, 0);

    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId);
    //Take 3 tasks and finish them
    for (int i = 0; i < 3; i++) {
      jobScheduler.run();  // run scheduler put in queue
      jobScheduler.run();  // run scheduler take from queue

      assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                            ClusterAction.CLUSTER_CREATE, 6, i);

      SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
      JsonObject result = new JsonObject();
      result.addProperty("ipaddress", "111.222.333." + i);
      FinishTaskRequest finishRequest =
        new FinishTaskRequest("workerX", PROVISIONER_ID, tenantId,
                              task.getTaskId(), null, null, 0, null, null, result);

      TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);
      assertResponseStatus(response, HttpResponseStatus.OK);
    }

    // 3 tasks are done, 3 more to go. We are also done with 1 task in a stage, with 1 remaining.
    // Now cancel the job
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    jobScheduler.run();
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 6, 3);

    // We should be not be able to take any more tasks once the job has been failed.
    SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
    Assert.assertNull(task);
    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.INCOMPLETE, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 6, 3);
  }

  @Test
  public void testCancelClusterJobNotAllowed1() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-after-solving";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    // Not possible to cancel the job before solving is done.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    // Not possible to cancel the job after solving is done.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();

    // Can cancel after job scheduler is run.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    jobScheduler.run();
    jobScheduler.run();

    // We should not be able to take any tasks after the job has been failed.
    SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), new TakeTaskRequest("workerX", PROVISIONER_ID, tenantId));
    Assert.assertNull(task);
    jobScheduler.run();

    // no tasks were run, so the cluster should be in terminated state
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    response = doGetExternalAPI("/clusters/" + clusterId, USER1_HEADERS);
    JsonObject clusterJson = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertEquals("Aborted by user.", clusterJson.get("message").getAsString());
  }

  @Test
  public void testCancelClusterJobNotAllowed2() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-after-solving";
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    // Not possible to cancel the job before solving is done.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    // Not possible to cancel the job after solving is done.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.CONFLICT);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.CLUSTER_CREATE, 0, 0);

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    // Cancel the job after cluster scheduler is done.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();

    // no tasks were taken, so cluster should be in terminated state
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    response = doGetExternalAPI("/clusters/" + clusterId, USER1_HEADERS);
    JsonObject clusterJson = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertEquals("Aborted by user.", clusterJson.get("message").getAsString());
  }

  @Test
  public void testCancelClusterNotRunningJob() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-should-be-canceled-after-solving";
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.NOT_SUBMITTED,
                          ClusterAction.SOLVE_LAYOUT, 0, 0);

    solverScheduler.run();

    clusterScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    // Cancel the job after cluster scheduler is done.
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    jobQueues.removeAll(tenantId);
    Assert.assertEquals(0, jobQueues.size(tenantId));

    jobScheduler.run();

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    // Should reschedule the job, even though it is FAILED
    response = doPostExternalAPI("/clusters/" + clusterId + "/abort", "", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    jobScheduler.run();

    // no tasks were taken so cluster should move to terminated state
    assertStatusWithUser1(clusterId, Cluster.Status.TERMINATED, ClusterJob.Status.FAILED,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    response = doGetExternalAPI("/clusters/" + clusterId, USER1_HEADERS);
    JsonObject clusterJson = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
    Assert.assertEquals("Aborted by user.", clusterJson.get("message").getAsString());
  }

  protected static void assertStatusWithUser1(String clusterId, Cluster.Status status, ClusterJob.Status actionStatus,
                                     ClusterAction action, int totalSteps, int completeSteps) throws Exception {
    // by default uses user1
    assertStatus(clusterId, status, actionStatus, action, totalSteps, completeSteps, USER1_HEADERS);
  }

  protected static void assertStatus(String clusterId, Cluster.Status status, ClusterJob.Status actionStatus,
      ClusterAction action, int totalSteps, int completeSteps, Header[] userHeaders) throws Exception {
    HttpResponse response = doGetExternalAPI(String.format("/clusters/%s/status", clusterId), userHeaders);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    ClusterStatusResponse statusResponse = gson.fromJson(reader, ClusterStatusResponse.class);

    Assert.assertEquals(clusterId, statusResponse.getClusterid());
    Assert.assertEquals(totalSteps, statusResponse.getStepstotal());
    Assert.assertEquals(completeSteps, statusResponse.getStepscompleted());
    Assert.assertEquals(actionStatus, statusResponse.getActionstatus());
    Assert.assertEquals(action, statusResponse.getAction());
    Assert.assertEquals(status, statusResponse.getStatus());
  }

  @Test
  public void testGetAllClusters() throws Exception {
    // First delete all clusters
    for (Cluster cluster : clusterStoreService.getView(USER1_ACCOUNT).getAllClusters()) {
      clusterStoreService.getView(USER1_ACCOUNT).deleteCluster(cluster.getId());
    }

    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster1")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    solverScheduler.run();

    clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster2")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(6)
      .build();
    response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    solverScheduler.run();

    response = doGetExternalAPI("/clusters", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    List<ClusterSummary> clusterInfos = gson.fromJson(reader, new TypeToken<List<ClusterSummary>>() {}.getType());

    Assert.assertNotNull(clusterInfos.get(0).getId());
    Assert.assertNotNull(clusterInfos.get(0).getCreateTime());
    Assert.assertEquals("cluster2", clusterInfos.get(0).getName());
    Assert.assertEquals("reactor-medium", clusterInfos.get(0).getClusterTemplate().getName());
    Assert.assertEquals(6, clusterInfos.get(0).getNumNodes());

    Assert.assertNotNull(clusterInfos.get(1).getId());
    Assert.assertNotNull(clusterInfos.get(1).getCreateTime());
    Assert.assertEquals("cluster1", clusterInfos.get(1).getName());
    Assert.assertEquals("reactor-medium", clusterInfos.get(1).getClusterTemplate().getName());
    Assert.assertEquals(5, clusterInfos.get(1).getNumNodes());
  }

  @Test
  public void testGetNonexistantClusterReturns404() throws Exception {
    assertResponseStatus(doGetExternalAPI("/clusters/567", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testGetServicesFromNonexistantClusterReturns404() throws Exception {
    assertResponseStatus(doGetExternalAPI("/clusters/567/services", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testGetClusterNotOwnedByUserReturns404() throws Exception {
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster1")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    assertResponseStatus(doGetExternalAPI("/clusters/" + clusterId, USER2_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAdminCanGetClustersOwnedByOthers() throws Exception {
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster1")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    assertResponseStatus(doGetExternalAPI("/clusters/" + clusterId, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  @Test
  public void testDeleteOnClusterNotOwnedByUserReturns404() throws Exception {
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster1")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String clusterId = getIdFromResponse(response);

    assertResponseStatus(doDeleteExternalAPI("/clusters/" + clusterId, USER2_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAdminCanDeleteClustersOwnedByOthers() throws Exception {
    String clusterId = "2";
    Cluster cluster = Entities.ClusterExample.createCluster();
    cluster.setStatus(Cluster.Status.ACTIVE);
    ClusterJob clusterJob = new ClusterJob(new JobId(clusterId, 1), ClusterAction.CLUSTER_DELETE);
    clusterJob.setJobStatus(ClusterJob.Status.COMPLETE);
    cluster.setLatestJobId(clusterJob.getJobId());
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    clusterStore.writeClusterJob(clusterJob);

    assertResponseStatus(doDeleteExternalAPI("/clusters/" + clusterId, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  @Test
  public void testGetAllClustersReturnsOnlyThoseOwnedByUser() throws Exception {
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster1")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String cluster1 = getIdFromResponse(response);

    clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster2")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(6)
      .build();
    response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER2_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    String cluster2 = getIdFromResponse(response);

    // check get call from user1 only gets back cluster1
    response = doGetExternalAPI("/clusters", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    List<ClusterSummary> clusterInfos = gson.fromJson(reader, new TypeToken<List<ClusterSummary>>() {}.getType());
    Assert.assertEquals(1, clusterInfos.size());
    Assert.assertEquals(cluster1, clusterInfos.get(0).getId());

    // check get call from user2 only gets back cluster2
    response = doGetExternalAPI("/clusters", USER2_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    clusterInfos = gson.fromJson(reader, new TypeToken<List<ClusterSummary>>() {}.getType());
    Assert.assertEquals(1, clusterInfos.size());
    Assert.assertEquals(cluster2, clusterInfos.get(0).getId());

    // check admin get all clusters
    response = doGetExternalAPI("/clusters", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    clusterInfos = gson.fromJson(reader, new TypeToken<List<ClusterSummary>>() {}.getType());
    Assert.assertEquals(2, clusterInfos.size());
    Set<String> ids = Sets.newHashSet();
    for (ClusterSummary clusterInfo : clusterInfos) {
      ids.add(clusterInfo.getId());
    }
    Assert.assertEquals(ImmutableSet.of(cluster1, cluster2), ids);
  }

  @Test
  public void testGetClusterServices() throws Exception {
    Map<String, Node> nodes = ImmutableMap.of(
      "node1",
      new Node("node1", "123",
               ImmutableSet.<Service>of(
                 Service.builder().setName("s1").build(),
                 Service.builder().setName("s2").build()),
               NodeProperties.builder().setHostname("node1-host").addIPAddress("access_v4", "node1-ip").build()),
      "node2",
      new Node("node2", "123",
               ImmutableSet.<Service>of(
                 Service.builder().setName("s2").build(),
                 Service.builder().setName("s3").build()),
               NodeProperties.builder().setHostname("node2-host").addIPAddress("access_v4", "node2-ip").build()));
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("my-cluster")
      .setNodes(nodes.keySet())
      .setServices(ImmutableSet.of("s1", "s2", "s3"))
      .build();
    clusterStoreService.getView(USER1_ACCOUNT).writeCluster(cluster);

    // check services
    HttpResponse response = doGetExternalAPI("/clusters/" + cluster.getId() + "/services", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Set<String> services = gson.fromJson(reader, new TypeToken<Set<String>>() {}.getType());
    Assert.assertEquals(ImmutableSet.of("s1", "s2", "s3"), services);

    // cleanup
    clusterStoreService.getView(USER1_ACCOUNT).deleteCluster(cluster.getId());
  }

  @Test
  public void testGetPlanForJob() throws Exception {
    //Create cluster
    String clusterName = "test-cluster-for-plan-job";
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName(clusterName)
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(1)
      .build();
    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    String clusterId = getIdFromResponse(response);

    solverScheduler.run();
    clusterScheduler.run();

    // Get the status - 0 tasks completed and RUNNING
    assertStatusWithUser1(clusterId, Cluster.Status.PENDING, ClusterJob.Status.RUNNING,
                          ClusterAction.CLUSTER_CREATE, 3, 0);

    // Setup expected plan
    JsonObject expected = gson.fromJson(SAMPLE_PLAN, JsonObject.class);
    JsonArray expectedAllPlans = gson.fromJson(ALL_SAMPLE_PLANS, JsonArray.class);

    // Verify plan for job
    Cluster cluster = clusterStoreService.getView(USER1_ACCOUNT).getCluster(clusterId);
    response = doGetExternalAPI("/clusters/" + clusterId + "/plans/" + cluster.getLatestJobId(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject actual = gson.fromJson(reader, JsonObject.class);

    verifyPlanJson(expected, actual);

    // Verify all plans for cluster
    response = doGetExternalAPI("/clusters/" + clusterId + "/plans", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonArray actualAllPlans = gson.fromJson(reader, JsonArray.class);
    Assert.assertEquals(expectedAllPlans.size(), actualAllPlans.size());

    for (int i = 0; i < expectedAllPlans.size(); ++i) {
      verifyPlanJson(expectedAllPlans.get(i).getAsJsonObject(), actualAllPlans.get(i).getAsJsonObject());
    }
  }

  @Test
  public void testMaxClusterSize() throws Exception {
    Configuration conf = Configuration.create();
    int maxClusterSize = conf.getInt(Constants.MAX_CLUSTER_SIZE);
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("cluster")
      .setClusterTemplateName(smallTemplate.getName())
      .setNumMachines(maxClusterSize + 1)
      .build();
    assertResponseStatus(doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testClusterCreateLeaseDuration() throws Exception {
    // Reactor template has forever initial lease duration
    verifyInitialLeaseDuration(200, HttpResponseStatus.OK, 200, reactorTemplate.getName());
    verifyInitialLeaseDuration(0, HttpResponseStatus.OK, 0, reactorTemplate.getName());
    verifyInitialLeaseDuration(0, HttpResponseStatus.OK, -1, reactorTemplate.getName());

    // Small template has 10000 initial lease duration
    verifyInitialLeaseDuration(10000, HttpResponseStatus.BAD_REQUEST, 20000, smallTemplate.getName());
    verifyInitialLeaseDuration(10000, HttpResponseStatus.OK, 10000, smallTemplate.getName());
    verifyInitialLeaseDuration(500, HttpResponseStatus.OK, 500, smallTemplate.getName());
    verifyInitialLeaseDuration(10000, HttpResponseStatus.BAD_REQUEST, 0, smallTemplate.getName());
    verifyInitialLeaseDuration(10000, HttpResponseStatus.OK, -1, smallTemplate.getName());
  }

  @Test
  public void testClusterTemplateSync() throws Exception {
    Cluster cluster = Entities.ClusterExample.createCluster();
    ClusterTemplate template = cluster.getClusterTemplate();
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStoreService.getView(USER1_ACCOUNT).writeCluster(cluster);
    clusterStore.writeNode(Entities.ClusterExample.NODE1);
    clusterStore.writeNode(Entities.ClusterExample.NODE2);

    // now edit the template
    Set<String> newCompatibleServices = Sets.newHashSet(template.getCompatibilities().getServices());
    newCompatibleServices.add("new-service");
    Compatibilities newCompatibilities =
      Compatibilities.builder()
        .setHardwaretypes(template.getCompatibilities().getHardwaretypes())
        .setImagetypes(template.getCompatibilities().getImagetypes())
        .setServices(newCompatibleServices).build();
    ClusterTemplate updatedTemplate = ClusterTemplate.builder()
      .setName(template.getName())
      .setDescription(template.getDescription())
      .setClusterDefaults(template.getClusterDefaults())
      .setCompatibilities(newCompatibilities)
      .setConstraints(template.getConstraints())
      .setAdministration(template.getAdministration())
      .build();
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(updatedTemplate);

    // now sync the cluster
    String path = "/clusters/" + cluster.getId() + "/clustertemplate/sync";
    assertResponseStatus(doPostExternalAPI(path, "", USER1_HEADERS), HttpResponseStatus.OK);

    // now check the cluster's template is as expected
    cluster = clusterStoreService.getView(cluster.getAccount()).getCluster(cluster.getId());
    cluster.getClusterTemplate().setVersion(Constants.DEFAULT_VERSION);
    Assert.assertEquals(updatedTemplate, cluster.getClusterTemplate());
  }

  @Test
  public void testClusterTemplateSync404Conditions() throws Exception {
    Cluster cluster = Entities.ClusterExample.createCluster();
    Account clusterAccount = cluster.getAccount();
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStoreService.getView(clusterAccount).writeCluster(cluster);
    clusterStore.writeNode(Entities.ClusterExample.NODE1);
    clusterStore.writeNode(Entities.ClusterExample.NODE2);
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(cluster.getClusterTemplate());
    String path = "/clusters/" + cluster.getId() + "/clustertemplate/sync";

    // test cluster that does not exist returns 404
    assertResponseStatus(
      doPostExternalAPI("/clusters/" + cluster.getId() + "1" + "/clustertemplate/sync", "", USER1_HEADERS),
      HttpResponseStatus.NOT_FOUND);

    // test cluster owned by another user returns 404
    assertResponseStatus(doPostExternalAPI(path, "", USER2_HEADERS), HttpResponseStatus.NOT_FOUND);

    // test missing template returns 404
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).deleteClusterTemplate(cluster.getClusterTemplate().getName());
    assertResponseStatus(doPostExternalAPI(path, "", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);

    // test missing nodes returns 404
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(cluster.getClusterTemplate());
    clusterStore.deleteNode(Entities.ClusterExample.NODE1.getId());
    clusterStore.deleteNode(Entities.ClusterExample.NODE2.getId());
    assertResponseStatus(doPostExternalAPI(path, "", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testClusterTemplateSyncOnlyAllowedOnActiveClusters() throws Exception {
    Cluster cluster = Entities.ClusterExample.createCluster();
    Account clusterAccount = cluster.getAccount();
    clusterStoreService.getView(clusterAccount).writeCluster(cluster);
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(cluster.getClusterTemplate());
    String path = "/clusters/" + cluster.getId() + "/clustertemplate/sync";

    // test cluster in bad state return 409
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(cluster.getClusterTemplate());
    for (Cluster.Status status : Cluster.Status.values()) {
      if (status != Cluster.Status.ACTIVE) {
        cluster.setStatus(status);
        clusterStoreService.getView(clusterAccount).writeCluster(cluster);
        assertResponseStatus(doPostExternalAPI(path, "", USER1_HEADERS), HttpResponseStatus.CONFLICT);
      }
    }
  }

  @Test
  public void testClusterTemplateSyncDisallowsIncompatibilities() throws Exception {
    Cluster cluster = Entities.ClusterExample.createCluster();
    Account clusterAccount = cluster.getAccount();
    ClusterTemplate template = cluster.getClusterTemplate();
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStoreService.getView(clusterAccount).writeCluster(cluster);
    clusterStore.writeNode(Entities.ClusterExample.NODE1);
    clusterStore.writeNode(Entities.ClusterExample.NODE2);
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(cluster.getClusterTemplate());

    // now edit the template, making centos incompatible with the template
    Set<String> newCompatibleImages = ImmutableSet.of(Entities.ImageTypeExample.UBUNTU_12.getName());
    Compatibilities newCompatibilities =
      Compatibilities.builder()
        .setHardwaretypes(template.getCompatibilities().getHardwaretypes())
        .setImagetypes(newCompatibleImages)
        .setServices(template.getCompatibilities().getServices())
        .build();
    ClusterTemplate updatedTemplate = ClusterTemplate.builder()
      .setName(template.getName())
      .setDescription(template.getDescription())
      .setClusterDefaults(template.getClusterDefaults())
      .setCompatibilities(newCompatibilities)
      .setConstraints(template.getConstraints())
      .setAdministration(template.getAdministration())
      .build();
    entityStoreService.getView(Entities.ADMIN_ACCOUNT).writeClusterTemplate(updatedTemplate);

    // syncing the cluster would make it invalid, should not be allowed
    String path = "/clusters/" + cluster.getId() + "/clustertemplate/sync";
    assertResponseStatus(doPostExternalAPI(path, "", USER1_HEADERS), HttpResponseStatus.BAD_REQUEST);
  }

  private void verifyInitialLeaseDuration(long expectedExpireTime, HttpResponseStatus expectedStatus,
                                          long requestedLeaseDuration,
                                          String clusterTemplate) throws Exception {
    ClusterCreateRequest clusterCreateRequest = ClusterCreateRequest.builder()
      .setName("test-lease")
      .setClusterTemplateName(clusterTemplate)
      .setNumMachines(4)
      .setInitialLeaseDuration(requestedLeaseDuration)
      .build();

    HttpResponse response = doPostExternalAPI("/clusters", gson.toJson(clusterCreateRequest), USER1_HEADERS);
    assertResponseStatus(response, expectedStatus);
    if (expectedStatus == HttpResponseStatus.BAD_REQUEST) {
      return;
    }

    solverScheduler.run();

    String clusterId = getIdFromResponse(response);
    Cluster cluster = clusterStoreService.getView(USER1_ACCOUNT).getCluster(clusterId);
    Assert.assertEquals(Cluster.Status.PENDING, cluster.getStatus());


    if (expectedExpireTime == 0) {
      Assert.assertEquals(expectedExpireTime, cluster.getExpireTime());
    } else {
      Assert.assertEquals(expectedExpireTime,
                          cluster.getExpireTime() == 0 ? 0 : cluster.getExpireTime() - cluster.getCreateTime());
    }
  }

  @Test
  public void testClusterProlongForever() throws Exception {
    ClusterTemplate foreverTemplate = ClusterTemplate.builder()
      .setName("forever-template")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("base")
          .setProvider("rackspace")
          .build())
      .build();

    long currentTime = 10000;
    Cluster foreverCluster = Cluster.builder()
      .setID("1002")
      .setAccount(USER1_ACCOUNT)
      .setName("prolong-test")
      .setCreateTime(currentTime)
      .setClusterTemplate(foreverTemplate)
      .build();
    foreverCluster.setExpireTime(currentTime + 10000);
    foreverCluster.setStatus(Cluster.Status.ACTIVE);
    clusterStoreService.getView(USER1_ACCOUNT).writeCluster(foreverCluster);

    HttpResponse response = doPostExternalAPI("/clusters/" + foreverCluster.getId(),
                                   "{'expireTime' : 90000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    foreverCluster = clusterStoreService.getView(USER1_ACCOUNT).getCluster(foreverCluster.getId());
    Assert.assertEquals(90000, foreverCluster.getExpireTime());
  }

  @Test
  public void testClusterProlong() throws Exception {
    ClusterTemplate template = ClusterTemplate.builder()
      .setName("limited-template")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("base")
          .setProvider("rackspace")
          .build())
      .setAdministration(new Administration(LeaseDuration.of("1s", "12s", "1s"))).build();

    long currentTime = 10000;
    Cluster cluster = Cluster.builder()
      .setID("1002")
      .setAccount(USER1_ACCOUNT)
      .setName("prolong-test")
      .setCreateTime(currentTime)
      .setClusterTemplate(template)
      .build();
    long expireTime = currentTime + 10000;
    cluster.setExpireTime(expireTime);
    cluster.setStatus(Cluster.Status.ACTIVE);
    ClusterStoreView clusterStore = clusterStoreService.getView(USER1_ACCOUNT);
    clusterStore.writeCluster(cluster);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to step size > specified
    HttpResponse response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to no expire time
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to no expire time
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'foo' : 'bar'}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should fail due to invalid expire size, since it is less than create time
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 9000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Reduction should succeed
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 19000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(19000, cluster.getExpireTime());

    // Should succeed
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 20000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(20000, cluster.getExpireTime());

    // Should succeed again
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 21000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());

    // Should succeed again
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(22000, cluster.getExpireTime());

    // Try again should fail since it exceeds max
    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 23000}",
                                   ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(22000, cluster.getExpireTime());

    // Expire time of incomplete cluster can be changed
    // Put cluster in incomplete state
    cluster.setStatus(Cluster.Status.INCOMPLETE);
    clusterStore.writeCluster(cluster);

    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 21000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());

    // Any attempt to change cluster expire time should fail on pending cluster
    // Put cluster in incomplete state
    cluster.setStatus(Cluster.Status.PENDING);
    clusterStore.writeCluster(cluster);

    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());

    // Any attempt to change cluster expire time should fail on terminated cluster
    // Terminate cluster
    cluster.setStatus(Cluster.Status.TERMINATED);
    clusterStore.writeCluster(cluster);

    response = doPostExternalAPI("/clusters/" + cluster.getId(), "{'expireTime' : 22000}",
                      ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.BAD_REQUEST);

    cluster = clusterStore.getCluster(cluster.getId());
    Assert.assertEquals(21000, cluster.getExpireTime());
  }

  @Test
  public void testInvalidGetClusterConfigRequests() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("get-config-test")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);

    assertResponseStatus(doGetExternalAPI("/clusters/" + cluster.getId() + "9/config", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGetExternalAPI("/clusters/" + cluster.getId() + "/config", USER2_HEADERS),
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
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("get-config-test")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setConfig(config)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    HttpResponse response = doGetExternalAPI("/clusters/" + cluster.getId() + "/config", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject actual = gson.fromJson(reader, JsonObject.class);
    Assert.assertEquals(config, actual);
  }

  @Test
  public void testInvalidClusterConfigRequests() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("get-config-test")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    String requestStr = gson.toJson(new ClusterConfigureRequest(null, new JsonObject(), false));

    assertResponseStatus(doPutExternalAPI("/clusters/" + cluster.getId() + "/config", "{}", USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);

    assertResponseStatus(doPutExternalAPI("/clusters/" + cluster.getId() + "9/config", requestStr, USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPutExternalAPI("/clusters/" + cluster.getId() + "/config", requestStr, USER2_HEADERS),
                         HttpResponseStatus.NOT_FOUND);

    cluster.setStatus(Cluster.Status.INCOMPLETE);
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);

    assertResponseStatus(doPutExternalAPI("/clusters/" + cluster.getId() + "/config", requestStr, USER1_HEADERS),
                         HttpResponseStatus.CONFLICT);
  }

  @Test
  public void testPutClusterConfigCanRunOnInconsistentClusters() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("get-config-test")
      .setProvider(Entities.ProviderExample.JOYENT)
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .build();
    cluster.setStatus(Cluster.Status.INCONSISTENT);
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    String requestStr = gson.toJson(new ClusterConfigureRequest(null, new JsonObject(), false));

    assertResponseStatus(doPutExternalAPI("/clusters/" + cluster.getId() + "/config", requestStr, USER1_HEADERS),
                         HttpResponseStatus.OK);
  }

  @Test
  public void testPutClusterConfig() throws Exception {
    JsonObject originalConfig = new JsonObject();
    originalConfig.addProperty("key1", "val1");
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("get-config-test")
      .setProvider(Entities.ProviderExample.JOYENT)
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setConfig(originalConfig)
      .build();
    cluster.setStatus(Cluster.Status.ACTIVE);
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);

    HttpResponse response = doGetExternalAPI("/clusters/123/config", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject actual = gson.fromJson(reader, JsonObject.class);
    Assert.assertEquals(originalConfig, actual);

    JsonObject newConfig = new JsonObject();
    newConfig.addProperty("key2", "val2");
    ClusterConfigureRequest configRequest = new ClusterConfigureRequest(null, newConfig, false);
    assertResponseStatus(doPutExternalAPI("/clusters/123/config", gson.toJson(configRequest), USER1_HEADERS),
                         HttpResponseStatus.OK);

    response = doGetExternalAPI("/clusters/123/config", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    actual = gson.fromJson(reader, JsonObject.class);
    Assert.assertEquals(newConfig, actual);
  }

  @Test
  public void testClusterServiceActions() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("service-actions")
      .setProvider(Entities.ProviderExample.JOYENT)
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setServices(ImmutableSet.<String>of("namenode", "datanode"))
      .build();
    cluster.setStatus(Cluster.Status.ACTIVE);
    ClusterStoreView clusterStore = clusterStoreService.getView(cluster.getAccount());
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
        assertResponseStatus(doPostExternalAPI("/clusters/123/services" + entry.getKey(), "", USER1_HEADERS),
                             HttpResponseStatus.OK);
        Assert.assertEquals(entry.getValue().name(),
                            clusterQueues.take(cluster.getAccount().getTenantId(), "0").getValue());
        cluster.setStatus(Cluster.Status.ACTIVE);
        clusterStore.writeCluster(cluster);
      }
    } finally {
      clusterQueues.removeAll();
    }
  }

  @Test
  public void testServiceActionsOnNonexistantClusterReturn404() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("test-cluster")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setStatus(Cluster.Status.ACTIVE)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
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
      assertResponseStatus(doPostExternalAPI("/clusters/1123/services" + action, "", USER1_HEADERS),
                           HttpResponseStatus.NOT_FOUND);
      // no cluster for user2
      assertResponseStatus(doPostExternalAPI("/clusters/123/services" + action, "", USER2_HEADERS),
                           HttpResponseStatus.NOT_FOUND);
    }
  }

  @Test
  public void testServiceActionsOnNonexistantClusterServiceReturn404() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("test-cluster")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setStatus(Cluster.Status.ACTIVE)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    assertResponseStatus(doPostExternalAPI("/clusters/123/services/fake/stop", "", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPostExternalAPI("/clusters/123/services/fake/start", "", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPostExternalAPI("/clusters/123/services/fake/restart", "", USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testServiceActionsCanOnlyRunOnActiveCluster() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("test-cluster")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setServices(ImmutableSet.<String>of("namenode", "datanode"))
      .build();
    Set<Cluster.Status> badStatuses = ImmutableSet.of(
      Cluster.Status.INCOMPLETE, Cluster.Status.PENDING, Cluster.Status.TERMINATED, Cluster.Status.INCONSISTENT);
    Set<String> resources = ImmutableSet.of(
      "/clusters/123/services/stop",
      "/clusters/123/services/start",
      "/clusters/123/services/restart",
      "/clusters/123/services/namenode/stop",
      "/clusters/123/services/namenode/start",
      "/clusters/123/services/namenode/restart"
    );
    for (Cluster.Status status : badStatuses) {
      cluster.setStatus(status);
      clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
      for (String resource : resources) {
        assertResponseStatus(doPostExternalAPI(resource, "", USER1_HEADERS), HttpResponseStatus.CONFLICT);
      }
    }
  }

  @Test
  public void testAddInvalidServicesReturns400() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("test-cluster")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setServices(ImmutableSet.<String>of("namenode", "datanode"))
      .setStatus(Cluster.Status.ACTIVE)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    // can't add nodemanager without resourcemanager
    AddServicesRequest body = new AddServicesRequest(null, ImmutableSet.of("nodemanager"));
    assertResponseStatus(doPostExternalAPI("/clusters/123/services", gson.toJson(body), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
    // can't add nonexistant service
    body = new AddServicesRequest(null, ImmutableSet.of("fakeservice"));
    assertResponseStatus(doPostExternalAPI("/clusters/123/services", gson.toJson(body), USER1_HEADERS),
                         HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testAddServicesOnNonexistantClusterReturns404() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("test-cluster")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setServices(ImmutableSet.<String>of("namenode", "datanode"))
      .setStatus(Cluster.Status.ACTIVE)
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    AddServicesRequest body = new AddServicesRequest(null, ImmutableSet.of("resourcemanager", "nodemanager"));
    assertResponseStatus(doPostExternalAPI("/clusters/1123/services", gson.toJson(body), USER1_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doPostExternalAPI("/clusters/123/services", gson.toJson(body), USER2_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testAddServicesCanOnlyRunOnActiveCluster() throws Exception {
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(USER1_ACCOUNT)
      .setName("test-cluster")
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setServices(ImmutableSet.<String>of("namenode", "datanode"))
      .build();
    Set<Cluster.Status> badStatuses = ImmutableSet.of(
      Cluster.Status.INCOMPLETE, Cluster.Status.PENDING, Cluster.Status.TERMINATED, Cluster.Status.INCONSISTENT);
    AddServicesRequest body = new AddServicesRequest(null, ImmutableSet.of("resourcemanager", "nodemanager"));
    for (Cluster.Status status : badStatuses) {
      cluster.setStatus(status);
      clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
      assertResponseStatus(doPostExternalAPI("/clusters/123/services", gson.toJson(body), USER1_HEADERS),
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
    return gson.fromJson(reader, JsonObject.class).get("id").getAsString();
  }

  @BeforeClass
  public static void initData() throws Exception {
    Set<String> services = ImmutableSet.of("namenode", "datanode", "resourcemanager", "nodemanager",
                                           "hbasemaster", "regionserver", "zookeeper", "reactor");
    reactorTemplate = ClusterTemplate.builder()
      .setName("reactor-medium")
      .setDescription("medium reactor cluster template")
      .setClusterDefaults(ClusterDefaults.builder().setServices(services).setProvider("joyent").build())
      .setCompatibilities(
        Compatibilities.builder().setHardwaretypes("large-mem", "large-cpu", "large", "medium", "small").build())
      .setConstraints(
        new Constraints(
          ImmutableMap.<String, ServiceConstraint>of(
            "namenode",
            new ServiceConstraint(
              ImmutableSet.of("large-mem"),
              ImmutableSet.of("centos6", "ubuntu12"), 1, 1),
            "datanode",
            new ServiceConstraint(
              ImmutableSet.of("medium", "large-cpu"),
              ImmutableSet.of("centos6", "ubuntu12"), 1, 50),
            "zookeeper",
            new ServiceConstraint(
              ImmutableSet.of("small", "medium"),
              ImmutableSet.of("centos6"), 1, 5),
            "reactor",
            new ServiceConstraint(
              ImmutableSet.of("medium", "large"),
              null, 1, 5)
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
          ),
          new SizeConstraint(2, 50)
        )).build();

    defaultClusterConfig = new JsonObject();
    defaultClusterConfig.addProperty("defaultconfig", "value1");

    smallTemplate = ClusterTemplate.builder()
      .setName("one-machine")
      .setDescription("one machine cluster template")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("zookeeper")
          .setProvider("joyent")
          .setConfig(defaultClusterConfig)
          .build())
      .setCompatibilities(Compatibilities.builder().setServices("zookeeper").build())
      .setAdministration(new Administration(LeaseDuration.of("10s", "30s", "5s")))
      .build();

    EntityStoreView adminView = entityStoreService.getView(ADMIN_ACCOUNT);
    EntityStoreView superadminView = entityStoreService.getView(SUPERADMIN_ACCOUNT);
    superadminView.writeProviderType(Entities.ProviderTypeExample.JOYENT);
    superadminView.writeProviderType(Entities.ProviderTypeExample.RACKSPACE);
    // create providers
    adminView.writeProvider(Entities.ProviderExample.JOYENT);
    adminView.writeProvider(Entities.ProviderExample.RACKSPACE);
    // create hardware types
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Medium 4GB")))
      .setName("medium")
      .build()
    );
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 32GB")))
        .setName("large-mem")
        .build()
    );
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 16GB")))
        .setName("large-cpu")
        .build()
    );
    // create image types
    adminView.writeImageType(
      ImageType.builder().setProviderMap(ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.<String, String>of("image", "joyent-hash-of-centos6.4")))
        .setName("centos6")
        .build()
    );
    // create services
    for (String serviceName : services) {
      adminView.writeService(Service.builder().setName(serviceName).build());
    }
    adminView.writeClusterTemplate(reactorTemplate);
    adminView.writeClusterTemplate(smallTemplate);
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
