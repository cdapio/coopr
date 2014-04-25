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
import com.continuuity.test.page.CreatePage.ServicesInstancePage;
import com.google.common.collect.ImmutableSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;

/**
 * TEST GET v1/loom/services/service/<service-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServicesInstanceTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ServicesInstancePage REACTOR_SERVICES_PAGE = new ServicesInstancePage();
  private static final String NAME = "reactor";
  private static final String IMAGE = "image";

  @BeforeClass
  public static void runInitial() throws  Exception {
    globalDriver.get(Constants.SERVICES_INSTANCE_URI);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_05_getService() throws Exception {
    Service reactor = EXAMPLE_READER.getServices(Constants.SERVICES_PATH).get(NAME);
    assertEquals("Name is not correct.", reactor.getName(), REACTOR_SERVICES_PAGE.getInputName());
    assertEquals("Description is not correct.", reactor.getDescription(),
                 REACTOR_SERVICES_PAGE.getDescription());
    assertEquals("DependsOn is not correct.", reactor.getDependencies().getRuntime().getRequires(),
                 REACTOR_SERVICES_PAGE.getDependsOn());
    assertEquals("Provisioner is not correct", reactor.getProvisionerActions(),
                 REACTOR_SERVICES_PAGE.getProvisionerActions());
  }

  @Test
  public void test_06_topmenu() {
    ImmutableSet<String> expectedServiceSet = ImmutableSet.of(
      "reactor", "haproxy", "fail2ban", "mysql-server", "zookeeper-server", "apache-httpd",
      "hadoop-yarn-resourcemanager", "php", "base", "hbase-regionserver", "hadoop-yarn-nodemanager",
      "hadoop-hdfs-datanode", "hadoop-hdfs-namenode", "hbase-master", "nodejs");
    String uriPrefix = Constants.SERVICES_URL + "/service/";
    assertEquals("The list of the topmenu is not correct.", expectedServiceSet, TEST_UTIL.getTopList(globalDriver));
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedServiceSet, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
