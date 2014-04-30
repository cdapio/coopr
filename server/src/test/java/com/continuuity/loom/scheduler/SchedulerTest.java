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

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.Entities;
import com.continuuity.loom.TestHelper;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.http.LoomService;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Test ClusterScheduler
 */
public class SchedulerTest extends BaseTest {
  private static final Gson GSON = new JsonSerde().getGson();

  private static TimeoutTrackingQueue inputQueue;
  private static TimeoutTrackingQueue provisionQueue;
  private static TimeoutTrackingQueue callbackQueue;
  private static TimeoutTrackingQueue solverQueue;
  private static LoomService loomService;
  private static TimeoutTrackingQueue jobQueue;
  private static Cluster cluster;
  private static ClusterJob job;

  @BeforeClass
  public static void start() throws Exception {
    inputQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.CLUSTER)));
    inputQueue.start();

    provisionQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.PROVISIONER)));
    provisionQueue.start();

    solverQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.SOLVER)));
    solverQueue.start();

    loomService = injector.getInstance(LoomService.class);
    loomService.startAndWait();

    jobQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.JOB)));
    jobQueue.start();

    callbackQueue = injector.getInstance(
      Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.CALLBACK)));
    callbackQueue.start();
  }

  @AfterClass
  public static void stop() throws Exception {
    loomService.stopAndWait();
    inputQueue.stop();
    provisionQueue.stop();
    solverQueue.stop();
  }

  @Before
  public void beforeTest() throws Exception {
    jobQueue.removeAll();
    inputQueue.removeAll();
    solverQueue.removeAll();
    provisionQueue.removeAll();
    callbackQueue.removeAll();
    mockClusterCallback.clear();

    cluster = new JsonSerde().getGson().fromJson(TEST_CLUSTER, Cluster.class);
    job = new ClusterJob(new JobId(cluster.getId(), 0), ClusterAction.CLUSTER_CREATE);
    cluster.setLatestJobId(job.getJobId());
    clusterStore.writeCluster(cluster);
    clusterStore.writeClusterJob(job);

    Node node = GSON.fromJson(NODE1, Node.class);
    clusterStore.writeNode(node);

    node = GSON.fromJson(NODE2, Node.class);
    clusterStore.writeNode(node);
  }

  @Test
  public void testScheduler() throws Exception {
    ClusterScheduler clusterScheduler = injector.getInstance(ClusterScheduler.class);
    CallbackScheduler callbackScheduler = injector.getInstance(CallbackScheduler.class);

    inputQueue.add(new Element(cluster.getId(), ClusterAction.CLUSTER_CREATE.name()));
    clusterScheduler.run();

    // Verify stages and actions
    List<Multiset<ActionService>> expectedStages =
      ImmutableList.<Multiset<ActionService>>of(
        ImmutableMultiset.of(new ActionService("CREATE", ""), new ActionService("CREATE", "")),

        ImmutableMultiset.of(new ActionService("CONFIRM", ""), new ActionService("CONFIRM", "")),

        ImmutableMultiset.of(new ActionService("BOOTSTRAP", ""), new ActionService("BOOTSTRAP", "")),

        ImmutableMultiset.of(new ActionService("CONFIGURE", "hosts"), new ActionService("CONFIGURE", "hosts"),
                             new ActionService("INSTALL", "hadoop-hdfs-datanode"),
                             new ActionService("INSTALL", "hadoop-hdfs-namenode")),

        ImmutableMultiset.of(new ActionService("CONFIGURE", "hadoop-hdfs-namenode"),
                             new ActionService("CONFIGURE", "hadoop-hdfs-datanode")),

        ImmutableMultiset.of(new ActionService("INITIALIZE", "hadoop-hdfs-namenode")),

        ImmutableMultiset.of(new ActionService("START", "hadoop-hdfs-namenode")),

        ImmutableMultiset.of(new ActionService("INITIALIZE", "hadoop-hdfs-datanode")),

        ImmutableMultiset.of(new ActionService("START", "hadoop-hdfs-datanode"))
      );

    List<Multiset<ActionService>> actualStages = Lists.newArrayList();
    callbackScheduler.run();

    Assert.assertEquals(1, jobQueue.size());
    String consumerId = "testJobScheduler";
    Element jobQueueElement = jobQueue.take(consumerId);
    String jobId = jobQueueElement.getValue();
    job = clusterStore.getClusterJob(JobId.fromString(jobId));
    while (true) {
      System.out.println("Stage " + job.getCurrentStageNumber());
      Multiset<ActionService> actionServices = HashMultiset.create();
      for (String taskId : job.getCurrentStage()) {
        ClusterTask task = clusterStore.getClusterTask(TaskId.fromString(taskId));
        System.out.println(GSON.toJson(task));
        actionServices.add(new ActionService(task.getTaskName().name(), task.getService()));
      }

      actualStages.add(actionServices);

      if (!job.hasNextStage()) {
        break;
      }
      job.advanceStage();
    }

    // 4th and 5th stage get deduped, hence merging them back for comparison
    Multiset<ActionService> actionServices = actualStages.remove(3);
    actualStages.get(3).addAll(actionServices);

    Assert.assertEquals(expectedStages, actualStages);
    jobQueue.recordProgress(consumerId, jobQueueElement.getId(),
                            TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "");


    // Add the job back into the jobQueue, and run job scheduler
    jobQueue.add(new Element(jobId));
    JobScheduler jobScheduler = injector.getInstance(JobScheduler.class);
    jobScheduler.run();
    Assert.assertEquals(0, jobQueue.size());

    // Two tasks should have been submitted for provisioning.
    JsonObject taskJson = TestHelper.takeTask(getLoomUrl(), "consumer1");
    System.out.println("Got task " + taskJson);

    JsonObject returnJson = new JsonObject();
    returnJson.addProperty("status", 0);
    returnJson.addProperty("workerId", "consumer1");
    returnJson.addProperty("taskId", taskJson.get("taskId").getAsString());
    returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "123.456.789.123")));
    TestHelper.finishTask(getLoomUrl(), returnJson);

    taskJson = TestHelper.takeTask(getLoomUrl(), "consumer1");
    System.out.println("Got task " + taskJson);
    returnJson = new JsonObject();
    returnJson.addProperty("status", 0);
    returnJson.addProperty("workerId", "consumer1");
    returnJson.addProperty("taskId", taskJson.get("taskId").getAsString());
    returnJson.add("result", GSON.toJsonTree(ImmutableMap.of("ipaddress", "456.789.123.123")));
    TestHelper.finishTask(getLoomUrl(), returnJson);

    taskJson = TestHelper.takeTask(getLoomUrl(), "consumer1");
    System.out.println("Got task " + taskJson);

    Assert.assertEquals(2, jobQueue.size());

    jobScheduler.run();
    jobScheduler.run();
    jobScheduler.run();
    jobScheduler.run();

    for (int i = 0; i < 5; i++) {
      taskJson = TestHelper.takeTask(getLoomUrl(), "consumer1");
      System.out.println("Got task " + taskJson);
      returnJson = new JsonObject();
      returnJson.addProperty("status", 0);
      returnJson.addProperty("workerId", "consumer1");
      returnJson.addProperty("taskId", taskJson.get("taskId").getAsString());
      TestHelper.finishTask(getLoomUrl(), returnJson);
      jobScheduler.run();
      jobScheduler.run();
    }
  }

  @Test
  public void testSuccessCallbacks() throws Exception {
    testCallbacks(false);
  }

  @Test
  public void testFailureCallbacks() throws Exception {
    testCallbacks(true);
  }

  @Test
  public void testFalseOnStartStopsJob() throws Exception {
    ClusterScheduler clusterScheduler = injector.getInstance(ClusterScheduler.class);

    inputQueue.add(new Element(cluster.getId(), ClusterAction.CLUSTER_CREATE.name()));
    clusterScheduler.run();

    CallbackScheduler callbackScheduler = injector.getInstance(CallbackScheduler.class);
    // should be no job in the queue until the start callback runs
    Assert.assertEquals(0, jobQueue.size());

    // tell mock callback to return false for onStart callback
    mockClusterCallback.setReturnOnStart(false);
    callbackScheduler.run();

    // at this point, the start callback should have run, but not the after callbacks
    Assert.assertEquals(1, mockClusterCallback.getStartCallbacks().size());
    Assert.assertEquals(0, mockClusterCallback.getSuccessCallbacks().size());
    Assert.assertEquals(0, mockClusterCallback.getFailureCallbacks().size());

    // there also should not be any jobs in the queue
    Assert.assertEquals(0, jobQueue.size());
  }

  private void testCallbacks(boolean failJob) throws Exception {
    ClusterScheduler clusterScheduler = injector.getInstance(ClusterScheduler.class);

    inputQueue.add(new Element(cluster.getId(), ClusterAction.CLUSTER_CREATE.name()));
    clusterScheduler.run();

    CallbackScheduler callbackScheduler = injector.getInstance(CallbackScheduler.class);
    // should be no job in the queue until the start callback runs
    Assert.assertEquals(0, jobQueue.size());
    callbackScheduler.run();
    // at this point, the start callback should have run, but not the after callbacks
    Assert.assertEquals(1, mockClusterCallback.getStartCallbacks().size());
    Assert.assertEquals(0, mockClusterCallback.getSuccessCallbacks().size());
    Assert.assertEquals(0, mockClusterCallback.getFailureCallbacks().size());

    JobScheduler jobScheduler = injector.getInstance(JobScheduler.class);
    jobScheduler.run();

    // take tasks until there are no more
    JsonObject taskJson = TestHelper.takeTask(getLoomUrl(), "consumer1");
    while (taskJson.entrySet().size() > 0) {
      System.out.println("Got task " + taskJson);
      JsonObject returnJson = new JsonObject();
      returnJson.addProperty("status", failJob ? 1 : 0);
      returnJson.addProperty("workerId", "consumer1");
      returnJson.addProperty("taskId", taskJson.get("taskId").getAsString());
      TestHelper.finishTask(getLoomUrl(), returnJson);
      jobScheduler.run();
      jobScheduler.run();
      callbackScheduler.run();

      taskJson = TestHelper.takeTask(getLoomUrl(), "consumer1");
    }
    jobScheduler.run();
    callbackScheduler.run();

    // at this point, the failure callback should have run
    Assert.assertEquals(1, mockClusterCallback.getStartCallbacks().size());
    Assert.assertEquals(failJob ? 0 : 1, mockClusterCallback.getSuccessCallbacks().size());
    Assert.assertEquals(failJob ? 1 : 0, mockClusterCallback.getFailureCallbacks().size());
  }


  private String getLoomUrl() {
    InetSocketAddress address = loomService.getBindAddress();
    return String.format("http://%s:%s", address.getHostName(), address.getPort());
  }

  private static class ActionService {
    private final String action;
    private final String service;

    private ActionService(String action, String service) {
      this.action = action;
      this.service = service;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ActionService that = (ActionService) o;

      return !(action != null ? !action.equals(that.action) : that.action != null) &&
        !(service != null ? !service.equals(that.service) : that.service != null);

    }

    @Override
    public int hashCode() {
      int result = action != null ? action.hashCode() : 0;
      result = 31 * result + (service != null ? service.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
        .add("action", action)
        .add("service", service)
        .toString();
    }
  }

  public static final String TEST_CLUSTER =
    "{\n" +
      "   \"id\":\"2\",\n" +
      "   \"ownerId\":\"user1\",\n" +
      "   \"name\":\"ashau-dev\",\n" +
      "   \"description\":\"\",\n" +
      "   \"status\":\"pending\",\n" +
      "   \"provider\":{\n" +
      "      \"name\":\"joyent\",\n" +
      "      \"description\":\"Joyent Compute Service\",\n" +
      "      \"providertype\":\"joyent\",\n" +
      "      \"provisioner\":{\n" +
      "         \"auth\":{\n" +
      "            \"joyent_username\":\"EXAMPLE_USERNAME\",\n" +
      "            \"joyent_keyname\":\"EXAMPLE_KEYNAME\",\n" +
      "            \"joyent_keyfile\":\"/path/to/example.key\",\n" +
      "            \"joyent_version\":\"~7.0\"\n" +
      "         }\n" +
      "      }\n" +
      "   },\n" +
      "  \"clusterTemplate\":" + Entities.ClusterTemplateExample.HDFS_STRING +
      "   ,\n" +
      "   \"nodes\":[\n" +
      "      \"c128b2fd-4cac-4ca1-ae99-b27a49b72e06\",\n" +
      "      \"091a6330-b87b-4095-90f3-02e39e4b6dba\"\n" +
      "   ],\n" +
      "   \"services\":[\n" +
      "      \"hosts\",\n" +
      "      \"hadoop-hdfs-datanode\",\n" +
      "      \"hadoop-hdfs-namenode\"\n" +
      "   ],\n" +
      "   \"latestJobId\":\"2-001\"\n" +
      "}";

  public static final String NODE1 =
      "       {\n" +
      "         \"id\":\"c128b2fd-4cac-4ca1-ae99-b27a49b72e06\",\n" +
      "         \"clusterId\":\"2\",\n" +
      "         \"services\":[\n" +
      "            {\n" +
      "               \"name\":\"hadoop-hdfs-namenode\",\n" +
      "               \"description\":\"Hadoop HDFS NameNode\",\n" +
      "               \"dependson\":[\n" +
      "                  \"hosts\"\n" +
      "               ],\n" +
      "               \"provisioner\":{\n" +
      "                  \"actions\":{\n" +
      "                     \"install\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[hadoop::hadoop_hdfs_namenode]\"\n" +
      "                     },\n" +
      "                     \"initialize\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]\"\n" +
      "                     },\n" +
      "                     \"configure\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[hadoop::default]\"\n" +
      "                     },\n" +
      "                     \"start\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[loom_service_runner::default]\",\n" +
      "                        \"data\":\"{\\\"loom\\\": { \\\"node\\\": { \\\"services\\\": [ \\\"hadoop-hdfs-namenode\\\": \\\"start\\\" ] } } }\"\n" +
      "                     },\n" +
      "                     \"stop\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[loom_service_runner::default]\",\n" +
      "                        \"data\":\"{\\\"loom\\\": { \\\"node\\\": { \\\"services\\\": [ \\\"hadoop-hdfs-namenode\\\": \\\"stop\\\" ] } } }\"\n" +
      "                     }\n" +
      "                  }\n" +
      "               }\n" +
      "            },\n" +
      "            {\n" +
      "               \"name\":\"hosts\",\n" +
      "               \"description\":\"Manages /etc/hosts\",\n" +
      "               \"dependson\":[\n" +
      "\n" +
      "               ],\n" +
      "               \"provisioner\":{\n" +
      "                  \"actions\":{\n" +
      "                     \"configure\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[loom_hosts::default]\"\n" +
      "                     }\n" +
      "                  }\n" +
      "               }\n" +
      "            }\n" +
      "         ],\n" +
      "         \"properties\":{\n" +
      "            \"flavor\":\"Large 8GB\",\n" +
      "            \"image\":\"325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d\",\n" +
      "            \"hostname\":\"node1.net\"\n" +
      "         },\n" +
      "         \"actions\":[]\n" +
      "      }";

  public static final String NODE2 =
      "       {\n" +
      "         \"id\":\"091a6330-b87b-4095-90f3-02e39e4b6dba\",\n" +
      "         \"clusterId\":\"2\",\n" +
      "         \"services\":[\n" +
      "            {\n" +
      "               \"name\":\"hadoop-hdfs-datanode\",\n" +
      "               \"description\":\"Hadoop HDFS DataNode\",\n" +
      "               \"dependson\":[\n" +
      "                  \"hosts\",\n" +
      "                  \"hadoop-hdfs-namenode\"\n" +
      "               ],\n" +
      "               \"provisioner\":{\n" +
      "                  \"actions\":{\n" +
      "                     \"install\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[hadoop::hadoop_hdfs_datanode]\"\n" +
      "                     },\n" +
        "                     \"initialize\":{\n" +
        "                        \"type\":\"chef\",\n" +
        "                        \"script\":\"recipe[hadoop_wrapper::hadoop_hdfs_datanode_init]\"\n" +
        "                     },\n" +
      "                     \"configure\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[hadoop::default]\"\n" +
      "                     },\n" +
      "                     \"start\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[loom_service_runner::default]\",\n" +
      "                        \"data\":\"{\\\"loom\\\": { \\\"node\\\": { \\\"services\\\": [ \\\"hadoop-hdfs-datanode\\\": \\\"start\\\" ] } } }\"\n" +
      "                     },\n" +
      "                     \"stop\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[loom_service_runner::default]\",\n" +
      "                        \"data\":\"{\\\"loom\\\": { \\\"node\\\": { \\\"services\\\": [ \\\"hadoop-hdfs-datanode\\\": \\\"stop\\\" ] } } }\"\n" +
      "                     }\n" +
      "                  }\n" +
      "               }\n" +
      "            },\n" +
      "            {\n" +
      "               \"name\":\"hosts\",\n" +
      "               \"description\":\"Manages /etc/hosts\",\n" +
      "               \"dependson\":[\n" +
      "\n" +
      "               ],\n" +
      "               \"provisioner\":{\n" +
      "                  \"actions\":{\n" +
      "                     \"configure\":{\n" +
      "                        \"type\":\"chef\",\n" +
      "                        \"script\":\"recipe[loom_hosts::default]\"\n" +
      "                     }\n" +
      "                  }\n" +
      "               }\n" +
      "            }\n" +
      "         ],\n" +
      "         \"properties\":{\n" +
      "            \"flavor\":\"Large 8GB\",\n" +
      "            \"image\":\"325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d\",\n" +
      "            \"hostname\":\"node2.net\"\n" +
      "         },\n" +
      "         \"actions\":[]\n" +
      "     }\n";
}
