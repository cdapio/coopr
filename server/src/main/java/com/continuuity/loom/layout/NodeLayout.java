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

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.TreeSet;

/**
 * Defines the layout of a node, which is a hardware type, an image type, and a set of services.
 */
public class NodeLayout {
  private final String hardwareType;
  private final String imageType;
  private final TreeSet<String> services;

  public NodeLayout(String hardwareType, String imageType, Set<String> services) {
    this.hardwareType = hardwareType;
    this.imageType = imageType;
    this.services = Sets.newTreeSet(services);
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
  public TreeSet<String> getServiceNames() {
    return services;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NodeLayout) || o == null) {
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
}
