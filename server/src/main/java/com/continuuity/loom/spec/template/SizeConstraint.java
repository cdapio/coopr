package com.continuuity.loom.spec.template;

import com.google.common.base.Objects;

/**
 * A size constraint on the cluster, enforcing some min and/or max on the number of nodes in the cluster.
 */
public class SizeConstraint {
  public static final SizeConstraint EMPTY = new SizeConstraint(1, Integer.MAX_VALUE);
  private final int min;
  private final int max;

  public SizeConstraint(Integer min, Integer max) {
    this.min = min == null ? 1 : min;
    if (this.min < 1) {
      throw new IllegalArgumentException("Minimum must be at least 1.");
    }
    this.max = max == null ? Integer.MAX_VALUE : max;
    if (this.max < this.min) {
      throw new IllegalArgumentException("Maximum must greater than or equal to the minimum.");
    }
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SizeConstraint that = (SizeConstraint) o;
    return min == that.min && max == that.max;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(min, max);
  }
}
