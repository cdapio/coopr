package com.continuuity.loom.provisioner.mock;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class MockProvisionerTenantStore {
  private static final MockProvisionerTenantStore INSTANCE = new MockProvisionerTenantStore();
  private final Map<String, Integer> liveWorkers;
  private final Map<String, Integer> assignedWorkers;

  private MockProvisionerTenantStore() {
    this.liveWorkers = Maps.newConcurrentMap();
    this.assignedWorkers = Maps.newConcurrentMap();
  }

  public static MockProvisionerTenantStore getInstance() {
    return INSTANCE;
  }

  public int getAssignedWorkers(String tenantId) {
    return assignedWorkers.containsKey(tenantId) ? assignedWorkers.get(tenantId) : 0;
  }

  public int getLiveWorkers(String tenantId) {
    return liveWorkers.containsKey(tenantId) ? liveWorkers.get(tenantId) : 0;
  }

  public Set<String> getAssignedTenants() {
    return assignedWorkers.keySet();
  }

  public Set<String> getLiveTenants() {
    return liveWorkers.keySet();
  }

  public void setAssignedWorkers(String tenantId, int numWorkers) {
    assignedWorkers.put(tenantId, numWorkers);
  }

  public void deleteTenant(String tenantId) {
    assignedWorkers.remove(tenantId);
    liveWorkers.remove(tenantId);
  }

  public void setLiveTenantWorkers(String tenantId, int numWorkers) {
    liveWorkers.put(tenantId, numWorkers);
  }

  public Map<String, Integer> getUsage() {
    return Collections.unmodifiableMap(liveWorkers);
  }
}
