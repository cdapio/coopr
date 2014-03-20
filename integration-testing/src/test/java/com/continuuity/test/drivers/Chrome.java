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
package com.continuuity.test.drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/** Driver is a class for chrome browser.
 * executable binary file for driver in resources directory
 * if it throw mistake that is not executable check for mode of the file
 * and chmod it to be executable 
 * chmod a+x file
 * @author elmira
 *
 */
public class Chrome extends Driver {
  public Chrome() {

  }
  public WebDriver getDriver() {
    if (driver == null) {
      if (Global.OS_VERSION == Global.OS.MAC_OS) {
        String path = Global.properties.getProperty("chromeMac");
        System.setProperty("webdriver.chrome.driver", path);
      } else {
        String path = Global.properties.getProperty("chromeLinux");
        System.setProperty("webdriver.chrome.driver", path);
      }
      driver = new ChromeDriver();
    }
    return driver;
  }

}
