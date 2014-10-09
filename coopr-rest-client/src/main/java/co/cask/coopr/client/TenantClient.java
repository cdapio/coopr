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

package co.cask.coopr.client;

import co.cask.coopr.spec.TenantSpecification;

import java.io.IOException;
import java.util.List;

/**
 * The client API for manage tenants.
 */
public interface TenantClient {

  /**
   * Returns the comprehensive set of tenants within the system. If no tenants exist, returns an empty list.
   *
   * @return List of {@link co.cask.coopr.spec.TenantSpecification} objects
   * @throws IOException in case of a problem or the connection was aborted
   */
  List<TenantSpecification> getTenants() throws IOException;

  /**
   * Retrieves details about a tenant by the name.
   *
   * @param tenantName String value of the tenant name
   * @return {@link co.cask.coopr.spec.TenantSpecification} object
   * @throws IOException in case of a problem or the connection was aborted
   */
  TenantSpecification getTenant(String tenantName) throws IOException;

  /**
   * Deletes the tenant by the specified name
   *
   * @param tenantName String value of the tenant name
   * @throws IOException in case of a problem or the connection was aborted
   */
  void deleteTenant(String tenantName) throws IOException;
}
