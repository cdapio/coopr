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

import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.ServiceConstraint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class ClusterLayoutFinderTest extends BaseSolverTest {
  @Test
  public void testIsValidCluster() {
    List<NodeLayout> nodeLayouts = ImmutableList.of(
      new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("large-cpu", "centos6", ImmutableSet.of("datanode", "nodemanager", "regionserver")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("reactor", "zookeeper")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("large", "centos6", ImmutableSet.of("reactor"))
    );

    // test clusters that are ok.
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 1, 0, 0}, 3, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 1, 0, 0}, 3, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 0, 1, 1}, 4, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 3, 1, 0, 0}, 5, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 2, 0, 1, 1}, 5, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 0, 1, 2}, 5, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 5, 1, 0, 0}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 4, 0, 1, 1}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 3, 0, 1, 2}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 2, 0, 1, 3}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 0, 1, 4}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 3, 3, 0, 0}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 5, 0, 0}, 7, true);
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 50, 3, 0, 0}, 54, true);

    // try some test clusters that are not ok.
    // violates minimum reactor count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 0, 1, 0}, 3, false);
    // violates minimum zookeeper count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 1, 0, 0, 1}, 3, false);
    // violates minimum namenode count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{0, 1, 0, 1, 1}, 3, false);
    // violates minimum datanode count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 0, 1, 0, 1}, 3, false);
    // violates maximum namenode count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{2, 1, 1, 0, 0}, 4, false);
    // violates maximum zookeeper count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 4, 10, 0, 0}, 15, false);
    // violates maximum datanode count
    assertClusterLayout(nodeLayouts, reactorTemplate, new int[]{1, 52, 1, 0, 0}, 54, false);
  }

  @Test
  public void testGetClusterNodes() {
    List<NodeLayout> nodePreferences = ImmutableList.of(
      new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("large-cpu", "centos6", ImmutableSet.of("datanode", "nodemanager", "regionserver")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("reactor", "zookeeper")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("large", "centos6", ImmutableSet.of("reactor"))
    );
    Set<String> services = ImmutableSet.of("namenode", "resourcemanager", "hbasemaster",
                                           "datanode", "nodemanager", "regionserver", "reactor", "zookeeper");

    ClusterLayoutFinder finder = new ClusterLayoutFinder(nodePreferences, reactorTemplate, services, 5);
    Assert.assertTrue(Arrays.equals(new int[]{1, 3, 1, 0, 0}, finder.findValidNodeCounts()));

    finder = new ClusterLayoutFinder(nodePreferences, reactorTemplate, services, 10);
    Assert.assertTrue(Arrays.equals(new int[]{1, 8, 1, 0, 0}, finder.findValidNodeCounts()));

    finder = new ClusterLayoutFinder(nodePreferences, reactorTemplate, services, 54);
    Assert.assertTrue(Arrays.equals(new int[]{1, 50, 3, 0, 0}, finder.findValidNodeCounts()));
  }

  @Test
  public void testNoSolutionReturnsNull() {
    Set<String> services = ImmutableSet.of("svc1", "svc2", "svc3");
    ClusterTemplate template = new ClusterTemplate(
      "simple", "all services on all nodes template",
      new ClusterDefaults(services, "joyent", null, null, null, new JsonObject()),
      new Compatibilities(null, null, services),
      new Constraints(
        ImmutableMap.<String, ServiceConstraint>of("svc1", new ServiceConstraint(null, null, 1, 1, 1, null)),
        new LayoutConstraint(
          ImmutableSet.<Set<String>>of(ImmutableSet.of("svc1", "svc2", "svc3")),
          ImmutableSet.<Set<String>>of()
        )
      ),
      Administration.EMPTY_ADMINISTRATION
    );
    List<NodeLayout> nodePreferences = ImmutableList.of(
      new NodeLayout("small", "centos6", ImmutableSet.of("svc1", "svc2", "svc3"))
    );
    ClusterLayoutFinder finder = new ClusterLayoutFinder(nodePreferences, template, services, 2);
    Assert.assertNull(finder.findValidNodeCounts());
  }

  private void assertClusterLayout(List<NodeLayout> nodeLayouts, ClusterTemplate template,
                                   int[] nodeCounts, int numMachines, boolean expected) {
    ClusterLayoutFinder clusterLayoutFinder =
      new ClusterLayoutFinder(nodeLayouts, template, template.getClusterDefaults().getServices(), numMachines);
    Assert.assertEquals(expected, clusterLayoutFinder.isValidCluster(nodeCounts));
  }
}
