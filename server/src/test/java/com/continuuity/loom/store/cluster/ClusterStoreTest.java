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
package com.continuuity.loom.store.cluster;

import com.continuuity.loom.Entities;
import com.continuuity.loom.account.Account;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Tests for getting and setting cluster objects.  Test classes for different types of stores must set the
 * protected store field before each test and make sure state is wiped out between tests.
 */
public abstract class ClusterStoreTest {
  private static final Account tenant1_user1 = new Account("user1", "tenant1");
  private static final Account tenant1_user2 = new Account("user2", "tenant1");
  private static final Account tenant1_admin = new Account(Constants.ADMIN_USER, "tenant1");
  private static final Account tenant2_user1 = new Account("user1", "tenant2");
  private static final Account tenant2_admin = new Account(Constants.ADMIN_USER, "tenant2");

  protected static ClusterStoreService clusterStoreService;
  protected static ClusterStore systemView;

  public abstract void clearState() throws Exception;
  public abstract ClusterStoreService getClusterStoreService() throws Exception;

  @Before
  public void setupTest() throws Exception {
    clusterStoreService = getClusterStoreService();
    systemView = clusterStoreService.getSystemView();
    clearState();
  }

  @Test
  public void testGetStoreDeleteClusterAsSystem() throws Exception {
    Cluster cluster = new Cluster(
      "104", tenant1_user1, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    assertGetStoreDeleteCluster(systemView, cluster);
  }

  @Test
  public void testGetStoreDeleteClusterAsUser() throws Exception {
    ClusterStoreView view = clusterStoreService.getView(tenant1_user1);
    Cluster cluster = new Cluster(
      "104", tenant1_user1, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    assertGetStoreDeleteCluster(view, cluster);
  }

  @Test
  public void testGetStoreDeleteClusterAsAdmin() throws Exception {
    ClusterStoreView view = clusterStoreService.getView(tenant1_admin);
    Cluster cluster = new Cluster(
      "104", tenant1_user1, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    assertGetStoreDeleteCluster(view, cluster);
  }

  @Test
  public void testUserSeparation() throws Exception {
    Cluster cluster = new Cluster(
      "104", tenant1_user1, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    clusterStoreService.getView(tenant1_user1).writeCluster(cluster);
    Assert.assertEquals(cluster, clusterStoreService.getView(tenant1_user1).getCluster(cluster.getId()));
    Assert.assertNull(clusterStoreService.getView(tenant1_user2).getCluster(cluster.getId()));
  }

  @Test
  public void testTenantSeparation() throws Exception {
    Cluster cluster = new Cluster(
      "104", tenant1_user1, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    clusterStoreService.getView(tenant1_admin).writeCluster(cluster);
    Assert.assertNull(clusterStoreService.getView(tenant2_admin).getCluster(cluster.getId()));
  }

  private void assertGetStoreDeleteCluster(ClusterStoreView view, Cluster cluster) throws IOException,
    IllegalAccessException {
    String clusterId = cluster.getId();
    Assert.assertNull(view.getCluster(clusterId));
    Assert.assertFalse(view.clusterExists(clusterId));

    view.writeCluster(cluster);
    Assert.assertTrue(view.clusterExists(clusterId));
    Assert.assertEquals(cluster, view.getCluster(clusterId));
    // check overwrite
    view.writeCluster(cluster);
    Assert.assertEquals(cluster, view.getCluster(clusterId));

    view.deleteCluster(clusterId);
    Assert.assertNull(view.getCluster(clusterId));
  }

  @Test
  public void testGetStoreDeleteJob() throws IOException {
    JobId id = new JobId("1", 1);
    ClusterJob job = new ClusterJob(id, ClusterAction.CLUSTER_DELETE);
    Assert.assertNull(systemView.getClusterJob(id));

    systemView.writeClusterJob(job);
    Assert.assertEquals(job, systemView.getClusterJob(id));
    systemView.writeClusterJob(job);
    Assert.assertEquals(job, systemView.getClusterJob(id));

    systemView.deleteClusterJob(id);
    Assert.assertNull(systemView.getClusterJob(id));
  }

  @Test
  public void testGetClusterJobs() throws Exception {
    Cluster cluster = new Cluster(
      "1", tenant1_user1, "example-hdfs-delete", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );
    ClusterStoreView user1view = clusterStoreService.getView(cluster.getAccount());
    user1view.writeCluster(cluster);
    Set<ClusterJob> jobs = Sets.newHashSet();
    for (int i = 0; i < 10; i++) {
      JobId jobId = new JobId(cluster.getId(), i);
      ClusterJob job = new ClusterJob(jobId, ClusterAction.RESTART_SERVICES);
      systemView.writeClusterJob(job);
      jobs.add(job);
    }
    // this job shouldn't get fetched
    ClusterJob randomJob = new ClusterJob(JobId.fromString("2123-0214"), ClusterAction.CLUSTER_CONFIGURE);
    systemView.writeClusterJob(randomJob);

    // shouldn't be able to get since the cluster isn't owned by this user
    Assert.assertTrue(clusterStoreService.getView(tenant1_user2).getClusterJobs(cluster.getId(), -1).isEmpty());

    // check we can get all the jobs
    Assert.assertEquals(Sets.newHashSet(user1view.getClusterJobs(cluster.getId(), -1)), jobs);
    Assert.assertEquals(Sets.newHashSet(systemView.getClusterJobs(cluster.getId(), -1)), jobs);

    // check the limit
    List<ClusterJob> fetchedJobs = user1view.getClusterJobs(cluster.getId(), 5);
    Assert.assertEquals(5, fetchedJobs.size());
    Assert.assertTrue(jobs.containsAll(fetchedJobs));
    fetchedJobs = systemView.getClusterJobs(cluster.getId(), 5);
    Assert.assertEquals(5, fetchedJobs.size());
    Assert.assertTrue(jobs.containsAll(fetchedJobs));
  }

  @Test
  public void testGetStoreDeleteTask() throws IOException {
    TaskId id = new TaskId(new JobId("1", 1), 1);
    ClusterTask task = new ClusterTask(ProvisionerAction.CONFIGURE, id,
                                       "node1", "service", ClusterAction.CLUSTER_CREATE, new JsonObject());
    Assert.assertNull(systemView.getClusterTask(id));

    systemView.writeClusterTask(task);
    Assert.assertEquals(task, systemView.getClusterTask(id));
    systemView.writeClusterTask(task);
    Assert.assertEquals(task, systemView.getClusterTask(id));

    systemView.deleteClusterTask(id);
    Assert.assertNull(systemView.getClusterTask(id));
  }

  @Test
  public void testGetAllClusters() throws Exception {
    Assert.assertEquals(0, systemView.getAllClusters().size());

    String clusterId1 = "123";
    Cluster cluster1 = new Cluster(
      clusterId1, tenant1_user1, "example-hdfs", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );

    // Make sure new cluster is at least 1ms older than the previous one.
    Cluster cluster2 = new Cluster(
      "1234", tenant1_user1, "example-hdfs2", System.currentTimeMillis() + 1, "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node3", "node4"),
      ImmutableSet.of("s1", "s4")
    );

    ClusterStoreView store = clusterStoreService.getView(tenant1_user1);
    store.writeCluster(cluster1);
    store.writeCluster(cluster2);

    List<Cluster> clusters = store.getAllClusters();
    Assert.assertEquals(2, clusters.size());

    // Clusters should be sorted in reverse order
    Assert.assertEquals(cluster2, clusters.get(0));
    Assert.assertEquals(cluster1, clusters.get(1));

    // set status to terminated
    cluster1.setStatus(Cluster.Status.TERMINATED);
    store.writeCluster(cluster1);

    // get all should still return the terminated cluster
    Assert.assertEquals(2, store.getAllClusters().size());
    // get nonterminated should not return the cluster
    clusters = store.getNonTerminatedClusters();
    Assert.assertEquals(1, clusters.size());
    Assert.assertEquals(cluster2, clusters.get(0));
  }

  @Test
  public void testAdminGetAllClusters() throws Exception {
    Assert.assertEquals(0, systemView.getAllClusters().size());

    String clusterId1 = "123";
    Cluster cluster1 = new Cluster(
      clusterId1, tenant1_user1, "example-hdfs", System.currentTimeMillis(), "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node1", "node2"),
      ImmutableSet.of("s1", "s2")
    );

    // Make sure new cluster is at least 1ms older than the previous one.
    Cluster cluster2 = new Cluster(
      "1234", tenant1_user2, "example-hdfs2", System.currentTimeMillis() + 1, "hdfs cluster",
      Entities.ProviderExample.RACKSPACE,
      Entities.ClusterTemplateExample.HDFS,
      ImmutableSet.of("node3", "node4"),
      ImmutableSet.of("s1", "s4")
    );

    ClusterStoreView store = clusterStoreService.getView(tenant1_admin);
    store.writeCluster(cluster1);
    store.writeCluster(cluster2);

    List<Cluster> clusters = store.getAllClusters();
    Assert.assertEquals(2, clusters.size());

    // Clusters should be sorted in reverse order
    Assert.assertEquals(cluster2, clusters.get(0));
    Assert.assertEquals(cluster1, clusters.get(1));

    // set status to terminated
    cluster1.setStatus(Cluster.Status.TERMINATED);
    store.writeCluster(cluster1);

    // get all should still return the terminated cluster
    Assert.assertEquals(2, store.getAllClusters().size());
    // get nonterminated should not return the cluster
    clusters = store.getNonTerminatedClusters();
    Assert.assertEquals(1, clusters.size());
    Assert.assertEquals(cluster2, clusters.get(0));
  }

  @Test
  public void testGetStoreDeleteNode() throws Exception {
    Node node = Entities.ClusterExample.NODE1;
    Assert.assertNull(systemView.getNode(node.getId()));

    systemView.writeNode(node);
    Assert.assertEquals(node, systemView.getNode(node.getId()));
    // check overwrite
    systemView.writeNode(node);
    Assert.assertEquals(node, systemView.getNode(node.getId()));

    systemView.deleteNode(node.getId());
    Assert.assertNull(systemView.getNode(node.getId()));
  }

  @Test
  public void testGetClusterNodes() throws Exception {
    Cluster cluster = Entities.ClusterExample.createCluster();
    ClusterStoreView store = clusterStoreService.getView(cluster.getAccount());
    store.writeCluster(cluster);

    Node node1 = Entities.ClusterExample.NODE1;
    systemView.writeNode(node1);

    Node node2 = Entities.ClusterExample.NODE2;
    systemView.writeNode(node2);

    Set<Node> nodes =  store.getClusterNodes(cluster.getId());
    Assert.assertEquals(ImmutableSet.of(node1, node2), nodes);

    nodes = store.getClusterNodes(cluster.getId());
    Assert.assertEquals(ImmutableSet.of(node1, node2), nodes);

    Assert.assertTrue(clusterStoreService.getView(tenant2_user1).getClusterNodes(cluster.getId()).isEmpty());

    systemView.deleteNode(node1.getId());
    Assert.assertNull(systemView.getNode(node1.getId()));

    systemView.deleteNode(node2.getId());
    Assert.assertNull(systemView.getNode(node1.getId()));

    store.deleteCluster(cluster.getId());
    Assert.assertNull(store.getCluster(cluster.getId()));
  }

  @Test
  public void testGetNodes() throws Exception {
    Node node1 = Entities.ClusterExample.NODE1;
    systemView.writeNode(node1);

    Node node2 = Entities.ClusterExample.NODE2;
    systemView.writeNode(node2);

    systemView.deleteNode(node1.getId());
    Assert.assertNull(systemView.getNode(node1.getId()));

    systemView.deleteNode(node2.getId());
    Assert.assertNull(systemView.getNode(node1.getId()));
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

    systemView.writeClusterTask(task1);
    systemView.writeClusterTask(task2);
    systemView.writeClusterTask(task3);
    systemView.writeClusterTask(task4);
    systemView.writeClusterTask(task5);

    Assert.assertEquals(ImmutableSet.of(task1, task2), systemView.getRunningTasks(currentTime - 500));
    Assert.assertEquals(ImmutableSet.of(task1, task2, task3, task4), systemView.getRunningTasks(currentTime));
    Assert.assertTrue(systemView.getRunningTasks(currentTime - 5000).isEmpty());
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

    Assert.assertEquals(ImmutableSet.of(cluster1, cluster3),
                        systemView.getExpiringClusters(System.currentTimeMillis()));
    Assert.assertEquals(ImmutableSet.of(cluster1, cluster2, cluster3, cluster4),
                        systemView.getExpiringClusters(System.currentTimeMillis() + 500000));
  }

  private Cluster createCluster(String id, long createTime, long expireTime, Cluster.Status status) throws Exception {
    Cluster cluster = new Cluster(id, tenant1_user1, "expire" + id, createTime, "", null, null,
                                  ImmutableSet.<String>of(), ImmutableSet.<String>of(), null);
    cluster.setStatus(status);
    cluster.setExpireTime(expireTime);

    clusterStoreService.getView(cluster.getAccount()).writeCluster(cluster);
    return cluster;
  }
}
