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
  private final Map<ClusterAction, List<ProvisionerAction>> actionOrder;
  private final Map<ProvisionerAction, ProvisionerAction> rollbackActions;
  private final Map<ProvisionerAction, ProvisionerAction> retryAction;
  private final Set<ProvisionerAction> hardwareActions;
  private final Map<ClusterAction, Set<Dependency>> actionDependency;

  /**
   * Represents action dependency.
   */
  public static class Dependency {
    private final ProvisionerAction from;
    private final ProvisionerAction to;

    public Dependency(ProvisionerAction from, ProvisionerAction to) {
      this.from = from;
      this.to = to;
    }

    public ProvisionerAction getFrom() {
      return from;
    }

    public ProvisionerAction getTo() {
      return to;
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

      return from == that.from && to == that.to;

    }

    @Override
    public int hashCode() {
      int result = from.hashCode();
      result = 31 * result + to.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
        .add("from", from)
        .add("to", to)
        .toString();
    }
  }

  public Actions() {
    this.actionOrder =
      ImmutableMap.<ClusterAction, List<ProvisionerAction>>of(
        ClusterAction.CLUSTER_CREATE,
                      ImmutableList.of(ProvisionerAction.CREATE, ProvisionerAction.CONFIRM, ProvisionerAction.BOOTSTRAP,
                                       ProvisionerAction.INSTALL, ProvisionerAction.CONFIGURE,
                                       ProvisionerAction.INITIALIZE, ProvisionerAction.START),
        ClusterAction.CLUSTER_DELETE,
                      ImmutableList.of(ProvisionerAction.DELETE),
        ClusterAction.ADD_SERVICES,
                      ImmutableList.of(ProvisionerAction.BOOTSTRAP, ProvisionerAction.INSTALL,
                                       ProvisionerAction.CONFIGURE, ProvisionerAction.INITIALIZE,
                                       ProvisionerAction.START));


    this.rollbackActions =
      ImmutableMap.of(ProvisionerAction.CONFIRM, ProvisionerAction.DELETE);

    this.retryAction =
      ImmutableMap.of(ProvisionerAction.CONFIRM, ProvisionerAction.CREATE);

    this.hardwareActions = ImmutableSet.of(ProvisionerAction.CREATE, ProvisionerAction.CONFIRM,
                                           ProvisionerAction.BOOTSTRAP, ProvisionerAction.DELETE);

    this.actionDependency =
      ImmutableMap.<ClusterAction, Set<Dependency>>of(
        ClusterAction.CLUSTER_CREATE,
                      ImmutableSet.of(
                        // Start of a dependent service can happen only after its dependency has started
                        new Dependency(ProvisionerAction.START, ProvisionerAction.START),
                        // Initialize of a dependent service can happen only after its dependency has started
                        new Dependency(ProvisionerAction.START, ProvisionerAction.INITIALIZE)),
        ClusterAction.ADD_SERVICES,
                      ImmutableSet.of(
                        // Start of a dependent service can happen only after its dependency has started
                        new Dependency(ProvisionerAction.START, ProvisionerAction.START),
                        // Initialize of a dependent service can happen only after its dependency has started
                        new Dependency(ProvisionerAction.START, ProvisionerAction.INITIALIZE))
      );
  }

  public Actions(Map<ClusterAction, List<ProvisionerAction>> actionOrder,
                 Map<ProvisionerAction, ProvisionerAction> rollbackActions,
                 Map<ProvisionerAction, ProvisionerAction> retryAction,
                 Set<ProvisionerAction> hardwareActions,
                 Map<ClusterAction, Set<Dependency>> actionDependency) {
    this.actionOrder = actionOrder;
    this.rollbackActions = rollbackActions;
    this.retryAction = retryAction;
    this.hardwareActions = hardwareActions == null ? ImmutableSet.<ProvisionerAction>of() : hardwareActions;
    this.actionDependency = actionDependency;
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
   * Get the set of {@link ProvisionerAction}s that are actions on hardware and not on services.
   *
   * @return Set of {@link ProvisionerAction}s that are actions on hardware and not on services.
   */
  public Set<ProvisionerAction> getHardwareActions() {
    return hardwareActions;
  }

  /**
   * Get a mapping of {@link ClusterAction} to the set of {@link Dependency} describing which {@link ProvisionerAction}s
   * depend on other {@link ProvisionerAction}s for a given {@link ClusterAction}. For example, when creating a cluster,
   * if serviceA depends on serviceB, starting serviceA depends on starting serviceB. Similarly, initializing serviceA
   * depends on starting serviceB.
   *
   * @return Map containing {@link ClusterAction} as its keys and a set of {@link Dependency} as the value describing
   *         what {@link ProvisionerAction}s depend on other {@link ProvisionerAction}s for the given cluster action.
   */
  public Map<ClusterAction, Set<Dependency>> getActionDependency() {
    return actionDependency;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("actionOrder", actionOrder)
      .add("rollbackActions", rollbackActions)
      .add("retryAction", retryAction)
      .add("hardwareActions", hardwareActions)
      .add("actionDependency", actionDependency)
      .toString();
  }
}
