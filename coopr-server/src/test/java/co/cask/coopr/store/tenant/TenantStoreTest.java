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

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Tests for the tenant store.  Test classes for different types of stores must set the
 * protected store field before each test and make sure state is wiped out between tests.
 */
public abstract class TenantStoreTest {
  protected static TenantStore store;

  @Before
  public abstract void clearState() throws Exception;

  @Test
  public void testGetNameForId() throws IOException {
    Tenant tenant1 = new Tenant(UUID.randomUUID().toString(), new TenantSpecification("name1", 10, 100, 1000));
    Tenant tenant2 = new Tenant(UUID.randomUUID().toString(), new TenantSpecification("name2", 10, 100, 1000));
    store.writeTenant(tenant1);
    store.writeTenant(tenant2);

    Assert.assertEquals(tenant1.getSpecification().getName(), store.getNameForId(tenant1.getId()));
    Assert.assertEquals(tenant2.getSpecification().getName(), store.getNameForId(tenant2.getId()));
  }

  @Test
  public void testSuperadminExistsOnStart() throws IOException {
    Assert.assertEquals(Tenant.DEFAULT_SUPERADMIN, store.getTenantByName(Constants.SUPERADMIN_TENANT));
  }

  @Test
  public void testGetNonExistantTenantReturnsNull() throws IOException {
    Assert.assertNull(store.getTenantByID(UUID.randomUUID().toString()));
  }

  @Test
  public void testAddWriteDelete() throws IOException {
    Tenant tenant = new Tenant(UUID.randomUUID().toString(), new TenantSpecification("name", 10, 100, 1000));
    store.writeTenant(tenant);

    Assert.assertEquals(tenant, store.getTenantByID(tenant.getId()));
    Assert.assertEquals(tenant, store.getTenantByName(tenant.getSpecification().getName()));

    store.deleteTenantByName(tenant.getSpecification().getName());
    Assert.assertNull(store.getTenantByID(tenant.getId()));
    Assert.assertNull(store.getTenantByName(tenant.getSpecification().getName()));
  }

  @Test
  public void testOverwrite() throws IOException {
    Tenant tenant = new Tenant(UUID.randomUUID().toString(), new TenantSpecification("name", 10, 100, 1000));
    store.writeTenant(tenant);
    tenant = new Tenant(tenant.getId(), new TenantSpecification("name", 10, 100, 500));
    store.writeTenant(tenant);
    Assert.assertEquals(tenant, store.getTenantByID(tenant.getId()));
  }
}
