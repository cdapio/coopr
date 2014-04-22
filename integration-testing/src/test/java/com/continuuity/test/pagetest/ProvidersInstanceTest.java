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

import com.continuuity.loom.admin.Provider;
import com.continuuity.test.Constants;
import com.continuuity.test.GenericTest;
import com.continuuity.test.TestUtil;
import com.continuuity.test.input.ExampleReader;
import com.continuuity.test.page.CreatePage.ProvidersInstancePage;
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
 * Test GET v1/loom/providers/provider/<provider-id></provider-id>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProvidersInstanceTest extends GenericTest {
  private static final ExampleReader EXAMPLE_READER = new ExampleReader();
  private static final TestUtil TEST_UTIL = new TestUtil();
  private static final ProvidersInstancePage JOYENT_PROVIDERS_PAGE = new ProvidersInstancePage();
  private static final String PROVIDER_NAME = "joyent";
  private static final String USERNAME = "joyent_username";
  private static final String KEYNAME = "joyent_keyname";
  private static final String KEYFILE = "joyent_keyfile";
  private static final String VERSION = "joyent_version";

  @BeforeClass
  public static void runInitial() throws Exception {
    globalDriver.get(Constants.PROVIDER_INSTANCE_URI);
  }

  @Test
  public void test_03_leftpanel() {
    assertEquals("Leftpanel is not correct.", Constants.LEFT_PANEL, TEST_UTIL.getLeftPanel(globalDriver));
  }

  @Test
  public void test_05_getProvider() throws Exception {
    Provider joyent = EXAMPLE_READER.getProviders(Constants.PROVIDERS_PATH).get(PROVIDER_NAME);
    assertEquals("Name is not correct.", joyent.getName(), JOYENT_PROVIDERS_PAGE.getInputName());
    assertEquals("Description is not correct.", joyent.getDescription(), JOYENT_PROVIDERS_PAGE.getDescription());
    assertEquals("Provisioner is not correct.", joyent.getProviderType(),
                 JOYENT_PROVIDERS_PAGE.getProvisioner());
    Map<String, String> authValue = joyent.getProvisionerFields();
    assertEquals("Username is not correct.", authValue.get(USERNAME), JOYENT_PROVIDERS_PAGE.getUsername());
    assertEquals("Keyname is not correct.", authValue.get(KEYNAME), JOYENT_PROVIDERS_PAGE.getKeyname());
    assertEquals("Keyfile is not correct.", authValue.get(KEYFILE), JOYENT_PROVIDERS_PAGE.getKeyfile());
    assertEquals("Version is not correct.", authValue.get(VERSION), JOYENT_PROVIDERS_PAGE.getVersion());
  }

  @Test
  public void test_06_topmenu() {
    ImmutableSet<String> expectedTopList = ImmutableSet.of("rackspace", "joyent", "openstack");
    assertEquals("The list of the topmenu is not correct.", expectedTopList, new TestUtil().getTopList(globalDriver));
    String uriPrefix = Constants.PROVIDERS_URL + "/provider/";
    assertEquals("The uri of top list is not correct.", TEST_UTIL.getTopListUri(expectedTopList, uriPrefix),
                 TEST_UTIL.getTopListUri(globalDriver));
  }

  @AfterClass
  public static void tearDown() {
    closeDriver();
  }
}
