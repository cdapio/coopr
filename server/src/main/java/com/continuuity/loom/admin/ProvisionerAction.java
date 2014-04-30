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

/**
 * Type of provisioner actions.
 */
public enum ProvisionerAction {
  CREATE,
  CONFIRM,
  BOOTSTRAP,
  INSTALL,
  REMOVE,
  INITIALIZE,
  CONFIGURE,
  START,
  STOP,
  DELETE;

  public boolean isHardwareAction() {
    return this == CREATE || this == CONFIRM || this == BOOTSTRAP || this == DELETE;
  }

  public boolean isRuntimeAction() {
    return this == INITIALIZE || this == START || this == STOP;
  }

  public boolean isInstallTimeAction() {
    return this == INSTALL || this == REMOVE;
  }
}
