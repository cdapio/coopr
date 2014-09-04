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
package co.cask.coopr.test.pagetest;

import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.test.Constants;
import co.cask.coopr.test.GenericTest;
import co.cask.coopr.test.TestUtil;
import co.cask.coopr.test.input.ExampleReader;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebElement;

import java.util.Map;

import static co.cask.coopr.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test GET main page.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IndexTest extends GenericTest {
  private static final String HREF = "href";
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();
  private static final String CLUSTERTEMPLATE = "Catalog";

  @BeforeClass
  public static void runInitial() throws  Exception {
    globalDriver.get(Constants.INDEX_URL);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_08_catalogPanel() throws Exception {
    // Verify panel title
    WebElement servicesPanel = TEST_UTIL.getPanelHead(CLUSTERTEMPLATE);
    assertEquals("Catalog", servicesPanel.getText());
    assertEquals(Constants.INDEX_URL + Constants.CLUSTERTEMPLATE_HREF, servicesPanel.getAttribute(HREF));

    Map<String, ClusterTemplate> nameServices = EXAMPLE_READER.getClusterTemplate(Constants.CLUSTERTEMPLATE_PATH);

    // Verify the table in the panel
    WebElement table = TEST_UTIL.getTable(Constants.CLUSTERTEMPLATE_TABLE);

    assertEquals("The number of rows is not equal to the number of providers in the examples.",
                 nameServices.size(), TEST_UTIL.getRowCount(table));

    for (int row = 0; row < TEST_UTIL.getRowCount(table); row++) {
      String name = TEST_UTIL.getTd(table, row, 0);
      assertTrue("Name is not in the examples.", nameServices.containsKey(name));
      assertEquals("Description is not equal.", nameServices.get(name).getDescription(),
                     TEST_UTIL.getTd(table, row, 1));
    }
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
