package co.cask.coopr.http.request;

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

  public ClusterConfigureRequest(Map<String, Object> providerFields, JsonObject config, Boolean restart) {
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

    ClusterConfigureRequest that = (ClusterConfigureRequest) o;
    return super.equals(that) &&
      Objects.equal(restart, that.restart) &&
      Objects.equal(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), restart, config);
  }
}
