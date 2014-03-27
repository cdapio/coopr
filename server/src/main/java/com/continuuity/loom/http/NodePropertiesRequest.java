package com.continuuity.loom.http;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.Set;

/**
 * Request for node properties in a cluster.
 */
public class NodePropertiesRequest {
  private final String clusterId;
  private final Set<String> properties;
  private final Set<String> services;

  public NodePropertiesRequest(String clusterId, Set<String> properties, Set<String> services) {
    Preconditions.checkArgument(clusterId != null, "clusterId must be specified.");
    this.clusterId = clusterId;
    this.properties = properties == null ? Collections.EMPTY_SET : properties;
    this.services = services == null ? Collections.EMPTY_SET : services;
  }

  public String getClusterId() {
    return clusterId;
  }

  public Set<String> getProperties() {
    return properties;
  }

  public Set<String> getServices() {
    return services;
  }
}
