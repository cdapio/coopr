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

import co.cask.coopr.metrics.MetricService;
import com.google.common.base.Objects;

import java.util.concurrent.TimeUnit;

/**
 * Class specifying filters to use when querying for cluster tasks.
 */
public class ClusterTaskFilter {

  private final String tenantId;
  private final String userId;
  private final String clusterId;
  private final String clusterTemplate;
  private final Long start;
  private final Long end;
  private final MetricService.Periodicity periodicity;
  private final TimeUnit timeUnit;

  public ClusterTaskFilter(String tenantId, String userId, String clusterId, String clusterTemplate, Long start,
                           Long end, MetricService.Periodicity periodicity, TimeUnit timeUnit) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.clusterId = clusterId;
    this.clusterTemplate = clusterTemplate;
    this.start = start;
    this.end = end;
    this.periodicity = periodicity;
    this.timeUnit = timeUnit != null ? timeUnit : TimeUnit.SECONDS;
  }

  /**
   * Retrieves tenant id of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return tenant id
   */
  public String getTenantId() {
    return tenantId;
  }

  /**
   * Retrieves user id of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return user id
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Retrieves cluster id of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return cluster id
   */
  public String getClusterId() {
    return clusterId;
  }

  /**
   * Retrieves cluster template of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return cluster template
   */
  public String getClusterTemplate() {
    return clusterTemplate;
  }

  /**
   * Retrieves start date of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return start date
   */
  public Long getStart() {
    return start;
  }

  /**
   * Retrieves end date of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return end date
   */
  public Long getEnd() {
    return end;
  }

  /**
   * Retrieves {@link co.cask.coopr.metrics.MetricService.Periodicity} of this filter.
   * This field is ignored if its value is {@code null}.
   *
   * @return periodicity
   */
  public MetricService.Periodicity getPeriodicity() {
    return periodicity;
  }

  /**
   * Retrieves {@link TimeUnit} of this filter.
   * The default value for this field is {@link TimeUnit}.SECONDS.
   *
   * @return timeUnit
   */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("tenantId", tenantId)
      .add("userId", userId)
      .add("clusterId", clusterId)
      .add("clusterTemplate", clusterTemplate)
      .add("start", start)
      .add("end", end)
      .add("periodicity", periodicity)
      .add("timeUnit", timeUnit)
      .toString();
  }
}
