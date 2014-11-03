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
import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.test.client.ClientTest;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ProvisionerRestClientTest extends ClientTest {

  private ProvisionerClient provisionerClient;

  @Before
  public void setUp() throws Exception {
    provisionerClient = superadminCientManager.getProvisionerClient();
  }

  @Test
  public void testGetAllProvisioners() throws IOException {
    List<Provisioner> result = provisionerClient.getAllProvisioners();
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(PROVISIONER_ID, result.get(0).getId());
  }

  @Test
  public void testGetAllProvisionersWithoutPermissions() throws IOException {
    try {
      adminClientManager.getProvisionerClient().getAllProvisioners();
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, e.getStatusCode());
    }
  }

  @Test
  public void testGetProvisioner() throws IOException {
    Provisioner result = provisionerClient.getProvisioner(PROVISIONER_ID);
    Assert.assertEquals(TEST_PROVISIONER, result);
  }

  @Test
  public void testGetProvisionerUnknownProvisioner() throws IOException {
    try {
      provisionerClient.getProvisioner("test");
      Assert.fail("Expected HttpFailureException");
    } catch (HttpFailureException e) {
      Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
    }
  }
}
