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
import com.continuuity.test.page.CreatePage.HardwaretypesInstancePage;
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
 * Test GET v1/loom/hardwaretypes/hardwaretype/<hardwaretype-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HardwaretypesInstanceTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();
  private static final TestUtil TEST_UTIL =  new TestUtil();
  private static final HardwaretypesInstancePage HARDWARETYPES_INSTANCE_PAGE = new HardwaretypesInstancePage();
  private static final String NAME = "small";
  private static final String FLAVOR = "flavor";

  @BeforeClass
  public static void runInitial() throws Exception {
    globalDriver.get(Constants.HARDWARETYPE_INSTANCE_URI);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_05_getHardwaretype() throws Exception {
    HardwareType small = EXAMPLE_READER.getHardwareTypes(Constants.HARDWARETYPES_PATH).get(NAME);
    assertEquals("Name is not correct.", small.getName(), HARDWARETYPES_INSTANCE_PAGE.getInputName());
    assertEquals("Description is not correct.", small.getDescription(), HARDWARETYPES_INSTANCE_PAGE.getDescription());

    Map<String, Map<String, String>> providerMap = small.getProviderMap();
    assertEquals(providerMap.size(), HARDWARETYPES_INSTANCE_PAGE.getProviderMap().size());
    for (Map.Entry<String, String> entry : HARDWARETYPES_INSTANCE_PAGE.getProviderMap().entrySet()) {
      assertEquals("Provider is not correct.", providerMap.get(entry.getKey()).get(FLAVOR),
                   entry.getValue());
    }
  }

  @Test
  public void test_06_topmenu() {
    ImmutableSet<String> expectedTopList = ImmutableSet.of("small", "medium", "large");
    String uriPrefix = Constants.HARDWARETYPES_URL + "/hardwaretype/";
    assertEquals("The list of the topmenu is not correct.", expectedTopList, TEST_UTIL.getTopList(globalDriver));
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedTopList, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
