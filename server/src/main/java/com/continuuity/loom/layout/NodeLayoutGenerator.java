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
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.ServiceConstraint;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates an ordered list of {@link NodeLayout}s sorted by preference given a {@link ClusterTemplate},
 * set of names of services that should be placed on the cluster,
 * set of allowed {@link com.continuuity.loom.admin.HardwareType}s, and set of allowed
 * {@link com.continuuity.loom.admin.ImageType}s.
 */
public class NodeLayoutGenerator {
  private static final Logger LOG  = LoggerFactory.getLogger(NodeLayoutGenerator.class);
  private final ClusterTemplate clusterTemplate;
  private final Set<String> clusterServices;
  private final Set<String> allowedHardwareTypes;
  private final Set<String> allowedImageTypes;

  public NodeLayoutGenerator(ClusterTemplate clusterTemplate, Set<String> clusterServices,
                             Set<String> allowedHardwareTypes, Set<String> allowedImageTypes) {
    this.clusterTemplate = clusterTemplate;
    this.clusterServices = clusterServices;
    this.allowedHardwareTypes = allowedHardwareTypes;
    this.allowedImageTypes = allowedImageTypes;
  }

  /**
   * Returns whether or not the service set based on the given layout constraint.
   *
   * @param serviceSet Service set to validate.
   * @param layoutConstraint Layout constraint to use for validation.
   * @param clusterServices Services on the cluster, used to prune the layout constraint.
   * @return True if the service set is valid, false if not.
   */
  static boolean isValidServiceSet(Set<String> serviceSet, LayoutConstraint layoutConstraint,
                                   Set<String> clusterServices) {
    // a valid service set must not be a superset of any of the cant coexist constraints
    for (Set<String> cantCoexist : layoutConstraint.getServicesThatMustNotCoexist()) {
      if (serviceSet.containsAll(cantCoexist)) {
        return false;
      }
    }

    // if the service set contains at least one service in a must coexist constraint, but not all clusterServices in the
    // constraint, then it is invalid.  Ignore clusterServices that are not on the cluster.
    for (Set<String> mustCoexist : layoutConstraint.getServicesThatMustCoexist()) {
      Set<String> trueMustCoexist = Sets.intersection(mustCoexist, clusterServices);
      if (containsOne(serviceSet, trueMustCoexist) && !serviceSet.containsAll(trueMustCoexist)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get an ordered list of possible {@link NodeLayout}s to use in the cluster. If earlier a node layout appears in the
   * list, the more preferred it is. Only one {@link NodeLayout} per valid service set will be returned.
   *
   * @return List of node layouts that can be used in the cluster, ordered by preference.
   */
  public List<NodeLayout> generateNodeLayoutPreferences() {
    long start = System.nanoTime();
    // heuristic: if we need to place some services on the cluster, and these services have
    // no constraints, place them everywhere on the cluster.  This can significantly shrink the search space.
    Set<String> unconstrainedServices = findUnconstrainedServices();

    // get valid service sets of clusterServices that have constraints
    Set<String> constrainedServices = Sets.difference(clusterServices, unconstrainedServices);
    Set<Set<String>> validServiceSets = findValidServiceSets(Sets.newHashSet(constrainedServices));

    // add unconstrained clusterServices to each valid service set.
    if (!unconstrainedServices.isEmpty()) {
      for (Set<String> validServiceSet : validServiceSets) {
        validServiceSet.addAll(unconstrainedServices);
      }
      validServiceSets.add(unconstrainedServices);
    }
    long dur = (System.nanoTime() - start) / 1000000;
    LOG.debug("took {} ms to find {} valid service sets", dur, validServiceSets.size());

    start = System.nanoTime();
    Set<NodeLayout> validNodeLayouts = findValidNodeLayouts(validServiceSets);
    dur = (System.nanoTime() - start) / 1000000;
    LOG.debug("took {} ms to find {} valid node layouts", dur, validNodeLayouts.size());

    // We need to deterministically choose the same cluster.  Nodelayouts earlier in the traversal order are
    // preferred.
    start = System.nanoTime();
    List<NodeLayout> traversalOrder = narrowNodeLayouts(validNodeLayouts, null, null);
    dur = (System.nanoTime() - start) / 1000000;
    LOG.debug("took {} ms to narrow to {} valid node layouts", dur, traversalOrder.size());

    return traversalOrder;
  }

  Set<String> findUnconstrainedServices() {
    Set<String> unconstrained = Sets.newHashSet();

    Map<String, ServiceConstraint> serviceConstraints = clusterTemplate.getConstraints().getServiceConstraints();
    LayoutConstraint layoutConstraint = clusterTemplate.getConstraints().getLayoutConstraint();
    Set<Set<String>> mustCoexistServices = layoutConstraint.getServicesThatMustCoexist();
    Set<Set<String>> mustNotCoexistServices = layoutConstraint.getServicesThatMustNotCoexist();

    for (String service : clusterServices) {
      if (!serviceConstraints.containsKey(service) && !inSetOfSets(service, mustCoexistServices)
        && !inSetOfSets(service, mustNotCoexistServices)) {
        unconstrained.add(service);
      }
    }
    return unconstrained;
  }

  // search through all possible service combinations, keeping track of valid service combinations.
  Set<Set<String>> findValidServiceSets(Set<String> services) {
    Set<Set<String>> validServiceSets = Sets.newHashSet();
    if (!services.isEmpty()) {
      LayoutConstraint layoutConstraint = clusterTemplate.getConstraints().getLayoutConstraint();
      Queue<Set<String>> nodes = Lists.newLinkedList();

      // starting with the full set of allowed clusterServices, systematically remove clusterServices
      // until we go through all combinations.
      nodes.add(services);
      while (!nodes.isEmpty()) {
        Set<String> serviceSet = nodes.remove();
        if (isValidServiceSet(serviceSet, layoutConstraint, clusterServices)) {
          validServiceSets.add(serviceSet);
        }
        if (serviceSet.size() > 1) {
          nodes.addAll(getOneSmallerSubsets(serviceSet));
        }
      }
    }

    return validServiceSets;
  }

  // given a set of valid service sets, a collection of available hardware types, and a collection of available
  // image types, find the set of all node layouts that are valid given the constraints in the cluster template.
  // a node layout is a service set, hardware type, and image type.
  Set<NodeLayout> findValidNodeLayouts(Set<Set<String>> validServiceSets) {
    Set<NodeLayout> validNodeLayouts = Sets.newHashSet();
    Map<String, ServiceConstraint> serviceConstraints = clusterTemplate.getConstraints().getServiceConstraints();

    for (String hardwareType : allowedHardwareTypes) {
      for (String imageType : allowedImageTypes) {
        for (Set<String> serviceSet : validServiceSets) {
          NodeLayout nodeLayout = new NodeLayout(hardwareType, imageType, serviceSet);
          if (nodeLayout.satisfiesServiceConstraints(serviceConstraints)) {
            validNodeLayouts.add(nodeLayout);
          }
        }
      }
    }

    return validNodeLayouts;
  }

  // if there are multiple node layouts with the same service set, choose just one of them to use to make future steps
  // faster.  For example, if we have ({s1, s2, s3}, hw1, img1), ({s1, s2, s3}, hw1, img2), ({s1, s2, s3}, hw2, img1),
  // then just pick one of them to use.  If hardware preferences and image preferences are non-null, the order of
  // hardware types and image types in the given lists will be used to pick the canonical node layout.  Entities
  // earlier in the list are preferred over entities later in the list, with hardware types being more important
  // than image types (in other words, image types are used to break ties when hardware types are the same).
  // Returns a sorted list of nodelayouts.
  static List<NodeLayout> narrowNodeLayouts(Set<NodeLayout> nodeLayouts,
                                            List<String> hardwarePreferences, List<String> imagePreferences) {
    Comparator<NodeLayout> comparator = new NodeLayoutComparator(hardwarePreferences, imagePreferences);
    Map<Set<String>, TreeSet<NodeLayout>> layoutsByServiceSet = Maps.newHashMap();
    for (NodeLayout layout : nodeLayouts) {
      Set<String> serviceSet = layout.getServiceNames();
      if (!layoutsByServiceSet.containsKey(serviceSet)) {
        layoutsByServiceSet.put(serviceSet, Sets.<NodeLayout>newTreeSet(comparator));
      }
      layoutsByServiceSet.get(serviceSet).add(layout);
    }

    List<NodeLayout> output = Lists.newArrayListWithCapacity(nodeLayouts.size());
    for (Set<String> serviceSet : layoutsByServiceSet.keySet()) {
      output.add(layoutsByServiceSet.get(serviceSet).iterator().next());
    }

    Collections.sort(output, comparator);
    return output;
  }

  static boolean inSetOfSets(String element, Set<Set<String>> setOfSets) {
    for (Set<String> set : setOfSets) {
      if (set.contains(element)) {
        return true;
      }
    }
    return false;
  }

  // return true if the service set contains at least one service in the constraint.  Otherwise return false.
  static boolean containsOne(Set<String> serviceSet, Set<String> constraint) {
    for (String service : constraint) {
      if (serviceSet.contains(service)) {
        return true;
      }
    }
    return false;
  }

  // given a set like [i1, i2, i3, i4], return all the subsets that are one element smaller:
  // [i2, i3, i4], [i1, i3, i4], [i1, i2, i4], [i1, i2, i3]
  static Set<Set<String>> getOneSmallerSubsets(Set<String> set) {
    Set<Set<String>> subsets = Sets.newHashSet();
    for (String element : set) {
      Set<String> subset = Sets.newHashSet(set);
      subset.remove(element);
      subsets.add(subset);
    }
    return subsets;
  }
}
