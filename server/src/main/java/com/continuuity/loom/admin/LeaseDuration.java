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
import com.google.common.base.Preconditions;

/**
 * Defines lease duration for a cluster. 0 for initial or max means forever;
 */
public class LeaseDuration {
  public static final LeaseDuration FOREVER_LEASE_DURATION = new LeaseDuration(0, 0, 0);

  private final long initial;
  private final long max;
  private final long step;

  public LeaseDuration(long initial, long max, long step) {
    Preconditions.checkArgument(initial >= 0, "initial lease duration should be >=0");
    Preconditions.checkArgument(max >= 0, "max lease duration should be >=0");
    Preconditions.checkArgument(step >= 0, "step should be >=0");
    this.initial = initial;
    this.max = max;
    this.step = step;
  }

  /**
   * Get the initial lease time in seconds, with 0 meaning forever.
   *
   * @return Initial lease time in seconds.
   */
  public long getInitial() {
    return initial;
  }

  /**
   * Get the maximum lease time in seconds, with 0 meaning forever.
   *
   * @return Maximum lease time in seconds.
   */
  public long getMax() {
    return max;
  }

  /**
   * Get the step size in seconds to use when extending a lease, with 0 meaning any step size.
   *
   * @return Step size in seconds to use when extending a lease, with 0 meaning any step size.
   */
  public long getStep() {
    return step;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("initial", initial)
      .add("max", max)
      .add("step", step)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LeaseDuration that = (LeaseDuration) o;

    return Objects.equal(this.initial, that.initial) &&
      Objects.equal(this.max, that.max) &&
      Objects.equal(this.step, that.step);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(initial, max, step);
  }
}
