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

import com.continuuity.loom.admin.HardwareType;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.TestUtil;
import com.continuuity.test.input.ExampleReader;
import com.continuuity.test.page.CreatePage.ImagetypesInstancePage;
import com.google.common.collect.ImmutableSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Map;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;

/**
 * Test GET v1/loom/imagetypes/imagetype/<imagetype-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImagetypesInstanceTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ImagetypesInstancePage IMAGETYPES_INSTANCE_PAGE = new ImagetypesInstancePage();
  private static final String NAME = "centos6";
  private static final String IMAGE = "image";

  @BeforeClass
  public static void runInitial() throws Exception {
    globalDriver.get(Constants.IMAGETYPE_INSTANCE_URI);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_05_getImagetype() throws Exception {
    HardwareType small = EXAMPLE_READER.getHardwareTypes(Constants.IMAGETYPES_PATH).get(NAME);
    assertEquals("Name is not correct.", small.getName(), IMAGETYPES_INSTANCE_PAGE.getInputName());
    assertEquals("Description is not correct.", small.getDescription(), IMAGETYPES_INSTANCE_PAGE.getDescription());

    Map<String, Map<String, String>> providerMap = small.getProviderMap();
    assertEquals(providerMap.size(), IMAGETYPES_INSTANCE_PAGE.getProviderMap().size());
    for (Map.Entry<String, String> entry : IMAGETYPES_INSTANCE_PAGE.getProviderMap().entrySet()) {
      assertEquals("Provider is not equal to the one in the example.", providerMap.get(entry.getKey()).get(IMAGE),
                   entry.getValue());
    }
  }

  @Test
  public void test_06_topmenu() {
    ImmutableSet<String> expectedTopList = ImmutableSet.of("centos6", "ubuntu12");
    assertEquals("The list of the topmenu is not correct.", expectedTopList, TEST_UTIL.getTopList(globalDriver));
    String uriPrefix = Constants.IMAGETYPES_URL + "/imagetype/";
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedTopList, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
