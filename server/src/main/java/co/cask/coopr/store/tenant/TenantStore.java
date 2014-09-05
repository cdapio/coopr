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
package co.cask.coopr.store.tenant;

import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import com.google.common.util.concurrent.Service;

import java.io.IOException;
import java.util.List;

/**
 * Store for adding, modifying, retrieving, and deleting tenants.
 */
public interface TenantStore extends Service {

  /**
   * Get the {@link co.cask.coopr.spec.Tenant} associated with the given id or null if none exists.
   *
   * @param id Id of the tenant.
   * @return Tenant for the given id or null if no such tenant exists.
   * @throws IOException
   */
  Tenant getTenantByID(String id) throws IOException;

  /**
   * Get the {@link co.cask.coopr.spec.Tenant} associated with the given name or null if none exists.
   *
   * @param name Name of the tenant.
   * @return Tenant for the given name or null if no such tenant exists.
   * @throws IOException
   */
  Tenant getTenantByName(String name) throws IOException;

  /**
   * Get an immutable list of all {@link co.cask.coopr.spec.Tenant tenants}.
   *
   * @return Collection of all tenants.
   * @throws IOException
   */
  List<Tenant> getAllTenants() throws IOException;

  /**
   * Get an immutable list of all {@link co.cask.coopr.spec.TenantSpecification}.
   *
   * @return Collection of all tenant specifications.
   * @throws IOException
   */
  List<TenantSpecification> getAllTenantSpecifications() throws IOException;

  /**
   * Write the given {@link co.cask.coopr.spec.Tenant} to the store. Will overwrite the existing
   * {@link co.cask.coopr.spec.Tenant} if one exists.
   *
   * @param tenant Tenant to write.
   * @throws IOException
   */
  void writeTenant(Tenant tenant) throws IOException;

  /**
   * Delete the {@link co.cask.coopr.spec.Tenant} associated with the given name.
   *
   * @param name Name of the tenant to delete.
   * @throws IOException
   */
  void deleteTenantByName(String name) throws IOException;

  /**
   * Get the name of the tenant that has the given id, or null if the tenant does not exist.
   *
   * @param id Id of the tenant to get the name for
   * @return Name of the tenant with the given id, or null if the tenant does not exist
   * @throws IOException
   */
  String getNameForId(String id) throws IOException;
}
