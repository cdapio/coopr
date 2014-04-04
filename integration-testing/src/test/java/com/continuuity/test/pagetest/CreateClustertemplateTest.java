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

import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.drivers.Global;
import com.continuuity.test.input.ExampleReader;
import com.google.common.collect.Sets;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test GET v1/loom/clustertemplates/clustertemplate/<clustertemplate-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateClustertemplateTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @Test
  public void test_01_submitHadoopDistributed() throws Exception {
    globalDriver.get(Constants.CLUSTERTEMPLATE_CREATE_URI);
    ClusterTemplate clusterTemplate = EXAMPLE_READER.getClusterTemplate(
      Constants.CLUSTERTEMPLATE_PATH).get("hadoop-distributed");
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys(clusterTemplate.getName());
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys(clusterTemplate.getDescription());

    List<WebElement> leaseDurationInputs = globalDriver.findElements(
      By.cssSelector("#lease-duration-table .number-input"));

    for (WebElement input : leaseDurationInputs) {
      input.sendKeys("0");
    }

    globalDriver.findElement(By.cssSelector("#general .next-tab-button")).click();
    Global.driverWait(1);

    Set<String> compatibleServices = clusterTemplate.getCompatibilities().getServices();
    Select serviceSelect = new Select(globalDriver.findElement(By.cssSelector("#compatibility .service-select")));
    WebElement addServiceBtn = globalDriver.findElement(By.cssSelector("#compatibility .add-service-btn"));
    for (String service : compatibleServices) {
      serviceSelect.selectByVisibleText(service);
      addServiceBtn.click();
    }

    Set<String> compatibleHardwaretypes = clusterTemplate.getCompatibilities().getHardwaretypes();
    Select hardwaretypeSelect = new Select(globalDriver.findElement(
      By.cssSelector("#compatibility .hardwaretype-select")));
    WebElement addHardwaretypeBtn = globalDriver.findElement(By.cssSelector("#compatibility .add-hardwaretype-btn"));
    for (String hardwaretype : compatibleHardwaretypes) {
      hardwaretypeSelect.selectByVisibleText(hardwaretype);
      addHardwaretypeBtn.click();
    }

    Set<String> compatibleImagetypes = clusterTemplate.getCompatibilities().getImagetypes();
    Select imagetypeSelect = new Select(globalDriver.findElement(
      By.cssSelector("#compatibility .imagetype-select")));
    WebElement addImagetypeBtn = globalDriver.findElement(By.cssSelector("#compatibility .add-imagetype-btn"));
    for (String imagetype : compatibleImagetypes) {
      imagetypeSelect.selectByVisibleText(imagetype);
      addImagetypeBtn.click();
    }

    globalDriver.findElement(By.cssSelector("#compatibility .next-tab-button")).click();
    Global.driverWait(1);

    new Select(globalDriver.findElement(By.cssSelector("#defaults .provider-select"))).selectByVisibleText(
      clusterTemplate.getClusterDefaults().getProvider());

    Select defaulthardwaretype = new Select(globalDriver.findElement(By.cssSelector("#defaults .hardwaretype-select")));
    Select defaultimagetype = new Select(globalDriver.findElement(By.cssSelector("#defaults .imagetype-select")));
    List<WebElement> defaultHardwareOptions = defaulthardwaretype.getOptions();
    List<WebElement> defaultImageOptions = defaultimagetype.getOptions();

    Set<String> defaultHardwareItems = Sets.newHashSet();
    for (WebElement item : defaultHardwareOptions) {
      if (item.getText().length() != 0) {
        defaultHardwareItems.add(item.getText());
      }

    }

    Set<String> defaultImageItems = Sets.newHashSet();
    for (WebElement item : defaultImageOptions) {
      if (item.getText().length() != 0) {
        defaultImageItems.add(item.getText());
      }
    }

    defaulthardwaretype.selectByVisibleText(clusterTemplate.getClusterDefaults().getHardwaretype());
    defaultimagetype.selectByVisibleText(clusterTemplate.getClusterDefaults().getImagetype());

    globalDriver.findElement(By.cssSelector("#inputDnsSuffix")).sendKeys(
      clusterTemplate.getClusterDefaults().getDnsSuffix().toString());

    globalDriver.findElement(By.cssSelector("#inputConfig")).sendKeys(
      clusterTemplate.getClusterDefaults().getConfig().toString());

    Set<String> defaultServices = clusterTemplate.getClusterDefaults().getServices();

    Select defaultServiceSelect = new Select(globalDriver.findElement(By.cssSelector("#defaults .service-select")));
    List<WebElement> defaultServiceSelectOptions = defaultServiceSelect.getOptions();

    Set<String> defaultServiceSelectItems = Sets.newHashSet();
    for (WebElement item : defaultServiceSelectOptions) {
      if (item.getText().length() != 0) {
        defaultServiceSelectItems.add(item.getText());
      }
    }

    assertEquals(compatibleHardwaretypes, defaultHardwareItems);
    assertEquals(compatibleImagetypes, defaultImageItems);
    assertEquals(defaultServices, defaultServiceSelectItems);

    WebElement addServicebtn = globalDriver.findElement(By.cssSelector("#defaults .add-service-btn"));
    for (String service : defaultServices) {
      defaultServiceSelect.selectByVisibleText(service);
      addServicebtn.click();
    }

    globalDriver.findElement(By.cssSelector("#defaults .next-tab-button")).click();

    Global.driverWait(1);
    globalDriver.findElement(By.cssSelector(".add-must-coexist-group")).click();
    Global.driverWait(1);

    Set<String> mustcoexistgroup1 = clusterTemplate.getConstraints().getLayoutConstraint()
      .getServicesThatMustCoexist().iterator().next();
    Select mustCoExistSelect = new Select(globalDriver.findElement(
      By.cssSelector("#must-coexist-modal .must-coexist-select")));
    addServiceBtn = globalDriver.findElement(By.cssSelector("#must-coexist-modal .add-service-btn"));
    for (WebElement close : globalDriver.findElements(By.cssSelector("#cant-coexist-modal .service-delete"))) {
      close.click();
    }

    for (String serviceEntry : mustcoexistgroup1) {
      mustCoExistSelect.selectByVisibleText(serviceEntry);
      addServiceBtn.click();
    }
    globalDriver.findElement(By.cssSelector("#must-coexist-modal .add-group-btn")).click();
    Global.driverWait(1);

    Set<Set<String>> cantcoexistgroups = clusterTemplate.getConstraints().getLayoutConstraint()
      .getServicesThatMustNotCoexist();
    for (Set<String> cantcoexistentry : cantcoexistgroups) {
      globalDriver.findElement(By.cssSelector(".add-cant-coexist-group")).click();
      Global.driverWait(1);
      for (WebElement close : globalDriver.findElements(By.cssSelector("#cant-coexist-modal .service-delete"))) {
        close.click();
      }
      Select cantcoexistSelect = new Select(globalDriver.findElement(
        By.cssSelector("#cant-coexist-modal .cant-coexist-select")));
      addServiceBtn = globalDriver.findElement(By.cssSelector("#cant-coexist-modal .add-service-btn"));
      for (String serviceEntry : cantcoexistentry) {
        cantcoexistSelect.selectByVisibleText(serviceEntry);
        addServiceBtn.click();
      }
      globalDriver.findElement(By.cssSelector("#cant-coexist-modal .add-group-btn")).click();
      Global.driverWait(1);
    }

    Map<String, ServiceConstraint> serviceConstraints = clusterTemplate.getConstraints().getServiceConstraints();
    Select serviceNameSelect = new Select(globalDriver.findElement(
      By.cssSelector("#service-constraint-modal .service-select")));
    Select serviceHardwaretypeSelect = new Select(globalDriver.findElement(
      By.cssSelector("#service-constraint-modal .hardwaretype-select")));
    addHardwaretypeBtn = globalDriver.findElement(By.cssSelector("#service-constraint-modal .add-hardwaretype-btn"));
    Select serviceImagetypeSelect = new Select(globalDriver.findElement(
      By.cssSelector("#service-constraint-modal .imagetype-select")));
    addImagetypeBtn = globalDriver.findElement(By.cssSelector("#service-constraint-modal .add-imagetype-btn"));
    WebElement minConstraint = globalDriver.findElement(By.cssSelector("#service-constraint-modal .constraint-min"));
    WebElement maxConstraint = globalDriver.findElement(By.cssSelector("#service-constraint-modal .constraint-max"));
    WebElement addServiceConstraintBtn = globalDriver.findElement(By.cssSelector(".add-service-constraint-btn"));
    WebElement addServiceConstraintModal = globalDriver.findElement(By.cssSelector(".add-serviceconstraint-modal"));
    for (Map.Entry<String, ServiceConstraint> entry : serviceConstraints.entrySet()) {
      addServiceConstraintModal.click();
      Global.driverWait(1);
      minConstraint.clear();
      maxConstraint.clear();
      String key = entry.getKey();
      ServiceConstraint constraint = entry.getValue();
      serviceNameSelect.selectByVisibleText(key);

      if (constraint.getRequiredHardwareTypes() != null) {
        for (String hardwaretype : constraint.getRequiredHardwareTypes()) {
          serviceHardwaretypeSelect.selectByVisibleText(hardwaretype);
          addHardwaretypeBtn.click();
        }
      }

      if (constraint.getRequiredImageTypes() != null) {
        for (String imagetype : constraint.getRequiredImageTypes()) {
          serviceImagetypeSelect.selectByVisibleText(imagetype);
          addImagetypeBtn.click();
        }
      }

      minConstraint.sendKeys(String.valueOf(constraint.getMinCount()));
      maxConstraint.sendKeys(String.valueOf(constraint.getMaxCount()));
      addServiceConstraintBtn.click();
      Global.driverWait(1);
    }
    globalDriver.findElement(By.cssSelector("#create-clustertemplate-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.CLUSTERTEMPLATES_URL, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_02_submitServiceError() throws Exception {
    globalDriver.get(Constants.CLUSTERTEMPLATE_CREATE_URI);
    assertFalse(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys("asdf");
    WebElement inputDescription = globalDriver.findElement(By.cssSelector("#inputDescription"));
    inputDescription.sendKeys("asdf");
    globalDriver.findElement(By.cssSelector("#create-clustertemplate-form")).submit();
    assertTrue(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
