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
import com.continuuity.loom.cluster.Node;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Defines the layout of a node, which is a hardware type, an image type, and a set of services.
 */
public class NodeLayout {
  private final String hardwareType;
  private final String imageType;
  private final ImmutableSortedSet<String> services;

  public NodeLayout(String hardwareType, String imageType, Set<String> services) {
    this.hardwareType = hardwareType;
    this.imageType = imageType;
    this.services = ImmutableSortedSet.copyOf(services);
  }

  /**
   * Get the name of the {@link com.continuuity.loom.admin.HardwareType} used on the node.
   *
   * @return Name of the hardware type used on the node.
   */
  public String getHardwareTypeName() {
    return hardwareType;
  }

  /**
   * Get the name of the {@link com.continuuity.loom.admin.ImageType} used on the node.
   *
   * @return Name of the image type used on the node.
   */
  public String getImageTypeName() {
    return imageType;
  }

  /**
   * Get the names of services on the node sorted in their natural order. Used by comparators to consistently choose
   * the same node layout when solving for a cluster layout.
   *
   * @return Sorted set of names of services on the node.
   */
  public SortedSet<String> getServiceNames() {
    return services;
  }

  /**
   * Determine if this is a valid node layout some constraints and the set of all services that are also on the cluster.
   *
   * @param constraints Constraints to use for checking validity.
   * @param clusterServices Services on the cluster with this node layout.
   * @return True if it satisfies all constraints, false if not.
   */
  public boolean satisfiesConstraints(Constraints constraints, Set<String> clusterServices) {
    return NodeLayoutGenerator.isValidServiceSet(services, constraints.getLayoutConstraint(), clusterServices) &&
      satisfiesServiceConstraints(constraints.getServiceConstraints());
  }

  /**
   * Determine if this is a valid node layout given the service constraints.
   *
   * @param serviceConstraints Service constraints to use for checking validity.
   * @return True if it satisfies all service constraints, false if not.
   */
  public boolean satisfiesServiceConstraints(Map<String, ServiceConstraint> serviceConstraints) {
    for (String service : services) {
      ServiceConstraint constraint = serviceConstraints.get(service);
      if (constraint != null) {
        // check that no required hardware rules are broken
        Set<String> requiredHardwareTypes = constraint.getRequiredHardwareTypes();
        if (requiredHardwareTypes != null && !requiredHardwareTypes.isEmpty() &&
          !requiredHardwareTypes.contains(hardwareType)) {
          return false;
        }
        // check that no required image rules are broken
        Set<String> requiredImageTypes = constraint.getRequiredImageTypes();
        if (requiredImageTypes != null && !requiredImageTypes.isEmpty() &&
          !requiredImageTypes.contains(imageType)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Return the node layout of a given {@link Node}.
   *
   * @param node Node to derive the node layout from.
   * @return Node layout of the node.
   */
  public static NodeLayout fromNode(Node node) {
    String hardwareType = node.getProperties().get(Node.Properties.HARDWARETYPE.name().toLowerCase()).getAsString();
    String imageType = node.getProperties().get(Node.Properties.IMAGETYPE.name().toLowerCase()).getAsString();
    Set<String> services = Sets.newHashSet();
    for (Service service : node.getServices()) {
      services.add(service.getName());
    }
    return new NodeLayout(hardwareType, imageType, services);
  }

  /**
   * Create a new NodeLayout by adding a service to the given NodeLayout.
   *
   * @param nodeLayout NodeLayout to add the service to.
   * @param service Service to add.
   * @return New layout obtained by adding the service to the given layout.
   */
  public static NodeLayout addServiceToNodeLayout(NodeLayout nodeLayout, String service) {
    return addServicesToNodeLayout(nodeLayout, ImmutableSet.of(service));
  }

  /**
   * Create a new NodeLayout by adding a set of services to the given NodeLayout.
   *
   * @param nodeLayout NodeLayout to add the services to.
   * @param services Services to add.
   * @return New layout obtained by adding the services to the given layout.
   */
  public static NodeLayout addServicesToNodeLayout(NodeLayout nodeLayout, Set<String> services) {
    Set<String> expandedServices = Sets.newTreeSet(nodeLayout.getServiceNames());
    expandedServices.addAll(services);
    return new NodeLayout(nodeLayout.hardwareType, nodeLayout.imageType, expandedServices);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NodeLayout)) {
      return false;
    }
    NodeLayout other = (NodeLayout) o;
    return Objects.equal(hardwareType, other.hardwareType) &&
      Objects.equal(imageType, other.imageType) &&
      Objects.equal(services, other.services);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(services, hardwareType, imageType);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("hardwareType", hardwareType)
      .add("imageType", imageType)
      .add("services", services)
      .toString();
  }
}
