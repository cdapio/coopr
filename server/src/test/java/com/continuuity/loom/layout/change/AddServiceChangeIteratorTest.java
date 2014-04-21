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
package com.continuuity.loom.layout.change;

import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.layout.ClusterLayout;
import com.continuuity.loom.layout.NodeLayout;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class AddServiceChangeIteratorTest {
  private static Constraints constraints;

  @Test
  public void testSimpleIterator() {
    NodeLayout masterNodeLayout = new NodeLayout("large", "centos6", ImmutableSet.of("master1"));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("slave1"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(slaveLayout);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Iterator<ClusterLayoutChange> iter = new AddServiceChangeIterator(layout, "slave2");

    // only possible change is to add slave2 to the slave node
    List<ClusterLayoutChange> expected = Lists.newArrayList();
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    expectedCounts.add(slaveLayout);
    expected.add(new AddServicesChange(expectedCounts, "slave2"));

    assertIterator(expected, iter);
  }

  @Test
  public void testMultiSlaveNodeIterator() {
    NodeLayout masterNodeLayout = new NodeLayout("large", "centos6", ImmutableSet.of("master1"));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("slave1"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(slaveLayout, 5);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Iterator<ClusterLayoutChange> iter = new AddServiceChangeIterator(layout, "slave2");

    // should try to add service to 5 slave nodes, then 4, then 3, then 2, then 1.
    List<ClusterLayoutChange> expected = Lists.newArrayList();
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    expectedCounts.setCount(slaveLayout, 5);
    expected.add(new AddServicesChange(expectedCounts, "slave2"));
    expectedCounts.setCount(slaveLayout, 4);
    expected.add(new AddServicesChange(expectedCounts, "slave2"));
    expectedCounts.setCount(slaveLayout, 3);
    expected.add(new AddServicesChange(expectedCounts, "slave2"));
    expectedCounts.setCount(slaveLayout, 2);
    expected.add(new AddServicesChange(expectedCounts, "slave2"));
    expectedCounts.setCount(slaveLayout, 1);
    expected.add(new AddServicesChange(expectedCounts, "slave2"));

    assertIterator(expected, iter);
  }

  @Test
  public void testMultiNodeIterator() {
    NodeLayout masterNodeLayout = new NodeLayout("large", "centos6", ImmutableSet.of("master1"));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("slave1", "slave2"));
    NodeLayout appLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("app"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(slaveLayout, 3);
    counts.add(appLayout, 2);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    Iterator<ClusterLayoutChange> iter = new AddServiceChangeIterator(layout, "base");

    // slave, master, app
    List<ClusterLayoutChange> expected = Lists.newArrayList();
    // add base to 6 nodes:
    // 3, 1, 2
    addExpected(expected, "base", slaveLayout, 3, masterNodeLayout, 1, appLayout, 2);
    // add base to 5 nodes:
    // 3, 1, 1
    // 3, 0, 2
    // 2, 1, 2
    addExpected(expected, "base", slaveLayout, 3, masterNodeLayout, 1, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 3, masterNodeLayout, 0, appLayout, 2);
    addExpected(expected, "base", slaveLayout, 2, masterNodeLayout, 1, appLayout, 2);
    // add base to 4 nodes:
    // 3, 1, 0
    // 3, 0, 1
    // 2, 1, 1
    // 2, 0, 2
    // 1, 1, 2
    addExpected(expected, "base", slaveLayout, 3, masterNodeLayout, 1, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 3, masterNodeLayout, 0, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 2, masterNodeLayout, 1, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 2, masterNodeLayout, 0, appLayout, 2);
    addExpected(expected, "base", slaveLayout, 1, masterNodeLayout, 1, appLayout, 2);
    // add base to 3 nodes:
    // 3, 0, 0
    // 2, 1, 0
    // 2, 0, 1
    // 1, 1, 1
    // 1, 0, 2
    // 0, 1, 2
    addExpected(expected, "base", slaveLayout, 3, masterNodeLayout, 0, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 2, masterNodeLayout, 1, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 2, masterNodeLayout, 0, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 1, masterNodeLayout, 1, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 1, masterNodeLayout, 0, appLayout, 2);
    addExpected(expected, "base", slaveLayout, 0, masterNodeLayout, 1, appLayout, 2);
    // add base to 2 nodes:
    // 2, 0, 0
    // 1, 1, 0
    // 1, 0, 1
    // 0, 1, 1
    // 0, 0, 2
    addExpected(expected, "base", slaveLayout, 2, masterNodeLayout, 0, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 1, masterNodeLayout, 1, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 1, masterNodeLayout, 0, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 0, masterNodeLayout, 1, appLayout, 1);
    addExpected(expected, "base", slaveLayout, 0, masterNodeLayout, 0, appLayout, 2);
    // add base to 1 node:
    // 1, 0, 0
    // 0, 1, 0
    // 0, 0, 1
    addExpected(expected, "base", slaveLayout, 1, masterNodeLayout, 0, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 0, masterNodeLayout, 1, appLayout, 0);
    addExpected(expected, "base", slaveLayout, 0, masterNodeLayout, 0, appLayout, 1);

    assertIterator(expected, iter);
  }

  private void addExpected(List<ClusterLayoutChange> expected, String service, NodeLayout nodeLayout1, int count1,
                           NodeLayout nodeLayout2, int count2, NodeLayout nodeLayout3, int count3) {
    Multiset<NodeLayout> expectedCounts = HashMultiset.create();
    expectedCounts.setCount(nodeLayout1, count1);
    expectedCounts.setCount(nodeLayout2, count2);
    expectedCounts.setCount(nodeLayout3, count3);
    expected.add(new AddServicesChange(expectedCounts, service));

  }

  private <T> void assertIterator(List<T> expected, Iterator<T> iter) {
    while (iter.hasNext()) {
      Assert.assertEquals(expected.remove(0), iter.next());
    }
    Assert.assertEquals(0, expected.size());
  }

  @BeforeClass
  public static void beforeClass() {
    constraints = new Constraints(
      ImmutableMap.<String, ServiceConstraint>of(
        "master1",
        new ServiceConstraint(
          ImmutableSet.of("large"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 1, 1, null),
        "slave1",
        new ServiceConstraint(
          ImmutableSet.of("medium"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 50, 1, null),
        "slave2",
        new ServiceConstraint(
          ImmutableSet.of("medium"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 50, 1, null),
        "base",
        new ServiceConstraint(
          null,
          ImmutableSet.of("centos6", "ubuntu12"), 0, 50, 1, null),
        "app",
        new ServiceConstraint(
          ImmutableSet.of("medium"),
          ImmutableSet.of("centos6", "ubuntu12"), 1, 50, 1, null)
      ),
      new LayoutConstraint(
        ImmutableSet.<Set<String>>of(
          ImmutableSet.of("slave1", "slave2")
        ),
        ImmutableSet.<Set<String>>of(
          ImmutableSet.of("master1", "slave1"),
          ImmutableSet.of("master1", "slave2")
        )
      )
    );
  }
}
