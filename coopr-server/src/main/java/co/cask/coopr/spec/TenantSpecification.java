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

package co.cask.coopr.spec;

import com.google.common.base.Objects;

/**
 * A tenant as seen from external world, meaning it contains the name and settings for a tenant, but no id.
 */
public final class TenantSpecification extends NamedEntity {
  private final String description;
  private final int workers;
  private final int maxClusters;
  private final int maxNodes;

  public TenantSpecification(String name, String description, Integer workers, Integer maxClusters, Integer maxNodes) {
    super(name);
    this.description = description;
    this.workers = workers == null ? 0 : workers;
    this.maxClusters = maxClusters == null ? Integer.MAX_VALUE : maxClusters;
    this.maxNodes = maxNodes == null ? Integer.MAX_VALUE : maxNodes;
  }

  // TODO: add builder so optional fields are easier to handle
  public TenantSpecification(String name, Integer workers, Integer maxClusters, Integer maxNodes) {
    this(name, "", workers, maxClusters, maxNodes);
  }

  public String getDescription() {
    return description;
  }

  public int getWorkers() {
    return workers;
  }

  public int getMaxClusters() {
    return maxClusters;
  }

  public int getMaxNodes() {
    return maxNodes;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TenantSpecification)) {
      return false;
    }
    TenantSpecification other = (TenantSpecification) o;
    return Objects.equal(name, other.name) &&
      Objects.equal(description, other.description) &&
      Objects.equal(workers, other.workers) &&
      Objects.equal(maxClusters, other.maxClusters) &&
      Objects.equal(maxNodes, other.maxNodes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, workers, maxClusters, maxNodes);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("description", description)
      .add("workers", workers)
      .add("maxClusters", maxClusters)
      .add("maxNodes", maxNodes)
      .toString();
  }
}
