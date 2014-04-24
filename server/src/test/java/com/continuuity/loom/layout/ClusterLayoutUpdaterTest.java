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
package com.continuuity.loom.layout;

import com.continuuity.loom.Entities;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class ClusterLayoutUpdaterTest extends BaseSolverTest {
  private ClusterLayoutUpdater updater;
  private Cluster cluster;
  private Set<Node> nodes;

  @Test
  public void testAddService() throws Exception {
    // adding resourcemanager
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    NodeLayout masterNodeLayout =
      new NodeLayout("large-mem", "centos6", ImmutableSet.of(namenode.getName(), resourcemanager.getName()));
    expectedCounts.add(masterNodeLayout, 1);
    NodeLayout slaveNodeLayout = new NodeLayout("medium", "centos6", ImmutableSet.of(datanode.getName()));
    expectedCounts.add(slaveNodeLayout, 50);
    ClusterLayout expected = new ClusterLayout(reactorTemplate.getConstraints(), expectedCounts);

    ClusterLayout layout =
      updater.addServicesToCluster(cluster, nodes, ImmutableSet.of(resourcemanager.getName())).getCurrentLayout();
    Assert.assertEquals(expected, layout);
  }

  @Test
  public void testAddServices() throws Exception {
    // adding resourcemanager, nodemanager, hbasemaster, regionserver
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    NodeLayout masterNodeLayout =
      new NodeLayout("large-mem", "centos6",
                     ImmutableSet.of(namenode.getName(), resourcemanager.getName(), hbasemaster.getName()));
    expectedCounts.add(masterNodeLayout, 1);
    NodeLayout slaveNodeLayout =
      new NodeLayout("medium", "centos6",
                     ImmutableSet.of(datanode.getName(), nodemanager.getName(), regionserver.getName()));
    expectedCounts.add(slaveNodeLayout, 50);
    ClusterLayout expected = new ClusterLayout(reactorTemplate.getConstraints(), expectedCounts);

    ClusterLayout layout = updater.addServicesToCluster(
      cluster, nodes, ImmutableSet.of(resourcemanager.getName(), nodemanager.getName(),
                                      hbasemaster.getName(), regionserver.getName())).getCurrentLayout();
    Assert.assertEquals(expected, layout);
  }

  @Test
  public void testNoSolutionReturnsNull() throws Exception {
    // zookeeper is forced onto its own node, should not be possible to add it
    Assert.assertNull(updater.addServicesToCluster(cluster, nodes, ImmutableSet.of(zookeeper.getName())));
  }

  @Test
  public void testNoSolutionWithMixedServicesReturnsNull() throws Exception {
    // it is possible to add resourcemanger and nodemanager, but
    // reactor is forced onto its own node, should not be possible to add it.
    Assert.assertNull(updater.addServicesToCluster(cluster, nodes, ImmutableSet.of(
      resourcemanager.getName(), nodemanager.getName(), zookeeper.getName())));
  }

  @Before
  public void beforeLayoutUpdater() throws Exception {
    updater = injector.getInstance(ClusterLayoutUpdater.class);

    nodes = Sets.newHashSet();
    // 200 node cluster, 1 hadoop master node, 199 hadoop slave nodes
    String clusterId = "123";
    Set<String> nodeIds = Sets.newHashSet();
    // hadoop master node
    Node node = new Node(UUID.randomUUID().toString(), clusterId,
                         ImmutableSet.of(namenode),
                         ImmutableMap.<String, String>of(
                           Node.Properties.HARDWARETYPE.name().toLowerCase(), "large-mem",
                           Node.Properties.IMAGETYPE.name().toLowerCase(), "centos6"));
    clusterStore.writeNode(node);
    nodeIds.add(node.getId());
    nodes.add(node);
    // slave nodes
    for (int i = 0; i < 50; i++) {
      node = new Node(UUID.randomUUID().toString(), clusterId, ImmutableSet.of(datanode),
                      ImmutableMap.<String, String>of(
                        Node.Properties.HARDWARETYPE.name().toLowerCase(), "medium",
                        Node.Properties.IMAGETYPE.name().toLowerCase(), "centos6"));
      clusterStore.writeNode(node);
      nodeIds.add(node.getId());
      nodes.add(node);
    }
    cluster = new Cluster(clusterId, "user1", "hadoop", System.currentTimeMillis(), "hadoop cluster",
                          Entities.ProviderExample.RACKSPACE, reactorTemplate, nodeIds,
                          ImmutableSet.of(namenode.getName(), datanode.getName()));
  }
}
