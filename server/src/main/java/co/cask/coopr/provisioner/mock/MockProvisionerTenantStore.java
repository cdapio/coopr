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
package co.cask.coopr.provisioner.mock;

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Mock in memory store for keeping track of how many workers each tenant should have as well as how many workers each
 * tenant has live.
 */
public class MockProvisionerTenantStore {
  private static final MockProvisionerTenantStore INSTANCE = new MockProvisionerTenantStore();
  private final Map<String, Integer> liveWorkers;
  private final Map<String, Integer> assignedWorkers;

  private MockProvisionerTenantStore() {
    this.liveWorkers = Maps.newConcurrentMap();
    this.assignedWorkers = Maps.newConcurrentMap();
  }

  /**
   * Get the instance of the store.
   *
   * @return Instance of the store
   */
  public static MockProvisionerTenantStore getInstance() {
    return INSTANCE;
  }

  /**
   * Get the number of workers assigned to the given tenant.
   *
   * @param tenantId Id of the tenant to get the number of workers for
   * @return Number of workers assigned to the given tenant
   */
  public int getAssignedWorkers(String tenantId) {
    return assignedWorkers.containsKey(tenantId) ? assignedWorkers.get(tenantId) : 0;
  }

  /**
   * Get the number of live workers running for the given tenant.
   *
   * @param tenantId Id of the tenant to get the number of live workers for
   * @return Number of live workers running for the given tenant
   */
  public int getLiveWorkers(String tenantId) {
    return liveWorkers.containsKey(tenantId) ? liveWorkers.get(tenantId) : 0;
  }

  /**
   * Get all tenants that have been assigned workers.
   *
   * @return All tenants that have been assigned workers
   */
  public Set<String> getAssignedTenants() {
    return assignedWorkers.keySet();
  }

  /**
   * Get all tenants that have live running workers.
   *
   * @return All tenants that have live running workers
   */
  public Set<String> getLiveTenants() {
    return liveWorkers.keySet();
  }

  /**
   * Set the number of assigned workers for a given tenant.
   *
   * @param tenantId Id of the tenant to assign workers to
   * @param numWorkers Number of workers the tenant should have
   */
  public void setAssignedWorkers(String tenantId, int numWorkers) {
    if (numWorkers == 0) {
      assignedWorkers.remove(tenantId);
    } else {
      assignedWorkers.put(tenantId, numWorkers);
    }
  }

  /**
   * Delete a tenant from the store.
   *
   * @param tenantId Id of the tenant to delete
   */
  public void deleteTenant(String tenantId) {
    assignedWorkers.remove(tenantId);
    liveWorkers.remove(tenantId);
  }

  /**
   * Set the number of live workers running for a given tenant.
   *
   * @param tenantId Id of the tenant to set the number of live workers for
   * @param numWorkers Number of live workers running for the tenant
   */
  public void setLiveTenantWorkers(String tenantId, int numWorkers) {
    if (numWorkers == 0) {
      liveWorkers.remove(tenantId);
    } else {
      liveWorkers.put(tenantId, numWorkers);
    }
  }

  /**
   * Get a mapping of tenant to number of live running workers.
   *
   * @return Mapping of tenant to number of live running workers
   */
  public Map<String, Integer> getUsage() {
    return Collections.unmodifiableMap(liveWorkers);
  }
}
