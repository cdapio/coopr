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

import co.cask.coopr.spec.BaseEntity;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plugin specification, including what parameters are supported and required and what types of resources are supported.
 */
public abstract class AbstractPluginSpecification extends BaseEntity {
  private final Map<String, ResourceTypeSpecification> resourceTypes;
  protected final Map<ParameterType, ParametersSpecification> parameters;

  protected AbstractPluginSpecification(BaseEntity.Builder baseBuilder,
                                        Map<ParameterType, ParametersSpecification> parameters,
                                        Map<String, ResourceTypeSpecification> resourceTypes) {
    super(baseBuilder);
    this.parameters = parameters == null ?
      ImmutableMap.<ParameterType, ParametersSpecification>of() : ImmutableMap.copyOf(parameters);
    this.resourceTypes = resourceTypes == null ?
      ImmutableMap.<String, ResourceTypeSpecification>of() : ImmutableMap.copyOf(resourceTypes);
  }

  /**
   * Get an immutable mapping of {@link ParameterType} to {@link ParametersSpecification} for that type.
   *
   * @return Mapping of {@link ParameterType} to {@link ParametersSpecification} for that type.
   *
   */
  public Map<ParameterType, ParametersSpecification> getParameters() {
    return parameters;
  }

  /**
   * Get the specification for parameters of the given type.
   *
   * @param parameterType Type of parameter to get.
   * @return Specification for parameters of the given type.
   */
  public ParametersSpecification getParametersSpecification(ParameterType parameterType) {
    return parameters.containsKey(parameterType) ?
      parameters.get(parameterType) : ParametersSpecification.EMPTY_SPECIFICATION;
  }

  /**
   * Get an immutable mapping of resource type to {@link ResourceTypeSpecification} for that type.
   *
   * @return Mapping of resource type to {@link ResourceTypeSpecification} for that type.
   */
  public Map<String, ResourceTypeSpecification> getResourceTypes() {
    return resourceTypes;
  }

  /**
   * Get an immutable list of missing required fields for the given parameter type from the given input set of fields.
   * Elements in the list are maps from field name to the schema for that field. Returns an empty list if required
   * fields are present. For example, if the required fields are one of [ [f1, f2, f3], [f3, f4, f5] ], and the input
   * fields are [f1, f5], then the result will be a list of maps, with each map containing the name of the missing
   * field and the value the schema of that missing field. In this example it would be
   * [ {f2:{schema}, f3:{schema}}, {f3:{schema}, f4:{schema}} ] . This is because
   * f2 and f3 are missing from the [f1, f2, f3] set and f3 and f4 are missing from the [f3, f4, f5] set.
   *
   * @param parameterType Type of fields to check.
   * @param fields Fields to check.
   * @return Immutable set of unmodifiable sets of required fields missing from the given input fields.
   */
  public List<Map<String, FieldSchema>> getMissingFields(ParameterType parameterType, Set<String> fields) {
    ParametersSpecification parametersSpecification = getParametersSpecification(parameterType);
    Set<Set<String>> requiredSets = parametersSpecification.getRequiredFields();
    ImmutableList.Builder<Map<String, FieldSchema>> missingFields = ImmutableList.builder();
    if (requiredSets == null || requiredSets.isEmpty()) {
      return ImmutableList.of();
    }

    Map<String, FieldSchema> fieldSchemas = parametersSpecification.getFields();
    for (Set<String> requiredSet : requiredSets) {
      Set<String> missingFieldNames = Sets.difference(requiredSet, fields);
      // all required fields are present, return an empty set,
      if (missingFieldNames.isEmpty()) {
        return ImmutableList.of();
      }
      ImmutableMap.Builder mapBuilder = ImmutableMap.builder();
      for (String missingFieldName : missingFieldNames) {
        // required fields are guaranteed to have a schema due to validation done when defining the fields.
        mapBuilder.put(missingFieldName, fieldSchemas.get(missingFieldName));
      }
      missingFields.add(mapBuilder.build());
    }
    return missingFields.build();
  }

  /**
   * Base builder for plugin specifications.
   *
   * @param <T> type of plugin.
   */
  public abstract static class Builder<T extends AbstractPluginSpecification> extends BaseEntity.Builder<T> {
    protected Map<String, ResourceTypeSpecification> resourceTypes;
    protected Map<ParameterType, ParametersSpecification> parameters;

    public Builder<T> setResourceTypes(Map<String, ResourceTypeSpecification> resourceTypes) {
      this.resourceTypes = resourceTypes;
      return this;
    }

    public Builder<T> setParameters(Map<ParameterType, ParametersSpecification> parameters) {
      this.parameters = parameters;
      return this;
    }

    public abstract T build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AbstractPluginSpecification)) {
      return false;
    }

    AbstractPluginSpecification that = (AbstractPluginSpecification) o;

    return Objects.equal(parameters, that.parameters) &&
      Objects.equal(resourceTypes, that.resourceTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(parameters, resourceTypes);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("parameters", parameters)
      .add("resourceTypes", resourceTypes)
      .toString();
  }

}
