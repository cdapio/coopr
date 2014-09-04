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
package co.cask.coopr.spec.plugin;

import co.cask.coopr.common.utils.ImmutablePair;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * An Automator type defines what parameters admins need to provide to a
 * {@link co.cask.coopr.spec.service.ServiceAction} in order for the
 * provisioner automator plugin to do its job.
 */
public class AutomatorType extends AbstractPluginSpecification {

  public AutomatorType(String name, String description, Map<ParameterType, ParametersSpecification> parameters,
                       Map<String, ResourceTypeSpecification> resourceTypes) {
    super(name, description, parameters, resourceTypes);
  }

  /**
   * Separate fields based on whether or not they are sensitive, and also removing fields that are not user fields and
   * are not admin overridable.
   *
   * @param input Input map of fields to values
   * @return Pair of maps of valid fields, the first containing non-sensitive fields and the second containing
   *         sensitive fields. Maps can be empty but not null.
   */
  public ImmutablePair<Map<String, String>, Map<String, String>> separateFields(Map<String, String> input) {
    Map<String, FieldSchema> adminFields = getParametersSpecification(ParameterType.ADMIN).getFields();
    Map<String, FieldSchema> userFields = getParametersSpecification(ParameterType.USER).getFields();

    Map<String, String> nonSensitive = Maps.newHashMap();
    Map<String, String> sensitive = Maps.newHashMap();
    for (Map.Entry<String, String> entry : input.entrySet()) {
      String field = entry.getKey();
      String fieldVal = entry.getValue();

      // see if this is an overridable admin field
      FieldSchema fieldSchema = adminFields.get(field);
      if (fieldSchema == null || !fieldSchema.isOverride()) {
        // not an overridable admin field. check if its a user field
        fieldSchema = userFields.get(field);
      }

      // if its not a user field or an overridable admin field, ignore it
      if (fieldSchema != null) {
        if (fieldSchema.isSensitive()) {
          sensitive.put(field, fieldVal);
        } else {
          nonSensitive.put(field, fieldVal);
        }
      }
    }
    return ImmutablePair.of(nonSensitive, sensitive);
  }
}
