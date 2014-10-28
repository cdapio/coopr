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
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.test.Constants;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Set;

public class ProvisionerCommandsTest extends AbstractTest {

  @BeforeClass
  public static void beforeClass()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException {
    createCli(SUPERADMIN_ACCOUNT);
  }

  @Test
  public void testListProvisioners() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_PROVISIONERS_COMMAND);
    Set<Provisioner> resultSet =  getSetFromOutput(new TypeToken<Set<Provisioner>>() {}.getType());
    Set<Provisioner> expectedSet = Sets.newHashSet(TEST_PROVISIONER);
    Assert.assertEquals(expectedSet, resultSet);
  }

  @Test
  public void testListProvisionersWithoutPermissions()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException, InvalidCommandException {
    createCli(ADMIN_ACCOUNT);

    execute(Constants.LIST_PROVISIONERS_COMMAND);
    checkError();

    createCli(SUPERADMIN_ACCOUNT);
  }

  @Test
  public void testGetProvisioner() throws InvalidCommandException, UnsupportedEncodingException {
    execute(String.format(Constants.GET_PROVISIONER_COMMAND, PROVISIONER_ID));
    checkCommandOutput(TEST_PROVISIONER);
  }

  @Test
  public void testGetProvisionerUnknownProvisioner() throws InvalidCommandException, UnsupportedEncodingException {
    execute(String.format(Constants.GET_PROVISIONER_COMMAND, "test"));
    checkError();
  }
}
