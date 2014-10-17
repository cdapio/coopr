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

import java.util.Map;

/**
 * An Automator type defines what parameters admins need to provide to a
 * {@link co.cask.coopr.spec.service.ServiceAction} in order for the
 * provisioner automator plugin to do its job.
 */
public class AutomatorType extends AbstractPluginSpecification {

  private AutomatorType(BaseEntity.Builder baseBuilder,
                        Map<ParameterType, ParametersSpecification> parameters,
                        Map<String, ResourceTypeSpecification> resourceTypes) {
    super(baseBuilder, parameters, resourceTypes);
  }

  /**
   * Get a builder for creating automator types.
   *
   * @return builder for creating automator types
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating automator types.
   */
  public static class Builder extends AbstractPluginSpecification.Builder<AutomatorType> {
    @Override
    public AutomatorType build() {
      return new AutomatorType(this, parameters, resourceTypes);
    }
  }
}
