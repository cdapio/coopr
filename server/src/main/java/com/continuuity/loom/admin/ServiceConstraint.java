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

import com.continuuity.loom.common.utils.ImmutablePair;
import com.google.common.base.Objects;

import java.util.Set;

/**
 * Service constraints restrict what hardware and images are allowed for some service.  Service constraints also
 * specify the minimum and maximum number of machines that can have the service in a cluster, as well as a step size
 * and ratio which will determine how to scale the service as the cluster grows or shrinks.  The ratio is in
 * reference to the total size of the cluster.
 */
public final class ServiceConstraint {
  private final Set<String> requiredHardwareTypes;
  private final Set<String> requiredImageTypes;
  private final Integer minCount;
  private final Integer maxCount;
  private final Integer stepSize;
  private final ImmutablePair<Integer, Integer> ratio;

  public ServiceConstraint(Set<String> requiredHardwareTypes, Set<String> requiredImageTypes,
                           Integer minCount, Integer maxCount, Integer stepSize,
                           ImmutablePair<Integer, Integer> ratio) {
    this.requiredHardwareTypes = requiredHardwareTypes;
    this.requiredImageTypes = requiredImageTypes;
    this.minCount = minCount == null ? 0 : minCount;
    this.maxCount = maxCount == null ? Integer.MAX_VALUE : maxCount;
    this.stepSize = stepSize == null ? 1 : stepSize;
    this.ratio = ratio;
  }

  /**
   * Get the set of required hardware type names.  If null, any hardware type is allowed.
   *
   * @return Set of required hardware type names.
   */
  public Set<String> getRequiredHardwareTypes() {
    return requiredHardwareTypes;
  }

  /**
   * Get the set of required image type names.  If null, any image type is allowed.
   *
   * @return Set of required image type names.
   */
  public Set<String> getRequiredImageTypes() {
    return requiredImageTypes;
  }

  /**
   * Get the minimum count of the service in the cluster.
   *
   * @return Minimum count of the service in the cluster.
   */
  public int getMinCount() {
    return minCount;
  }

  /**
   * Get the maximum count of the service in the cluster.
   *
   * @return Maximum count of the service in the cluster.
   */
  public int getMaxCount() {
    return maxCount;
  }

  /**
   * Get the step size that the service must grow/shrink by.  For example, a step size of 2 means that when the cluster
   * size is being increased or decreased, the service is only allowed to grow or shrink by 2 machines at a time.
   *
   * @return Step size that the service must grow/shrink by.
   */
  public int getStepSize() {
    return stepSize;
  }

  /**
   * Get the ratio of machines with the service to the total size of the cluster.
   *
   * @return Ratio of machines with the service to the total size of the cluster.
   */
  public ImmutablePair<Integer, Integer> getRatio() {
    return ratio;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServiceConstraint) || o == null) {
      return false;
    }
    ServiceConstraint other = (ServiceConstraint) o;
    return Objects.equal(minCount, other.minCount) &&
      Objects.equal(maxCount, other.maxCount) &&
      Objects.equal(stepSize, other.stepSize) &&
      Objects.equal(ratio, other.ratio) &&
      Objects.equal(requiredHardwareTypes, other.requiredHardwareTypes) &&
      Objects.equal(requiredImageTypes, other.requiredImageTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(minCount, maxCount, stepSize, ratio, requiredHardwareTypes, requiredImageTypes);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("requiredHardwareTypes", requiredHardwareTypes)
      .add("requiredImageTypes", requiredImageTypes)
      .add("minCount", minCount)
      .add("maxCount", maxCount)
      .add("stepSize", stepSize)
      .add("ratio", ratio)
      .toString();
  }
}
