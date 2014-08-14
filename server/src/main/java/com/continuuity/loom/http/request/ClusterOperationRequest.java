package com.continuuity.loom.http.request;

import com.continuuity.loom.admin.ParameterType;
import com.continuuity.loom.admin.ProviderType;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Request to perform some cluster operation. These requests may include provider fields for things like
 * credentials that may be needed to access cluster nodes, or things of that nature.
 */
public class ClusterOperationRequest {
  private final Map<String, String> providerFields;

  public ClusterOperationRequest(Map<String, String> providerFields) {
    this.providerFields = providerFields == null ?
      ImmutableMap.<String, String>of() : ImmutableMap.copyOf(providerFields);
  }

  /**
   * Get an immutable copy of the provider fields.
   *
   * @return Immutable copy of the provider fields
   */
  public Map<String, String> getProviderFields() {
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
