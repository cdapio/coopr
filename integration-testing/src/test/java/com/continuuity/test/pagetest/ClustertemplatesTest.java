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

import com.continuuity.loom.admin.ClusterTemplate;
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
 * Test GET v1/loom/clustertemplates
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClustertemplatesTest extends GenericTest {
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @BeforeClass
  public static void runInitial() throws Exception {
    globalDriver.get(Constants.CLUSTERTEMPLATES_URL);
  }

  @Test
  public void test_01_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_02_Title() {
    assertEquals("Catalog", globalDriver.findElement(Constants.TITLE_BY).getText());
  }

  @Test
  public void test_03_clustertemplatesPanel() throws Exception {
    Map<String, ClusterTemplate> nameProvider = EXAMPLE_READER.getClusterTemplate(Constants.CLUSTERTEMPLATE_PATH);

    // Verify the table in the panel
    WebElement table = TEST_UTIL.getTable(Constants.TABLE);

    assertEquals("The number of rows is not equal to the number of providers in the examples.",
                 nameProvider.size(), TEST_UTIL.getRowCount(table));

    for (int row = 0; row < TEST_UTIL.getRowCount(table); row++) {
      String name = TEST_UTIL.getTd(table, row, 0);
      if (!name.equals(Constants.DELTEST)) {
        assertTrue("Name is not correct.", nameProvider.containsKey(name));
        assertEquals("Description is not correct.", nameProvider.get(name).getDescription().toString(),
                   TEST_UTIL.getTd(table, row, 1));
      }
    }
  }

  @Test
  public void test_04_topmenu() {
    ImmutableSet<String> expectedServiceSet = ImmutableSet.of("hadoop-hbase-distributed", "lamp", "hadoop-distributed",
                 "hadoop-singlenode", "reactor-singlenode");
    String uriPrefix = Constants.CLUSTERTEMPLATES_URL + "/clustertemplate/";
    assertEquals("The list of the topmenu is not correct.", expectedServiceSet,
                 TEST_UTIL.getTopList(globalDriver));
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedServiceSet, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @Test
  public void test_05_testClusterTemplatesSelected() {
    assertEquals("Cluster templates menu item is not selected",
                 "overview active nav-item", TEST_UTIL.getHTMLClasses("#nav-clustertemplates-container"));
  }

  @Test
  public void test_06_deleteClustertemplates() {
    TEST_UTIL.genericDeleteTest(Constants.CLUSTERTEMPLATES_URL);
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
