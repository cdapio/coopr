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
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.dag.TaskDag;
import com.continuuity.loom.scheduler.dag.TaskNode;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.utils.ImmutablePair;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ClusterSchedulerTest {

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

    List<Set<ClusterTask>> actual = ClusterScheduler.deDupNodePerStage(tasks);

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
  public void testMinimizeDependencies() {
    Map<ProvisionerAction, ServiceAction> emptyActions = ImmutableMap.of();
    Service base =  new Service("base", "", ImmutableSet.<String>of(), emptyActions);
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of("base"), emptyActions);
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("base", "s1"), emptyActions);
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("base", "s1", "s2"), emptyActions);
    Service s4 =  new Service("s4", "", ImmutableSet.<String>of("base"), emptyActions);
    Service s5 =  new Service("s5", "", ImmutableSet.<String>of("base", "s1", "s2", "s3", "s4"), emptyActions);
    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(base.getName(), base);
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2.getName(), s2);
    serviceMap.put(s3.getName(), s3);
    serviceMap.put(s4.getName(), s4);
    serviceMap.put(s5.getName(), s5);
    SetMultimap<String, String> expected = HashMultimap.create();
    expected.put("s1", "base");
    expected.put("s2", "s1");
    expected.put("s3", "s2");
    expected.put("s4", "base");
    expected.put("s5", "s3");
    expected.put("s5", "s4");
    SetMultimap<String, String> actual = ClusterScheduler.minimizeDependencies(serviceMap);
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.get("base").isEmpty());
  }

  /**
   *     |---> s2 ---|           |---> s6
   * s1--|           |---> s4 ---|
   *     |---> s3 ---|           |---> s7
   *           |
   *           |---------> s5
   */
  @Test
  public void testDependsOn() {
    Multimap<String, String> dependencies = HashMultimap.create();
    dependencies.put("s2", "s1");
    dependencies.put("s3", "s1");
    dependencies.put("s4", "s3");
    dependencies.put("s4", "s2");
    dependencies.put("s5", "s3");
    dependencies.put("s6", "s4");
    dependencies.put("s7", "s4");
    Map<String, Set<String>> serviceDeps = Maps.newHashMap();
    serviceDeps.put("s1", ImmutableSet.<String>of());
    serviceDeps.put("s2", ImmutableSet.<String>of("s1"));
    serviceDeps.put("s3", ImmutableSet.<String>of("s1"));
    serviceDeps.put("s4", ImmutableSet.<String>of("s1", "s2", "s3"));
    serviceDeps.put("s5", ImmutableSet.<String>of("s1", "s3"));
    serviceDeps.put("s6", ImmutableSet.<String>of("s1", "s2", "s3", "s4"));
    serviceDeps.put("s7", ImmutableSet.<String>of("s1", "s2", "s3", "s4"));

    for (String service1 : serviceDeps.keySet()) {
      for (String service2 : serviceDeps.keySet()) {
        Assert.assertEquals(serviceDeps.get(service1).contains(service2),
                            ClusterScheduler.dependsOn(service1, service2, dependencies));
      }
    }
  }

  /**
   *     |---> s2 ---|
   * s1--|           |---> s4
   *     |---> s3 ---|      |
   *                        |
   * s5 -----> s6 ----------|
   *
   */
  @Test
  public void testFindDirectActionDependencies() {
    ServiceAction sAction = new ServiceAction(null, null, null);
    // s1 has initialize and start
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction,
                                ProvisionerAction.START, sAction));
    // s2 has configure and initialize
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, sAction,
                                ProvisionerAction.INITIALIZE, sAction));
    // s3 has start
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, sAction));
    // s4 has initialize and start
    Service s4 =  new Service("s4", "", ImmutableSet.<String>of("s2", "s3", "s6"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction,
                                ProvisionerAction.START, sAction));
    Service s5 =  new Service("s5", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction,
                                ProvisionerAction.START, sAction));
    Service s6 =  new Service("s6", "", ImmutableSet.<String>of("s5"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction));

    SetMultimap<String, String> serviceDependencies = HashMultimap.create();
    serviceDependencies.putAll(s1.getName(), s1.getDependsOn());
    serviceDependencies.putAll(s2.getName(), s2.getDependsOn());
    serviceDependencies.putAll(s3.getName(), s3.getDependsOn());
    serviceDependencies.putAll(s4.getName(), s4.getDependsOn());
    serviceDependencies.putAll(s5.getName(), s5.getDependsOn());
    serviceDependencies.putAll(s6.getName(), s6.getDependsOn());

    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2.getName(), s2);
    serviceMap.put(s3.getName(), s3);
    serviceMap.put(s4.getName(), s4);
    serviceMap.put(s5.getName(), s5);
    serviceMap.put(s6.getName(), s6);

    Set<Actions.Dependency> actionDependencies = ImmutableSet.of(
      new Actions.Dependency(ProvisionerAction.START, ProvisionerAction.START),
      new Actions.Dependency(ProvisionerAction.START, ProvisionerAction.INITIALIZE)
    );

    SetMultimap<ImmutablePair<String, ProvisionerAction>, ImmutablePair<String, ProvisionerAction>> expected =
      HashMultimap.create();

    // s2 initialize depends on s1 start
    expected.put(ImmutablePair.of("s2", ProvisionerAction.INITIALIZE), ImmutablePair.of("s1", ProvisionerAction.START));
    // s3 start depends on s1 start
    expected.put(ImmutablePair.of("s3", ProvisionerAction.START), ImmutablePair.of("s1", ProvisionerAction.START));
    // s4 start depends on s1 start, s3 start, and s5 start.  but s3 depends on s1 so s3 start should not be here.
    expected.put(ImmutablePair.of("s4", ProvisionerAction.START), ImmutablePair.of("s3", ProvisionerAction.START));
    expected.put(ImmutablePair.of("s4", ProvisionerAction.START), ImmutablePair.of("s5", ProvisionerAction.START));
    // s4 initialize depends on s1 start, s3 start, and s5 start.  but s3 depends on s1 so s3 start should not be here.
    expected.put(ImmutablePair.of("s4", ProvisionerAction.INITIALIZE), ImmutablePair.of("s3", ProvisionerAction.START));
    expected.put(ImmutablePair.of("s4", ProvisionerAction.INITIALIZE), ImmutablePair.of("s5", ProvisionerAction.START));
    // s6 initialize depends on s5 start
    expected.put(ImmutablePair.of("s6", ProvisionerAction.INITIALIZE), ImmutablePair.of("s5", ProvisionerAction.START));

    SetMultimap<ImmutablePair<String, ProvisionerAction>, ImmutablePair<String, ProvisionerAction>> actual =
      ClusterScheduler.findDirectActionDependencies(serviceDependencies, actionDependencies, serviceMap);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testCreateTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.START, new ServiceAction("chef", "script", "data")));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data")));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.INITIALIZE, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.START, new ServiceAction("chef", "script", "data")));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), Collections.EMPTY_MAP);
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), Collections.EMPTY_MAP);

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);
    Multimap<String, Node> serviceNodeMap = HashMultimap.create();
    serviceNodeMap.put(s1.getName(), node1);
    serviceNodeMap.put(s1.getName(), node2);
    serviceNodeMap.put(s2.getName(), node2);
    serviceNodeMap.put(s3.getName(), node2);
    Map<String, Service> serviceMap = ImmutableMap.of(s1.getName(), s1, s2.getName(), s2, s3.getName(), s3);

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
    // s1 configure, s2 install, s3 install all just need boostrap to be done
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

    TaskDag dag = ClusterScheduler.createTaskDag(ClusterAction.CLUSTER_CREATE,
                                                 new Actions(), clusterNodes, serviceNodeMap, serviceMap);

    Assert.assertEquals(expected, dag);
  }

  @Test
  public void testConfigureTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data")));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, new ServiceAction("chef", "script", "data")));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data")));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), Collections.EMPTY_MAP);
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), Collections.EMPTY_MAP);

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);
    Multimap<String, Node> serviceNodeMap = HashMultimap.create();
    serviceNodeMap.put(s1.getName(), node1);
    serviceNodeMap.put(s1.getName(), node2);
    serviceNodeMap.put(s2.getName(), node2);
    serviceNodeMap.put(s3.getName(), node2);
    Map<String, Service> serviceMap = ImmutableMap.of(s1.getName(), s1, s2.getName(), s2, s3.getName(), s3);

    TaskDag expected = new TaskDag();
    // node1 has bootstrap -> configure s1
    expected.addDependency(new TaskNode(node1.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node1.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()));
    // node2 has bootstrap -> configure s1, bootstrap -> configure s3
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s1.getName()));
    expected.addDependency(new TaskNode(node2.getId(), ProvisionerAction.BOOTSTRAP.name(), ""),
                           new TaskNode(node2.getId(), ProvisionerAction.CONFIGURE.name(), s3.getName()));

    TaskDag dag = ClusterScheduler.createTaskDag(ClusterAction.CLUSTER_CONFIGURE,
                                                 new Actions(), clusterNodes, serviceNodeMap, serviceMap);
    Assert.assertEquals(expected, dag);
  }

  @Test
  public void testConfigureWithRestartTaskDag() {
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.START, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.STOP, new ServiceAction("chef", "script", "data")));
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INSTALL, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.START, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.STOP, new ServiceAction("chef", "script", "data")));
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1", "s2"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.START, new ServiceAction("chef", "script", "data"),
                                ProvisionerAction.STOP, new ServiceAction("chef", "script", "data")));
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), Collections.EMPTY_MAP);
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1, s2, s3), Collections.EMPTY_MAP);

    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);
    Multimap<String, Node> serviceNodeMap = HashMultimap.create();
    serviceNodeMap.put(s1.getName(), node1);
    serviceNodeMap.put(s1.getName(), node2);
    serviceNodeMap.put(s2.getName(), node2);
    serviceNodeMap.put(s3.getName(), node2);
    Map<String, Service> serviceMap = ImmutableMap.of(s1.getName(), s1, s2.getName(), s2, s3.getName(), s3);

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


    TaskDag dag = ClusterScheduler.createTaskDag(ClusterAction.CLUSTER_CONFIGURE_WITH_RESTART,
                                                 new Actions(), clusterNodes, serviceNodeMap, serviceMap);

    Assert.assertEquals(expected, dag);
  }

  @Test
  public void testNoEdgeNodesInDag() {
    Service s1 = new Service("s1", "", Collections.EMPTY_SET, Collections.EMPTY_MAP);
    Node node1 = new Node("node1", "1", ImmutableSet.<Service>of(s1), Collections.EMPTY_MAP);
    Node node2 = new Node("node2", "1", ImmutableSet.<Service>of(s1), Collections.EMPTY_MAP);
    Multimap<String, Node> serviceNodeMap = HashMultimap.create();
    serviceNodeMap.put(s1.getName(), node1);
    serviceNodeMap.put(s1.getName(), node2);
    Map<String, Service> serviceMap = ImmutableMap.of(s1.getName(), s1);
    Set<Node> clusterNodes = ImmutableSet.of(node1, node2);

    TaskDag expected = new TaskDag();
    TaskNode taskNode1 = new TaskNode(node1.getId(), ProvisionerAction.DELETE.name(), "");
    TaskNode taskNode2 = new TaskNode(node2.getId(), ProvisionerAction.DELETE.name(), "");
    expected.addTaskNode(taskNode1);
    expected.addTaskNode(taskNode2);
    TaskDag actual = ClusterScheduler.createTaskDag(ClusterAction.CLUSTER_DELETE, new Actions(),
                                                    clusterNodes, serviceNodeMap, serviceMap);

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
