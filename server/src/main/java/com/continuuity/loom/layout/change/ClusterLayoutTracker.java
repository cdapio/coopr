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
package com.continuuity.loom.layout.change;

import com.continuuity.loom.layout.ClusterLayout;
import com.google.common.collect.Lists;

import java.util.Deque;
import java.util.Queue;

/**
 * Helper for keeping track of changes made to a {@link com.continuuity.loom.layout.ClusterLayout}.
 */
public class ClusterLayoutTracker {
  private final Deque<ClusterLayoutChange> changes;
  private final Deque<ClusterLayout> states;

  /**
   * Create a layout change tracker given some starting layout.
   *
   * @param startingLayout starting layout of the cluster.
   */
  public ClusterLayoutTracker(ClusterLayout startingLayout) {
    this.changes = Lists.newLinkedList();
    this.states = Lists.newLinkedList();
    this.states.add(startingLayout);
  }

  /**
   * Adds a change to the layout if the change can be applied to the current layout, returning whether or not
   * the change was added.
   *
   * @param change Change to add, assuming it can be applied to the current layout.
   * @return True if the change was added, false if not.
   */
  public boolean addChangeIfValid(ClusterLayoutChange change) {
    ClusterLayout currentLayout = getCurrentLayout();
    if (change.canApplyChange(currentLayout)) {
      changes.addLast(change);
      states.addLast(change.applyChange(currentLayout));
      return true;
    }
    return false;
  }

  /**
   * Remove the last change performed on the cluster layout.
   */
  public void removeLastChange() {
    changes.removeLast();
    states.removeLast();
  }

  /**
   * Get the current layout of the cluster.
   *
   * @return Current layout of the cluster.
   */
  public ClusterLayout getCurrentLayout() {
    return states.getLast();
  }

  /**
   * Get the changes that were applied to the starting cluster layout.
   *
   * @return Changes that were applied to the starting cluster layout.
   */
  public Queue<ClusterLayoutChange> getChanges() {
    return changes;
  }
}
