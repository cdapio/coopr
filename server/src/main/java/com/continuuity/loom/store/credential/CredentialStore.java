package com.continuuity.loom.store.credential;

import java.io.IOException;
import java.util.Map;

/**
 * Credential store for getting, setting, and wiping sensitive information such as user credentials. Implementations
 * should not persist any data to disk. All fields are stored as a single entity for a cluster in a tenant.
 */
public interface CredentialStore {

  /**
   * Set fields for the given tenant and cluster.
   *
   * @param tenantId Id of the tenant
   * @param clusterId Id of the cluster
   * @param fields Fields to set
   * @throws IOException if there was an exception setting the fields
   */
  void set(String tenantId, String clusterId, Map<String, String> fields) throws IOException;

  /**
   * Get the fields for the given tenant and cluster. Returns an empty map if no fields exist.
   *
   * @param tenantId Id of the tenant
   * @param clusterId Id of the cluster
   * @return fields for the tenant and cluster
   * @throws IOException if there was an exception getting the fields
   */
  Map<String, String> get(String tenantId, String clusterId) throws IOException;

  /**
   * Wipe out the fields for the tenant and cluster.
   *
   * @param tenantId Id of the tenant
   * @param clusterId Id of the cluster
   * @throws IOException if there was an exception wiping the fields
   */
  void wipe(String tenantId, String clusterId) throws IOException;

  /**
   * Wipe out all fields for all tenants and all clusters
   *
   * @throws IOException if there was an exception wiping all fields
   */
  void wipe() throws IOException;
}
