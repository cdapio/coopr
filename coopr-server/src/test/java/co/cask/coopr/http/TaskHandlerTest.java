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
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.http.request.FinishTaskRequest;
import co.cask.coopr.http.request.TakeTaskRequest;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.SchedulableTask;
import co.cask.coopr.scheduler.task.TaskConfig;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.scheduler.task.TaskServiceAction;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.template.ClusterTemplate;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
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
public class TaskHandlerTest extends ServiceTestBase {

  @After
  public void cleanupTaskHandlerTest() {
    provisionerQueues.removeAll();
  }

  @Test
  public void testTakeTask() throws Exception {
    String tenantId = USER1_ACCOUNT.getTenantId();
    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), "node_id", "service", ClusterAction.CLUSTER_CREATE,
      "test", USER1_ACCOUNT);
    clusterStore.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);
    TaskConfig taskConfig = new TaskConfig(
      NodeProperties.builder().build(),
      Entities.ProviderExample.JOYENT,
      ImmutableMap.<String, NodeProperties>of(),
      new TaskServiceAction("svcA", new ServiceAction("shell", ImmutableMap.<String, String>of())),
      new JsonObject(),
      new JsonObject()
    );
    SchedulableTask schedulableTask= new SchedulableTask(clusterTask, taskConfig);
    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(schedulableTask)));

    TakeTaskRequest takeRequest = new TakeTaskRequest("worker1", PROVISIONER_ID, TENANT_ID);
    HttpResponse response = doPostInternalAPI("/tasks/take", gson.toJson(takeRequest));
    assertResponseStatus(response, HttpResponseStatus.OK);
    JsonObject responseJson = getResponseJson(response);
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());
  }

  @Test
  public void testTakeTaskForDeadProvisionerErrors() throws Exception {
    TakeTaskRequest takeRequest = new TakeTaskRequest("workerX", "nonexistant-provider", "tenantY");
    assertResponseStatus(doPostInternalAPI("/tasks/take", gson.toJson(takeRequest)), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testFinishTaskForDeadProvisionerErrors() throws Exception {
    FinishTaskRequest finishRequest = new FinishTaskRequest("workerX", "nonexistant-provider", "tenantY", "taskId",
                                                            "stdout", "stderr", 0, null, null, null);
    assertResponseStatus(doPostInternalAPI("/tasks/finish", gson.toJson(finishRequest)), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testFinishTask() throws Exception {
    String tenantId = USER1_ACCOUNT.getTenantId();
    Node node = new Node("node_id1", "1", ImmutableSet.<Service>of(),
                         NodeProperties.builder().setHostname("host").build());
    clusterStore.writeNode(node);

    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), node.getId(), "service", ClusterAction.CLUSTER_CREATE,
      "test", USER1_ACCOUNT);
    clusterStore.writeClusterTask(clusterTask);

    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);

    TaskConfig taskConfig = new TaskConfig(
      node.getProperties(),
      Entities.ProviderExample.JOYENT,
      ImmutableMap.<String, NodeProperties>of(node.getId(), node.getProperties()),
      new TaskServiceAction("svcA", new ServiceAction("shell", ImmutableMap.<String, String>of())),
      new JsonObject(),
      new JsonObject()
    );
    SchedulableTask schedulableTask= new SchedulableTask(clusterTask, taskConfig);
    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(schedulableTask)));

    TakeTaskRequest takeRequest = new TakeTaskRequest("worker1", PROVISIONER_ID, tenantId);
    SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
    Assert.assertEquals(clusterTask.getTaskId(), task.getTaskId());

    JsonObject provisionerResult = new JsonObject();
    provisionerResult.addProperty("ip", "127.0.0.1");
    provisionerResult.addProperty("ssh_key", "id-rsa");

    FinishTaskRequest finishRequest =
      new FinishTaskRequest("worker1", PROVISIONER_ID, tenantId, clusterTask.getTaskId(),
                            "some stdout", "some stderr", 0, null, null, provisionerResult);
    TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);

    ClusterTask actualTask = clusterStore.getClusterTask(TaskId.fromString(clusterTask.getTaskId()));
    Assert.assertEquals(ClusterTask.Status.COMPLETE, actualTask.getStatus());

    Node actualNode = clusterStore.getNode(clusterTask.getNodeId());
    Assert.assertNotNull(actualNode);
    Node.Action lastAction = actualNode.getActions().get(actualNode.getActions().size() - 1);
    Assert.assertEquals(lastAction.getStatus(), Node.Status.COMPLETE);
    Assert.assertEquals(provisionerResult, actualNode.getProvisionerResults());

    Assert.assertFalse(provisionerQueues.takeIterator("worker1").hasNext());
  }

  @Test
  public void testFailTask() throws Exception {
    String tenantId = USER1_ACCOUNT.getTenantId();
    Node node = new Node("node_id2", "1", ImmutableSet.<Service>of(),
                         NodeProperties.builder().setHostname("host").build());
    clusterStore.writeNode(node);

    Cluster cluster = Cluster.builder()
      .setID("1")
      .setAccount(USER1_ACCOUNT)
      .setName("cluster1")
      .build();
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);

    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), node.getId(), "service", ClusterAction.CLUSTER_CREATE,
      "test", cluster.getAccount());
    clusterStore.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStore.writeClusterJob(clusterJob);
    TaskConfig taskConfig = new TaskConfig(
      node.getProperties(),
      Entities.ProviderExample.JOYENT,
      ImmutableMap.<String, NodeProperties>of(node.getId(), node.getProperties()),
      new TaskServiceAction("svcA", new ServiceAction("shell", ImmutableMap.<String, String>of())),
      new JsonObject(),
      new JsonObject()
    );
    SchedulableTask schedulableTask= new SchedulableTask(clusterTask, taskConfig);
    provisionerQueues.add(tenantId, new Element(clusterTask.getTaskId(), gson.toJson(schedulableTask)));

    TakeTaskRequest takeRequest = new TakeTaskRequest("worker1", PROVISIONER_ID, tenantId);
    SchedulableTask task = TestHelper.takeTask(getBaseUrlInternalAPI(), takeRequest);
    Assert.assertEquals(clusterTask.getTaskId(), task.getTaskId());

    FinishTaskRequest finishRequest =
      new FinishTaskRequest("worker1", PROVISIONER_ID, tenantId, clusterTask.getTaskId(),
                            "some stdout", "some stderr", 1, null, null, null);
    TestHelper.finishTask(getBaseUrlInternalAPI(), finishRequest);

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
