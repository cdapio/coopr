/**
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
package com.continuuity.test.pagetest;

import com.continuuity.loom.admin.Provider;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.drivers.Global;
import com.continuuity.test.input.ExampleReader;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test GET v1/loom/providers/provider/<provider-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateProviderTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @Test
  public void test_01_submitJoyent() throws Exception {
    globalDriver.get(Constants.PROVIDER_CREATE_URI);
    Provider joyent = EXAMPLE_READER.getProviders(Constants.PROVIDERS_PATH).get("joyent");

    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(joyent.getName());

    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(joyent.getDescription());

    WebElement inputProvisioner = globalDriver.findElement(By.cssSelector("#provisioner-select"));
    inputProvisioner.sendKeys(joyent.getProviderType().toString().toLowerCase());

    WebElement joyentUsername = globalDriver.findElement(By.cssSelector("#joyent_username"));
    joyentUsername.sendKeys(joyent.getProvisionerData().get("auth").get("joyent_username"));

    WebElement joyentKeyname = globalDriver.findElement(By.cssSelector("#joyent_keyname"));
    joyentKeyname.sendKeys(joyent.getProvisionerData().get("auth").get("joyent_keyname"));

    WebElement joyentKeyfile = globalDriver.findElement(By.cssSelector("#joyent_keyfile"));
    joyentKeyfile.sendKeys(joyent.getProvisionerData().get("auth").get("joyent_keyfile"));

    WebElement joyentApiurl = globalDriver.findElement(By.cssSelector("#joyent_api_url"));
    joyentApiurl.sendKeys(joyent.getProvisionerData().get("auth").get("joyent_api_url"));

    WebElement joyentVersion = globalDriver.findElement(By.cssSelector("#joyent_version"));
    joyentVersion.sendKeys(joyent.getProvisionerData().get("auth").get("joyent_version"));

    globalDriver.findElement(By.cssSelector("#create-provider-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.PROVIDERS_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_02_submitOpenstack() throws Exception {
    globalDriver.get(Constants.PROVIDER_CREATE_URI);
    Provider openstack = EXAMPLE_READER.getProviders(Constants.PROVIDERS_PATH).get("openstack");
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(openstack.getName());
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(openstack.getDescription());
    WebElement inputProvisioner = globalDriver.findElement(By.cssSelector("#provisioner-select"));
    inputProvisioner.sendKeys(openstack.getProviderType().toString().toLowerCase());

    WebElement username = globalDriver.findElement(By.cssSelector("#openstack_username"));
    username.sendKeys(openstack.getProvisionerData().get("auth").get("openstack_username"));
    WebElement password = globalDriver.findElement(By.cssSelector("#openstack_password"));
    password.sendKeys(openstack.getProvisionerData().get("auth").get("openstack_password"));
    WebElement tenant = globalDriver.findElement(By.cssSelector("#openstack_tenant"));
    tenant.sendKeys(openstack.getProvisionerData().get("auth").get("openstack_tenant"));
    WebElement apiurl = globalDriver.findElement(By.cssSelector("#openstack_auth_url"));
    apiurl.sendKeys(openstack.getProvisionerData().get("auth").get("openstack_auth_url"));
    WebElement keyid = globalDriver.findElement(By.cssSelector("#openstack_ssh_key_id"));
    keyid.sendKeys(openstack.getProvisionerData().get("auth").get("openstack_ssh_key_id"));
    WebElement identityfile = globalDriver.findElement(By.cssSelector("#openstack-auth-fields #identity_file"));
    identityfile.sendKeys(openstack.getProvisionerData().get("auth").get("identity_file"));
    globalDriver.findElement(By.cssSelector("#create-provider-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.PROVIDERS_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_03_submitRacksapce() throws Exception {
    globalDriver.get(Constants.PROVIDER_CREATE_URI);
    Provider rackspace = EXAMPLE_READER.getProviders(Constants.PROVIDERS_PATH).get("rackspace");
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(rackspace.getName());
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(rackspace.getDescription());
    WebElement inputProvisioner = globalDriver.findElement(By.cssSelector("#provisioner-select"));
    inputProvisioner.sendKeys(rackspace.getProviderType().toString().toLowerCase());

    WebElement username = globalDriver.findElement(By.cssSelector("#rackspace_username"));
    username.sendKeys(rackspace.getProvisionerData().get("auth").get("rackspace_username"));
    WebElement password = globalDriver.findElement(By.cssSelector("#rackspace_api_key"));
    password.sendKeys(rackspace.getProvisionerData().get("auth").get("rackspace_api_key"));

    globalDriver.findElement(By.cssSelector("#create-provider-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.PROVIDERS_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_04_submitProviderError() throws Exception {
    globalDriver.get(Constants.PROVIDER_CREATE_URI);
    assertFalse(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys("asdf");
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys("asdf");
    // Submit form without provisioner field.
    globalDriver.findElement(By.cssSelector("#create-provider-form")).submit();
    assertTrue(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
    String errText = globalDriver.findElement(By.cssSelector("#notification")).getText();
    assertEquals("Provider type empty.", errText);

    // Submit form after selecting provisioner field.
    WebElement inputProvisioner = globalDriver.findElement(By.cssSelector("#provisioner-select"));
    inputProvisioner.sendKeys("joyent");
    globalDriver.findElement(By.cssSelector("#create-provider-form")).submit();
    errText = globalDriver.findElement(By.cssSelector("#notification")).getText();
    assertEquals("Route handler not available.", errText);
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
