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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * An Automator type defines what parameters admins need to provide to a {@link ServiceAction} in order for the
 * provisioner automator plugin to do its job.
 */
public class AutomatorType extends NamedEntity {
  private final String description;
  private final Map<ParameterType, ParametersSpecification> parameters;

  public AutomatorType(String name, String description, Map<ParameterType, ParametersSpecification> parameters) {
    super(name);
    this.description = description == null ? "" : description;
    this.parameters = parameters == null ? ImmutableMap.<ParameterType, ParametersSpecification>of() : parameters;
  }

  /**
   * Get the description of the automator type.
   *
   * @return Description of the automator type.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the mapping of {@link ParameterType} to {@link ParametersSpecification} for that type.
   *
   * @return Mapping of {@link ParameterType} to {@link ParametersSpecification} for that type.
   *
   */
  public Map<ParameterType, ParametersSpecification> getParameters() {
    return parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AutomatorType)) {
      return false;
    }

    AutomatorType that = (AutomatorType) o;

    return Objects.equal(description, that.description) &&
      Objects.equal(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(description, parameters);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("description", description)
      .add("parameters", parameters)
      .toString();
  }
}
