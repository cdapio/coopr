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
package co.cask.coopr.scheduler.task;

import co.cask.coopr.spec.service.ServiceAction;
import com.google.common.base.Objects;

/**
 * Service in a {@link TaskConfig}, containing the name of the service and the action to be performed.
 * Used purely for serialization into json.
 */
public class TaskServiceAction {
  private final String name;
  private final ServiceAction action;

  public TaskServiceAction(String name, ServiceAction action) {
    this.name = name;
    this.action = action;
  }

  /**
   * Get the name of the service.
   *
   * @return Name of the service.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the service action to perform.
   *
   * @return Service action to perform.
   */
  public ServiceAction getAction() {
    return action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskServiceAction that = (TaskServiceAction) o;

    return Objects.equal(name, that.name) &&
      Objects.equal(action, that.action);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, action);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("action", action)
      .toString();
  }
}
