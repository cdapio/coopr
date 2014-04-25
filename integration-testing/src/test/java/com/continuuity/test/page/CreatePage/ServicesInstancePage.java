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
package com.continuuity.test.page.CreatePage;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.test.Constants;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.continuuity.test.drivers.Global.globalDriver;

/**
 * Get Service instance data from the web page.
 */
public class ServicesInstancePage extends GenericPage {
  private static final By DEPENDSON = By.cssSelector("#inputDependsOn");
  private static final By CATEGORY = By.name("inputCategory");
  private static final By TYPE = By.name("inputType");
  private static final By SCRIPT = By.name("inputScript");
  private static final By DATA = By.name("inputData");
  private static final By SERVICE_ENTRIES = By.cssSelector(".service-entries");
  private static final By SERVICE_NAME = By.cssSelector(".service-name");

  public Set<String> getDependsOn() {
    Set<String> dependsOn = Sets.newHashSet();
    for (WebElement element : globalDriver.findElement(SERVICE_ENTRIES).findElements(SERVICE_NAME)) {
      dependsOn.add(element.getAttribute(Constants.INNER_HTML));
    }
    return dependsOn;
  }

  public Map<ProvisionerAction, ServiceAction> getProvisionerActions() {
    Map<ProvisionerAction, ServiceAction> provisionerActions = Maps.newHashMap();
    List<WebElement> categories = globalDriver.findElements(CATEGORY);
    List<WebElement> types = globalDriver.findElements(TYPE);
    List<WebElement> scripts = globalDriver.findElements(SCRIPT);
    List<WebElement> dataList = globalDriver.findElements(DATA);
    for (int i = 0; i < categories.size(); i++) {
      Select actionSelect = new Select(categories.get(i));
      ProvisionerAction action = ProvisionerAction.valueOf(actionSelect.getFirstSelectedOption()
                                                                        .getText().toUpperCase());
      String type = new Select(types.get(i)).getFirstSelectedOption().getText();
      String script = scripts.get(i).getAttribute(Constants.VALUE);
      String data = dataList.get(i).getAttribute(Constants.VALUE);
      Map<String, String> fields = Maps.newHashMap();
      if (script != null && !script.isEmpty()) {
        fields.put("script", script);
      }
      if (data != null && !data.isEmpty()) {
        fields.put("data", data);
      }
      provisionerActions.put(action, new ServiceAction(type, fields));
    }
    return provisionerActions;
  }
}
