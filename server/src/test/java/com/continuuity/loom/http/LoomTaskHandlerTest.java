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
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 *
 */
public class LoomTaskHandlerTest extends LoomServiceTestBase {
  private static final Gson GSON = new JsonSerde().getGson();

  @Test
  public void testTakeTask() throws Exception {
    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), "node_id", "service", ClusterAction.CLUSTER_CREATE,
      new JsonObject());
    clusterStoreService.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStoreService.writeClusterJob(clusterJob);
    nodeProvisionTaskQueue.add(new Element(clusterTask.getTaskId(), GSON.toJson(clusterTask)));

    HttpResponse response = doPost("/v1/loom/tasks/take", "{ \"workerId\":\"worker1\" }");
    assertResponseStatus(response, HttpResponseStatus.OK);
    JsonObject responseJson = getResponseJson(response);
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());

    nodeProvisionTaskQueue.removeAll();
  }

  @Test
  public void testFinishTask() throws Exception {
    Node node = new Node("node_id1", "1", ImmutableSet.<Service>of(), ImmutableMap.<String, String>of());
    clusterStoreService.writeNode(node);

    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), node.getId(), "service", ClusterAction.CLUSTER_CREATE,
      new JsonObject());
    clusterStoreService.writeClusterTask(clusterTask);

    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStoreService.writeClusterJob(clusterJob);

    nodeProvisionTaskQueue.add(new Element(clusterTask.getTaskId(), GSON.toJson(clusterTask)));

    JsonObject responseJson = TestHelper.takeTask(getBaseUrl(), "worker1");
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());

    JsonObject finishResponse = new JsonObject();
    finishResponse.addProperty("workerId", "worker1");
    finishResponse.addProperty("taskId", clusterTask.getTaskId());
    finishResponse.addProperty("status", "0");
    JsonObject provisionerResult = new JsonObject();
    provisionerResult.addProperty("ip", "127.0.0.1");
    provisionerResult.addProperty("ssh_key", "id-rsa");
    provisionerResult.add(Node.Properties.AUTOMATORS.name().toLowerCase(), new JsonArray());
    provisionerResult.add(Node.Properties.SERVICES.name().toLowerCase(), new JsonArray());
    finishResponse.add("result", provisionerResult);

    TestHelper.finishTask(getBaseUrl(), finishResponse);

    ClusterTask actualTask = clusterStoreService.getClusterTask(TaskId.fromString(clusterTask.getTaskId()));
    Assert.assertEquals(ClusterTask.Status.COMPLETE, actualTask.getStatus());

    Node actualNode = clusterStoreService.getNode(clusterTask.getNodeId());
    Assert.assertNotNull(actualNode);
    Node.Action lastAction = actualNode.getActions().get(actualNode.getActions().size() - 1);
    Assert.assertEquals(lastAction.getStatus(), Node.Status.COMPLETE);
    Assert.assertEquals(provisionerResult, actualNode.getProperties());

    Assert.assertNull(nodeProvisionTaskQueue.take("worker1"));

    nodeProvisionTaskQueue.removeAll();
  }

  @Test
  public void testFailTask() throws Exception {
    Node node = new Node("node_id2", "1", ImmutableSet.<Service>of(), ImmutableMap.<String, String>of());
    clusterStoreService.writeNode(node);

    Cluster cluster = new Cluster("1", USER1_ACCOUNT, "cluster1" , System.currentTimeMillis(), "", null, null,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of());
    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);

    ClusterTask clusterTask = new ClusterTask(
      ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), node.getId(), "service", ClusterAction.CLUSTER_CREATE,
      new JsonObject());
    clusterStoreService.writeClusterTask(clusterTask);
    ClusterJob clusterJob = new ClusterJob(JobId.fromString("1-1"), ClusterAction.CLUSTER_CREATE);
    clusterStoreService.writeClusterJob(clusterJob);
    nodeProvisionTaskQueue.add(new Element(clusterTask.getTaskId(), GSON.toJson(clusterTask)));

    JsonObject responseJson = TestHelper.takeTask(getBaseUrl(), "worker1");
    Assert.assertEquals(clusterTask.getTaskId(), responseJson.get("taskId").getAsString());

    JsonObject finishResponse = new JsonObject();
    finishResponse.addProperty("workerId", "worker1");
    finishResponse.addProperty("taskId", clusterTask.getTaskId());
    finishResponse.addProperty("status", "1");
    finishResponse.addProperty("stdout", "some stdout");
    finishResponse.addProperty("stderr", "some stderr");

    TestHelper.finishTask(getBaseUrl(), finishResponse);

    ClusterTask actualTask = clusterStoreService.getClusterTask(TaskId.fromString(clusterTask.getTaskId()));
    Assert.assertEquals(ClusterTask.Status.FAILED, actualTask.getStatus());

    Node actualNode = clusterStoreService.getNode(clusterTask.getNodeId());
    Assert.assertNotNull(actualNode);
    Node.Action lastAction = actualNode.getActions().get(actualNode.getActions().size() - 1);
    Assert.assertEquals(lastAction.getStatus(), Node.Status.FAILED);
    Assert.assertEquals("some stdout", lastAction.getStdout());
    Assert.assertEquals("some stderr", lastAction.getStderr());

    Element element = nodeProvisionTaskQueue.take("worker1");
    Assert.assertNull(element);

    nodeProvisionTaskQueue.removeAll();
  }

  private JsonObject getResponseJson(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    return GSON.fromJson(reader, JsonObject.class);
  }
}
