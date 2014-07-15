package com.continuuity.loom.provisioner;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Tenant information to send to a specific provisioner.
 */
public class ProvisionerTenant {
  private final int workers;
  // TODO: placeholder right now, fill in with correct objects.
  private final Map<String, String> modules;

  public ProvisionerTenant(int workers) {
    this.workers = workers;
    this.modules = ImmutableMap.of();
  }
}
