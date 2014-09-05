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
package co.cask.coopr.management;

import co.cask.coopr.spec.ProvisionerAction;

/**
 * Provisioner stats for JMX.
 */
@SuppressWarnings("UnusedDeclaration")
public class ProvisionerStats extends StatCounter<ProvisionerAction> {

  public long getCreate() {
    return getValue(ProvisionerAction.CREATE);
  }

  public long getConfirm() {
    return getValue(ProvisionerAction.CONFIRM);
  }

  public long getBootstrap() {
    return getValue(ProvisionerAction.BOOTSTRAP);
  }

  public long getInstall() {
    return getValue(ProvisionerAction.CREATE);
  }

  public long getRemove() {
    return getValue(ProvisionerAction.REMOVE);
  }

  public long getIntialize() {
    return getValue(ProvisionerAction.INITIALIZE);
  }

  public long getConfigure() {
    return getValue(ProvisionerAction.CONFIGURE);
  }

  public long getStart() {
    return getValue(ProvisionerAction.START);
  }

  public long getStop() {
    return getValue(ProvisionerAction.STOP);
  }

  public long getDelete() {
    return getValue(ProvisionerAction.DELETE);
  }
}
