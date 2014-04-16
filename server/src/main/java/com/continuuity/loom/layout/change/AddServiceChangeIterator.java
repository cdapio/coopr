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

import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.layout.ClusterLayout;
import com.continuuity.loom.layout.NodeLayout;
import com.continuuity.loom.layout.NodeLayoutComparator;
import com.continuuity.loom.layout.SlottedCombinationIterator;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Return all the ways in which the given cluster layout can be expanded by adding the given service to one or more
 * nodes in the cluster layout.
 */
public class AddServiceChangeIterator implements Iterator<ClusterLayoutChange> {
  private final String service;
  private final List<NodeLayout> expandableNodeLayouts;
  private Iterator<int[]> nodeLayoutCountIterator;
  private int[] nodeLayoutMaxCounts;
  private int nodesToAddTo;
  private int minNodesToAddTo;

  public AddServiceChangeIterator(ClusterLayout clusterLayout, String service) {
    this.service = service;
    // cluster services are needed in order to prune the constraints to only use ones that pertain to services
    // on the cluster
    Set<String> expandedClusterServices = Sets.newHashSet(service);
    for (NodeLayout nodeLayout : clusterLayout.getLayout().elementSet()) {
      expandedClusterServices.addAll(nodeLayout.getServiceNames());
    }
    // first figure out which node layouts can add this service
    this.expandableNodeLayouts = Lists.newArrayListWithCapacity(clusterLayout.getLayout().elementSet().size());
    Multiset<NodeLayout> expandedCounts = HashMultiset.create();
    for (NodeLayout originalNodeLayout : clusterLayout.getLayout().elementSet()) {
      NodeLayout expandedNodeLayout = NodeLayout.addServiceToNodeLayout(originalNodeLayout, service);
      if (expandedNodeLayout.satisfiesConstraints(clusterLayout.getConstraints(), expandedClusterServices)) {
        expandableNodeLayouts.add(originalNodeLayout);
        expandedCounts.add(originalNodeLayout, clusterLayout.getLayout().count(originalNodeLayout));
      }
    }
    // sort expandable node layouts by preference order
    Collections.sort(this.expandableNodeLayouts, new NodeLayoutComparator(null, null));
    // need to pass this to the slotted iterator so we don't try and add the service to a node layout more times
    // than there are nodes for the node layout.
    this.nodeLayoutMaxCounts = new int[expandableNodeLayouts.size()];
    for (int i = 0; i < nodeLayoutMaxCounts.length; i++) {
      nodeLayoutMaxCounts[i] = expandedCounts.count(expandableNodeLayouts.get(i));
    }
    // figure out the max number of nodes we can add the service to. Start off by saying we can add it to all nodes.
    this.nodesToAddTo = expandedCounts.size();
    // we always need to add the service to at least one node.
    this.minNodesToAddTo = 1;
    ServiceConstraint serviceConstraint = clusterLayout.getConstraints().getServiceConstraints().get(service);
    // if there is a max constraint on this service and its less than the number of nodes in the cluster, start
    // there instead. Similarly, if there is a min constraint on this service higher than 1, use that instead.
    if (serviceConstraint != null) {
      this.nodesToAddTo = Math.min(serviceConstraint.getMaxCount(), this.nodesToAddTo);
      this.minNodesToAddTo = Math.max(serviceConstraint.getMinCount(), this.minNodesToAddTo);
    }
    this.nodeLayoutCountIterator = (this.nodesToAddTo < 1) ? null :
      new SlottedCombinationIterator(expandableNodeLayouts.size(), nodesToAddTo, nodeLayoutMaxCounts);
  }

  @Override
  public boolean hasNext() {
    if (nodeLayoutCountIterator == null) {
      return false;
    }
    // run out of possibilities with this iterator, reduce the number of nodes we're adding the service to
    // and look for more possibilites.
    while (!nodeLayoutCountIterator.hasNext()) {
      nodesToAddTo--;
      if (nodesToAddTo < minNodesToAddTo) {
        return false;
      } else {
        nodeLayoutCountIterator =
          new SlottedCombinationIterator(expandableNodeLayouts.size(), nodesToAddTo, nodeLayoutMaxCounts);
      }
    }
    return true;
  }

  @Override
  public ClusterLayoutChange next() {
    if (hasNext()) {
      int[] nodeLayoutCounts = nodeLayoutCountIterator.next();
      // create the change object from the integer array
      Multiset<NodeLayout> counts = HashMultiset.create();
      for (int i = 0; i < nodeLayoutCounts.length; i++) {
        counts.add(expandableNodeLayouts.get(i), nodeLayoutCounts[i]);
      }
      return new AddServicesChange(counts, service);
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
