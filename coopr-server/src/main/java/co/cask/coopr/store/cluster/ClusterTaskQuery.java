/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.store.cluster;

import com.google.common.base.Objects;

/**
 * Class wrapper around querying task parameters.
 */
public class ClusterTaskQuery {

  private final String tenantId;
  private final String userId;
  private final String clusterId;
  private final String clusterTemplate;
  private final String startDate;
  private final String endDate;

  public ClusterTaskQuery(String tenantId, String userId, String clusterId, String clusterTemplate,
                          Long startDate, Long endDate) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.clusterId = clusterId;
    this.clusterTemplate = clusterTemplate;
    this.startDate = startDate.toString();
    this.endDate = endDate.toString();
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getUserId() {
    return userId;
  }

  public String getClusterId() {
    return clusterId;
  }

  public String getClusterTemplate() {
    return clusterTemplate;
  }

  public String getStartDate() {
    return startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("tenantId", tenantId)
      .add("userId", userId)
      .add("clusterId", clusterId)
      .add("clusterTemplate", clusterTemplate)
      .add("startDate", startDate)
      .add("endDate", endDate)
      .toString();
  }
}
