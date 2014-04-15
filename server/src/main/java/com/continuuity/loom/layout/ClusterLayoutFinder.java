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

import java.util.Arrays;
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
  private final int numMachines;
  private final Map<String, ServiceConstraint> serviceConstraints;
  private Map<String, Integer> serviceCounts;

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

    // calculate number of each service across the entire cluster
    serviceCounts = Maps.newHashMap();
    for (String service : services) {
      serviceCounts.put(service, 0);
    }
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
    int[] initialLayout = getInitialNodeCounts();
    if (initialLayout == null) {
      return null;
    }

    SlottedCombinationIterator layoutIter = new SlottedCombinationIterator(initialLayout, getGlobalNodeLayoutMaxes());
    int[] previousLayout = initialLayout;
    // while we haven't looked at all candidate layouts
    while (layoutIter.hasNext()) {
      int[] candidateLayout = layoutIter.next();
      // update service counts for layout validation
      for (int i = 0; i < candidateLayout.length; i++) {
        // update is based on the difference between this layout and the previous one.
        int countDiff = candidateLayout[i] - previousLayout[i];
        updateServiceCounts(i, countDiff);
      }
      // if this is a valid layout, return it
      if (isValidCluster(serviceCounts)) {
        return candidateLayout;
      }
      previousLayout = candidateLayout;
    }
    return null;
  }

  // get the max number of each nodelayout possible, assuming no other node layouts are used.
  private int[] getGlobalNodeLayoutMaxes() {
    int[] layoutsMaxes = new int[nodePreferences.size()];
    for (int i = 0; i < layoutsMaxes.length; i++) {
      NodeLayout layout = nodePreferences.get(i);
      int max = Integer.MAX_VALUE;
      for (String service : layout.getServiceNames()) {
        ServiceConstraint constraint = serviceConstraints.get(service);
        if (constraint != null) {
          int constraintMax = constraint.getMaxCount();
          if (constraintMax < max) {
            max = constraintMax;
          }
        }
      }
      layoutsMaxes[i] = max;
    }
    return layoutsMaxes;
  }

  // initialize the node counts to the first possible cluster layout based on max service counts.
  private int[] getInitialNodeCounts() {
    int[] nodeCounts = new int[nodePreferences.size()];
    // start off with as many of the most preferred node layout as possible
    for (int i = 0; i < nodeCounts.length; i++) {
      int maxNodeCount = getMaxForNodelayout(i, nodeCounts);
      nodeCounts[i] = maxNodeCount;
      if (maxNodeCount > 0) {
        updateServiceCounts(i, maxNodeCount);
      }
    }
    // if the max # of nodes we can place is below the # of nodes in the cluster, there is no possible solution.
    int total = 0;
    for (int nodeCount : nodeCounts) {
      total += nodeCount;
    }
    if (total < numMachines) {
      return null;
    }
    return nodeCounts;
  }

  // get the max number of nodes of the i'th node layout, given the service constraints and how many machines are
  // accounted for already.
  int getMaxForNodelayout(int i, int[] counts) {
    int maxSoFar = numMachines - getTotalCount(counts);
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

  private int getTotalCount(int[] counts) {
    int sum = 0;
    for (int count : counts) {
      sum += count;
    }
    return sum;
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
  private boolean isValidCluster(Map<String, Integer> serviceCounts) {
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

  // for unit testing only
  boolean isValidCluster(int[] nodeCounts) {
    for (String service : serviceCounts.keySet()) {
      serviceCounts.put(service, 0);
    }
    for (int i = 0; i < nodeCounts.length; i++) {
      int nodeCount = nodeCounts[i];
      if (nodeCount > 0) {
        updateServiceCounts(i, nodeCount);
      }
    }
    return isValidCluster(serviceCounts);
  }
}
