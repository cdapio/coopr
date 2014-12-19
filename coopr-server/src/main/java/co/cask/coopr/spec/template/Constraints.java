/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.spec.template;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Constraints for how a cluster can be laid out.  This includes a mapping of service name to {@link ServiceConstraint},
 * and a {@link LayoutConstraint} for the cluster.
 */
public final class Constraints {
  public static final Constraints EMPTY_CONSTRAINTS = new Constraints(null, null, null);
  final Map<String, ServiceConstraint> serviceConstraints;
  final LayoutConstraint layoutConstraint;
  final SizeConstraint sizeConstraint;

  public Constraints(Map<String, ServiceConstraint> serviceConstraints, LayoutConstraint layoutConstraint,
                     SizeConstraint sizeConstraint) {
    this.serviceConstraints = serviceConstraints == null ?
      ImmutableMap.<String, ServiceConstraint>of() : serviceConstraints;
    this.layoutConstraint = layoutConstraint == null ? LayoutConstraint.EMPTY_LAYOUT_CONSTRAINT : layoutConstraint;
    this.sizeConstraint = sizeConstraint == null ? SizeConstraint.EMPTY : sizeConstraint;
  }

  /**
   * Get the mapping from service name to {@link ServiceConstraint}.
   *
   * @return Mapping from service name to {@link ServiceConstraint}.
   */
  public Map<String, ServiceConstraint> getServiceConstraints() {
    return serviceConstraints;
  }

  /**
   * Get the {@link LayoutConstraint} for the cluster.
   *
   * @return {@link LayoutConstraint} for the cluster.
   */
  public LayoutConstraint getLayoutConstraint() {
    return layoutConstraint;
  }

  /**
   * Get the {@link SizeConstraint} for the cluster.
   *
   * @return {@link SizeConstraint} for the cluster.
   */
  public SizeConstraint getSizeConstraint() {
    return sizeConstraint;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Constraints)) {
      return false;
    }
    Constraints other = (Constraints) o;
    return Objects.equal(serviceConstraints, other.serviceConstraints) &&
      Objects.equal(layoutConstraint, other.layoutConstraint) &&
      Objects.equal(sizeConstraint, other.sizeConstraint);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceConstraints, layoutConstraint, sizeConstraint);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("serviceConstraints", serviceConstraints)
      .add("layoutConstraint", layoutConstraint)
      .add("sizeContraint", sizeConstraint)
      .toString();
  }
}
