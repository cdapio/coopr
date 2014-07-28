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

import com.continuuity.loom.common.conf.Constants;
import com.google.common.base.Objects;

/**
 * A tenant contains the id, name, and settings for a tenant.
 */
public final class Tenant extends NamedEntity {
  public static final Tenant DEFAULT_SUPERADMIN =
    new Tenant(Constants.SUPERADMIN_TENANT, Constants.SUPERADMIN_TENANT, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
  private final String id;
  private final int workers;
  private final int maxClusters;
  private final int maxNodes;

  public Tenant(String name, String id, Integer workers, Integer maxClusters, Integer maxNodes) {
    super(name);
    this.id = id;
    this.workers = workers == null ? 0 : workers;
    this.maxClusters = maxClusters == null ? Integer.MAX_VALUE : maxClusters;
    this.maxNodes = maxNodes == null ? Integer.MAX_VALUE : maxClusters;
  }

  public String getId() {
    return id;
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
    if (!(o instanceof Tenant)) {
      return false;
    }
    Tenant other = (Tenant) o;
    return Objects.equal(name, other.name) &&
      Objects.equal(id, other.id) &&
      Objects.equal(workers, other.workers) &&
      Objects.equal(maxClusters, other.maxClusters) &&
      Objects.equal(maxNodes, other.maxNodes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, id, workers, maxClusters, maxNodes);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("id", id)
      .add("workers", workers)
      .add("maxClusters", maxClusters)
      .add("maxNodes", maxNodes)
      .toString();
  }
}
