package com.continuuity.loom.layout;

import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.utils.ImmutablePair;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

/**
 *
 */
public class ClusterLayoutTest {
  private static Constraints constraints;

  @Test
  public void testExpandClusterLayout() {
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
    ClusterLayout expanded = ClusterLayout.expandClusterLayout(layout, "resourcemanager", addCounts);
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    expectedCounts.add(expandedMasterNodeLayout, 1);
    expectedCounts.add(slaveLayout, 50);
    ClusterLayout expected = new ClusterLayout(constraints, expectedCounts);
    Assert.assertEquals(expected, expanded);

    // add nodemanager to half of the slave nodes
    addCounts = HashMultiset.create();
    addCounts.add(slaveLayout, 25);
    expanded = ClusterLayout.expandClusterLayout(expanded, "nodemanager", addCounts);
    expectedCounts = HashMultiset.create();
    expectedCounts.add(expandedMasterNodeLayout, 1);
    expectedCounts.add(slaveLayout, 25);
    expectedCounts.add(expandedSlaveNodeLayout, 25);
    expected = new ClusterLayout(constraints, expectedCounts);
    Assert.assertEquals(expected, expanded);

    // add nodemanager to the rest of the slave nodes
    expanded = ClusterLayout.expandClusterLayout(expanded, "nodemanager", addCounts);
    expectedCounts = HashMultiset.create();
    expectedCounts.add(expandedMasterNodeLayout, 1);
    expectedCounts.add(expandedSlaveNodeLayout, 50);
    expected = new ClusterLayout(constraints, expectedCounts);
    Assert.assertEquals(expected, expanded);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNodeLayoutsThrowsException() {
    NodeLayout masterNodeLayout = new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    NodeLayout badNodeLayout = new NodeLayout("large-mem", "ubuntu12", ImmutableSet.of("namenode"));
    Multiset badCounts = HashMultiset.create();
    badCounts.add(badNodeLayout);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    ClusterLayout.expandClusterLayout(layout, "resourcemanager", badCounts);
  }

  @Test
  public void testInvalidHardwareTypeShowsAsInvalid() {
    NodeLayout badNodeLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("namenode"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(badNodeLayout, 1);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Assert.assertFalse(layout.isValid());
  }

  @Test
  public void testInvalidImageTypeShowsAsInvalid() {
    NodeLayout badNodeLayout = new NodeLayout("large-mem", "rhel5", ImmutableSet.of("namenode"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(badNodeLayout, 1);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Assert.assertFalse(layout.isValid());
  }

  @Test
  public void testInvalidCountsShowsAsInvalid() {
    NodeLayout goodNodeLayout = new NodeLayout("large-mem", "ubuntu12", ImmutableSet.of("namenode"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(goodNodeLayout, 2);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Assert.assertFalse(layout.isValid());
  }

  @Test
  public void testValidLayout() {
    NodeLayout masterNodeLayout = new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode"));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("datanode"));
    NodeLayout reactorLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("reactor", "zookeeper"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(reactorLayout);
    counts.add(slaveLayout, 50);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Assert.assertTrue(layout.isValid());
  }

  @BeforeClass
  public static void beforeClusterLayoutTest() {
    constraints = new Constraints(
      ImmutableMap.<String, ServiceConstraint>of(
        "namenode",
        new ServiceConstraint(
          ImmutableSet.of("large-mem"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 1, 1, null),
        "datanode",
        new ServiceConstraint(
          ImmutableSet.of("medium", "large-cpu"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 50, 1, null),
        "zookeeper",
        new ServiceConstraint(
          ImmutableSet.of("small", "medium"),
          ImmutableSet.of("centos6"), 1, 5, 2, ImmutablePair.of(1, 20)),
        "reactor",
        new ServiceConstraint(
          ImmutableSet.of("medium", "large"),
          null, 1, 5, 1, ImmutablePair.of(1, 10))
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
      )
    );
  }
}
