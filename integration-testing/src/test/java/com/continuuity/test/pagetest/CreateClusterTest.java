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
package com.continuuity.test.pagetest;

import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.drivers.Global;
import com.continuuity.test.input.ClusterReader;
import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test GET v1/loom/user/clusters/create
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateClusterTest extends GenericTest {
  private static final ClusterReader CLUSTER_READER = new ClusterReader();

  @BeforeClass
  public static void runInitial() throws Exception{
    globalDriver.get(Constants.CLUSTER_CREATE_URL);
  }

  @Test
  public void test_01_createCluster () throws Exception {
    JsonObject cluster = CLUSTER_READER.getCreateCluster();
    globalDriver.findElement(By.cssSelector("#inputName")).sendKeys(cluster.get("name").getAsString());
    globalDriver.findElement(By.cssSelector("#inputNumMachines")).sendKeys(cluster.get("numMachines").getAsString());
    new Select(globalDriver.findElement(
      By.cssSelector("#inputConfiguration"))).selectByVisibleText(cluster.get("clusterTemplate").getAsString());
    globalDriver.findElement(By.cssSelector(".toggle-advanced-header")).click();
    globalDriver.findElement(By.cssSelector(".initial-days")).clear();
    globalDriver.findElement(By.cssSelector(".initial-days")).sendKeys("0");
    globalDriver.findElement(By.cssSelector("#create-cluster-form")).submit();
    Global.driverWait(1);
    assertEquals(Constants.USER_CLUSTERS, globalDriver.getCurrentUrl());
  }

  @Test
  public void test_02_submitClusterError() throws Exception {
    JsonObject cluster = CLUSTER_READER.getCreateCluster();
    globalDriver.get(Constants.CLUSTER_CREATE_URL);
    WebElement inputName = globalDriver.findElement(By.cssSelector("#inputName"));
    inputName.sendKeys("asdf");
    globalDriver.findElement(By.cssSelector("#create-cluster-form")).submit();
    assertTrue(globalDriver.findElement(By.cssSelector("#secondary-notification")).isDisplayed());
    new Select(globalDriver.findElement(By.cssSelector("#inputConfiguration"))).selectByVisibleText(
      cluster.get("clusterTemplate").getAsString());
    globalDriver.findElement(By.cssSelector("#create-cluster-form")).submit();
    assertTrue(globalDriver.findElement(By.cssSelector("#notification")).isDisplayed());
  }



  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
