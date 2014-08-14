package com.continuuity.loom.http.request;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Request for reconfiguring a cluster.
 */
public class ClusterConfigureRequest extends ClusterOperationRequest {
  private final boolean restart;
  private final JsonObject config;

  public ClusterConfigureRequest(Map<String, String> providerFields, JsonObject config, Boolean restart) {
    super(providerFields);
    Preconditions.checkArgument(config != null, "config must be specified");
    this.restart = restart == null ? true : restart;
    this.config = config;
  }

  public boolean getRestart() {
    return restart;
  }

  public JsonObject getConfig() {
    return config;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    ClusterConfigureRequest that = (ClusterConfigureRequest) o;
    return Objects.equal(restart, that.restart) &&
      Objects.equal(config, that.config);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (restart ? 1 : 0);
    result = 31 * result + (config != null ? config.hashCode() : 0);
    return result;
  }
}
