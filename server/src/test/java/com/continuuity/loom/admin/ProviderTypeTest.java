package com.continuuity.loom.admin;

import com.continuuity.loom.common.utils.ImmutablePair;
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
public class ProviderTypeTest {

  @Test
  public void testRequiredFields() throws IOException {
    // 3 admin fields a1, a2, a3. One of { a1 }, { a2, a3 }, { a1, a3 } must be present.
    // 3 user fields u1, u2, u3. All are optional.
    ProviderType providerType = new ProviderType(
      "name",
      "desc",
      ImmutableMap.of(
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
      ),
      null
    );

    Assert.assertTrue(providerType.requiredFieldsExist(ParameterType.ADMIN, ImmutableSet.of("a1")));
    Assert.assertTrue(providerType.requiredFieldsExist(ParameterType.ADMIN, ImmutableSet.of("a2", "a3")));
    Assert.assertTrue(providerType.requiredFieldsExist(ParameterType.ADMIN, ImmutableSet.of("a1", "a3")));
    Assert.assertTrue(providerType.requiredFieldsExist(ParameterType.ADMIN, ImmutableSet.of("a1", "a2", "a3")));
    Assert.assertFalse(providerType.requiredFieldsExist(ParameterType.ADMIN, ImmutableSet.of("a2")));
    Assert.assertFalse(providerType.requiredFieldsExist(ParameterType.ADMIN, ImmutableSet.of("a3", "a3")));
    Assert.assertTrue(providerType.requiredFieldsExist(ParameterType.USER, ImmutableSet.<String>of()));
    Assert.assertTrue(providerType.requiredFieldsExist(ParameterType.USER, ImmutableSet.of("u1")));
  }

  @Test
  public void testSeparateFields() throws IOException {
    // one field for each combination of override and sensitive
    ProviderType providerType = new ProviderType(
      "name",
      "desc",
      ImmutableMap.of(
        ParameterType.ADMIN, new ParametersSpecification(
          ImmutableMap.of(
            "a1", FieldSchema.builder().setLabel("a1").setType("text").setOverride(false).setSensitive(false).build(),
            "a2", FieldSchema.builder().setLabel("a2").setType("text").setOverride(false).setSensitive(true).build(),
            "a3", FieldSchema.builder().setLabel("a3").setType("text").setOverride(true).setSensitive(false).build(),
            "a4", FieldSchema.builder().setLabel("a4").setType("text").setOverride(true).setSensitive(true).build()),
          ImmutableSet.<Set<String>>of()
        ),
        ParameterType.USER, new ParametersSpecification(
          ImmutableMap.of(
            "u1", FieldSchema.builder().setLabel("u1").setType("text").setSensitive(false).build(),
            "u2", FieldSchema.builder().setLabel("u2").setType("text").setSensitive(true).build()),
          ImmutableSet.<Set<String>>of()
        )
      ),
      null
    );

    Map<String, String> input = Maps.newHashMap();
    input.put("a1", "v1");
    input.put("a2", "v2");
    input.put("a3", "v3");
    input.put("a4", "v4");
    input.put("u1", "v5");
    input.put("u2", "v6");
    input.put("bogus1", "asdf");
    // a1, and a2 should not be in results since they are non-overridable admin fields
    // bogus1 should not be in results since its not an admin or user field
    Map<String, String> expectedNonsensitive = Maps.newHashMap();
    expectedNonsensitive.put("a3", "v3");
    expectedNonsensitive.put("u1", "v5");
    Map<String, String> expectedSensitive = Maps.newHashMap();
    expectedSensitive.put("a4", "v4");
    expectedSensitive.put("u2", "v6");
    ImmutablePair<Map<String, String>, Map<String, String>> output = providerType.separateFields(input);
    Assert.assertEquals(expectedNonsensitive, output.getFirst());
    Assert.assertEquals(expectedSensitive, output.getSecond());
  }

}
