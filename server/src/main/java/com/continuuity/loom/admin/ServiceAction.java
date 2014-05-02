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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Actions that provisioner automator plugins can execute on a service on a node. An action
 * consists of a type, describing which type of automator plugin to use, and a set of fields the automator will use.
 */
public final class ServiceAction {
  private final String type;
  private final Map<String, String> fields;

  public ServiceAction(String type, Map<String, String> fields) {
    Preconditions.checkArgument(type != null && !type.isEmpty(), "Type must be specified.");
    this.type = type;
    this.fields = fields == null ? ImmutableMap.<String, String>of() : fields;
  }

  /**
   * Get the type of provisioner to use.
   *
   * @return Type of provisioner to use.
   */
  public String getType() {
    return type;
  }

  /**
   * Get the fields the automator plugin will need to perform the action.
   *
   * @return Fields the automator plugin will need to perform the action.
   */
  public Map<String, String> getFields() {
    return fields;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServiceAction)) {
      return false;
    }
    ServiceAction other = (ServiceAction) o;
    return Objects.equal(type, other.type) &&
      Objects.equal(fields, other.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, fields);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("fields", fields)
      .toString();
  }
}
