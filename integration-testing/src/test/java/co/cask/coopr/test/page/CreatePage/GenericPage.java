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
package co.cask.coopr.test.page.CreatePage;

import co.cask.coopr.test.Constants;
import org.openqa.selenium.By;

import static co.cask.coopr.test.drivers.Global.globalDriver;

/**
 * Get common infomation from pages.
 */
public class GenericPage {
  private static final By INPUT_NAME = By.cssSelector("#inputName");
  private static final By INPUT_DESCRIPTION = By.cssSelector("#inputDescription");

  public String getInputName() {
    return globalDriver.findElement(INPUT_NAME).getAttribute(Constants.VALUE);
  }

  public String getDescription() {
    return globalDriver.findElement(INPUT_DESCRIPTION).getAttribute(Constants.VALUE);
  }
}
