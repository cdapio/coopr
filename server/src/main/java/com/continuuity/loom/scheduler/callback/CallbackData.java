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
package com.continuuity.loom.scheduler.callback;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.google.common.base.Objects;

/**
 * Data that a {@link ClusterCallback} may use before and after cluster jobs.
 */
public class CallbackData {
  private final Type type;
  private final Cluster cluster;
  private final ClusterJob job;

  /**
   * Type of callback the data is for.
   */
  public enum Type {
    START,
    SUCCESS,
    FAILURE;
  }

  public CallbackData(Type type, Cluster cluster, ClusterJob job) {
    this.type = type;
    this.cluster = cluster;
    this.job = job;
  }

  /**
   * Get the type of callback the data is for.
   *
   * @return Type of callback the data is for.
   */
  public Type getType() {
    return type;
  }

  /**
   * Get the cluster that is being operated on.
   *
   * @return Cluster that is being operated on.
   */
  public Cluster getCluster() {
    return cluster;
  }

  /**
   * Get the job being performed or about to be performed on the cluster.
   *
   * @return Job being performed or about to be performed on the cluster.
   */
  public ClusterJob getJob() {
    return job;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CallbackData)) {
      return false;
    }

    CallbackData that = (CallbackData) o;

    return Objects.equal(cluster, that.cluster) &&
      Objects.equal(job, that.job);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(cluster, job);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("cluster", cluster)
      .add("job", job)
      .toString();
  }
}
