/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.spec.plugin;

import co.cask.coopr.BaseTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ProviderTypeTest extends BaseTest {

  @Test
  public void testGroupFields() {
    ProviderType providerType = ProviderType.builder()
      .setParameters(ImmutableMap.of(
        ParameterType.ADMIN, new ParametersSpecification(
        ImmutableMap.of(
          "region",
          FieldSchema.builder()
            .setLabel("region")
            .setType("text")
            .setOverride(true)
            .setSensitive(false)
            .build(),
          "account",
          FieldSchema.builder()
            .setLabel("account")
            .setType("text")
            .setOverride(false)
            .setSensitive(false)
            .build(),
          "url",
          FieldSchema.builder()
            .setLabel("url")
            .setType("text")
            .setOverride(true)
            .setSensitive(true)
            .build()),
        ImmutableSet.<Set<String>>of()
      ),
        ParameterType.USER, new ParametersSpecification(
        ImmutableMap.of(
          "keyname",
          FieldSchema.builder()
            .setLabel("keyname")
            .setType("text")
            .setSensitive(false)
            .build(),
          "key",
          FieldSchema.builder()
            .setLabel("key")
            .setType("text")
            .setSensitive(true)
            .build()),
        ImmutableSet.<Set<String>>of(ImmutableSet.of("keyname", "key"))
      )))
      .setName("providertype")
      .build();
    Map<String, String> expectedSensitive = ImmutableMap.of("key", "keycontents", "url", "abc.com/api");
    Map<String, String> expectedNonsensitive = ImmutableMap.of("keyname", "dev", "region", "iad");
    Map<String, Object> input = Maps.newHashMap();
    input.putAll(expectedSensitive);
    input.putAll(expectedNonsensitive);
    // should get ignored, not an overridable admin field
    input.put("account", "asdf");
    // should get ignored, boguskey is not a field
    input.put("boguskey", "bogusval");

    PluginFields pluginFields = providerType.groupFields(input);
    Assert.assertEquals(expectedSensitive, pluginFields.getSensitive());
    Assert.assertEquals(expectedNonsensitive, pluginFields.getNonsensitive());
  }

  @Test
  public void testGetMissingFields() throws IOException {
    // 3 admin fields a1, a2, a3. One of { a1 }, { a2, a3 }, { a1, a3 } must be present.
    // 3 user fields u1, u2, u3. All are optional.
    ProviderType providerType = ProviderType.builder()
      .setParameters(ImmutableMap.of(
        ParameterType.ADMIN, new ParametersSpecification(
        ImmutableMap.of(
          "a1", FieldSchema.builder().setLabel("field1").setType("text").build(),
          "a2", FieldSchema.builder().setLabel("field2").setType("text").build(),
          "a3", FieldSchema.builder().setLabel("field3").setType("text").build()),
        ImmutableSet.<Set<String>>of(
          ImmutableSet.<String>of("a1"),
          ImmutableSet.<String>of("a2", "a3"),
          ImmutableSet.<String>of("a1", "a3")
        )
      ),
        ParameterType.USER, new ParametersSpecification(
        ImmutableMap.of(
          "u1", FieldSchema.builder().setLabel("field1").setType("text").build(),
          "u2", FieldSchema.builder().setLabel("field2").setType("text").build(),
          "u3", FieldSchema.builder().setLabel("field3").setType("text").build()),
        ImmutableSet.<Set<String>>of()
      )
      ))
      .setName("name")
      .build();

    Assert.assertTrue(providerType.getMissingFields(ParameterType.ADMIN, ImmutableSet.of("a1")).isEmpty());
    Assert.assertTrue(providerType.getMissingFields(ParameterType.ADMIN, ImmutableSet.of("a2", "a3")).isEmpty());
    Assert.assertTrue(providerType.getMissingFields(ParameterType.ADMIN, ImmutableSet.of("a1", "a3")).isEmpty());
    Assert.assertTrue(providerType.getMissingFields(ParameterType.ADMIN, ImmutableSet.of("a1", "a2", "a3")).isEmpty());
    Assert.assertEquals(
      ImmutableList.of(
        ImmutableMap.of("a1", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a1")),
        ImmutableMap.of("a3", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a3")),
        ImmutableMap.of("a1", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a1"),
                        "a3", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a3"))),
      providerType.getMissingFields(ParameterType.ADMIN, ImmutableSet.of("a2")));
    Assert.assertEquals(
      ImmutableList.of(
        ImmutableMap.of("a1", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a1")),
        ImmutableMap.of("a2", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a2")),
        ImmutableMap.of("a1", providerType.getParametersSpecification(ParameterType.ADMIN).getFields().get("a1"))),
      providerType.getMissingFields(ParameterType.ADMIN, ImmutableSet.of("a3", "a3")));
    Assert.assertTrue(providerType.getMissingFields(ParameterType.USER, ImmutableSet.<String>of()).isEmpty());
    Assert.assertTrue(providerType.getMissingFields(ParameterType.USER, ImmutableSet.of("u1")).isEmpty());
  }
}
