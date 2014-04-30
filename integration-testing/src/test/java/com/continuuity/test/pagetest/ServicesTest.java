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
package com.continuuity.test.pagetest;

import com.continuuity.loom.admin.Service;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.TestUtil;
import com.continuuity.test.input.ExampleReader;
import com.google.common.collect.ImmutableSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test GET v1/loom/services
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServicesTest extends GenericTest {
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @BeforeClass
  public static void runInitial() throws  Exception {
    globalDriver.get(Constants.SERVICES_URL);
  }

  @Test
  public void test_01_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_02_Title() {
    assertEquals("Services", globalDriver.findElement(Constants.TITLE_BY).getText());
  }

  @Test
  public void test_03_ServicesPanel() throws Exception {
    Map<String, Service> nameProvider = EXAMPLE_READER.getServices(Constants.SERVICES_PATH);

    // Verify the table in the panel
    WebElement table = TEST_UTIL.getTable(Constants.TABLE);

    assertEquals("The number of services is not correct.", nameProvider.size(), TEST_UTIL.getRowCount(table));

    for (int row = 0; row < TEST_UTIL.getRowCount(table); row++) {
      String name = TEST_UTIL.getTd(table, row, 0);
      if (!name.equals(Constants.DELTEST)) {
        assertTrue("Name is not correct.", nameProvider.containsKey(name));
        assertEquals("Description is not correct.", nameProvider.get(name).getDescription(),
                     TEST_UTIL.getTd(table, row, 1));
        assertEquals("Depends on is not correct", TEST_UTIL.convertSetToString(
                     nameProvider.get(name).getDependencies().getRuntime().getRequires()),
                     TEST_UTIL.getTd(table, row, 2));
      }
    }
  }

  @Test
  public void test_04_topmenu() {
    ImmutableSet<String> expectedServiceSet = ImmutableSet.of("reactor", "haproxy", "fail2ban",
                "mysql-server", "zookeeper-server", "apache-httpd",
                "hadoop-yarn-resourcemanager", "php", "base", "hbase-regionserver", "hadoop-yarn-nodemanager",
                "hadoop-hdfs-datanode", "hadoop-hdfs-namenode", "hbase-master", "nodejs");
    String uriPrefix = Constants.SERVICES_URL + "/service/";
    assertEquals("The list of the topmenu is not correct.", expectedServiceSet, TEST_UTIL.getTopList(globalDriver));
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedServiceSet, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @Test
  public void test_05_testServicesSelected() {
    assertEquals("Services menu item is not selected", "active nav-item", TEST_UTIL.getHTMLClasses("#nav-services-container"));
  }

  @Test
  public void test_06_deleteService() {
    TEST_UTIL.genericDeleteTest(Constants.SERVICES_URL);
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
