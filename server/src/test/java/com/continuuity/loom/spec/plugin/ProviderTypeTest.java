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
package com.continuuity.loom.spec.plugin;

import com.continuuity.loom.BaseTest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ProviderTypeTest extends BaseTest {

  @Test
  public void testFilterFields() {
    ProviderType providerType = new ProviderType(
      "providertype",
      "some description",
      ImmutableMap.of(
        ParameterType.ADMIN, new ParametersSpecification(
          ImmutableMap.of(
            "region", FieldSchema.builder().setLabel("region").setType("text").setOverride(true).build(),
            "url", FieldSchema.builder().setLabel("url").setType("text").setOverride(false).build()),
          ImmutableSet.<Set<String>>of()
        ),
        ParameterType.USER, new ParametersSpecification(
          ImmutableMap.of(
            "keyname", FieldSchema.builder().setLabel("keyname").setType("text").build(),
            "key", FieldSchema.builder().setLabel("key").setType("text").build()),
          ImmutableSet.<Set<String>>of(ImmutableSet.of("keyname", "key"))
        )
      ),
      null
    );
    Map<String, String> expected = Maps.newHashMap();
    expected.put("region", "iad");
    expected.put("keyname", "name");
    expected.put("key", "keycontents");
    Map<String, String> input = Maps.newHashMap();
    input.putAll(expected);
    // url is a non-overridable admin field and should be ignored
    input.put("url", "abc.com");
    // boguskey is not a field and should be ignored
    input.put("boguskey", "bogusval");
    Assert.assertEquals(expected, providerType.filterFields(input));
  }
}
