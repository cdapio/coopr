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
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.common.utils.ImmutablePair;
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

  @Test
  public void testCompatibleWithTemplate() {
    ClusterTemplate template = Entities.ClusterTemplateExample.REACTOR;
    NodeLayout masterNodeLayout = new NodeLayout("large", "centos6", ImmutableSet.of("namenode"));
    NodeLayout slaveLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("datanode"));
    NodeLayout reactorLayout = new NodeLayout("medium", "centos6", ImmutableSet.of("reactor", "zookeeper"));
    Multiset<NodeLayout> counts = HashMultiset.create();
    counts.add(masterNodeLayout);
    counts.add(reactorLayout);
    counts.add(slaveLayout, 50);
    ClusterLayout layout = new ClusterLayout(template.getConstraints(), counts);

    Assert.assertTrue(layout.isCompatibleWithTemplate(template));

    // test if service compatibilities are violated.
    Compatibilities newCompatibilities = new Compatibilities(
      template.getCompatibilities().getHardwaretypes(),
      template.getCompatibilities().getImagetypes(),
      ImmutableSet.<String>of("namenode", "datanode", "zookeeper")
    );
    template = new ClusterTemplate(template.getName(), template.getDescription(), template.getClusterDefaults(),
                                   newCompatibilities, template.getConstraints(), template.getAdministration());
    Assert.assertFalse(layout.isCompatibleWithTemplate(template));

    // test if hardware type compatibilities are violated
    newCompatibilities = new Compatibilities(
      ImmutableSet.<String>of("large"),
      template.getCompatibilities().getImagetypes(),
      template.getCompatibilities().getServices()
    );
    template = new ClusterTemplate(template.getName(), template.getDescription(), template.getClusterDefaults(),
                                   newCompatibilities, template.getConstraints(), template.getAdministration());
    Assert.assertFalse(layout.isCompatibleWithTemplate(template));

    // test if image type compatibilities are violated
    newCompatibilities = new Compatibilities(
      template.getCompatibilities().getHardwaretypes(),
      ImmutableSet.<String>of("ubuntu12"),
      template.getCompatibilities().getServices()
    );
    template = new ClusterTemplate(template.getName(), template.getDescription(), template.getClusterDefaults(),
                                   newCompatibilities, template.getConstraints(), template.getAdministration());
    Assert.assertFalse(layout.isCompatibleWithTemplate(template));
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
