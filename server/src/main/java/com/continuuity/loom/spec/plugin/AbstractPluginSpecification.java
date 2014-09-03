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

import com.continuuity.loom.spec.NamedEntity;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

/**
 * Plugin specification, including what parameters are supported and required and what types of resources are supported.
 */
public abstract class AbstractPluginSpecification extends NamedEntity {
  private final String description;
  private final Map<String, ResourceTypeSpecification> resourceTypes;
  protected final Map<ParameterType, ParametersSpecification> parameters;

  public AbstractPluginSpecification(String name, String description,
                                     Map<ParameterType, ParametersSpecification> parameters,
                                     Map<String, ResourceTypeSpecification> resourceTypes) {
    super(name);
    this.description = description == null ? "" : description;
    this.parameters = parameters == null ?
      ImmutableMap.<ParameterType, ParametersSpecification>of() : ImmutableMap.copyOf(parameters);
    this.resourceTypes = resourceTypes == null ?
      ImmutableMap.<String, ResourceTypeSpecification>of() : ImmutableMap.copyOf(resourceTypes);
  }

  /**
   * Get the description of the plugin.
   *
   * @return Description of the plugin.
   */
  public String getDescription() {
    return description;
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
   * Check that the given type of fields contain all required fields.
   *
   * @param parameterType Type of fields to check.
   * @param fields Fields to check.
   * @return True if the given fields contain all required fields, false if not.
   */
  public boolean requiredFieldsExist(ParameterType parameterType, Set<String> fields) {
    Set<Set<String>> requiredSets = getParametersSpecification(parameterType).getRequiredFields();
    // if nothing required is specified, anything is ok
    if (requiredSets == null || requiredSets.isEmpty()) {
      return true;
    }
    // required fields is a set of sets. As long as the fields contains all of one of these required sets, we're good.
    for (Set<String> requiredSet : getParametersSpecification(parameterType).getRequiredFields()) {
      if (fields.containsAll(requiredSet)) {
        return true;
      }
    }
    return false;
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

    return Objects.equal(description, that.description) &&
      Objects.equal(parameters, that.parameters) &&
      Objects.equal(resourceTypes, that.resourceTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(description, parameters, resourceTypes);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("description", description)
      .add("parameters", parameters)
      .add("resourceTypes", resourceTypes)
      .toString();
  }

}
