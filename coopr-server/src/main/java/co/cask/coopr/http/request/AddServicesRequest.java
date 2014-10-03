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
package co.cask.coopr.http.request;

import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.Set;

/**
 * Request for add services to a cluster.
 */
public class AddServicesRequest extends ClusterOperationRequest {
  private final Set<String> services;

  /**
   * Create a request to add services to a cluster.
   *
   * @param providerFields provider fields given by the user.
   * @param services Services to add to the cluster.
   */
  public AddServicesRequest(Map<String, Object> providerFields, Set<String> services) {
    super(providerFields);
    Preconditions.checkArgument(services != null && !services.isEmpty(), "Services to add must be specified.");
    this.services = services;
  }

  /**
   * Get the services to add to the cluster.
   *
   * @return Services to add to the cluster.
   */
  public Set<String> getServices() {
    return services;
  }
}
