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

import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.ServiceConstraint;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

import java.util.Map;
import java.util.Set;

/**
 * Class describing the layout of a cluster, giving a mapping of {@link NodeLayout} to how many nodes use that layout.
 */
public class ClusterLayout {
  private final Constraints constraints;
  private final Multiset<NodeLayout> layout;
  private final Multiset<String> serviceCounts;

  public ClusterLayout(Constraints constraints, Multiset<NodeLayout> layout) {
    this.constraints = constraints;
    this.layout = ImmutableMultiset.copyOf(layout);
    this.serviceCounts = HashMultiset.create();
    for (Multiset.Entry<NodeLayout> entry : layout.entrySet()) {
      for (String service : entry.getElement().getServiceNames()) {
        serviceCounts.add(service, entry.getCount());
      }
    }
  }

  /**
   * Returns the cluster layout, which maps each node layout to how many nodes have that layout.
   *
   * @return Layout of the cluster.
   */
  public Multiset<NodeLayout> getLayout() {
    return layout;
  }

  public Constraints getConstraints() {
    return constraints;
  }

  /**
   * Create a new cluster layout that is derived by adding a service to the node layout given in the input.
   *
   * @param service Service to add to node layouts.
   * @param countsPerNodeLayout Number of nodes of the node layout to add the service to.
   * @return A new cluster layout derived from the current one.
   */
  public static ClusterLayout expandClusterLayout(ClusterLayout originalLayout, String service,
                                                  Multiset<NodeLayout> countsPerNodeLayout) {
    Preconditions.checkArgument(originalLayout.layout.containsAll(countsPerNodeLayout.elementSet()),
                                "Cannot add service to non-existent node layout.");
    for (Multiset.Entry<NodeLayout> entry : countsPerNodeLayout.entrySet()) {
      Preconditions.checkArgument(originalLayout.layout.count(entry.getElement()) >= entry.getCount(),
                                  "cannot add the service to more nodes than exist of the given node layout.");
    }

    Multiset<NodeLayout> newLayout = HashMultiset.create(originalLayout.layout);
    for (Multiset.Entry<NodeLayout> entry : countsPerNodeLayout.entrySet()) {
      NodeLayout originalNodeLayout = entry.getElement();
      NodeLayout expandedNodeLayout = NodeLayout.addServiceToNodeLayout(originalNodeLayout, service);
      // add the service count times
      newLayout.add(expandedNodeLayout, entry.getCount());
      // subtract count nodes from the original node layout since that many have now been expanded.
      newLayout.setCount(originalNodeLayout, originalLayout.layout.count(originalNodeLayout) - entry.getCount());
    }
    return new ClusterLayout(originalLayout.constraints, newLayout);
  }

  /**
   * Returns whether or not the cluster layout is valid based on the constraints it has.
   *
   * @return True if the cluster layout is valid, false if not.
   */
  public boolean isValid() {
    // check node layouts
    Set<String> clusterServices = serviceCounts.elementSet();
    for (NodeLayout nodeLayout : layout.elementSet()) {
      if (!nodeLayout.satisfiesConstraints(constraints, clusterServices)) {
        return false;
      }
    }

    // check service counts
    Map<String, ServiceConstraint> serviceConstraints = constraints.getServiceConstraints();
    for (Multiset.Entry<String> entry : serviceCounts.entrySet()) {
      ServiceConstraint constraint = serviceConstraints.get(entry.getElement());
      if (constraint != null) {
        int serviceCount = entry.getCount();
        // TODO: ratio constraint
        if (serviceCount < constraint.getMinCount() || serviceCount > constraint.getMaxCount()
          || serviceCount > layout.size()) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClusterLayout)) {
      return false;
    }

    ClusterLayout that = (ClusterLayout) o;

    return Objects.equal(constraints, that.constraints) &&
      Objects.equal(layout, that.layout) &&
      Objects.equal(serviceCounts, that.serviceCounts);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(constraints, layout, serviceCounts);
  }
}
