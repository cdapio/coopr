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
package co.cask.coopr.layout.change;

import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.layout.BaseSolverTest;
import co.cask.coopr.layout.ClusterLayout;
import co.cask.coopr.layout.NodeLayout;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.LayoutConstraint;
import co.cask.coopr.spec.template.ServiceConstraint;
import co.cask.coopr.spec.template.SizeConstraint;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class AddServicesChangeTest extends BaseSolverTest {
  private static Constraints constraints;

  @Test
  public void testApplyChangeToClusterLayout() {
    NodeLayout masterNodeLayout = new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode"));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("datanode"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(slaveLayout, 50);
    ClusterLayout layout = new ClusterLayout(constraints, counts);

    NodeLayout expandedMasterNodeLayout =
      new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode", "resourcemanager"));
    NodeLayout expandedSlaveNodeLayout =
      new NodeLayout("medium", "centos6", ImmutableSet.of("datanode", "nodemanager"));

    // add resourcemanager to master node
    Multiset<NodeLayout> addCounts = HashMultiset.create();
    addCounts.add(masterNodeLayout, 1);
    AddServicesChange change = new AddServicesChange(addCounts, "resourcemanager");
    ClusterLayout expanded = change.applyChange(layout);
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    expectedCounts.add(expandedMasterNodeLayout, 1);
    expectedCounts.add(slaveLayout, 50);
    ClusterLayout expected = new ClusterLayout(constraints, expectedCounts);
    Assert.assertEquals(expected, expanded);

    // add nodemanager to half of the slave nodes
    addCounts = HashMultiset.create();
    addCounts.add(slaveLayout, 25);
    change = new AddServicesChange(addCounts, "nodemanager");
    expanded = change.applyChange(expanded);
    expectedCounts = HashMultiset.create();
    expectedCounts.add(expandedMasterNodeLayout, 1);
    expectedCounts.add(slaveLayout, 25);
    expectedCounts.add(expandedSlaveNodeLayout, 25);
    expected = new ClusterLayout(constraints, expectedCounts);
    Assert.assertEquals(expected, expanded);

    // add nodemanager to the rest of the slave nodes
    expanded = change.applyChange(expanded);
    expectedCounts = HashMultiset.create();
    expectedCounts.add(expandedMasterNodeLayout, 1);
    expectedCounts.add(expandedSlaveNodeLayout, 50);
    expected = new ClusterLayout(constraints, expectedCounts);
    Assert.assertEquals(expected, expanded);
  }

  @Test
  public void testInvalidNodeLayouts() {
    NodeLayout masterNodeLayout = new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    NodeLayout badNodeLayout = new NodeLayout("large-mem", "ubuntu12", ImmutableSet.of("namenode"));
    Multiset<NodeLayout> badCounts = HashMultiset.create();
    badCounts.add(badNodeLayout);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    AddServicesChange change = new AddServicesChange(badCounts, "resourcemanager");
    Assert.assertFalse(change.canApplyChange(layout));
  }

  @Test
  public void testApplyChangeToCluster() {
    String clusterId = "123";
    Set<String> nodeIds = Sets.newHashSet();
    Set<Node> nodes = Sets.newHashSet();
    // hadoop master node
    Node node = new Node(UUID.randomUUID().toString(), clusterId,
                         ImmutableSet.of(namenode),
                         NodeProperties.builder().setHardwaretype("large-mem").setImagetype("centos6").build());
    nodeIds.add(node.getId());
    nodes.add(node);
    // slave nodes
    for (int i = 0; i < 50; i++) {
      node = new Node(UUID.randomUUID().toString(), clusterId, ImmutableSet.of(datanode),
                      NodeProperties.builder().setHardwaretype("medium").setImagetype("centos6").build());
      nodeIds.add(node.getId());
      nodes.add(node);
    }
    Cluster cluster = Cluster.builder()
      .setID("123")
      .setAccount(new Account("user1", "tenant1"))
      .setName("hadoop")
      .setProvider(Entities.ProviderExample.RACKSPACE)
      .setClusterTemplate(reactorTemplate)
      .setNodes(nodeIds)
      .setServices(ImmutableSet.of(namenode.getName(), datanode.getName()))
      .build();
    Constraints constraints = cluster.getClusterTemplate().getConstraints();

    // create the change objects
    Set<Node> affectedNodes = Sets.newHashSet();
    NodeLayout masterNodeLayout = new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode"));
    NodeLayout expandedMasterNodeLayout =
      new NodeLayout("large-mem", "centos6", ImmutableSet.of(namenode.getName(), resourcemanager.getName()));
    NodeLayout finalMasterNodeLayout =
      new NodeLayout("large-mem", "centos6",
                     ImmutableSet.of(namenode.getName(), resourcemanager.getName(), hbasemaster.getName()));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of(datanode.getName()));
    NodeLayout expandedSlaveLayout =
      new NodeLayout("medium", "centos6", ImmutableSet.of(datanode.getName(), nodemanager.getName()));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    AddServicesChange resourceManagerChange = new AddServicesChange(counts, resourcemanager.getName());
    counts = HashMultiset.create();
    counts.add(slaveLayout, 25);
    AddServicesChange nodeManagerChange = new AddServicesChange(counts, nodemanager.getName());
    counts = HashMultiset.create();
    counts.add(expandedMasterNodeLayout);
    AddServicesChange hbaseMasterChange = new AddServicesChange(counts, hbasemaster.getName());

    // expected cluster has the master node expanded with the resourcemanager and 50 slaves nodes same as before
    Cluster expectedCluster = copyOfClusterWith(cluster, ImmutableSet.of(
      namenode.getName(), datanode.getName(), resourcemanager.getName()));
    counts = HashMultiset.create();
    counts.add(expandedMasterNodeLayout);
    counts.add(slaveLayout, 50);
    ClusterLayout expectedLayout = new ClusterLayout(constraints, counts);
    affectedNodes.addAll(resourceManagerChange.applyChange(cluster, nodes, serviceMap));
    Assert.assertEquals(expectedCluster, cluster);
    Assert.assertEquals(expectedLayout, ClusterLayout.fromNodes(nodes, constraints));
    Assert.assertEquals(1, affectedNodes.size());

    // adding nodemanager to 25 slaves nodes
    expectedCluster = copyOfClusterWith(cluster, ImmutableSet.of(
      namenode.getName(), datanode.getName(), resourcemanager.getName(), nodemanager.getName()));
    counts = HashMultiset.create();
    counts.add(expandedMasterNodeLayout);
    counts.add(expandedSlaveLayout, 25);
    counts.add(slaveLayout, 25);
    expectedLayout = new ClusterLayout(constraints, counts);
    affectedNodes.addAll(nodeManagerChange.applyChange(cluster, nodes, serviceMap));
    Assert.assertEquals(expectedCluster, cluster);
    Assert.assertEquals(expectedLayout, ClusterLayout.fromNodes(nodes, constraints));
    Assert.assertEquals(26, affectedNodes.size());

    // adding nodemanager to rest of 25 slaves
    counts = HashMultiset.create();
    counts.add(expandedMasterNodeLayout);
    counts.add(expandedSlaveLayout, 50);
    expectedLayout = new ClusterLayout(constraints, counts);
    affectedNodes.addAll(nodeManagerChange.applyChange(cluster, nodes, serviceMap));
    Assert.assertEquals(expectedCluster, cluster);
    Assert.assertEquals(expectedLayout, ClusterLayout.fromNodes(nodes, constraints));

    // all nodes should have been affected by now.
    Assert.assertEquals(nodes, affectedNodes);

    // add hbase-master to master node
    expectedCluster = copyOfClusterWith(cluster, ImmutableSet.of(
      namenode.getName(), datanode.getName(), resourcemanager.getName(), nodemanager.getName(), hbasemaster.getName()));
    counts = HashMultiset.create();
    counts.add(finalMasterNodeLayout);
    counts.add(expandedSlaveLayout, 50);
    expectedLayout = new ClusterLayout(constraints, counts);
    affectedNodes.addAll(hbaseMasterChange.applyChange(cluster, nodes, serviceMap));
    Assert.assertEquals(expectedCluster, cluster);
    Assert.assertEquals(expectedLayout, ClusterLayout.fromNodes(nodes, constraints));
    // all nodes are affected, shouldn't be more.
    Assert.assertEquals(nodes, affectedNodes);
  }

  private Cluster copyOfClusterWith(Cluster cluster, Set<String> services) {
    return Cluster.builder()
      .setID(cluster.getId())
      .setAccount(cluster.getAccount())
      .setName(cluster.getName())
      .setDescription(cluster.getDescription())
      .setCreateTime(cluster.getCreateTime())
      .setProvider(cluster.getProvider())
      .setClusterTemplate(cluster.getClusterTemplate())
      .setNodes(cluster.getNodeIDs())
      .setServices(services)
      .build();
  }

  @BeforeClass
  public static void beforeClass() {
    constraints = new Constraints(
      ImmutableMap.<String, ServiceConstraint>of(
        "namenode",
        new ServiceConstraint(
          ImmutableSet.of("large-mem"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 1),
        "datanode",
        new ServiceConstraint(
          ImmutableSet.of("medium", "large-cpu"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 50),
        "zookeeper",
        new ServiceConstraint(
          ImmutableSet.of("small", "medium"),
          ImmutableSet.of("centos6"), 1, 5),
        "reactor",
        new ServiceConstraint(
          ImmutableSet.of("medium", "large"),
          null, 1, 5)
      ),
      new LayoutConstraint(
        ImmutableSet.<Set<String>>of(
          ImmutableSet.of("datanode", "nodemanager", "regionserver"),
          ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")
        ),
        ImmutableSet.<Set<String>>of(
          ImmutableSet.of("datanode", "namenode"),
          ImmutableSet.of("datanode", "zookeeper"),
          ImmutableSet.of("namenode", "zookeeper"),
          ImmutableSet.of("datanode", "reactor"),
          ImmutableSet.of("namenode", "reactor")
        )
      ),
      SizeConstraint.EMPTY
    );
  }
}
