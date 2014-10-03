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
package co.cask.coopr.http.request;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Request to perform some cluster operation that needs to communicate with a provider. These requests may include
 * provider fields for things like credentials that may be needed to access cluster nodes, or things of that nature.
 */
public class ClusterOperationRequest {
  private final Map<String, Object> providerFields;

  public ClusterOperationRequest(Map<String, Object> providerFields) {
    this.providerFields = providerFields == null ?
      ImmutableMap.<String, Object>of() : ImmutableMap.copyOf(providerFields);
  }

  /**
   * Get an immutable copy of the provider fields.
   *
   * @return Immutable copy of the provider fields
   */
  public Map<String, Object> getProviderFields() {
    return providerFields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterOperationRequest that = (ClusterOperationRequest) o;

    return Objects.equal(providerFields, that.providerFields);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(providerFields);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("providerFields", providerFields)
      .toString();
  }
}
