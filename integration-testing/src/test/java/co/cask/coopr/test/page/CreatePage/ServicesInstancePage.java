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
package co.cask.coopr.test.page.CreatePage;

import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.test.Constants;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static co.cask.coopr.test.drivers.Global.globalDriver;

/**
 * Get Service instance data from the web page.
 */
public class ServicesInstancePage extends GenericPage {
  private static final By DEPENDSON = By.cssSelector("#inputDependsOn");
  private static final By CATEGORY = By.name("inputCategory");
  private static final By TYPE = By.name("inputType");
  private static final By SERVICE_ENTRIES = By.cssSelector(".service-entries");
  private static final By SERVICE_NAME = By.cssSelector(".service-name");

  public Set<String> getDependsOn() {
    Set<String> dependsOn = Sets.newHashSet();
    for (WebElement element : globalDriver.findElements(SERVICE_ENTRIES).get(4).findElements(SERVICE_NAME)) {
      dependsOn.add(element.getAttribute(Constants.INNER_HTML));
    }
    return dependsOn;
  }

  public Map<ProvisionerAction, ServiceAction> getProvisionerActions() {
    Map<ProvisionerAction, ServiceAction> provisionerActions = Maps.newHashMap();
    List<WebElement> actions = globalDriver.findElements(By.cssSelector(".action-entry" ));
    for (WebElement actionEntry : actions) {
      Select actionSelect = new Select(actionEntry.findElement(CATEGORY));
      ProvisionerAction category = ProvisionerAction.valueOf(actionSelect.getFirstSelectedOption()
                                                             .getText().toUpperCase());
      Select actionType = new Select(actionEntry.findElement(TYPE));
      String type = actionType.getFirstSelectedOption().getText();
      List<WebElement> automatorDetails = actionEntry.findElements(By.cssSelector(".automator-field input"));
      Map<String, String> fields = Maps.newHashMap();
      for (WebElement automator : automatorDetails) {
        String value = automator.getAttribute(Constants.VALUE);
        if (!value.equals("")) {
          fields.put(automator.getAttribute("name"), automator.getAttribute(Constants.VALUE));
        }
      }
      provisionerActions.put(category, new ServiceAction(type, fields));
    }
    return provisionerActions;
  }
}
