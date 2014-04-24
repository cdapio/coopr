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

import com.continuuity.loom.admin.ProvisionerAction;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class describing what {@link ProvisionerAction}s are only on hardware and not on services, which actions rollback
 * other actions, which actions retry other actions, the order {@link ProvisionerAction}s should be performed in order
 * to carry out specific {@link ClusterAction}s, and action dependencies.
 */
public class Actions {
  private static final Actions INSTANCE = new Actions();
  private final Map<ClusterAction, List<ProvisionerAction>> actionOrder;
  private final Map<ProvisionerAction, ProvisionerAction> rollbackActions;
  private final Map<ProvisionerAction, ProvisionerAction> retryAction;
  private final Set<Dependency> actionDependencies;

  /**
   * Represents action dependency between services. Suppose we have a service A that depends on service B. A dependency
   * from start to initialize means that initialize service B must run before start service A runs. A dependency from
   * start to start means that start service B must run before start service A runs. Setting the reverse flag reverses
   * the order of services. For example, a reverse dependency from stop to stop means that stop service A must run
   * before stop service B. Similarly, a reverse dependency from stop to remove means that stop service A must run
   * before remove service B.
   */
  public static class Dependency {
    private final ProvisionerAction from;
    private final ProvisionerAction to;
    private final boolean isReversed;

    public Dependency(ProvisionerAction from, ProvisionerAction to) {
      this(from, to, false);
    }

    public Dependency(ProvisionerAction from, ProvisionerAction to, boolean isReversed) {
      this.from = from;
      this.to = to;
      this.isReversed = isReversed;
    }

    public ProvisionerAction getFrom() {
      return from;
    }

    public ProvisionerAction getTo() {
      return to;
    }

    public boolean getIsReversed() {
      return isReversed;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Dependency that = (Dependency) o;

      return from == that.from && to == that.to && isReversed == that.isReversed;

    }

    @Override
    public int hashCode() {
      return Objects.hashCode(from, to, isReversed);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
        .add("from", from)
        .add("to", to)
        .add("reverse", isReversed)
        .toString();
    }
  }

  public static Actions getInstance() {
    return INSTANCE;
  }

  private Actions() {
    this.actionOrder = ImmutableMap.<ClusterAction, List<ProvisionerAction>>builder()
      .put(ClusterAction.CLUSTER_CREATE,
           ImmutableList.of(
             ProvisionerAction.CREATE,
             ProvisionerAction.CONFIRM,
             ProvisionerAction.BOOTSTRAP,
             ProvisionerAction.INSTALL,
             ProvisionerAction.CONFIGURE,
             ProvisionerAction.INITIALIZE,
             ProvisionerAction.START))
      .put(ClusterAction.CLUSTER_DELETE,
           ImmutableList.of(
             ProvisionerAction.DELETE))
      .put(ClusterAction.ADD_SERVICES,
           ImmutableList.of(
             ProvisionerAction.BOOTSTRAP,
             ProvisionerAction.INSTALL,
             ProvisionerAction.CONFIGURE,
             ProvisionerAction.INITIALIZE,
             ProvisionerAction.START))
      .put(ClusterAction.CLUSTER_CONFIGURE,
           ImmutableList.of(
             ProvisionerAction.BOOTSTRAP,
             ProvisionerAction.CONFIGURE))
      .put(ClusterAction.CLUSTER_CONFIGURE_WITH_RESTART,
           ImmutableList.of(
             ProvisionerAction.BOOTSTRAP,
             ProvisionerAction.STOP,
             ProvisionerAction.CONFIGURE,
             ProvisionerAction.START))
      .put(ClusterAction.STOP_SERVICES,
           ImmutableList.of(
             ProvisionerAction.STOP))
      .put(ClusterAction.START_SERVICES,
           ImmutableList.of(
             ProvisionerAction.START))
      .put(ClusterAction.RESTART_SERVICES,
           ImmutableList.of(
             ProvisionerAction.STOP,
             ProvisionerAction.START))
      .build();

    this.rollbackActions =
      ImmutableMap.of(ProvisionerAction.CONFIRM, ProvisionerAction.DELETE);

    this.retryAction =
      ImmutableMap.of(ProvisionerAction.CONFIRM, ProvisionerAction.CREATE);

    this.actionDependencies = ImmutableSet.of(
      new Dependency(ProvisionerAction.START, ProvisionerAction.START),
      new Dependency(ProvisionerAction.START, ProvisionerAction.INITIALIZE),
      new Dependency(ProvisionerAction.STOP, ProvisionerAction.STOP, true),
      new Dependency(ProvisionerAction.INSTALL, ProvisionerAction.INSTALL)
    );
  }

  /**
   * Get the order {@link ProvisionerAction}s should be performed in order to carry out different
   * {@link ClusterAction}s.
   *
   * @return Mapping of {@link ClusterAction} to a list of {@link ProvisionerAction}s.
   */
  public Map<ClusterAction, List<ProvisionerAction>> getActionOrder() {
    return actionOrder;
  }

  /**
   * Get a mapping of {@link ProvisionerAction}s that should be run to roll back another
   * failed {@link ProvisionerAction}. Actions that require no rollback will not be keys in the map.
   *
   * @return Map containing what {@link ProvisionerAction} should be run to roll back a failed
   *         {@link ProvisionerAction}. The map key is the failed action and the corresponding value is the action
   *         that should be run to roll back the failed action.
   */
  public Map<ProvisionerAction, ProvisionerAction> getRollbackActions() {
    return rollbackActions;
  }

  /**
   * Get a mapping of {@link ProvisionerAction}s that should be run to retry another failed {@link ProvisionerAction}.
   * Actions that should be retried by rerunning themselves are not included in the map. For example, to retry
   * installing a service, we just try installing the service again. However, to retry a confirm node, we first create
   * another node and then confirm that node.
   *
   * @return Map containing what {@link ProvisionerAction} should be run to retry a failed {@link ProvisionerAction}.
   *         The map key is the failed action and the corresponding value is the action that should be run to retry
   *         the failed action.
   */
  public Map<ProvisionerAction, ProvisionerAction> getRetryAction() {
    return retryAction;
  }

  /**
   * Get the set of {@link Dependency} describing which {@link ProvisionerAction}s
   * depend on other {@link ProvisionerAction}s for service provisioner actions.
   * For example, if serviceA depends on serviceB, starting serviceA depends on starting
   * serviceB. Similarly, initializing serviceA depends on starting serviceB.
   *
   * @return Set of {@link Dependency} as the value describing what {@link ProvisionerAction}s depend on other
   *         {@link ProvisionerAction}s for the given cluster action.
   */
  public Set<Dependency> getActionDependencies() {
    return actionDependencies;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("actionOrder", actionOrder)
      .add("rollbackActions", rollbackActions)
      .add("retryAction", retryAction)
      .add("actionDependencies", actionDependencies)
      .toString();
  }
}
