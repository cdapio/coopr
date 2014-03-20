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

import com.continuuity.loom.admin.ImageType;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.TestUtil;
import com.continuuity.test.drivers.Global;
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
 * Test GET v1/loom/imagetypes
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImagetypesTest extends GenericTest {
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();

  @BeforeClass
  public static void setUp(){
    Global.getDriver();
    globalDriver.get(Constants.IMAGETYPES_URL);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_04_Title() {
    assertEquals("Image types", globalDriver.findElement(Constants.TITLE_BY).getText());
  }

  @Test
  public void test_05_imagetypesPanel() throws Exception {
    Map<String, ImageType> nameProvider = EXAMPLE_READER.getImageTypes(Constants.IMAGETYPES_PATH);

    // Verify the table in the panel
    WebElement table = TEST_UTIL.getTable(Constants.TABLE);

    assertEquals("The number of rows is not equal to the number of providers in the examples.",
                 nameProvider.size(), TEST_UTIL.getRowCount(table));

    for (int row = 0; row < TEST_UTIL.getRowCount(table); row++) {
      String name = TEST_UTIL.getTd(table, row, 0);
      if (!name.equals(Constants.DELTEST)) {
        assertTrue("Name is not correct.", nameProvider.containsKey(name));
        assertEquals("Description is not correct.", nameProvider.get(name).getDescription(),
                     TEST_UTIL.getTd(table, row, 1));
      }
    }
  }

  @Test
  public void test_06_topmenu() {
    assertEquals("The list of the topmenu is not correct.", ImmutableSet.of("centos6", "ubuntu12"),
                 TEST_UTIL.getTopList(globalDriver));
  }

  @Test
  public void test_07_testImagesSelected() {
    ImmutableSet<String> expectedTopList = ImmutableSet.of("centos6", "ubuntu12");
    assertEquals("The list of the topmenu is not correct.", expectedTopList, TEST_UTIL.getTopList(globalDriver));
    String uriPrefix = Constants.IMAGETYPES_URL + "/imagetype/";
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedTopList, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @Test
  public void test_08_deleteImagetype() {
    TEST_UTIL.genericDeleteTest(Constants.IMAGETYPES_URL);
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
