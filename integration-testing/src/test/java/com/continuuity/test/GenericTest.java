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
package com.continuuity.test;


import com.continuuity.test.drivers.Global;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;

import static com.continuuity.test.drivers.Global.driverWait;
import static com.continuuity.test.drivers.Global.globalDriver;

/** Generic Test is parent object for all tests classes
 * Created for common helpers methods
 *
 */
public class GenericTest {

  @BeforeClass
  public static void setUp() throws Exception {
    Global.getDriver();
    globalDriver.get(Constants.LOGIN_URL);
    Global.driverWait(1);
    globalDriver.findElement(By.cssSelector("#username")).sendKeys("admin");
    globalDriver.findElement(By.cssSelector("#password")).sendKeys("admin");
    globalDriver.findElement(By.cssSelector("#create-provider-form")).submit();
  }

  public String switchToNewTab(WebElement elemForClick, String url) {
    String oldTab = globalDriver.getWindowHandle();
    elemForClick.click();
    driverWait(5);
    ArrayList<String> newTab = new ArrayList<String>(globalDriver.getWindowHandles());
    newTab.remove(oldTab);
    // change focus to new tab
    globalDriver.switchTo().window(newTab.get(0));
    String newUrl = globalDriver.getCurrentUrl();
    globalDriver.close();
    // change focus back to old tab
    globalDriver.switchTo().window(oldTab);

    return newUrl;
  }
  public static void closeDriver() {
    globalDriver.close();
    globalDriver.quit();
    globalDriver = null;
  }

}
