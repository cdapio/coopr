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

import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.TestUtil;
import com.continuuity.test.input.ExampleReader;
import com.continuuity.test.page.CreatePage.ClustertemplatesInstancePage;
import com.google.common.collect.ImmutableSet;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;

/**
 * Test GET v1/loom/clustertemplates/clustertemplate/<template-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClustertemplatesInstanceTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ClustertemplatesInstancePage CLUSTERTEMPLATES_INSTANCE_PAGE = new ClustertemplatesInstancePage();
  private static final String NAME = "reactor-singlenode";
  private static ClusterTemplate clustertemplate;

  @BeforeClass
  public static void runInitial() throws Exception {
    clustertemplate =  EXAMPLE_READER.getClusterTemplate(Constants.CLUSTERTEMPLATE_PATH).get(NAME);
    globalDriver.get(Constants.CLUSTERTEMPLATE_INSTANCE_URI);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, new TestUtil().getLeftPanel(globalDriver));
  }

  @Test
  public void test_05_general() throws IOException {
    assertEquals("Name is not correct.", clustertemplate.getName(), CLUSTERTEMPLATES_INSTANCE_PAGE.getInputName());
    assertEquals("Description is not correct.", clustertemplate.getDescription(),
                 CLUSTERTEMPLATES_INSTANCE_PAGE.getDescription());
  }

  @Test
  public void test_06_compatibility() throws IOException {
    assertEquals("Compatibilities are not correct.", clustertemplate.getCompatibilities(),
                 CLUSTERTEMPLATES_INSTANCE_PAGE.getCompatibility());

  }

  @Test
  public void test_07_defaults() throws IOException, JSONException {
    ClusterDefaults expectedClusterDefaults = clustertemplate.getClusterDefaults();
    ClusterDefaults actualClusterDefaults = CLUSTERTEMPLATES_INSTANCE_PAGE.getClusterDefaults();
    assertEquals("ClusterDefaults.provider is not correct.", expectedClusterDefaults.getProvider(),
                 actualClusterDefaults.getProvider());
    assertEquals("ClusterDefaults.hardwaretype is not correct.", expectedClusterDefaults.getHardwaretype(),
                 actualClusterDefaults.getHardwaretype());
    assertEquals("ClusterDefaults.imagetype is not correct.", expectedClusterDefaults.getImagetype(),
                 actualClusterDefaults.getImagetype());
    assertEquals("ClusterDefaults.config is not correct.", expectedClusterDefaults.getConfig(),
                 actualClusterDefaults.getConfig());
    assertEquals("ClusterDefaults.services is not correct.", expectedClusterDefaults.getServices(),
                 actualClusterDefaults.getServices());
    assertEquals("ClusterDefaults.config is not correct.", expectedClusterDefaults.getDnsSuffix(),
                 actualClusterDefaults.getDnsSuffix());
  }

  @Test
  public void test_08_layoutConstraints() throws IOException {
    assertEquals("LayoutConstraints.mustcoexist is not correct.", clustertemplate.getConstraints()
                .getLayoutConstraint().getServicesThatMustCoexist(), CLUSTERTEMPLATES_INSTANCE_PAGE
                .getMustCoexistLayoutConstraint());
    assertEquals("LayoutConstraints.cantcoexist is not correct", clustertemplate.getConstraints().getLayoutConstraint()
                .getServicesThatMustNotCoexist(), CLUSTERTEMPLATES_INSTANCE_PAGE.getCantCoexistLayoutConstraint());

  }

  @Test
  public void test_09_serviceConstraints() throws IOException {
    assertEquals("ServiceConstraints are not correct.", clustertemplate.getConstraints().getServiceConstraints(),
                 CLUSTERTEMPLATES_INSTANCE_PAGE.getServiceConstraints());
  }

  @Test
  public void test_10_topmenu() {
    ImmutableSet<String> expectedServiceSet = ImmutableSet.of("hadoop-distributed", "lamp", "hadoop-singlenode",
                         "hadoop-hbase-distributed", "reactor-singlenode");
    String uriPrefix = Constants.CLUSTERTEMPLATES_URL + "/clustertemplate/";
    assertEquals("The list of the topmenu is not correct.", expectedServiceSet,
                 TEST_UTIL.getTopList(globalDriver));
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedServiceSet, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
