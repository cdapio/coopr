/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.store.tenant;

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.store.tenant.TenantStore;
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
  public void testSuperadminExistsOnStart() throws IOException {
    Assert.assertEquals(Tenant.DEFAULT_SUPERADMIN, store.getTenant(Constants.SUPERADMIN_TENANT));
  }

  @Test
  public void testGetNonExistantTenantReturnsNull() throws IOException {
    Assert.assertNull(store.getTenant(UUID.randomUUID().toString()));
  }

  @Test
  public void testAddWriteDelete() throws IOException {
    Tenant tenant = new Tenant("name", UUID.randomUUID().toString(), 10, 100, 1000);
    store.writeTenant(tenant);

    Assert.assertEquals(tenant, store.getTenant(tenant.getId()));

    store.deleteTenant(tenant.getId());
    Assert.assertNull(store.getTenant(tenant.getId()));
  }

  @Test
  public void testOverwrite() throws IOException {
    Tenant tenant = new Tenant("name", UUID.randomUUID().toString(), 10, 100, 1000);
    store.writeTenant(tenant);
    tenant = new Tenant("name", tenant.getId(), 10, 100, 500);
    store.writeTenant(tenant);
    Assert.assertEquals(tenant, store.getTenant(tenant.getId()));
  }
}
