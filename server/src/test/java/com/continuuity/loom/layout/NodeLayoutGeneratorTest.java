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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class NodeLayoutGeneratorTest extends BaseSolverTest {

  @Test
  public void testIsValidServiceSet() {
    LayoutConstraint layoutConstraint = reactorTemplate.getConstraints().getLayoutConstraint();
    Set<String> services =
      ImmutableSet.of("namenode", "datanode", "regionserver", "nodemanager", "hbasemaster", "reactor", "zookeeper");
    Assert.assertTrue(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("datanode", "regionserver", "nodemanager"), layoutConstraint, services));
    Assert.assertTrue(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("namenode", "resourcemanager", "hbasemaster"), layoutConstraint, services));
    Assert.assertTrue(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("reactor", "zookeeper"), layoutConstraint, services));
    Assert.assertTrue(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("reactor"), layoutConstraint, services));
    Assert.assertTrue(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("zookeeper"), layoutConstraint, services));

    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("datanode"), layoutConstraint, services));
    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("datanode", "nodemanager"), layoutConstraint, services));
    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("namenode"), layoutConstraint, services));
    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("namenode", "resourcemanager"), layoutConstraint, services));
    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("namenode", "datanode"), layoutConstraint, services));
    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("reactor", "datanode"), layoutConstraint, services));
    Assert.assertFalse(NodeLayoutGenerator.isValidServiceSet(
      ImmutableSet.of("zookeeper", "datanode"), layoutConstraint, services));
  }

  @Test
  public void testIsValidNodeLayout() {
    Map<String, ServiceConstraint> serviceConstraints = reactorTemplate.getConstraints().getServiceConstraints();

    // test all possible valid node layouts
    Set<String> masterServices = ImmutableSet.of("namenode", "resourcemanager", "hbasemaster");
    assertSatisfiesServiceConstraints("large-mem", "centos6", masterServices, serviceConstraints);
    assertSatisfiesServiceConstraints("large-mem", "ubuntu12", masterServices, serviceConstraints);

    Set<String> slaveServices = ImmutableSet.of("datanode", "nodemanager", "regionserver");
    assertSatisfiesServiceConstraints("medium", "centos6", slaveServices, serviceConstraints);
    assertSatisfiesServiceConstraints("medium", "ubuntu12", slaveServices, serviceConstraints);
    assertSatisfiesServiceConstraints("large-cpu", "centos6", slaveServices, serviceConstraints);
    assertSatisfiesServiceConstraints("large-cpu", "ubuntu12", slaveServices, serviceConstraints);

    assertSatisfiesServiceConstraints("small", "centos6", ImmutableSet.of("zookeeper"), serviceConstraints);
    assertSatisfiesServiceConstraints("medium", "centos6", ImmutableSet.of("zookeeper"), serviceConstraints);

    assertSatisfiesServiceConstraints("large", "centos6", ImmutableSet.of("reactor"), serviceConstraints);
    assertSatisfiesServiceConstraints("medium", "centos6", ImmutableSet.of("reactor"), serviceConstraints);
    assertSatisfiesServiceConstraints("large", "ubuntu12", ImmutableSet.of("reactor"), serviceConstraints);
    assertSatisfiesServiceConstraints("medium", "ubuntu12", ImmutableSet.of("reactor"), serviceConstraints);
    // there are no image type constraints for reactor so this should pass
    assertSatisfiesServiceConstraints("medium", "asdf", ImmutableSet.of("reactor"), serviceConstraints);


    assertSatisfiesServiceConstraints("medium", "centos6", ImmutableSet.of("reactor", "zookeeper"), serviceConstraints);

    // test hardware type is invalid
    assertUnsatisfiesServiceConstraints("large", "centos6", masterServices, serviceConstraints);
    // test image type is invalid
    assertUnsatisfiesServiceConstraints("large", "asdf", masterServices, serviceConstraints);
    // test both are invalid
    assertUnsatisfiesServiceConstraints("large-mem", "ubuntu12",
                                        ImmutableSet.of("reactor", "zookeeper"), serviceConstraints);
  }

  @Test
  public void testGetValidServiceSets() {
    Set<Set<String>> expected = ImmutableSet.<Set<String>>of(
      ImmutableSet.of("datanode", "regionserver", "nodemanager"),
      ImmutableSet.of("namenode", "resourcemanager", "hbasemaster"),
      ImmutableSet.of("reactor", "zookeeper"),
      ImmutableSet.of("zookeeper"),
      ImmutableSet.of("reactor"));
    Set<String> services = ImmutableSet.of("datanode", "regionserver", "nodemanager", "namenode",
                                           "resourcemanager", "hbasemaster", "reactor", "zookeeper");
    NodeLayoutGenerator nodeLayoutGenerator =
      new NodeLayoutGenerator(reactorTemplate, services, ImmutableSet.<String>of(), ImmutableSet.<String>of());
    Assert.assertEquals(
      expected, nodeLayoutGenerator.findValidServiceSets(reactorTemplate.getClusterDefaults().getServices()));
  }

  @Test
  public void testGetValidNodeLayouts() {
    Set<NodeLayout> expected = ImmutableSet.of(
      new NodeLayout("large-mem", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("large-mem", "ubuntu12", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("datanode", "nodemanager", "regionserver")),
      new NodeLayout("medium", "ubuntu12", ImmutableSet.of("datanode", "nodemanager", "regionserver")),
      new NodeLayout("large-cpu", "centos6", ImmutableSet.of("datanode", "nodemanager", "regionserver")),
      new NodeLayout("large-cpu", "ubuntu12", ImmutableSet.of("datanode", "nodemanager", "regionserver")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("reactor", "zookeeper")),
      new NodeLayout("small", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("reactor")),
      new NodeLayout("large", "centos6", ImmutableSet.of("reactor")),
      new NodeLayout("medium", "ubuntu12", ImmutableSet.of("reactor")),
      new NodeLayout("large", "ubuntu12", ImmutableSet.of("reactor")),
      new NodeLayout("medium", "sl6", ImmutableSet.of("reactor")),
      new NodeLayout("large", "sl6", ImmutableSet.of("reactor"))
    );
    Set<String> services = ImmutableSet.of("datanode", "regionserver", "nodemanager", "namenode",
                                           "resourcemanager", "hbasemaster", "reactor", "zookeeper");
    NodeLayoutGenerator nodeLayoutGenerator =
      new NodeLayoutGenerator(reactorTemplate, services,
                              ImmutableSet.<String>of("small", "medium", "large", "large-cpu", "large-mem"),
                              ImmutableSet.<String>of("centos6", "ubuntu12", "sl6"));
    Set<Set<String>> validServiceSets =
      nodeLayoutGenerator.findValidServiceSets(reactorTemplate.getClusterDefaults().getServices());
    Assert.assertEquals(expected, nodeLayoutGenerator.findValidNodeLayouts(validServiceSets));
  }

  @Test
  public void testNarrowNodeLayouts() {
    Set<NodeLayout> input = ImmutableSet.of(
      new NodeLayout("medium", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("medium", "sl6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("large", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("large", "ubuntu12", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("large", "sl6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("small", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("medium", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("large", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("huge", "centos6", ImmutableSet.of("zookeeper")),
      new NodeLayout("huge", "ubuntu12", ImmutableSet.of("zookeeper")),
      new NodeLayout("small", "ubuntu12", ImmutableSet.of("zookeeper")),
      new NodeLayout("small", "sl6", ImmutableSet.of("zookeeper"))
    );
    List<NodeLayout> expected = ImmutableList.of(
      new NodeLayout("large", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("huge", "centos6", ImmutableSet.of("zookeeper"))
    );
    Assert.assertEquals(
      expected, NodeLayoutGenerator.narrowNodeLayouts(input, null, null));

    // now try with a different hardware type preference
    expected = ImmutableList.of(
      new NodeLayout("medium", "centos6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("small", "centos6", ImmutableSet.of("zookeeper"))
    );
    Assert.assertEquals(
      expected, NodeLayoutGenerator.narrowNodeLayouts(input, ImmutableList.of("small", "medium"), null));

    // now try with a different image type preference
    expected = ImmutableList.of(
      new NodeLayout("large", "sl6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("small", "sl6", ImmutableSet.of("zookeeper"))
    );
    Assert.assertEquals(
      expected, NodeLayoutGenerator.narrowNodeLayouts(input, null, ImmutableList.of("sl6", "ubuntu12")));

    // now try with both preferences
    expected = ImmutableList.of(
      new NodeLayout("medium", "sl6", ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")),
      new NodeLayout("small", "ubuntu12", ImmutableSet.of("zookeeper"))
    );
    Assert.assertEquals(
      expected, NodeLayoutGenerator.narrowNodeLayouts(input,
                                                      ImmutableList.of("small", "medium", "large", "huge"),
                                                      ImmutableList.of("ubuntu12", "sl6", "centos6")));
  }

  @Test
  public void testGetUnconstrainedService() throws Exception {
    NodeLayoutGenerator nodeLayoutGenerator =
      new NodeLayoutGenerator(reactorTemplate2, reactorTemplate2.getClusterDefaults().getServices(),
                              ImmutableSet.<String>of(), ImmutableSet.<String>of());
    Set<String> unconstrained = nodeLayoutGenerator.findUnconstrainedServices();
    Assert.assertEquals(ImmutableSet.of("hosts", "firewall"), unconstrained);
  }

  @Test
  public void testNodesMustHaveServices() throws Exception {

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
    List<NodeLayout> expected = ImmutableList.of(
      new NodeLayout("small", "centos6", ImmutableSet.of("svc1", "svc2", "svc3"))
    );
    NodeLayoutGenerator nodeLayoutGenerator =
      new NodeLayoutGenerator(template, services, ImmutableSet.<String>of("small"), ImmutableSet.<String>of("centos6"));
    List<NodeLayout> actual = nodeLayoutGenerator.generateNodeLayoutPreferences();
    Assert.assertEquals(expected, actual);
  }

  private void assertSatisfiesServiceConstraints(String hardwareType, String imageType, Set<String> services,
                                           Map<String, ServiceConstraint> serviceConstraints) {
    NodeLayout nodeLayout = new NodeLayout(hardwareType, imageType, services);
    Assert.assertTrue(nodeLayout.satisfiesServiceConstraints(serviceConstraints));
  }

  private void assertUnsatisfiesServiceConstraints(String hardwareType, String imageType, Set<String> services,
                                                   Map<String, ServiceConstraint> serviceConstraints) {
    NodeLayout nodeLayout = new NodeLayout(hardwareType, imageType, services);
    Assert.assertFalse(nodeLayout.satisfiesServiceConstraints(serviceConstraints));
  }
}
