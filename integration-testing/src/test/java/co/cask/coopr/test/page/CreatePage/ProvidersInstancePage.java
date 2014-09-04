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

import co.cask.coopr.test.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static co.cask.coopr.test.drivers.Global.globalDriver;

/**
 * Get Provider instance data from the web page.
 */
public class ProvidersInstancePage extends GenericPage {
  private static final By PROVISIONER = By.cssSelector("#provisioner-select");
  private static final By JOYENT_USERNAME = By.cssSelector("#joyent_username");
  private static final By JOYENT_KEY_NAME = By.cssSelector("#joyent_keyname");
  private static final By JOYENT_KEY_FILE = By.cssSelector("#joyent_keyfile");
  private static final By JOYENT_API_URL = By.cssSelector("#joyent_api_url");
  private static final By JOYENT_VERSION = By.cssSelector("#joyent_version");

  public String getProvisioner() {
    Select select = new Select(globalDriver.findElement(PROVISIONER));
    return select.getFirstSelectedOption().getText();
  }

  public String getUsername() {
    return globalDriver.findElement(JOYENT_USERNAME).getAttribute(Constants.VALUE);
  }

  public String getKeyname() {
    return globalDriver.findElement(JOYENT_KEY_NAME).getAttribute(Constants.VALUE);
  }

  public String getKeyfile() {
    return globalDriver.findElement(JOYENT_KEY_FILE).getAttribute(Constants.VALUE);
  }

  public String getApiUrl() {
    return globalDriver.findElement(JOYENT_API_URL).getAttribute(Constants.VALUE);
  }

  public String getVersion() {
    return globalDriver.findElement(JOYENT_VERSION).getAttribute(Constants.VALUE);
  }
}
