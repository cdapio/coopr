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

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Provisioner parameters describe what fields the provisioner plugin understands and which fields are required.
 */
public class ParametersSpecification {
  public static final ParametersSpecification EMPTY_SPECIFICATION = new ParametersSpecification(null, null);
  private final Map<String, FieldSchema> fields;
  private final Set<Set<String>> required;

  public ParametersSpecification(Map<String, FieldSchema> fields, Set<Set<String>> required) {
    this.fields = fields == null ? ImmutableMap.<String, FieldSchema>of() : ImmutableMap.copyOf(fields);
    this.required = required == null ? ImmutableSet.<Set<String>>of() : ImmutableSet.copyOf(required);
    Set<String> flattenedRequires = Sets.newHashSet();
    for (Set<String> requiredSet : this.required) {
      flattenedRequires.addAll(requiredSet);
    }
    Set<String> badRequires = Sets.difference(flattenedRequires, this.fields.keySet());
    if (badRequires.size() > 0) {
      String badRequiresStr = Joiner.on(',').join(badRequires);
      throw new IllegalArgumentException(badRequiresStr + " specified as required, but are not fields.");
    }
  }

  /**
   * Get the immutable mapping of field name to the {@link FieldSchema} to use for the field.
   *
   * @return Immutable mapping of field name to the {@link FieldSchema} to use for the field.
   */
  public Map<String, FieldSchema> getFields() {
    return fields;
  }

  /**
   * Get the immutable set of required field combinations, one of which must be satisfied. For example, if it contains
   * the sets {f1, f2} and {f3, f4}, then it means either f1 and f2 must be given, or f3 and f4 must be given.
   *
   * @return Immutable set of required field sets.
   */
  public Set<Set<String>> getRequiredFields() {
    return required;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParametersSpecification)) {
      return false;
    }

    ParametersSpecification that = (ParametersSpecification) o;

    return Objects.equal(fields, that.fields) &&
      Objects.equal(required, that.required);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fields, required);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("fields", fields)
      .add("required", required)
      .toString();
  }
}
