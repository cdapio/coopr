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

import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.ServiceConstraint;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that takes in an ordered list of {@link NodeLayout}s by preference, {@link ClusterTemplate}, number of
 * machines, and a set of services to place on the cluster, and figures out how many of each type of node should
 * be used to satisfy constraints in the template.
 */
public class ClusterLayoutFinder {
  private final List<NodeLayout> nodePreferences;
  private final int[] nodeCounts;
  private final int numMachines;
  private Map<String, Integer> serviceCounts;
  private final Map<String, ServiceConstraint> serviceConstraints;
  private boolean doneSearching;

  public ClusterLayoutFinder(List<NodeLayout> nodePreferences, ClusterTemplate template, Set<String> services,
                             int numMachines) {
    this.nodePreferences = nodePreferences;
    this.numMachines = numMachines;
    serviceConstraints = Maps.newHashMap();

    // we only care about the constraints that apply to services that are on the cluster
    Map<String, ServiceConstraint> allServiceConstraints = template.getConstraints().getServiceConstraints();
    for (String service : services) {
      if (allServiceConstraints.containsKey(service)) {
        serviceConstraints.put(service, allServiceConstraints.get(service));
      }
    }

    // initialize data structures
    nodeCounts = new int[nodePreferences.size()];

    // calculate number of each service across the entire cluster
    serviceCounts = Maps.newHashMap();
    for (String service : services) {
      serviceCounts.put(service, 0);
    }
    initializeNodeCounts();
  }

  /**
   * Get how many of each node type to use in the cluster, or null if there is no possible cluster layout that
   * satisfies all cluster constraints. The cluster layout is returned as an array integer, with the i'th value in the
   * array being the number of nodes to use of the i'th node layout. For example, returning {5, 3, 0, 0, 1} means
   * there should be 5 of the first node layout, 3 of the second, and 1 of the fifth.
   *
   * @return Array containing how many of each node type to use.
   */
  public int[] findValidNodeCounts() {
    while (!doneSearching) {
      if (isValidCluster()) {
        return nodeCounts;
      }
      advanceToNextClusterLayout();
    }
    return null;
  }

  // for unit testing only
  void setNodeCount(int[] nodeCounts) {
    for (String service : serviceCounts.keySet()) {
      serviceCounts.put(service, 0);
    }
    for (int i = 0; i < nodeCounts.length; i++) {
      int nodeCount = nodeCounts[i];
      this.nodeCounts[i] = nodeCount;
      if (nodeCount > 0) {
        updateServiceCounts(i, nodeCount);
      }
    }
  }

  // initialize the node counts to the first possible cluster layout based on max service counts.
  void initializeNodeCounts() {
    doneSearching = false;
    // start off with as many of the most preferred node layout as possible
    for (int i = 0; i < nodeCounts.length; i++) {
      int maxNodeCount = getMaxForNodelayout(i);
      nodeCounts[i] = maxNodeCount;
      if (maxNodeCount > 0) {
        updateServiceCounts(i, maxNodeCount);
      }
    }
    // if the max # of nodes we can place is below the # of nodes in the cluster, there is no possible solution.
    if (getTotalCount() < numMachines) {
      doneSearching = true;
    }
  }

  // get the max number of nodes of the i'th node layout, given the service constraints and how many machines are
  // accounted for already.
  int getMaxForNodelayout(int i) {
    int maxSoFar = numMachines - getTotalCount();
    if (maxSoFar == 0) {
      return 0;
    }
    NodeLayout nodeLayout = nodePreferences.get(i);
    for (String service : nodeLayout.getServiceNames()) {
      ServiceConstraint constraint = serviceConstraints.get(service);
      if (constraint != null) {
        int serviceCount = serviceCounts.get(service);
        int serviceMax = constraint.getMaxCount() - serviceCount;
        if (serviceMax < maxSoFar) {
          maxSoFar = serviceMax;
        }
      }
    }
    return maxSoFar;
  }

  // get total number of nodes accounted for.
  int getTotalCount() {
    int sum = 0;
    for (int i = 0; i < nodeCounts.length; i++) {
      sum += nodeCounts[i];
    }
    return sum;
  }

  /**
   * start at the rightmost layout from the traversal order, moving left until you find something
   * non-zero that you can move one slot to the right.  For example:
   * 3, 0, 0, 0
   * 2, 1, 0, 0
   * 2, 0, 1, 0
   * 2, 0, 0, 1
   * 1, 2, 0, 0
   * 1, 1, 1, 0
   * 1, 1, 0, 1
   * 1, 0, 2, 0
   * 1, 0, 1, 1
   * 1, 0, 0, 2
   * 0, 3, 0, 0
   * 0, 2, 1, 0
   * 0, 2, 0, 1
   * 0, 1, 2, 0
   * ...
   * // TODO: perform search in a smarter manner based on min, max service counts
   */
  private void advanceToNextClusterLayout() {
    int end = nodeCounts.length - 1;
    for (int i = end - 1; i >= 0; i--) {
      // we've found the first non-zero
      if (nodeCounts[i] > 0) {
        // this part takes 1 from the non-zero and moves it to the right one slot
        nodeCounts[i]--;
        updateServiceCounts(i, -1);

        // add clusterlayout[end] because we want as many in slot i+1 as possible
        // ex: going from 2, 0, 0, 1 -> 1, 2, 0, 0
        int inc = 1 + nodeCounts[end] - nodeCounts[i + 1];
        nodeCounts[i + 1] += inc;
        updateServiceCounts(i + 1, inc);

        // need to zero out clusterlayout[end] if we've added it to slot i+1
        if (i < end - 1) {
          updateServiceCounts(end, 0 - nodeCounts[end]);
          nodeCounts[end] = 0;
        }
        return;
      }
    }
    doneSearching = true;
  }

  // update service counts from changing nodePreferences[nodeNum] by nodesChanged
  private void updateServiceCounts(int nodeNum, int nodesChanged) {
    for (String service : nodePreferences.get(nodeNum).getServiceNames()) {
      serviceCounts.put(service, serviceCounts.get(service) + nodesChanged);
    }
  }

  // given a list of node layouts, a cluster template, a number of machines, and the number of each nodelayout to use,
  // determine whether it is a valid cluster based on the size constraints for each service as
  // defined in the cluster template.  If clusterlayout[x] = y, this means the x'th node layout in nodeLayouts has
  // y nodes in the cluster.  For example, if clusterlayout[0] = 5, this means there are 5 nodes with nodelayout of
  // nodelayouts.get(0) in the cluster.
  boolean isValidCluster() {
    for (Map.Entry<String, ServiceConstraint> entry : serviceConstraints.entrySet()) {
      String service = entry.getKey();
      ServiceConstraint constraint = entry.getValue();
      int serviceCount = serviceCounts.get(service);
      // TODO: ratio constraint
      if (serviceCount < constraint.getMinCount() || serviceCount > constraint.getMaxCount()
        || serviceCount > numMachines) {
        return false;
      }
    }

    return true;
  }
}
