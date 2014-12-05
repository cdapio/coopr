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
package co.cask.coopr.scheduler;

import co.cask.coopr.Entities;
import co.cask.coopr.TestHelper;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.http.ServiceTestBase;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.cluster.ClusterService;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.scheduler.task.NodeService;
import co.cask.coopr.scheduler.task.SchedulableTask;
import co.cask.coopr.scheduler.task.TaskConfig;
import co.cask.coopr.scheduler.task.TaskId;
import co.cask.coopr.scheduler.task.TaskService;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test ClusterCleanup.
 */
public class ClusterCleanupTest extends ServiceTestBase {
  static NodeService nodeService;
  static ClusterService clusterService;
  static TaskService taskService;
  private static final Account account = new Account(USER1, TENANT_ID);

  @BeforeClass
  public static void initTests() throws IOException, IllegalAccessException {
    nodeService = injector.getInstance(NodeService.class);
    clusterService = injector.getInstance(ClusterService.class);
    taskService = injector.getInstance(TaskService.class);
    entityStoreService.getView(SUPERADMIN_ACCOUNT).writeProviderType(Entities.ProviderTypeExample.JOYENT);
  }

  @Before
  public void clearData() {
    jobQueues.removeAll();
    provisionerQueues.removeAll();
  }

  @Test
  public void testCleanup() throws Exception {
    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, clusterService, nodeService, taskService,
                                                       jobQueues, provisionerQueues, 1, 1, 1);

    String queueName = account.getTenantId();

