package com.continuuity.loom.provisioner;

import com.continuuity.loom.provisioner.plugin.ResourceCollection;

/**
 * Tenant information to send to a specific provisioner.
 */
public class ProvisionerTenant {
  private final int workers;
  private final ResourceCollection resources;

  public ProvisionerTenant(int workers, ResourceCollection resources) {
    this.workers = workers;
    this.resources = resources;
  }
}
