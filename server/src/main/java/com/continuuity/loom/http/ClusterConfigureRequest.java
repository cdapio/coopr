package com.continuuity.loom.http;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

/**
 * Request for reconfiguring a cluster.
 */
public class ClusterConfigureRequest {
  private final boolean restart;
  private final JsonObject config;

  public ClusterConfigureRequest(JsonObject config, Boolean restart) {
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
}