    ClusterTask task1 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-1-1"), "node1", "service",
                                        ClusterAction.CLUSTER_CREATE, "test", account);
    ClusterTask task2 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-2-2"), "node2", "service",
                                        ClusterAction.CLUSTER_CREATE, "test", account);
    ClusterTask task3 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-3-3"), "node3", "service",
                                        ClusterAction.CLUSTER_CREATE, "test", account);
    ClusterTask task4 = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("2-4-4"), "node3", "service",
                                        ClusterAction.CLUSTER_CREATE, "test", account);

    task1.setStatus(ClusterTask.Status.IN_PROGRESS);
    task2.setStatus(ClusterTask.Status.IN_PROGRESS);
    task3.setStatus(ClusterTask.Status.IN_PROGRESS);
    task4.setStatus(ClusterTask.Status.IN_PROGRESS);

    clusterStore.writeClusterTask(task1);
    clusterStore.writeClusterTask(task2);
    clusterStore.writeClusterTask(task3);
    clusterStore.writeClusterTask(task4);

    Node node1 = new Node("node1", "2", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    nodeService.startAction(node1, task1.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node1.getActions().get(0).getStatus());

    Node node2 = new Node("node2", "2", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    nodeService.startAction(node2, task2.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node2.getActions().get(0).getStatus());

    Node node3 = new Node("node3", "2", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    nodeService.startAction(node3, task3.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node3.getActions().get(0).getStatus());

    Node node4 = new Node("node4", "2", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    nodeService.startAction(node4, task4.getTaskId(), "service", "action");
    Assert.assertEquals(Node.Status.IN_PROGRESS, node4.getActions().get(0).getStatus());

    Assert.assertTrue(jobQueues.removeAll(queueName));
    Assert.assertEquals(0, Iterators.size(provisionerQueues.getQueued(queueName)));
    Assert.assertEquals(0, Iterators.size(provisionerQueues.getBeingConsumed(queueName)));

    provisionerQueues.add(queueName, new Element(task1.getTaskId(), ""));
    provisionerQueues.add(queueName, new Element(task2.getTaskId(), ""));
    provisionerQueues.add(queueName, new Element(task3.getTaskId(), ""));
    provisionerQueues.add(queueName, new Element(task4.getTaskId(), ""));

    Assert.assertEquals(4, Iterators.size(provisionerQueues.getQueued(queueName)));

    provisionerQueues.takeIterator("consumer1").next();
    provisionerQueues.takeIterator("consumer2").next();
    Assert.assertEquals(2, Iterators.size(provisionerQueues.getBeingConsumed(queueName)));
    Assert.assertEquals(2, Iterators.size(provisionerQueues.getQueued(queueName)));

    TimeUnit.SECONDS.sleep(1);

    provisionerQueues.takeIterator("consumer3").next();
    Assert.assertEquals(3, Iterators.size(provisionerQueues.getBeingConsumed(queueName)));
    Assert.assertEquals(1, Iterators.size(provisionerQueues.getQueued(queueName)));

    clusterCleanup.run();

    Assert.assertEquals(1, Iterators.size(provisionerQueues.getBeingConsumed(queueName)));
    Assert.assertEquals(1, Iterators.size(provisionerQueues.getQueued(queueName)));
    Assert.assertEquals(2, jobQueues.size(queueName));

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

    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, clusterService, nodeService, taskService,
                                                       jobQueues, provisionerQueues, 1, 1, 1);

    String queueName = account.getTenantId();
    Assert.assertEquals(0, Iterators.size(clusterQueues.getQueued(queueName)));

    clusterCleanup.run();

    Assert.assertEquals(2, Iterators.size(clusterQueues.getQueued(queueName)));

    Element e1 = clusterQueues.take(queueName, "consumer1");
    Element e2 = clusterQueues.take(queueName, "consumer1");
    Assert.assertEquals(ImmutableSet.of("1001", "1003"), ImmutableSet.of(e1.getId(), e2.getId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e1.getValue());
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e2.getValue());

    Cluster actualCluster1 = clusterStoreService.getView(account).getCluster(cluster1.getId());
    ClusterJob delJob1 = clusterStore.getClusterJob(JobId.fromString(actualCluster1.getLatestJobId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE, delJob1.getClusterAction());

    Cluster actualCluster3 = clusterStoreService.getView(account).getCluster(cluster3.getId());
    ClusterJob delJob2 = clusterStore.getClusterJob(JobId.fromString(actualCluster3.getLatestJobId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE, delJob2.getClusterAction());
  }

  @Test
  public void testQueuedTaskMissingFromStoreIsRemovedFromQueue() {
    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, clusterService, nodeService, taskService,
                                                       jobQueues, provisionerQueues, -10, 1, 1);

    String queueName = account.getTenantId();

    ClusterTask task = new ClusterTask(ProvisionerAction.CREATE, TaskId.fromString("3-1-1"), "node1", "service",
                                       ClusterAction.CLUSTER_CREATE, "test", account);
    task.setStatus(ClusterTask.Status.IN_PROGRESS);
    Cluster cluster = Entities.ClusterExample.createCluster();
    TaskConfig taskConfig = TaskConfig.from(cluster, Entities.ClusterExample.NODE1, Entities.ServiceExample.NAMENODE,
                                            cluster.getConfig(), ProvisionerAction.START, null);
    SchedulableTask schedulableTask = new SchedulableTask(task, taskConfig);

    // add a task to the queue without storing it.x
    provisionerQueues.add(queueName, new Element(task.getTaskId(), gson.toJson(schedulableTask)));
    provisionerQueues.takeIterator("0").next();

    clusterCleanup.run();
    Assert.assertEquals(0, Iterators.size(provisionerQueues.getBeingConsumed(queueName)));
  }

  @Test
  public void testOnlyCorrectClustersAreCleaned() throws Exception {
    long now = System.currentTimeMillis();
    for (int i = 0; i < 20; i++) {
      createCluster(String.valueOf(i), now - 1000, now - 100, Cluster.Status.ACTIVE);
    }

    String queueName = account.getTenantId();
    ClusterCleanup clusterCleanup = new ClusterCleanup(clusterStore, clusterService, nodeService, taskService,
                                                       jobQueues, provisionerQueues, -10, 3, 7);
    Assert.assertEquals(0, Iterators.size(clusterQueues.getQueued(queueName)));

    clusterCleanup.run();

    // clusters 3, 10, and 17 should have been scheduled for deletion
    Assert.assertEquals(3, Iterators.size(clusterQueues.getQueued(queueName)));

    Element e1 = clusterQueues.take(queueName, "consumer1");
    Element e2 = clusterQueues.take(queueName, "consumer1");
    Element e3 = clusterQueues.take(queueName, "consumer1");
    Assert.assertEquals(ImmutableSet.of("3", "10", "17"), ImmutableSet.of(e1.getId(), e2.getId(), e3.getId()));
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e1.getValue());
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e2.getValue());
    Assert.assertEquals(ClusterAction.CLUSTER_DELETE.name(), e3.getValue());
  }

  private Cluster createCluster(String id, long createTime, long expireTime, Cluster.Status status) throws Exception {
    Cluster cluster = Cluster.builder()
      .setID(id)
      .setAccount(account)
      .setName("expire" + id)
      .setCreateTime(createTime)
      .setProvider(Entities.ProviderExample.JOYENT)
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .build();
    cluster.setStatus(status);
    cluster.setExpireTime(expireTime);

    clusterStoreService.getView(account).writeCluster(cluster);
    return cluster;
  }
}
