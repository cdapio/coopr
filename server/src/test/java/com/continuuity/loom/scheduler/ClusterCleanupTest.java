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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.http.LoomServiceTestBase;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterService;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.NodeService;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.ClusterStore;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.gson.JsonObject;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Test ClusterCleanup.
 */
public class ClusterCleanupTest extends LoomServiceTestBase {
  static ClusterStore clusterStore;
  static TrackingQueue provisionQueue;
  static TrackingQueue clusterQueue;
  static TrackingQueue jobQueue;
  static NodeService nodeService;
  static ClusterService clusterService;
  static TaskService taskService;

  @BeforeClass
  public static void initTest() {
    clusterStore = injector.getInstance(ClusterStore.class);
    provisionQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.PROVISIONER)));
    clusterQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.CLUSTER)));
    jobQueue = injector.getInstance(
      Key.get(TrackingQueue.class, Names.named(Constants.Queue.JOB)));
    nodeService = injector.getInstance(NodeService.class);
    clusterService = injector.getInstance(ClusterService.class);
    taskService = injector.getInstance(TaskService.class);

    // Stop scheduler
    Scheduler scheduler = injector.getInstance(Scheduler.class);
    scheduler.stopAndWait();
  }

  @Test
  public void testCleanup() throws Exception {
    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, nodeService, clusterService, taskService,
                                                       provisionQueue, jobQueue, 1, 1, 1);

    jobQueue.removeAll();
    nodeProvisionTaskQueue.removeAll();

    ClusterTask task1 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-1-1"), "node1", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task2 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-2-2"), "node2", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task3 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-3-3"), "node3", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task4 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-4-4"), "node3", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());

    task1.setStatus(ClusterTask.Status.IN_PROGRESS);
    task2.setStatus(ClusterTask.Status.IN_PROGRESS);
    task3.setStatus(ClusterTask.Status.IN_PROGRESS);
    task4.setStatus(ClusterTask.Status.IN_PROGRESS);

    clusterStore.writeClusterTask(task1);
    clusterStore.writeClusterTask(task2);
    clusterStore.writeClusterTask(task3);
    clusterStore.writeClusterTask(task4);

    Node node1 = new Node("node1", "2", ImmutableSet.<Service>of(), ImmutableMap.<String, String>of());
    nodeService.startAction(node1, task1.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node1.getActions().get(0).getStatus());

    Node node2 = new Node("node2", "2", ImmutableSet.<Service>of(), ImmutableMap.<String, String>of());
    nodeService.startAction(node2, task2.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node2.getActions().get(0).getStatus());

    Node node3 = new Node("node3", "2", ImmutableSet.<Service>of(), ImmutableMap.<String, String>of());
    nodeService.startAction(node3, task3.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node3.getActions().get(0).getStatus());

    Node node4 = new Node("node4", "2", ImmutableSet.<Service>of(), ImmutableMap.<String, String>of());
    nodeService.startAction(node4, task4.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node4.getActions().get(0).getStatus());

    Assert.assertTrue(jobQueue.removeAll());
    Assert.assertEquals(0, Iterators.size(nodeProvisionTaskQueue.getQueued()));
    Assert.assertEquals(0, Iterators.size(nodeProvisionTaskQueue.getBeingConsumed()));

    nodeProvisionTaskQueue.add(new Element(task1.getTaskId(), ""));
    nodeProvisionTaskQueue.add(new Element(task2.getTaskId(), ""));
    nodeProvisionTaskQueue.add(new Element(task3.getTaskId(), ""));
    nodeProvisionTaskQueue.add(new Element(task4.getTaskId(), ""));

    Assert.assertEquals(4, Iterators.size(nodeProvisionTaskQueue.getQueued()));

    nodeProvisionTaskQueue.take("consumer1");
    nodeProvisionTaskQueue.take("consumer2");
    Assert.assertEquals(2, Iterators.size(nodeProvisionTaskQueue.getBeingConsumed()));
    Assert.assertEquals(2, Iterators.size(nodeProvisionTaskQueue.getQueued()));

    TimeUnit.SECONDS.sleep(1);

    nodeProvisionTaskQueue.take("consumer3");
    Assert.assertEquals(3, Iterators.size(nodeProvisionTaskQueue.getBeingConsumed()));
    Assert.assertEquals(1, Iterators.size(nodeProvisionTaskQueue.getQueued()));

    clusterCleanup.run();

    Assert.assertEquals(1, Iterators.size(nodeProvisionTaskQueue.getBeingConsumed()));
    Assert.assertEquals(1, Iterators.size(nodeProvisionTaskQueue.getQueued()));
    Assert.assertEquals(2, jobQueue.size());

    Assert.assertEquals(ClusterTask.Status.FAILED,
                        clusterStore.getClusterTask(TaskId.fromString(task1.getTaskId())).getStatus());
    Assert.assertEquals(ClusterTask.Status.FAILED,
                        clusterStore.getClusterTask(TaskId.fromString(task2.getTaskId())).getStatus());
    Assert.assertEquals(ClusterTask.Status.IN_PROGRESS,
                        clusterStore.getClusterTask(TaskId.fromString(task3.getTaskId())).getStatus());
    Assert.assertEquals(ClusterTask.Status.IN_PROGRESS,
                        clusterStore.getClusterTask(TaskId.fromString(task4.getTaskId())).getStatus());

    Assert.assertEquals(Node.Status.FAILED, clusterStore.getNode("node1").getActions().get(0).getStatus());
    Assert.assertEquals(Node.Status.FAILED, clusterStore.getNode("node2").getActions().get(0).getStatus());
    Assert.assertEquals(Node.Status.IN_PROGRESS, clusterStore.getNode("node3").getActions().get(0).getStatus());
    Assert.assertEquals(Node.Status.IN_PROGRESS, clusterStore.getNode("node4").getActions().get(0).getStatus());

    jobQueue.removeAll();
    nodeProvisionTaskQueue.removeAll();
  }

  @SuppressWarnings("UnusedDeclaration")
  @Test
  public void testExpire() throws Exception {
    Cluster cluster1 = createCluster("1001", System.currentTimeMillis() - 1000, System.currentTimeMillis() - 100,
                                     Cluster.Status.INCOMPLETE);
    Cluster cluster2 = createCluster("1002", System.currentTimeMillis() - 1000, System.currentTimeMillis() + 100000,
                                     Cluster.Status.INCOMPLETE);

    Cluster cluster3 = createCluster("1003", System.currentTimeMillis() - 1000, System.currentTimeMillis() - 100,
                                     Cluster.Status.ACTIVE);
    Cluster cluster4 = createCluster("1004", System.currentTimeMillis() - 1000, System.currentTimeMillis() + 100000,
                                     Cluster.Status.ACTIVE);

    Cluster cluster5 = createCluster("1005", System.currentTimeMillis() - 1000, System.currentTimeMillis() - 100,
                                     Cluster.Status.PENDING);
    Cluster cluster6 = createCluster("1006", System.currentTimeMillis() - 1000, System.currentTimeMillis() + 100000,
                                     Cluster.Status.PENDING);

    Cluster cluster7 = createCluster("1007", System.currentTimeMillis() - 1000, System.currentTimeMillis() - 100,
                                     Cluster.Status.TERMINATED);
    Cluster cluster8 = createCluster("1008", System.currentTimeMillis() - 1000, System.currentTimeMillis() + 100000,
                                     Cluster.Status.TERMINATED);

    Cluster clusterForever = createCluster("1000", System.currentTimeMillis() - 1000, 0, Cluster.Status.ACTIVE);

    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, nodeService, clusterService, taskService,
                                                       provisionQueue, jobQueue, 1, 1, 1);

    clusterQueue.removeAll();
    Assert.assertEquals(0, Iterators.size(clusterQueue.getQueued()));

    clusterCleanup.run();

    Assert.assertEquals(2, Iterators.size(clusterQueue.getQueued()));

    Element e1 = clusterQueue.take("consumer1");
    Element e2 = clusterQueue.take("consumer1");
    Assert.assertEquals(ImmutableSet.of("1001", "1003"), ImmutableSet.of(e1.getId(), e2.getId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e1.getValue());
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e2.getValue());

    ClusterJob delJob1 = clusterStore.getClusterJob(JobId.fromString(clusterStore.getCluster("1001").getLatestJobId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE, delJob1.getClusterAction());

    ClusterJob delJob2 = clusterStore.getClusterJob(JobId.fromString(clusterStore.getCluster("1003").getLatestJobId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE, delJob2.getClusterAction());

    clusterQueue.removeAll();
  }

  @Test
  public void testQueuedTaskMissingFromStoreIsRemovedFromQueue() {
    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, nodeService, clusterService, taskService,
                                                       provisionQueue, jobQueue, -10, 1, 1);

    jobQueue.removeAll();
    nodeProvisionTaskQueue.removeAll();

    ClusterTask task = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("3-1-1"), "node1", "service",
                                       ClusterAction.CLUSTER_CREATE, new JsonObject());
    task.setStatus(ClusterTask.Status.IN_PROGRESS);
    SchedulableTask schedulableTask = new SchedulableTask(task);

    // add a task to the queue without storing it.
    nodeProvisionTaskQueue.add(new Element(task.getTaskId(), new JsonSerde().getGson().toJson(schedulableTask)));
    nodeProvisionTaskQueue.take("0");

    clusterCleanup.run();
    Assert.assertEquals(0, Iterators.size(nodeProvisionTaskQueue.getBeingConsumed()));
  }

  @Test
  public void testOnlyCorrectClustersAreCleaned() throws Exception {
    long now = System.currentTimeMillis();
    for (int i = 0; i < 20; i++) {
      createCluster(String.valueOf(i), now - 1000, now - 100, Cluster.Status.ACTIVE);
    }

    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, nodeService, clusterService, taskService,
                                                       provisionQueue, jobQueue, -10, 3, 7);
    clusterQueue.removeAll();
    Assert.assertEquals(0, Iterators.size(clusterQueue.getQueued()));

    clusterCleanup.run();

    // clusters 3, 10, and 17 should have been scheduled for deletion
    Assert.assertEquals(3, Iterators.size(clusterQueue.getQueued()));

    Element e1 = clusterQueue.take("consumer1");
    Element e2 = clusterQueue.take("consumer1");
    Element e3 = clusterQueue.take("consumer1");
    Assert.assertEquals(ImmutableSet.of("3", "10", "17"), ImmutableSet.of(e1.getId(), e2.getId(), e3.getId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e1.getValue());
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e2.getValue());
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e3.getValue());
    clusterQueue.removeAll();
  }

  private Cluster createCluster(String id, long createTime, long expireTime, Cluster.Status status) throws Exception {
    Cluster cluster = new Cluster(id, "", "expire" + id, createTime, "", null, null,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), null);
    cluster.setStatus(status);
    cluster.setExpireTime(expireTime);

    clusterStore.writeCluster(cluster);
    return cluster;
  }
}
