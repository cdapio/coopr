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

package co.cask.coopr.test.cli;

import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.test.Constants;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

public class TenantCommndsTest extends AbstractTest {

  @BeforeClass
  public static void beforeClass()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException {
    createCli(SUPERADMIN_ACCOUNT);
  }

  @Test
  public void testListTenants() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_TENANTS_COMMAND);
    Set<TenantSpecification> result = getSetFromOutput(new TypeToken<Set<TenantSpecification>>() {}.getType());
    // first is the tenant created in the RestClientTest and the second one is the default superadmin tenant
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(result.contains(TEST_TENANT.getSpecification()));
  }

  @Test
   public void testListTenantsWithoutPermissions()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException, InvalidCommandException {
    createCli(ADMIN_ACCOUNT);

    execute(Constants.LIST_TENANTS_COMMAND);
    checkError();

    createCli(SUPERADMIN_ACCOUNT);
  }

  @Test
  public void testGetTenant() throws InvalidCommandException, UnsupportedEncodingException {
    execute(String.format(Constants.GET_TENANT_COMMAND, TENANT));
    checkCommandOutput(TEST_TENANT.getSpecification());
  }

  @Test
  public void testGetTenantUnknownTenantName() throws InvalidCommandException, UnsupportedEncodingException {
    execute(String.format(Constants.GET_TENANT_COMMAND, "test"));
    checkError();
  }

  @Test
  public void testDeleteTenantSuccess() throws InvalidCommandException, UnsupportedEncodingException {
    execute(String.format(Constants.GET_TENANT_COMMAND, TENANT));
    checkCommandOutput(TEST_TENANT.getSpecification());

    OUTPUT_STREAM.reset();

    execute(String.format(Constants.DELETE_TENANT_COMMAND, TENANT));
    execute(String.format(Constants.GET_TENANT_COMMAND, TENANT));
    checkError();
  }
}
