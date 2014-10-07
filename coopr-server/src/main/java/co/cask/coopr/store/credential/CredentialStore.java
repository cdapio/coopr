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
package co.cask.coopr.store.credential;

import com.google.common.util.concurrent.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Credential store for getting, setting, and wiping sensitive information such as user credentials. Implementations
 * should not persist any data to disk. All fields are stored as a single entity for a cluster in a tenant.
 */
public interface CredentialStore extends Service {

  /**
   * Set fields for the given tenant and cluster.
   *
   * @param tenantId Id of the tenant
   * @param clusterId Id of the cluster
   * @param fields Fields to set
   * @throws IOException if there was an exception setting the fields
   */
  void set(String tenantId, String clusterId, Map<String, Object> fields) throws IOException;

  /**
   * Get the fields for the given tenant and cluster. Returns an empty map if no fields exist.
   *
   * @param tenantId Id of the tenant
   * @param clusterId Id of the cluster
   * @return fields for the tenant and cluster
   * @throws IOException if there was an exception getting the fields
   */
  Map<String, Object> get(String tenantId, String clusterId) throws IOException;

  /**
   * Wipe out the fields for the tenant and cluster.
   *
   * @param tenantId Id of the tenant
   * @param clusterId Id of the cluster
   * @throws IOException if there was an exception wiping the fields
   */
  void wipe(String tenantId, String clusterId) throws IOException;

  /**
   * Wipe out all fields for all tenants and all clusters.
   *
   * @throws IOException if there was an exception wiping all fields
   */
  void wipe() throws IOException;
}
