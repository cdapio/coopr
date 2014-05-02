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

import com.continuuity.loom.TestHelper;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.dag.TaskDag;
import com.continuuity.loom.scheduler.dag.TaskNode;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class JobPlannerTest {
  private static final ServiceAction CHEF_ACTION = new ServiceAction("chef", TestHelper.actionMapOf("script", "data"));

  @Test
  public void testDedupNodesPerStage() throws Exception {

    List<Set<ClusterTask>> tasks = ImmutableList.of(
      createSortedSet(createClusterTask("INSTALL", "1-1-1", "host1"),
                      createClusterTask("CONFIGURE", "1-1-2", "host1"),
                      createClusterTask("INSTALL", "1-1-3", "host3")),

      createSortedSet(createClusterTask("INSTALL", "1-1-12", "host1"),
                      createClusterTask("INSTALL", "1-1-22", "host2"),
                      createClusterTask("INSTALL", "1-1-32", "host3")),

      createSortedSet(createClusterTask("INSTALL", "1-1-13", "host1"),
                      createClusterTask("CONFIGURE", "1-1-22", "host1"),
                      createClusterTask("INSTALL", "1-1-32", "host3"))
    );

    List<Set<ClusterTask>> actual = JobPlanner.deDupNodePerStage(tasks);

    List<Set<ClusterTask>> expected = ImmutableList.of(
      createSortedSet(createClusterTask("INSTALL", "1-1-1", "host1"),
                      createClusterTask("INSTALL", "1-1-3", "host3")),

      createSortedSet(createClusterTask("CONFIGURE", "1-1-2", "host1")),

      createSortedSet(createClusterTask("INSTALL", "1-1-12", "host1"),
                      createClusterTask("INSTALL", "1-1-22", "host2"),
                      createClusterTask("INSTALL", "1-1-32", "host3")),

      createSortedSet(createClusterTask("INSTALL", "1-1-13", "host1"),
                      createClusterTask("INSTALL", "1-1-32", "host3")),

      createSortedSet(createClusterTask("CONFIGURE", "1-1-22", "host1"))
    );

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testCreateTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.INITIALIZE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    TaskDag expected = new TaskDag();
    // all nodes start with create -> confirm -> bootstrap
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CREATE.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIRM.name(), ""));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CONFIRM.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CREATE.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIRM.name(), ""));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIRM.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""));

    // node1 just has s1, which has configure and start
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), "s1"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), "s1"),
                           new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"));

    // node 2 has all 3 services
    // s1 configure, s2 install, s3 install all just need bootstrap to be done
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s1"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s2"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s3"));
    // configure -> start for s1.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"));
    // install -> configure for s2.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s2"));
    // install -> configure -> initialize -> start for s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));
    // s1 start on both nodes has to happen before s3 initialize
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    // s1 start on both nodes also has to happen before s3 start
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));

    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.CLUSTER_CREATE);
    JobPlanner planner = new JobPlanner(job, clusterNodes);
    TaskDag dag = planner.createTaskDag();

    Assert.assertEquals(expected, dag);
  }

  @Test
  public void testPlannerObeysNodesToPlanSet() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.INITIALIZE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    // job for creating node1
    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.CLUSTER_CREATE,
                                    null, ImmutableSet.<String>of(node1.getId()));
    JobPlanner planner = new JobPlanner(job, clusterNodes);

    // nothing on node1 depends on node2, so it should just be
    // create -> confirm -> bootstrap -> s1 configure -> s1 start
    TaskDag expected = new TaskDag();
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CREATE.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIRM.name(), ""));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CONFIRM.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), "s1"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), "s1"),
                           new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"));
    Assert.assertEquals(expected, planner.createTaskDag());


    // job for creating node2
    job = new ClusterJob(JobId.fromString("123-002"), ClusterAction.CLUSTER_CREATE,
                         null, ImmutableSet.<String>of(node2.getId()));
    planner = new JobPlanner(job, clusterNodes);

    // services on node2 depend on service s1 on node1, so not all dag nodes will be for node2.
    expected = new TaskDag();
    // start with create -> confirm -> bootstrap
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CREATE.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIRM.name(), ""));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIRM.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""));
    // node 2 has all 3 services
    // s1 configure, s2 install, s3 install all just need bootstrap to be done
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s1"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s2"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s3"));
    // configure -> start for s1.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"));
    // install -> configure for s2.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s2"));
    // install -> configure -> initialize -> start for s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));
    // s1 start on both nodes has to happen before s3 initialize
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    // s1 start on both nodes also has to happen before s3 start
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));
    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testAddServicesPlan() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.INITIALIZE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.INITIALIZE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.INITIALIZE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    // Add service 1 to the nodes
    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.ADD_SERVICES,
                                    ImmutableSet.<String>of(s1.getName()), null);
    JobPlanner planner = new JobPlanner(job, clusterNodes);
    TaskDag expected = new TaskDag();

    // s1 does not depend on any other service, so no task nodes should be for any other service
    // bootstrap -> install s1 -> configure s1 -> initialize s1 -> start s1 on both nodes
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.INSTALL.name(), "s1"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.INSTALL.name(), "s1"),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), "s1"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), "s1"),
                           new TaskNode(node1.getId(), ProvisionerAction.INITIALIZE.name(), "s1"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.INITIALIZE.name(), "s1"),
                           new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"));

    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s1"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s1"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s1"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"));

    Assert.assertEquals(expected, planner.createTaskDag());

    // add services s2 and s3 to node2
    node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());
    clusterNodes = ImmutableSet.of(node1, node2);
    job = new ClusterJob(JobId.fromString("123-002"), ClusterAction.ADD_SERVICES,
                         ImmutableSet.<String>of(s2.getName(), s3.getName()), ImmutableSet.<String>of(node2.getId()));
    planner = new JobPlanner(job, clusterNodes);
    expected = new TaskDag();

    // bootstrap -> install -> configure -> initialize -> start for both services on node 2
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s2"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s2"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s2"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s2"));

    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INSTALL.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));

    // in addition, s3 depends on s2, so s3 initialize and s3 start depend on s2 start
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s3"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s2"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s3"));

    // since s2 depends on s1, initialize and start of s2 depends on start s1 on both nodes.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s2"));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s2"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.INITIALIZE.name(), "s2"));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), "s1"),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), "s2"));

    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testConfigureTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    TaskDag expected = new TaskDag();
    // node1 has bootstrap -> configure s1
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()));
    // node2 has bootstrap -> configure s1, bootstrap -> configure s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s3.getName()));

    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.CLUSTER_CONFIGURE);
    JobPlanner planner = new JobPlanner(job, clusterNodes);
    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testConfigureWithRestartTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, CHEF_ACTION,
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    TaskDag expected = new TaskDag();
    // stop of each service on each node depends on bootstrap
    // node1 only has service s1
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()));
    // stop s3 -> stop s2 -> stop s1
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    // stop s1 on node1 depends on stop s2 on node2
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node1.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    // configure of each service depends on stop of that service. no configure s2 since it has no configure action.
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.STOP.name(), s1.getName()),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s3.getName()));
    // start of each service depends on configure of that service.
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()),
                           new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));
    // except for s2. since it has no configure, start should depend on stop.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    // start s2 depends on start s1 on each node
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    // start s3 depends on start s2
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));

    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.CLUSTER_CONFIGURE_WITH_RESTART);
    JobPlanner planner = new JobPlanner(job, clusterNodes);
    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testStopServiceTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    // stop all services
    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.STOP_SERVICES);
    JobPlanner planner = new JobPlanner(job, clusterNodes);

    TaskDag expected = new TaskDag();
    // s3 needs to be stopped first, then s2, then s1.
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node1.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    Assert.assertEquals(expected, planner.createTaskDag());

    // request to stop just s2, which will stop s3 as well.
    job = new ClusterJob(JobId.fromString("123-002"), ClusterAction.STOP_SERVICES,
                         ImmutableSet.of(s2.getName()), null);
    planner = new JobPlanner(job, clusterNodes);

    expected = new TaskDag();
    // s3 needs to be stopped first, then s2
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()));
    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testStartServiceTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    // start all services
    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.START_SERVICES);
    JobPlanner planner = new JobPlanner(job, clusterNodes);

    TaskDag expected = new TaskDag();
    // start s1 -> start s2 -> start s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    Assert.assertEquals(expected, planner.createTaskDag());

    // request to start just s2, which will start s1 as well.
    job = new ClusterJob(JobId.fromString("123-002"), ClusterAction.START_SERVICES,
                         ImmutableSet.of(s2.getName()), null);
    planner = new JobPlanner(job, clusterNodes);

    expected = new TaskDag();
    // s1 needs to be started first, then s2
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testRestartServiceTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, CHEF_ACTION,
                                ProvisionerAction.STOP, CHEF_ACTION));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), ImmutableMap.<String, String>of());

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    // restart all services
    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.RESTART_SERVICES);
    JobPlanner planner = new JobPlanner(job, clusterNodes);

    TaskDag expected = new TaskDag();
    // stop s3 -> stop s2 -> stop s1 -> start s1 -> start s2 -> start s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node1.getId(), ProvisionerAction.STOP.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));
    // stop service -> start service
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.STOP.name(), s1.getName()),
                           new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));
    Assert.assertEquals(expected, planner.createTaskDag());

    // request to restart s2 and s3
    job = new ClusterJob(JobId.fromString("123-002"), ClusterAction.RESTART_SERVICES,
                         ImmutableSet.of(s2.getName()), null);
    planner = new JobPlanner(job, clusterNodes);

    expected = new TaskDag();
    // stop s3 -> stop s2 -> start s2 -> start s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));
    // start s1 -> start s2 gets thrown in for safety
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.START.name(), s1.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    // stop service -> start service
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s2.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s2.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.STOP.name(), s3.getName()),
                           new TaskNode(node2.getId(), ProvisionerAction.START.name(), s3.getName()));
    Assert.assertEquals(expected, planner.createTaskDag());
  }

  @Test
  public void testNoEdgeNodesInDag() {
    Service s1 = new Service("s1", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1), ImmutableMap.<String, String>of());
    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    TaskDag expected = new TaskDag();
    TaskNode taskNode1 = new TaskNode(node1.getId(), ProvisionerAction.DELETE.name(), "");
    TaskNode taskNode2 = new TaskNode(node2.getId(), ProvisionerAction.DELETE.name(), "");
    expected.addTaskNode(taskNode1);
    expected.addTaskNode(taskNode2);

    ClusterJob job = new ClusterJob(JobId.fromString("123-001"), ClusterAction.CLUSTER_DELETE);
    JobPlanner planner = new JobPlanner(job, clusterNodes);
    TaskDag actual = planner.createTaskDag();

    Assert.assertEquals(expected, actual);

    List<Set<TaskNode>> linearizedTasks = actual.linearize();
    Assert.assertEquals(1, linearizedTasks.size());
    Assert.assertEquals(2, linearizedTasks.get(0).size());
    Assert.assertTrue(linearizedTasks.get(0).contains(taskNode1));
    Assert.assertTrue(linearizedTasks.get(0).contains(taskNode2));
  }

  private ClusterTask createClusterTask(String name, String taskId, String hostId) {
    return new ClusterTask(
      ProvisionerAction.valueOf(name), TaskId.fromString(taskId), hostId, "service", ClusterAction.CLUSTER_CREATE,
      new JsonObject());
  }

  private Set<ClusterTask> createSortedSet(ClusterTask... t) {
    Set<ClusterTask> set = Sets.newTreeSet(CLUSTER_TASK_COMPARATOR);
    Collections.addAll(set, t);
    return set;
  }

  private static final Comparator<ClusterTask> CLUSTER_TASK_COMPARATOR =
    new Comparator<ClusterTask>() {
      @Override
      public int compare(ClusterTask task1, ClusterTask task2) {
        return task1.getTaskName().compareTo(task2.getTaskName());
      }
    };
}
