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
package com.continuuity.test.pagetest;

import com.continuuity.loom.spec.ImageType;
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

import java.util.List;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test GET v1/loom/imagetypes/imagetype/<imagetype-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateImagetypeTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @Test
  public void test_01_submitCentOs6() throws Exception {
    globalDriver.get(Constants.IMAGETYPE_CREATE_URI);
    ImageType imageType = EXAMPLE_READER.getImageTypes(Constants.IMAGETYPES_PATH).get("centos6");

    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(imageType.getName());

    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(imageType.getDescription());

    WebElement addProvider = globalDriver.findElement(By.cssSelector("#add-provider"));
    addProvider.click();

    List<WebElement> providerEntries = globalDriver.findElements(By.cssSelector(".provider-entry"));
    providerEntries.get(0).findElement(By.cssSelector("select")).sendKeys("joyent");
    providerEntries.get(0).findElement(By.cssSelector("input[name=inputImage]")).sendKeys(
      imageType.getProviderMap().get("joyent").get("image"));

    providerEntries.get(1).findElement(By.cssSelector("select")).sendKeys("rackspace");
    providerEntries.get(1).findElement(By.cssSelector("input[name=inputImage]")).sendKeys(
      imageType.getProviderMap().get("rackspace").get("image"));

    globalDriver.findElement(By.cssSelector("#create-imagetype-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.IMAGETYPES_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_02_submitFail() throws Exception {
    globalDriver.get(Constants.IMAGETYPE_CREATE_URI);
    assertFalse(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys("asdf");

    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys("asdfsadf");

    globalDriver.findElement(By.cssSelector("#create-imagetype-form")).submit();
    Global.driverWait(1);
    assertTrue(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
