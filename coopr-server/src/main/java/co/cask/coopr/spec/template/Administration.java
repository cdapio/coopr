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

/**
 * Defines settings for cluster administration like lease duration, etc.
 */
public final class Administration {
  public static final Administration EMPTY_ADMINISTRATION = new Administration(LeaseDuration.FOREVER_LEASE_DURATION);

  final LeaseDuration leaseDuration;

  public Administration(LeaseDuration leaseDuration) {
    this.leaseDuration = leaseDuration;
  }

  /**
   * Get the {@link LeaseDuration} used in administration of the cluster.
   *
   * @return lease duration used in administration of the cluster.
   */
  public LeaseDuration getLeaseDuration() {
    return leaseDuration;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("leaseDuration", leaseDuration)
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

    Administration that = (Administration) o;

    return Objects.equal(this.leaseDuration, that.leaseDuration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(leaseDuration);
  }
}
