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
package com.continuuity.test.page.CreatePage;

import com.continuuity.test.Constants;
import com.google.common.collect.Maps;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Map;

import static com.continuuity.test.drivers.Global.globalDriver;

/**
 * Get Hardwaretype instance data from the webpage.
 */
public class HardwaretypesInstancePage extends GenericPage {
  private static final By PROVIDER = By.name("inputProvider");
  private static final By FLAVOR = By.name("inputFlavor");

  public Map<String, String> getProviderMap() {
    Map<String, String> providerMap = Maps.newHashMap();
    List<WebElement> providers = globalDriver.findElements(PROVIDER);
    List<WebElement> flavors = globalDriver.findElements(FLAVOR);
    for (int i = 0; i < providers.size(); i++) {
      Select select = new Select(providers.get(i));
      providerMap.put(select.getFirstSelectedOption().getText(), flavors.get(i).getAttribute(Constants.VALUE));
    }
    return providerMap;
  }
}
