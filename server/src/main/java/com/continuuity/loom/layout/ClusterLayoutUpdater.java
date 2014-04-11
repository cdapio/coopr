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
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.store.ClusterStore;
import com.continuuity.loom.store.EntityStore;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class that takes in an existing cluster and a request to update the cluster in some way, whether its by adding
 * services, removing services, adding nodes, or removing nodes.
 */
public class ClusterLayoutUpdater {
  private final ClusterStore clusterStore;
  private final EntityStore entityStore;

  @Inject
  ClusterLayoutUpdater(ClusterStore clusterStore, EntityStore entityStore) {
    this.clusterStore = clusterStore;
    this.entityStore = entityStore;
  }

  public ClusterLayout addServicesToCluster(String clusterId, Set<String> servicesToAdd) throws Exception {
    Cluster cluster = clusterStore.getCluster(clusterId);
    if (cluster == null) {
      throw new IllegalArgumentException("cluster " + clusterId + " does not exist.");
    }
    Set<Node> clusterNodes = clusterStore.getClusterNodes(clusterId);
    if (clusterNodes == null || clusterNodes.isEmpty()) {
      throw new IllegalArgumentException("cluster " + clusterId + " has no nodes.");
    }
    validateServicesToAdd(cluster, servicesToAdd);

    Multiset<NodeLayout> nodeLayoutCounts = HashMultiset.create();
    for (Node node : clusterNodes) {
      Set<String> nodeServices = Sets.newHashSet();
      for (Service service : node.getServices()) {
        nodeServices.add(service.getName());
      }
      // TODO: node really should be refactored so these are proper fields
      String hardwareType = node.getProperties().get(Node.Properties.HARDWARETYPE.name().toLowerCase()).getAsString();
      String imageType = node.getProperties().get(Node.Properties.IMAGETYPE.name().toLowerCase()).getAsString();
      nodeLayoutCounts.add(new NodeLayout(hardwareType, imageType, nodeServices));
    }
    Constraints clusterConstraints = cluster.getClusterTemplate().getConstraints();
    ClusterLayout clusterLayout = new ClusterLayout(clusterConstraints, nodeLayoutCounts);

    // heuristic: try and add services in order of lowest max count allowed.
    Set<String> servicesToAddCopy = Sets.newHashSet(servicesToAdd);
    SortedSet<Map.Entry<String, ServiceConstraint>> sortedConstraints = Sets.newTreeSet(new ServiceMaxComparator());
    sortedConstraints.addAll(clusterConstraints.getServiceConstraints().entrySet());
    Queue<String> sortedServices = Lists.newLinkedList();
    for (Map.Entry<String, ServiceConstraint> entry : sortedConstraints) {
      if (servicesToAddCopy.contains(entry.getKey())) {
        sortedServices.add(entry.getKey());
        servicesToAddCopy.remove(entry.getKey());
      }
    }
    // any service without a constraint has no limit on the number of nodes it can be placed on, so add them to the end
    sortedServices.addAll(servicesToAddCopy);

    return addServicesToCluster(clusterLayout, sortedServices);
  }

  private void validateServicesToAdd(Cluster cluster, Set<String> servicesToAdd) throws Exception {
    Preconditions.checkArgument(servicesToAdd != null && !servicesToAdd.isEmpty(),
                                "At least one service to add must be specified.");

    // check compatibility
    Set<String> compatibleServices = cluster.getClusterTemplate().getCompatibilities().getServices();
    Set<String> incompatibleServices = Sets.difference(servicesToAdd, compatibleServices);
    if (!incompatibleServices.isEmpty()) {
      String incompatibleStr = Joiner.on(',').join(incompatibleServices).toString();
      throw new IllegalArgumentException(incompatibleStr + " are incompatible with the cluster");
    }

    // check dependencies
    boolean dependenciesSatisfied = true;
    StringBuilder errMsg = new StringBuilder();
    Set<String> existingClusterServices = cluster.getServices();
    for (String serviceName : servicesToAdd) {
      Service service = entityStore.getService(serviceName);
      if (service == null) {
        throw new IllegalArgumentException(serviceName + " does not exist");
      }
      for (String serviceDependency : service.getDependsOn()) {
        if (!existingClusterServices.contains(serviceDependency) && !servicesToAdd.contains(serviceDependency)) {
          dependenciesSatisfied = false;
          errMsg.append(serviceName);
          errMsg.append(" requires ");
          errMsg.append(serviceDependency);
          errMsg.append(", which is not on the cluster or in the list of services to add.");
        }
      }
    }
    if (!dependenciesSatisfied) {
      throw new IllegalArgumentException(errMsg.toString());
    }
  }

