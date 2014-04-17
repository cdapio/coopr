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
package com.continuuity.loom.store;

import com.continuuity.loom.Entities;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.SchedulerTest;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskException;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

/**
 *
 */
public abstract class ClusterStoreTest {
  private static final Gson GSON = new JsonSerde().getGson();

  protected static ClusterStore store;

  @Test
  public void testGetStoreDeleteCluster() throws Exception {
    Cluster cluster = new Cluster(
      "104", "user", "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    String clusterId = cluster.getId();
    Assert.assertNull(store.getCluster(clusterId));
    Assert.assertFalse(store.clusterExists(clusterId));

    store.writeCluster(cluster);
    Assert.assertTrue(store.clusterExists(clusterId));
    Assert.assertTrue(store.clusterExists(clusterId, cluster.getOwnerId()));
    Assert.assertFalse(store.clusterExists(clusterId, "not" + cluster.getOwnerId()));
    Assert.assertEquals(cluster, store.getCluster(clusterId));
    // check overwrite
    store.writeCluster(cluster);
    Assert.assertEquals(cluster, store.getCluster(clusterId));

    store.deleteCluster(clusterId);
    Assert.assertNull(store.getCluster(clusterId));
  }

  @Test
  public void testGetStoreDeleteClusterWithOwner() throws Exception {
    String ownerId = "user1";
    Cluster cluster = new Cluster(
      "104", ownerId, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    String clusterId = cluster.getId();
    Assert.assertNull(store.getCluster(clusterId, ownerId));

    // check that we can write the cluster
    store.writeCluster(cluster);
    Assert.assertEquals(cluster, store.getCluster(clusterId, ownerId));

    // check that getting it with a different ownerid returns nothing
    Assert.assertNull(store.getCluster(clusterId, ownerId + "123"));

    // check that we can overwrite the owner of a cluster
    cluster.setOwnerId(ownerId + "123");
    store.writeCluster(cluster);
    Assert.assertEquals(cluster, store.getCluster(clusterId, ownerId + "123"));

    // check that deleting it works
    store.deleteCluster(clusterId);
    Assert.assertNull(store.getCluster(clusterId, ownerId));
  }

  @Test
  public void testGetStoreDeleteJob() throws TaskException {
    JobId id = new JobId("1", 1);
    ClusterJob job = new ClusterJob(id, ClusterAction.CLUSTER_DELETE);
    Assert.assertNull(store.getClusterJob(id));

    store.writeClusterJob(job);
    Assert.assertEquals(job, store.getClusterJob(id));
    store.writeClusterJob(job);
    Assert.assertEquals(job, store.getClusterJob(id));

    store.deleteClusterJob(id);
    Assert.assertNull(store.getClusterJob(id));
  }

  @Test
  public void testGetClusterJobs() throws Exception {
    String ownerId = "user1";
    Cluster cluster = new Cluster(
      "1", ownerId, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    store.writeCluster(cluster);
    Set<ClusterJob> jobs = Sets.newHashSet();
    for (int i = 0; i < 10; i++) {
      JobId jobId = new JobId(cluster.getId(), i);
      ClusterJob job = new ClusterJob(jobId, ClusterAction.RESTART_SERVICES);
      store.writeClusterJob(job);
      jobs.add(job);
    }
    // this job shouldn't get fetched
    ClusterJob randomJob = new ClusterJob(JobId.fromString("2123-0214"), ClusterAction.CLUSTER_CONFIGURE);
    store.writeClusterJob(randomJob);

    // shouldn't be able to get since the cluster isn't owned by this user
    Assert.assertTrue(store.getClusterJobs(cluster.getId(), "not" + ownerId, -1).isEmpty());

    // check we can get all the jobs
    Assert.assertEquals(Sets.newHashSet(store.getClusterJobs(cluster.getId(), ownerId, -1)), jobs);
    Assert.assertEquals(Sets.newHashSet(store.getClusterJobs(cluster.getId(), -1)), jobs);

    // check the limit
    List<ClusterJob> fetchedJobs = store.getClusterJobs(cluster.getId(), ownerId, 5);
    Assert.assertEquals(5, fetchedJobs.size());
    Assert.assertTrue(jobs.containsAll(fetchedJobs));
    fetchedJobs = store.getClusterJobs(cluster.getId(), 5);
    Assert.assertEquals(5, fetchedJobs.size());
    Assert.assertTrue(jobs.containsAll(fetchedJobs));
  }

  @Test
  public void testGetStoreDeleteTask() throws TaskException {
    TaskId id = new TaskId(new JobId("1", 1), 1);
    ClusterTask task = new ClusterTask(ProvisionerAction.CONFIGURE, id,
                                       "node1", "service", ClusterAction.CLUSTER_CREATE, new JsonObject());
    Assert.assertNull(store.getClusterTask(id));

    store.writeClusterTask(task);
    Assert.assertEquals(task, store.getClusterTask(id));
    store.writeClusterTask(task);
    Assert.assertEquals(task, store.getClusterTask(id));

    store.deleteClusterTask(id);
    Assert.assertNull(store.getClusterTask(id));
  }

  @Test
  public void testGetAllClusters() throws Exception {
    Assert.assertEquals(0, store.getAllClusters().size());

    String clusterId1 = store.getNewClusterId();
    Cluster cluster1 = new Cluster(
      clusterId1, "user", "example-hdfs", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );

    // Make sure new cluster is at least 1ms older than the previous one.
    Cluster cluster2 = new Cluster(
      store.getNewClusterId(), "user", "example-hdfs2", System.currentTimeMillis() + 1, "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node3", "node4"),
      ImmutableSet.of("s1", "s4")
    );
    store.writeCluster(cluster1);
    store.writeCluster(cluster2);

    List<Cluster> clusters = store.getAllClusters();
    Assert.assertEquals(2, clusters.size());

    // Clusters should be sorted in reverse order
    Assert.assertEquals(cluster2, clusters.get(0));
    Assert.assertEquals(cluster1, clusters.get(1));
  }

  @Test
  public void testGetStoreDeleteNode() throws Exception {
    Node node = GSON.fromJson(SchedulerTest.NODE1, Node.class);
    Assert.assertNull(store.getNode(node.getId()));

    store.writeNode(node);
    Assert.assertEquals(node, store.getNode(node.getId()));
    // check overwrite
    store.writeNode(node);
    Assert.assertEquals(node, store.getNode(node.getId()));

    store.deleteNode(node.getId());
    Assert.assertNull(store.getNode(node.getId()));
  }

  @Test
  public void testGetClusterNodes() throws Exception {
    Cluster cluster = new JsonSerde().getGson().fromJson(SchedulerTest.TEST_CLUSTER, Cluster.class);
    store.writeCluster(cluster);

    Node node1 = GSON.fromJson(SchedulerTest.NODE1, Node.class);
    store.writeNode(node1);

    Node node2 = GSON.fromJson(SchedulerTest.NODE2, Node.class);
    store.writeNode(node2);

    Set<Node> nodes =  store.getClusterNodes(cluster.getId());
    Assert.assertEquals(ImmutableSet.of(node1, node2), nodes);

    nodes = store.getClusterNodes(cluster.getId(), cluster.getOwnerId());
    Assert.assertEquals(ImmutableSet.of(node1, node2), nodes);

    Assert.assertTrue(store.getClusterNodes(cluster.getId(), "not" + cluster.getOwnerId()).isEmpty());

    store.deleteNode(node1.getId());
    Assert.assertNull(store.getNode(node1.getId()));

    store.deleteNode(node2.getId());
    Assert.assertNull(store.getNode(node1.getId()));

    store.deleteCluster(cluster.getId());
    Assert.assertNull(store.getCluster(cluster.getId()));
  }

  @Test
  public void testGetNodes() throws Exception {
    Node node1 = GSON.fromJson(SchedulerTest.NODE1, Node.class);
    store.writeNode(node1);

    Node node2 = GSON.fromJson(SchedulerTest.NODE2, Node.class);
    store.writeNode(node2);

    store.deleteNode(node1.getId());
    Assert.assertNull(store.getNode(node1.getId()));

    store.deleteNode(node2.getId());
    Assert.assertNull(store.getNode(node1.getId()));
  }

  @Test
  public void testGetRunningTasks() throws Exception {
    ClusterTask task1 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("1-1-1"), "node1", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task2 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("1-1-2"), "node2", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task3 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("1-1-3"), "node3", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task4 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("1-1-4"), "node4", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());
    ClusterTask task5 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("1-1-5"), "node5", "service",
                                        ClusterAction.CLUSTER_CREATE, new JsonObject());

    long currentTime = System.currentTimeMillis();
    task1.setSubmitTime(currentTime - 1000);
    task1.setStatus(ClusterTask.Status.IN_PROGRESS);

    task2.setSubmitTime(currentTime - 1000);
    task2.setStatus(ClusterTask.Status.IN_PROGRESS);

    task3.setSubmitTime(currentTime - 200);
    task3.setStatus(ClusterTask.Status.IN_PROGRESS);

    task4.setSubmitTime(currentTime - 200);
    task4.setStatus(ClusterTask.Status.IN_PROGRESS);

    task5.setSubmitTime(currentTime - 1000);
    task5.setStatus(ClusterTask.Status.NOT_SUBMITTED);

    store.writeClusterTask(task1);
    store.writeClusterTask(task2);
    store.writeClusterTask(task3);
    store.writeClusterTask(task4);
    store.writeClusterTask(task5);

    Assert.assertEquals(ImmutableSet.of(task1, task2), store.getRunningTasks(currentTime - 500));
    Assert.assertEquals(ImmutableSet.of(task1, task2, task3, task4), store.getRunningTasks(currentTime));
    Assert.assertTrue(store.getRunningTasks(currentTime - 5000).isEmpty());
  }

  @SuppressWarnings("UnusedDeclaration")
  @Test
  public void testGetExpiringClusters() throws Exception {
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

    Assert.assertEquals(ImmutableSet.of(cluster1, cluster3), store.getExpiringClusters(System.currentTimeMillis()));
    Assert.assertEquals(ImmutableSet.of(cluster1, cluster2, cluster3, cluster4),
                        store.getExpiringClusters(System.currentTimeMillis() + 500000));
  }

  private Cluster createCluster(String id, long createTime, long expireTime, Cluster.Status status) throws Exception {
    Cluster cluster = new Cluster(id, "", "expire" + id, createTime, "", null, null,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), null);
    cluster.setStatus(status);
    cluster.setExpireTime(expireTime);

    store.writeCluster(cluster);
    return cluster;
  }
}
