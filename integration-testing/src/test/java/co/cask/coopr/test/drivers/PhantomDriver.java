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
package co.cask.coopr.test.drivers;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

/** Driver is a class for phantom browser (headless without GUI).
 * executable binary file for driver in resources directory
 * if it throw mistake that is not executable check for mode of the file
 * and chmod it to be executable 
 * chmod a+x file
 *
 */
public class PhantomDriver extends Driver {
  public PhantomDriver () {

  }
  public WebDriver getDriver() {
    String path;
    Global.OS osVersion = Global.detectOs();
    if (System.getProperty("sun.arch.data.model").contains("64")) {
      if (osVersion == Global.OS.LINUX) {
        path = Global.properties.getProperty("phantomLinux64");
      } else {
        path = Global.properties.getProperty("phantomMac64");
      }
    } else {
      if (osVersion == Global.OS.MAC_OS) {
        path = Global.properties.getProperty("phantomMac64");
      } else {
        path = Global.properties.getProperty("phantomLinux");
      }
    }
    System.setProperty("phantomjs.binary.path", path);
    driver = new CustomPhantomJSDriver();
    driver.manage().window().setSize(new Dimension(2500,2500));
    return this.driver;

  }
}
