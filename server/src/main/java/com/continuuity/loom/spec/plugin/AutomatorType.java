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

import java.util.Map;

/**
 * An Automator type defines what parameters admins need to provide to a
 * {@link com.continuuity.loom.spec.service.ServiceAction} in order for the
 * provisioner automator plugin to do its job.
 */
public class AutomatorType extends AbstractPluginSpecification {

  public AutomatorType(String name, String description, Map<ParameterType, ParametersSpecification> parameters,
                       Map<String, ResourceTypeSpecification> resourceTypes) {
    super(name, description, parameters, resourceTypes);
  }
}
