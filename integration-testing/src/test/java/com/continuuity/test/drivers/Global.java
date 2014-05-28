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
 *
 */
public class Global {
  public static WebDriver globalDriver;
  public static Properties properties;
  public static boolean read = readProperties();
  public static  final OS OS_VERSION = detectOs();
  
  static {
    chooseDriver();
  }

  public static void driverWait(int sec) {
    try {
      Thread.sleep(sec * 1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    if (osVersion.contains("mac")) {
      return OS.MAC_OS;
    } else if (osVersion.contains("nux")) {
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

  public static void waitForLoading() {
    Global.driverWait(15);
  }

  public enum OS {
    MAC_OS, LINUX
  }
   
}

