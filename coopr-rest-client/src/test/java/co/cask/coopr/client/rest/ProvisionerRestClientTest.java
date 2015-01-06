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
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.client.rest.exception.UnauthorizedAccessTokenException;
import co.cask.coopr.client.rest.handler.TestStatusUserId;
import co.cask.coopr.provisioner.Provisioner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ProvisionerRestClientTest extends RestClientTest {

  public static final List<Provisioner> PROVISIONERS = Lists.newArrayList(
      new Provisioner("p1", "host1", 12345, 10,
                      ImmutableMap.of("tenantX", 5, "tenantY", 3),
                      ImmutableMap.of("tenantX", 5, "tenantY", 5)),
      new Provisioner("p2", "host2", 12345, 100,
                      ImmutableMap.of("tenantA", 5, "tenantY", 3, "tenantZ", 2),
                      ImmutableMap.of("tenantA", 5, "tenantY", 5, "tenantZ", 2)));

  public static final Provisioner PROVISIONER = new Provisioner("p1", "host1", 12345, 10,
                           ImmutableMap.of("tenantX", 5, "tenantY", 3),
                           ImmutableMap.of("tenantX", 5, "tenantY", 5));

  private ProvisionerClient provisionerClient;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    provisionerClient = clientManager.getProvisionerClient();
  }

  @Test
  public void testGetAllProvisionersSuccess() throws IOException {
    List<Provisioner> result = provisionerClient.getAllProvisioners();
    assertEquals(PROVISIONERS, result);
  }

  @Test
  public void testGetAllProvisionersBadRequest() throws IOException {
    clientManager = createClientManager(TestStatusUserId.BAD_REQUEST_STATUS_USER_ID.getValue());
    provisionerClient = clientManager.getProvisionerClient();
    try {
      provisionerClient.getAllProvisioners();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());
    }
  }

  @Test
  public void testGetAllProvisionersUnauthorized() throws IOException {
    clientManager = createClientManager(TestStatusUserId.UNAUTHORIZED_STATUS_USER_ID.getValue());
    provisionerClient = clientManager.getProvisionerClient();
    try {
      provisionerClient.getAllProvisioners();
      Assert.fail("Expected UnauthorizedAccessTokenException");
    } catch (UnauthorizedAccessTokenException ignored) {
    }
  }

  @Test
  public void testGetProvisionerSuccess() throws IOException {
    Provisioner result = provisionerClient.getProvisioner(PROVISIONER.getId());
    assertEquals(PROVISIONER, result);
  }

  @Test
  public void testGetProvisionerNotFound() throws IOException {
    clientManager = createClientManager(TestStatusUserId.NOT_FOUND_STATUS_USER_ID.getValue());
    provisionerClient = clientManager.getProvisionerClient();
    try {
      provisionerClient.getProvisioner(PROVISIONER.getId());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }

  @Test
  public void testGetProvisionerConflict() throws IOException {
    clientManager = createClientManager(TestStatusUserId.CONFLICT_STATUS_USER_ID.getValue());
    provisionerClient = clientManager.getProvisionerClient();
    try {
      provisionerClient.getProvisioner(PROVISIONER.getId());
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getStatusCode());
    }
  }

  @After
  public void shutDown() throws Exception {
    super.shutDown();
  }
}
