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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/** Global is a class for keeping global constants.
 * constants are imported using static import.
 * globalDriver is an instance of a driver (singleton)
 * // variants of drivers
 * //driver = new FireFox().getDriver();
 * //driver = new Chrome().getDriver();
 * //driver = new PhantomDriver().getDriver();
 * 
 * ROOT_URL is root url of application
 * ACCOUNT_URL is url that should appear when click on "Account"
 * SUPPORT_URL is url that should appear when click on "Support"
 * TERMS_URL is url that should appear when click on "Terms and Privacy"
 * CONTACT_URL is url that should appear when click on "Contact"
 * @author elmira
 *
 */
public class Global {
  public static WebDriver globalDriver;
  public static Properties properties;
  public static boolean read = readProperties();
  public static final String ROOT_URL = Global.properties.getProperty("rootUrl");
  public static final String ACCOUNT_URL = Global.properties.getProperty("accountUrl");
  public static final String SUPPORT_URL = Global.properties.getProperty("supportUrl");
  public static final String TERMS_URL = Global.properties.getProperty("termsUrl");
  public static final String CONTACT_URL = Global.properties.getProperty("contactUrl");;
  public static  final OS OS_VERSION = detectOs();
  public static final boolean RUN_SUITE = Boolean.parseBoolean(Global.properties.getProperty("runSuite"));
  
  static {
    chooseDriver();
  }
  public static void assignUrls() {
    
  }
  public static void driverWait(int sec) {
    try {
      Thread.sleep(sec * 1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //globalDriver.manage().timeouts().implicitlyWait(sec, TimeUnit.SECONDS);
  }
  public static boolean readProperties() {
    properties = new Properties();
    try {
      String pfile = "resources/properties/Global.properties";
      FileInputStream in = new FileInputStream(pfile);
      properties.load(in);
      in.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
    return true;
  }
  public static void getDriver() {
    if (globalDriver == null) {
      chooseDriver();
    }
  }
  public static OS detectOs() {
    String osVersion = System.getProperty("os.name").toLowerCase();
    if (osVersion.indexOf("mac") >= 0) {
      return OS.MAC_OS;
    } else if (osVersion.indexOf("nux") >= 0) {
      return OS.LINUX;
    } else {
      System.err.println("Wrong os");
      System.exit(1);
      return null;
    }
      
  }
  public static void chooseDriver() {
    String driverType = Global.properties.getProperty("driverType");
    if (driverType.equals("Chrome")) {
      globalDriver = new Chrome().getDriver();
    } else {
      globalDriver = new PhantomDriver().getDriver();
    }
  }
  public static void waitForLoading(String url) {
    Global.driverWait(15);
    /*
    try {
      globalDriver.get(url);
      WebElement message = (new WebDriverWait(globalDriver, 5))
          .until(new ExpectedCondition<WebElement>(){
            public WebElement apply(WebDriver d) {
              return  globalDriver.findElement(By.id("load-app-trigger"));
            }});
    } catch (NoSuchElementException e) {
      fail("Element not found!!");
      e.printStackTrace();
    } 
    */
  }
  public static void waitForLoading() {
    Global.driverWait(15);
    /*
    try {
      WebElement message = (new WebDriverWait(globalDriver, 15))
          .until(new ExpectedCondition<WebElement>(){
            public WebElement apply(WebDriver d) {
              WebElement elem = globalDriver.findElement(By.id("load-app-trigger"));
              //System.out.println(elem.getText());
              return  elem;
            }});
    } catch (NoSuchElementException e) {
      fail("Element not found!!");
      e.printStackTrace();
    } 
    */
  }
 /**
  * enum class for detection running os
  * @author elmira
  *
  */
  public enum OS {
    MAC_OS, LINUX
  }
   
}

