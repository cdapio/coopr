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
import com.google.common.collect.Multiset;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

/**
 *
 */
public class ClusterLayoutTrackerTest {
  private static Constraints constraints;

  @Test
  public void testAddIfValidDoesNotAddInvalid() {
    NodeLayout masterNodeLayout = new NodeLayout("large", "centos6", ImmutableSet.of("master1"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    ClusterLayout layout = new ClusterLayout(constraints, counts);
    ClusterLayoutTracker tracker = new ClusterLayoutTracker(layout);

    counts = HashMultiset.create();
    counts.add(new NodeLayout("medium", "centos6", ImmutableSet.of("master1")));
    Assert.assertFalse(tracker.addChangeIfValid(new AddServicesChange(counts, "slave1")));
  }

  @Test
  public void testAddAndRemove() {
    NodeLayout masterNodeLayout = new NodeLayout("large", "centos6", ImmutableSet.of("master1"));
    NodeLayout slaveNodeLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("slave1"));
    NodeLayout expandedSlaveNodeLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("slave1", "slave2"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(slaveNodeLayout, 5);
    ClusterLayout originalLayout = new ClusterLayout(constraints, counts);
    ClusterLayoutTracker tracker = new ClusterLayoutTracker(originalLayout);

    // add slave2 to 3 slave nodes
    counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(expandedSlaveNodeLayout, 3);
    counts.add(slaveNodeLayout, 2);
    ClusterLayout expectedMiddleState = new ClusterLayout(constraints, counts);
    counts = HashMultiset.create();
    counts.add(slaveNodeLayout, 3);
    Assert.assertTrue(tracker.addChangeIfValid(new AddServicesChange(counts, "slave2")));
    Assert.assertEquals(expectedMiddleState, tracker.getCurrentLayout());

    // add slave2 to rest of slave nodes
    counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(expandedSlaveNodeLayout, 5);
    ClusterLayout expectedEndState = new ClusterLayout(constraints, counts);
    counts = HashMultiset.create();
    counts.add(slaveNodeLayout, 2);
    Assert.assertTrue(tracker.addChangeIfValid(new AddServicesChange(counts, "slave2")));
    Assert.assertEquals(expectedEndState, tracker.getCurrentLayout());

    // remove last change
    tracker.removeLastChange();
    Assert.assertEquals(expectedMiddleState, tracker.getCurrentLayout());

    // remove last change
    tracker.removeLastChange();
    Assert.assertEquals(originalLayout, tracker.getCurrentLayout());
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
