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
package co.cask.coopr.provisioner;

import co.cask.coopr.provisioner.plugin.ResourceCollection;

/**
 * Tenant information to send to a specific provisioner. Will be serialized and sent to provisioners as part of
 * requests to change the number of workers for the tenant on the provisioner or for syncing the tenant
 * resources for the tenant on the provisioner.
 */
public class ProvisionerTenant {
  private final int workers;
  private final ResourceCollection resources;

  public ProvisionerTenant(int workers, ResourceCollection resources) {
    this.workers = workers;
    this.resources = resources;
  }

  /**
   * Get the number of workers for the tenant on the provisioner.
   *
   * @return Number of workers for the tenant on the provisioner
   */
  public int getWorkers() {
    return workers;
  }

  /**
   * Get the resources for the tenant on the provisioner.
   *
   * @return Resources for the tenant on the provisioner
   */
  public ResourceCollection getResources() {
    return resources;
  }
}