  private ClusterLayout addServicesToCluster(ClusterLayout clusterLayout, Queue<String> servicesToAdd) {
    ClusterLayout result;
    if (!servicesToAdd.isEmpty()) {
      String service = servicesToAdd.remove();
      // find valid moves, where a move is adding some number of the first service in the queue to nodes in the cluster
      Iterator<Multiset<NodeLayout>> moves = new ClusterLayoutExpanderIterator(clusterLayout, service);

      while (moves.hasNext()) {
        // expand the cluster
        ClusterLayout expandedClusterLayout = ClusterLayout.expandClusterLayout(clusterLayout, service, moves.next());
        if (expandedClusterLayout.isValid()) {
          result = addServicesToCluster(expandedClusterLayout, Lists.newLinkedList(servicesToAdd));
          if (result != null) {
            return result;
          }
        }
      }
    } else if (clusterLayout.isValid()) {
      return clusterLayout;
    }
    return null;
  }

  /**
   * Return all the ways in which the given cluster layout can be expanded by adding the given service to one or more
   * nodes in the cluster layout.
   */
  private class ClusterLayoutExpanderIterator implements Iterator<Multiset<NodeLayout>> {
    private final List<NodeLayout> expandableNodeLayouts;
    private Iterator<int[]> nodeLayoutCountIterator;
    private int[] nodeLayoutMaxCounts;
    private int nodesToAddTo;
    private int minNodesToAddTo;

    private ClusterLayoutExpanderIterator(ClusterLayout clusterLayout, String service) {
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
    public Multiset<NodeLayout> next() {
      if (hasNext()) {
        int[] nodeLayoutCounts = nodeLayoutCountIterator.next();
        Multiset<NodeLayout> counts = HashMultiset.create();
        for (int i = 0; i < nodeLayoutCounts.length; i++) {
          counts.add(expandableNodeLayouts.get(i), nodeLayoutCounts[i]);
        }
        return counts;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Comparator to sort services constraints so that services are sorted first by their max count (lower max count means
   * the constraint is lower), sorted next by their min count (higher min count means the constraint is lower), and
   * finally by the service name if all else is equal.
   */
  private class ServiceMaxComparator implements Comparator<Map.Entry<String, ServiceConstraint>> {

    @Override
    public int compare(Map.Entry<String, ServiceConstraint> entry1, Map.Entry<String, ServiceConstraint> entry2) {
      ServiceConstraint constraint1 = entry1.getValue();
      ServiceConstraint constraint2 = entry2.getValue();
      if (constraint1 == null && constraint2 != null) {
        return 1;
      } else if (constraint1 != null && constraint2 == null) {
        return -1;
      } else if (constraint1 != null && constraint2 != null) {
        int compare = ((Integer) constraint1.getMaxCount()).compareTo(constraint2.getMaxCount());
        if (compare != 0) {
          return compare;
        }
        compare = 0 - ((Integer) constraint1.getMinCount()).compareTo(constraint2.getMinCount());
        if (compare != 0) {
          return compare;
        }
      }
      return entry1.getKey().compareTo(entry2.getKey());
    }
  }
}
