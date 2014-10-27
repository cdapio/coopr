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

package co.cask.coopr.test.client;

import co.cask.coopr.TestHelper;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.service.ServiceDependencies;
import com.google.common.collect.ImmutableMap;

public class ClientTestEntities {
  private ClientTestEntities() {
  }

  public static final Service ZOOKEEPER = Service.builder()
    .setName("zookeeper")
    .setDescription("Zookeeper Service")
    .setDependencies(ServiceDependencies.runtimeRequires("hosts"))
    .setProvisionerActions(ImmutableMap.<ProvisionerAction, ServiceAction>of(
      ProvisionerAction.INSTALL,
      new ServiceAction("chef-solo",
                        TestHelper.actionMapOf("recipe[zookeeper::zookeeper_namenode]", null)),
      ProvisionerAction.INITIALIZE,
      new ServiceAction("chef-solo",
                        TestHelper.actionMapOf("recipe[zookeeper_wrapper::zookeeper_namenode_init]", null)),
      ProvisionerAction.CONFIGURE,
      new ServiceAction("chef-solo",
                        TestHelper.actionMapOf("recipe[zookeeper::default]", null)),
      ProvisionerAction.START,
      new ServiceAction("chef-solo",
                        TestHelper.actionMapOf("recipe[coopr_service_runner::default]", "start data")),
      ProvisionerAction.STOP,
      new ServiceAction("chef-solo",
                        TestHelper.actionMapOf("recipe[coopr_service_runner::default]", "stop data"))))
    .build();
}
