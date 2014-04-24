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

package com.continuuity.loom.admin;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A Layout constraint specifies sets of services that must coexist on the same node, and sets of services that must not
 * coexist on the same node in a cluster.  These constraints are used to determine which services can be placed
 * together when solving for the layout of a cluster.
 */
public final class LayoutConstraint {
  private final Set<Set<String>> servicesThatMustCoexist;
  private final Set<Set<String>> servicesThatMustNotCoexist;
  public static final LayoutConstraint EMPTY_LAYOUT_CONSTRAINT = new LayoutConstraint(null, null);

  public LayoutConstraint(Set<Set<String>> servicesThatMustCoexist,
                          Set<Set<String>> servicesThatMustNotCoexist) {
    this.servicesThatMustCoexist = servicesThatMustCoexist == null ?
      ImmutableSet.<Set<String>>of() : servicesThatMustCoexist;
    this.servicesThatMustNotCoexist = servicesThatMustNotCoexist == null ?
      ImmutableSet.<Set<String>>of() : servicesThatMustNotCoexist;
  }

  /**
   * Get the set of service sets that must coexist on the same node in the cluster. These rules are not transitive.
   * For example, if {serviceA, serviceB} must coexist, and {serviceB, serviceC} must coexist, this does NOT mean that
   * serviceA and serviceC must coexist.
   *
   * @return Set of service sets that must coexist on the same node in the cluster.
   */
  public Set<Set<String>> getServicesThatMustCoexist() {
    return servicesThatMustCoexist;
  }

  /**
   * Get the set of service sets that must not coexist on the same node in the cluster.  For example, if
   * {datanode, zookeeper, nodemanager} is a service set that must not coexist, this means no node in the cluster
   * can have all three of those services on it.  However, this service set would not disallow
   * datanode and zookeeper to be on the same node, just the full set of services.
   *
   * @return Set of service sets that must not coexist on the same node in the cluster.
   */
  public Set<Set<String>> getServicesThatMustNotCoexist() {
    return servicesThatMustNotCoexist;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LayoutConstraint)) {
      return false;
    }
    LayoutConstraint other = (LayoutConstraint) o;
    return Objects.equal(servicesThatMustCoexist, other.servicesThatMustCoexist) &&
      Objects.equal(servicesThatMustNotCoexist, other.servicesThatMustNotCoexist);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(servicesThatMustCoexist, servicesThatMustNotCoexist);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("servicesThatMustCoexist", servicesThatMustCoexist)
      .add("servicesThatMustNotCoexist", servicesThatMustNotCoexist)
      .toString();
  }
}
