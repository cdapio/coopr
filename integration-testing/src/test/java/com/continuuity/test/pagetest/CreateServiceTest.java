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

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
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
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test GET v1/loom/services/service/<service-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateServiceTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @Test
  public void test_01_submitHadoopHdfsDatanode() throws Exception {
    globalDriver.get(Constants.SERVICE_CREATE_URI);
    Service service = EXAMPLE_READER.getServices(Constants.SERVICES_PATH).get("hadoop-hdfs-datanode");
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(service.getName());
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(service.getDescription());

    Select dependsOn = new Select(globalDriver.findElement(By.cssSelector("#inputRuntimeRequires")));
    WebElement addService = globalDriver.findElement(By.cssSelector("#add-service"));
    dependsOn.selectByVisibleText("base");
    addService.click();

    dependsOn.selectByVisibleText("hadoop-hdfs-namenode");
    addService.click();

    WebElement addAction = globalDriver.findElement(By.cssSelector("#add-action"));
    addAction.click();
    addAction.click();
    addAction.click();


    List<WebElement> actionEntries = globalDriver.findElements(By.cssSelector(".action-entry"));

    actionEntries.get(0).findElement(By.cssSelector(".inputCategory")).sendKeys("install");
    actionEntries.get(0).findElement(By.cssSelector(".inputType")).sendKeys("chef");
    actionEntries.get(0).findElement(By.cssSelector(".inputScript")).sendKeys(
      service.getProvisionerActions().get(ProvisionerAction.INSTALL).getFields().get("script"));
    String data = service.getProvisionerActions().get(ProvisionerAction.INSTALL).getFields().get("data");
    if (data != null) {
      actionEntries.get(0).findElement(By.cssSelector(".inputData")).sendKeys(data);
    }

    actionEntries.get(1).findElement(By.cssSelector(".inputCategory")).sendKeys("configure");
    actionEntries.get(1).findElement(By.cssSelector(".inputType")).sendKeys("chef");
    actionEntries.get(1).findElement(By.cssSelector(".inputScript")).sendKeys(
      service.getProvisionerActions().get(ProvisionerAction.CONFIGURE).getFields().get("script"));
    data = service.getProvisionerActions().get(ProvisionerAction.CONFIGURE).getFields().get("data");
    if (data != null) {
      actionEntries.get(1).findElement(By.cssSelector(".inputData")).sendKeys(data);
    }

    actionEntries.get(2).findElement(By.cssSelector(".inputCategory")).sendKeys("start");
    actionEntries.get(2).findElement(By.cssSelector(".inputType")).sendKeys("chef");
    actionEntries.get(2).findElement(By.cssSelector(".inputScript")).sendKeys(
      service.getProvisionerActions().get(ProvisionerAction.START).getFields().get("script"));
    data = service.getProvisionerActions().get(ProvisionerAction.START).getFields().get("data");
    if (data != null) {
      actionEntries.get(2).findElement(By.cssSelector(".inputData")).sendKeys(data);
    }

    actionEntries.get(3).findElement(By.cssSelector(".inputCategory")).sendKeys("stop");
    actionEntries.get(3).findElement(By.cssSelector(".inputType")).sendKeys("chef");
    actionEntries.get(3).findElement(By.cssSelector(".inputScript")).sendKeys(
      service.getProvisionerActions().get(ProvisionerAction.STOP).getFields().get("script"));
    data = service.getProvisionerActions().get(ProvisionerAction.STOP).getFields().get("data");
    if (data != null) {
      actionEntries.get(3).findElement(By.cssSelector(".inputData")).sendKeys(data);
    }

    globalDriver.findElement(By.cssSelector("#create-service-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.SERVICES_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_02_submitServiceError() throws Exception {
    globalDriver.get(Constants.SERVICE_CREATE_URI);
    assertFalse(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys("asdf");
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys("asdf");
    globalDriver.findElement(By.cssSelector("#create-service-form")).submit();
    assertTrue(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
