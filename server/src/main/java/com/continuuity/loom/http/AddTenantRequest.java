package com.continuuity.loom.http;

import com.continuuity.loom.admin.Tenant;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * Request for adding a tenant.
 */
public class AddTenantRequest {
  private final Tenant tenant;

  public AddTenantRequest(Tenant tenant) {
    Preconditions.checkArgument(tenant != null, "tenant must be specified.");
    this.tenant = tenant;
  }

  public Tenant getTenant() {
    return tenant;
  }
}
