/**
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
package co.cask.coopr.test.pagetest;

import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.test.Constants;
import co.cask.coopr.test.GenericTest;
import co.cask.coopr.test.drivers.Global;
import co.cask.coopr.test.input.ExampleReader;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static co.cask.coopr.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test GET /hardwaretypes/hardwaretype/<hardwaretype-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateHardwaretypeTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @Test
  public void test_01_submitSmall() throws Exception {
    globalDriver.get(Constants.HARDWARETYPE_CREATE_URI);
    HardwareType hardwareType = EXAMPLE_READER.getHardwareTypes(Constants.HARDWARETYPES_PATH).get("small");

    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(hardwareType.getName());

    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(hardwareType.getDescription());

    WebElement addProvider = globalDriver.findElement(By.cssSelector("#add-provider"));
    addProvider.click();
    addProvider.click();

    List<WebElement> providerEntries = globalDriver.findElements(By.cssSelector(".provider-entry"));

    providerEntries.get(0).findElement(By.cssSelector("select")).sendKeys("joyent");
    providerEntries.get(0).findElement(By.cssSelector("input")).sendKeys(
      hardwareType.getProviderMap().get("joyent").get("flavor"));

    providerEntries.get(1).findElement(By.cssSelector("select")).sendKeys("rackspace");
    providerEntries.get(1).findElement(By.cssSelector("input")).sendKeys(
      hardwareType.getProviderMap().get("rackspace").get("flavor"));

    providerEntries.get(2).findElement(By.cssSelector("select")).sendKeys("openstack");
    providerEntries.get(2).findElement(By.cssSelector("input")).sendKeys(
      hardwareType.getProviderMap().get("openstack").get("flavor"));


    globalDriver.findElement(By.cssSelector("#create-hardwaretype-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.HARDWARETYPES_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_02_submitFail() throws Exception {
    globalDriver.get(Constants.HARDWARETYPE_CREATE_URI);
    assertFalse(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys("asdf");

    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys("asdfsadf");

    globalDriver.findElement(By.cssSelector("#create-hardwaretype-form")).submit();
    Global.driverWait(1);
    assertTrue(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
