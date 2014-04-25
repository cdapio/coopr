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

/**
 * Actions that provisioners can execute on a service on a node. An action
 * consists of a type, a script, and optional arbitrary data the script may need.  For example, type may be chef, script
 * may be a chef recipe, and data may be arguments to pass into the chef recipe.
 */
public final class ServiceAction {
  private final String type;
  private final String script;
  private final String data;

  public ServiceAction(String type, String script, String data) {
    this.type = type;
    this.script = script;
    this.data = data;
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
   * Get the script used to perform the action.
   *
   * @return Script used to perform the action.
   */
  public String getScript() {
    return script;
  }

  /**
   * Get any data the script may need to perform the action.
   *
   * @return Optional data the script may need to perform the action.
   */
  public String getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ServiceAction)) {
      return false;
    }
    ServiceAction other = (ServiceAction) o;
    return Objects.equal(type, other.type) &&
      Objects.equal(script, other.script) &&
      Objects.equal(data, other.data);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, script, data);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("script", script)
      .add("data", data)
      .toString();
  }
}
