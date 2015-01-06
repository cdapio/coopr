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

package co.cask.coopr.client.rest;

import co.cask.common.http.exception.HttpFailureException;
import co.cask.coopr.client.TenantClient;
import co.cask.coopr.client.rest.exception.UnauthorizedAccessTokenException;
import co.cask.coopr.client.rest.handler.TestStatusUserId;
import co.cask.coopr.spec.TenantSpecification;
import com.google.common.collect.Lists;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TenantRestClientTest extends RestClientTest {
  public static final List<TenantSpecification> TENANTS = Lists.newArrayList(
    new TenantSpecification("companyX", 10000, 100, 1000),
    new TenantSpecification("companyY", 10, 100, 1000),
    new TenantSpecification("companyZ", 1000, 50, 500));

  public static final TenantSpecification TENANT = new TenantSpecification("companyX", 10000, 100, 1000);

  private TenantClient tenantClient;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    tenantClient = clientManager.getTenantClient();
  }

  @Test
  public void testGetAllTenantsSuccess() throws IOException {
    List<TenantSpecification> result = tenantClient.getTenants();
    assertEquals(TENANTS, result);
  }

  @Test
  public void testGetAllTenantsBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    tenantClient = clientManager.getTenantClient();
    try {
      tenantClient.getTenants();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllTenantsUnauthorized() throws IOException {
    clientManager = createClientManager(TestStatusUserId.UNAUTHORIZED_STATUS_USER_ID.getValue());
    tenantClient = clientManager.getTenantClient();
    try {
      tenantClient.getTenants();
      Assert.fail("Expected UnauthorizedAccessTokenException");
    } catch (UnauthorizedAccessTokenException ignored) {
    }
  }

  @Test
  public void testGetTenantSuccess() throws IOException {
    TenantSpecification result = tenantClient.getTenant(TENANT.getName());
    assertEquals(TENANT, result);
  }

  @Test
  public void testGetTenantNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    tenantClient = clientManager.getTenantClient();
    try {
      tenantClient.getTenant(TENANT.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetTenantConflict() throws IOException {
    clientManager = createClientManager(TestStatusUserId.CONFLICT_STATUS_USER_ID.getValue());
    tenantClient = clientManager.getTenantClient();
    try {
      tenantClient.getTenant(TENANT.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getStatusCode());
    }
  }

  @Test
  public void testDeleteTenantSuccess() throws IOException {
    tenantClient.deleteTenant(TENANT.getName());
  }

  @Test
  public void testDeleteImageTypeNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    tenantClient = clientManager.getTenantClient();
    try {
      tenantClient.deleteTenant(TENANT.getName());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @After
  public void shutDown() throws Exception {
    super.shutDown();
  }
}
