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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents an action to perform on a service.
 */
public class ActionOnService {
  private final ProvisionerAction action;
  private final String service;

  public ActionOnService(ProvisionerAction action, String service) {
    Preconditions.checkArgument(service != null, "Service cannot be null");
    Preconditions.checkArgument(action != null, "Action cannot be null");
    this.service = service;
    this.action = action;
  }

  public String getService() {
    return service;
  }

  public ProvisionerAction getAction() {
    return action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ActionOnService)) {
      return false;
    }

    ActionOnService that = (ActionOnService) o;

    return Objects.equal(action, that.action) &&
      Objects.equal(service, that.service);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(action, service);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("action", action)
      .add("service", service)
      .toString();
  }
}
