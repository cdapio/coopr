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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.cluster.Cluster;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Actions that can be performed on a cluster.
 */
public enum ClusterAction {
  SOLVE_LAYOUT(Cluster.Status.TERMINATED),
  CLUSTER_CREATE(Cluster.Status.INCOMPLETE),
  CLUSTER_DELETE(Cluster.Status.INCOMPLETE),
  CLUSTER_CONFIGURE(Cluster.Status.INCONSISTENT),
  CLUSTER_CONFIGURE_WITH_RESTART(Cluster.Status.INCONSISTENT),
  STOP_SERVICES(Cluster.Status.INCONSISTENT),
  START_SERVICES(Cluster.Status.INCONSISTENT),
  RESTART_SERVICES(Cluster.Status.INCONSISTENT),
  ADD_SERVICES(Cluster.Status.INCONSISTENT);

  // these are runtime actions for services that don't change cluster state
  public static final Set<ClusterAction> SERVICE_RUNTIME_ACTIONS = ImmutableSet.of(
    STOP_SERVICES, START_SERVICES, RESTART_SERVICES);
  private final Cluster.Status failureStatus;

  ClusterAction(Cluster.Status status) {
    failureStatus = status;
  }

  public Cluster.Status getFailureStatus() {
    return failureStatus;
  }
}
