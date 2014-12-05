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

package co.cask.coopr.test.client.rest;

import co.cask.common.http.exception.HttpFailureException;
import co.cask.coopr.client.TenantClient;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.test.client.ClientTest;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TenantRestClientTest extends ClientTest {

  private TenantClient tenantClient;

  @Before
  public void setUp() throws Exception {
    tenantClient = superadminCientManager.getTenantClient();
  }

  @Test
  public void testGetAllTenants() throws IOException {
    List<TenantSpecification> result = tenantClient.getTenants();
    // first is the tenant created in the RestClientTest and the second one is the default superadmin tenant
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(result.contains(TEST_TENANT.getSpecification()));
  }

  @Test
   public void testGetAllTenantsWithoutPermissions() throws IOException {
    try {
      adminClientManager.getTenantClient().getTenants();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, e.getStatusCode());
    }
  }

  @Test
  public void testGetTenant() throws IOException {
    TenantSpecification result = tenantClient.getTenant(TENANT);
    Assert.assertEquals(TEST_TENANT.getSpecification(), result);
  }

  @Test
  public void testGetTenantUnknownTenantName() throws IOException {
    try {
      tenantClient.getTenant("test");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteTenantSuccess() throws IOException {
    TenantSpecification result = tenantClient.getTenant(TENANT);
    Assert.assertNotNull(result);
    Assert.assertEquals(TEST_TENANT.getSpecification(), result);

    tenantClient.deleteTenant(TENANT);

    try {
      tenantClient.getTenant(TENANT);
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }
}
