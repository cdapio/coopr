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

import com.continuuity.loom.TestHelper;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.http.request.FinishTaskRequest;
import com.continuuity.loom.http.request.TakeTaskRequest;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 *
 */
public class LoomTaskHandlerTest extends LoomServiceTestBase {

  @After
  public void cleanupTaskHandlerTest() {
    provisionerQueues.removeAll();
  }

  @Test
  public void testTakeTask() throws Exception {
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), "node_id", "service", ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);
    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(clusterTask)));

    TakeTaskRequest takeRequest = new TakeTaskRequest("worker1", PROVISIONER_ID, TENANT_ID);
    HttpResponse response = doPost("/v1/loom/tasks/take", gson.toJson(takeRequest));
    assertResponseStatus(response, HttpResponseStatus.OK);
    JsonObject responseJson = getResponseJson(response);
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());
  }

  @Test
  public void testTakeTaskForDeadProvisionerErrors() throws Exception {
    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", "nonexistant-provider", "tenantY");
    assertResponseStatus(doPost("/v1/loom/tasks/take", gson.toJson(takeRequest)), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testFinishTaskForDeadProvisionerErrors() throws Exception {
    FinishTaskRequest finishRequest = new FinishTaskRequest("workerX", "nonexistant-provider", "tenantY", "taskId",
                                                            "stdout", "stderr", 0, null);
    assertResponseStatus(doPost("/v1/loom/tasks/finish", gson.toJson(finishRequest)), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testFinishTask() throws Exception {
    String tenantId = USER1_ACCOUNT.getTenantId();
    Node node = new Node("node_id1", "1", ImmutableSet.<Service>of(), TestHelper.nodePropertiesOf("host", null));
    clusterStore.writeNode(node);

    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), node.getId(), "service", ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterTask(clusterTask);

    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);

    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(clusterTask)));

    TakeTaskRequest takeRequest = new TakeTaskRequest("worker1", PROVISIONER_ID, tenantId);
    JsonObject responseJson = TestHelper.takeTask(getBaseUrl(), takeRequest);
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());

    JsonObject provisionerResult = new JsonObject();
    provisionerResult.addProperty("ip", "127.0.0.1");
    provisionerResult.addProperty("ssh_key", "id-rsa");

    FinishTaskRequest finishRequest =
      new FinishTaskRequest("worker1", PROVISIONER_ID, tenantId, clusterTask.getTaskId(),
                            "some stdout", "some stderr", 0, provisionerResult);
    TestHelper.finishTask(getBaseUrl(), finishRequest);

    ClusterTask actualTask = clusterStore.getClusterTask(TaskId.fromString(clusterTask.getTaskId()));
    Assert.assertEquals(ClusterTask.Status.COMPLETE, actualTask.getStatus());

    Node actualNode = clusterStore.getNode(clusterTask.getNodeId());
    Assert.assertNotNull(actualNode);
    Node.Action lastAction = actualNode.getActions().get(actualNode.getActions().size() - 1);
    Assert.assertEquals(lastAction.getStatus(), Node.Status.COMPLETE);
    Assert.assertEquals(provisionerResult, actualNode.getProvisionerResults());

    Assert.assertNull(provisionerQueues.take("worker1"));
  }

  @Test
  public void testFailTask() throws Exception {
    String tenantId = USER1_ACCOUNT.getTenantId();
    Node node = new Node("node_id2", "1", ImmutableSet.<Service>of(), TestHelper.nodePropertiesOf("host", null));
    clusterStore.writeNode(node);

    Cluster cluster = new Cluster("1", USER1_ACCOUNT, "cluster1" , System.currentTimeMillis(), "", null, null,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of());
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);

    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), node.getId(), "service", ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);
    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(clusterTask)));

    TakeTaskRequest takeRequest = new TakeTaskRequest("worker1", PROVISIONER_ID, tenantId);
    JsonObject responseJson = TestHelper.takeTask(getBaseUrl(), takeRequest);
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());

    FinishTaskRequest finishRequest =
      new FinishTaskRequest("worker1", PROVISIONER_ID, tenantId, clusterTask.getTaskId(),
                            "some stdout", "some stderr", 1, null);
    TestHelper.finishTask(getBaseUrl(), finishRequest);

    ClusterTask actualTask = clusterStore.getClusterTask(TaskId.fromString(clusterTask.getTaskId()));
    Assert.assertEquals(ClusterTask.Status.FAILED, actualTask.getStatus());

    Node actualNode = clusterStore.getNode(clusterTask.getNodeId());
    Assert.assertNotNull(actualNode);
    Node.Action lastAction = actualNode.getActions().get(actualNode.getActions().size() - 1);
    Assert.assertEquals(lastAction.getStatus(), Node.Status.FAILED);
    Assert.assertEquals("some stdout", lastAction.getStdout());
    Assert.assertEquals("some stderr", lastAction.getStderr());

    Element element = provisionerQueues.take(tenantId, "worker1");
    Assert.assertNull(element);
  }

  private JsonObject getResponseJson(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    return gson.fromJson(reader, JsonObject.class);
  }
}
