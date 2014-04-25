/*
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
package com.continuuity.loom.admin;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.Entities;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

/**
 *
 */
public class ProviderTest extends BaseTest {

  @Test
  public void testAddUserFields() {
    Map<String, String> fields = ImmutableMap.of("adminfield1", "val1", "adminfield2", "val2");
    Provider provider =
      new Provider("test-provider", "description", Entities.ProviderTypeExample.USER_RACKSPACE.getName(), fields);
    Map<String, String> userFields = ImmutableMap.of("rackspace_username", "user", "rackspace_apikey", "key");
    provider.addUserFields(userFields, Entities.ProviderTypeExample.USER_RACKSPACE);

    Map<String, String> expected = Maps.newHashMap();
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      expected.put(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : userFields.entrySet()) {
      expected.put(entry.getKey(), entry.getValue());
    }
    Assert.assertEquals(expected, provider.getProvisionerFields());
  }

  @Test
  public void testAddNonUserFieldsIgnored() {
    Map<String, String> fields = ImmutableMap.of("adminfield1", "val1", "adminfield2", "val2");
    Provider provider =
      new Provider("test-provider", "description", Entities.ProviderTypeExample.USER_RACKSPACE.getName(), fields);
    Map<String, String> userFields = ImmutableMap.of(
      "rackspace_username", "user",
      "rackspace_apikey", "key",
      "boguskey", "val"
    );
    provider.addUserFields(userFields, Entities.ProviderTypeExample.USER_RACKSPACE);


    Map<String, String> expected = Maps.newHashMap();
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      expected.put(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : userFields.entrySet()) {
      expected.put(entry.getKey(), entry.getValue());
    }
    expected.remove("boguskey");
    Assert.assertEquals(expected, provider.getProvisionerFields());
  }

  @BeforeClass
  public static void setupProviderTest() throws Exception {
    entityStore.writeProviderType(Entities.ProviderTypeExample.USER_RACKSPACE);
    entityStore.writeProvider(Entities.ProviderExample.JOYENT);
  }
}
