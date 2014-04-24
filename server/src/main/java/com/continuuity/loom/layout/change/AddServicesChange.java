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

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.layout.ClusterLayout;
import com.continuuity.loom.layout.NodeLayout;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Adds services to specified number of nodes of given nodelayouts in a cluster.
 */
public class AddServicesChange implements ClusterLayoutChange {
  private final Set<String> services;
  private final Multiset<NodeLayout> countsPerNodeLayout;

  public AddServicesChange(Multiset<NodeLayout> countsPerNodeLayout, String service) {
    this(countsPerNodeLayout, ImmutableSet.of(service));
  }

  public AddServicesChange(Multiset<NodeLayout> countsPerNodeLayout, Set<String> services) {
    this.services = ImmutableSet.copyOf(services);
    this.countsPerNodeLayout = HashMultiset.create(countsPerNodeLayout);
  }

  @Override
  public boolean canApplyChange(ClusterLayout layout) {
    // check that all node layouts we need to add the service to exist in the given cluster layout
    if (layout == null || !layout.getLayout().containsAll(countsPerNodeLayout.elementSet())) {
      return false;
    }
    // check that if we need to add the service to 5 nodes with node layout XYZ, that there are at least 5 nodes in the
    // cluster with node layout XYZ.
    for (Multiset.Entry<NodeLayout> entry : countsPerNodeLayout.entrySet()) {
      if (layout.getLayout().count(entry.getElement()) < entry.getCount()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public ClusterLayout applyChange(ClusterLayout originalLayout) {
    Multiset<NodeLayout> newLayout = HashMultiset.create(originalLayout.getLayout());
    for (Multiset.Entry<NodeLayout> entry : countsPerNodeLayout.entrySet()) {
      NodeLayout originalNodeLayout = entry.getElement();
      NodeLayout expandedNodeLayout = NodeLayout.addServicesToNodeLayout(originalNodeLayout, services);
      // add the service count times
      newLayout.add(expandedNodeLayout, entry.getCount());
      // subtract count nodes from the original node layout since that many have now been expanded.
      newLayout.setCount(originalNodeLayout, originalLayout.getLayout().count(originalNodeLayout) - entry.getCount());
    }
    return new ClusterLayout(originalLayout.getConstraints(), newLayout);
  }

  @Override
  public Set<Node> applyChange(Cluster cluster, Set<Node> clusterNodes, Map<String, Service> serviceMap) {
    Set<Node> changedNodes = Sets.newHashSet();
    Multiset<NodeLayout> countsToAdd = HashMultiset.create(countsPerNodeLayout);
    for (Node node : clusterNodes) {
      NodeLayout nodeLayout = NodeLayout.fromNode(node);
      if (countsToAdd.contains(nodeLayout)) {
        for (String service : services) {
          node.addService(serviceMap.get(service));
        }
        countsToAdd.setCount(nodeLayout, countsToAdd.count(nodeLayout) - 1);
        changedNodes.add(node);
      }
    }
    cluster.setServices(Sets.union(cluster.getServices(), services));
    return changedNodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AddServicesChange)) {
      return false;
    }

    AddServicesChange that = (AddServicesChange) o;

    return Objects.equal(countsPerNodeLayout, that.countsPerNodeLayout) &&
      Objects.equal(services, that.services);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(services, countsPerNodeLayout);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("services", services)
      .add("countsPerNodeLayout", countsPerNodeLayout)
      .toString();
  }
}
