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
package com.continuuity.loom.layout;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Iterator through all possibilities of X items placed into Y slots, where as many of the leftmost slots are used up as
 * possible. For example, with 3 items and 4 slots, the iterator would enumerate all possibilities like so:
 * 3, 0, 0, 0
 * 2, 1, 0, 0
 * 2, 0, 1, 0
 * 2, 0, 0, 1
 * 1, 2, 0, 0
 * 1, 1, 1, 0
 * 1, 1, 0, 1
 * 1, 0, 2, 0
 * 1, 0, 1, 1
 * 1, 0, 0, 2
 * 0, 3, 0, 0
 * 0, 2, 1, 0
 * 0, 2, 0, 1
 * 0, 1, 2, 0
 * ...
 *
 * Also supports passing in an array specifying the max number of items that can be placed in any given slot. If no
 * max counts are given, the max defaults to the integer max.
 */
public class SlottedCombinationIterator implements Iterator<int[]> {
  private final int[] counts;
  private final int[] maxCounts;
  private final int lastSlot;
  private boolean stillSearching = true;
  private boolean canAdvance = false;

  /**
   * Create an iterator that starts at the initial state given.
   *
   * @param initialState inital state to use.
   */
  public SlottedCombinationIterator(int[] initialState) {
    this(initialState, null);
  }

  /**
   * Create an iterator that starts at the initial state given, and which uses the given max counts.
   *
   * @param initialState inital state to use.
   * @param maxCounts Maximum number of items each slot can have. Null means each slot can have integer max.
   */
  public SlottedCombinationIterator(int[] initialState, int[] maxCounts) {
    this.counts = Arrays.copyOf(initialState, initialState.length);
    this.lastSlot = initialState.length - 1;
    if (maxCounts == null) {
      this.maxCounts = defaultMaxCounts(initialState.length);
    } else {
      Preconditions.checkArgument(maxCounts.length == initialState.length,
                                  "length of max counts does not match the number of slots");
      this.maxCounts = Arrays.copyOf(maxCounts, maxCounts.length);
    }
    // if the initial state does not follow max count constraints, we need to advance before the first next().
    for (int i = 0; i < initialState.length; i++) {
      if (initialState[i] > maxCounts[i]) {
        canAdvance = true;
        break;
      }
    }
  }

  /**
   * Create an iterator with the number of slots given and the number of items given.
   *
   * @param numSlots Number of slots to use.
   * @param numItems Number of items to use.
   */
  public SlottedCombinationIterator(int numSlots, int numItems) {
    this(numSlots, numItems, null);
  }

  /**
   * Create an iterator with the number of slots given and the number of items given, with a maximum number of items
   * in each slot specified by the max counts array. The first combination returned will be the first valid combination
   * given the maximum constraints.
   *
   * @param numSlots Number of slots to use.
   * @param numItems Number of items to use.
   * @param maxCounts Maximum number of items each slot can have. Null means each slot can have integer max.
   */
  public SlottedCombinationIterator(int numSlots, int numItems, int[] maxCounts) {
    Preconditions.checkArgument(numSlots > 0, "must have more than 0 slots");
    Preconditions.checkArgument(numItems > 0, "must have more than 0 items");

    if (maxCounts != null) {
      Preconditions.checkArgument(maxCounts.length == numSlots,
                                  "length of max counts does not match the number of slots");
      this.maxCounts = Arrays.copyOf(maxCounts, maxCounts.length);
    } else {
      this.maxCounts = new int[numSlots];
      for (int i = 0; i < this.maxCounts.length; i++) {
        this.maxCounts[i] = Integer.MAX_VALUE;
      }
    }

    this.lastSlot = numSlots - 1;
    int[] counts = new int[numSlots];
    counts[0] = numItems;
    for (int i = 1; i < counts.length; i++) {
      counts[i] = 0;
    }
    this.counts = counts;
    // if the initial state is invalid, we want to advance to a valid one before returning hasNext or next
    canAdvance = counts[0] > this.maxCounts[0];
  }

  @Override
  public boolean hasNext() {
    if (canAdvance) {
      advanceState();
      canAdvance = false;
    }
    return stillSearching;
  }

  @Override
  public int[] next() {
    if (this.hasNext()) {
      canAdvance = true;
      return Arrays.copyOf(counts, counts.length);
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove unsupported");
  }

  private int[] defaultMaxCounts(int numSlots) {
    int[] defaultMax = new int[numSlots];
    for (int i = 0; i < this.maxCounts.length; i++) {
      this.maxCounts[i] = Integer.MAX_VALUE;
    }
    return defaultMax;
  }

  /**
   * Move one item from one slot to another slot. Start at the rightmost slot, moving left until we find something
   * non-zero that we can move one slot to its right.
   */
  private void advanceState() {
    int index = rightMostMovableSlot();
    if (index == -1) {
      stillSearching = false;
      return;
    }
    int amountOverMax = counts[index] - maxCounts[index];
    int numToMove = Math.max(amountOverMax, 1);
    moveItemsRight(index, numToMove);
    /*
    int end = counts.length - 1;
    for (int i = end - 1; i >= 0; i--) {
      // we've found the first non-zero
      if (counts[i] > 0) {
        // this part takes 1 from the non-zero and moves it to the right one slot
        counts[i]--;

        // add count[end] because we want as many in slot i+1 as possible
        // ex: going from 2, 0, 0, 1 -> 1, 2, 0, 0
        int inc = 1 + counts[end] - counts[i + 1];
        counts[i + 1] += inc;

        // need to zero out counts[end] if we've added it to slot i+1
        if (i < end - 1) {
          counts[end] = 0;
        }
        return;
      }
    }
    stillSearching = false;*/
  }

  /**
   * Starting from the slot index, move the min number of items we can to the right.
   * For example, if we start out with:
   *
   * max:    5, 1, Inf
   * count:  2, 1, 0
   *
   * and are asked to move items from slot 0 to the right, we try and move 1 item from slot 0 into slot 1.  But since
   * slot 1 is already at its max, we instead move it to slot 2 to end up with:
   *
   * count:  1, 1, 1
   *
   * If we've reached the end and are unable to move anything more to the right, we need to take all items from slots
   * not at the front, and add them to the front.  For example:
   *
   * max:   2, 5, 5
   * count: 2, 2, 5
   *
   * Moving an item from slot 1 to slot 2 is impossible since it would violate the max.  Therefore we move all items
   * not in the front (5 from slot 2 and 2 from slot 1), one item from the front (1 item from slot 0), and place all
   * those items into the next slot from the front (7 items into slot 1), and continue from there.  In this case,
   * we would end up doing:
   *
   * moveItemsRight(1, 1)
   * count:  2, 1, 6
   * moveItemsRight(2, 1)
   * count:  1, 8, 0
   * moveItemsRight(1, 3)
   * count:  1, 5, 3
   *
   * Additionally, if we're moving items from the front, we need to take one item from the front plus all items in the
   * last slot and move them one slot to the right of the front. For example:
   *
   * count: 1, 0, 0, 2
   *
   * should transition to
   *
   * count: 0, 3, 0, 0
   *
   * Finally, if we ever get to the end and can't move anymore, we're done and there are no more combinations to
   * iterate through.
   *
   * @param slot slot to move items from.
   * @param count number of items to move from the slot.
   */
  private void moveItemsRight(int slot, int count) {
    // if we're at the front
    int frontSlot = leftMostOccupiedSlot();
    if (slot == frontSlot) {
      counts[slot] -= count;
      int itemsToAdd = count + counts[lastSlot];
      counts[lastSlot] = 0;
      addItemsToSlot(slot + 1, itemsToAdd);
    } else if (slot == lastSlot) {
      if (atEnd(frontSlot)) {
        stillSearching = false;
        return;
      }
      moveItemsRight(frontSlot, 1);
    } else {
      // we're not at the end yet. Take the count from this slot and try and move it one over to the right
      counts[slot] -= count;
      addItemsToSlot(slot + 1, count);
    }
  }

  private void addItemsToSlot(int slot, int count) {
    counts[slot] += count;
    int amountOverMax = counts[slot] - maxCounts[slot];
    if (amountOverMax > 0) {
      moveItemsRight(slot, amountOverMax);
    }
  }

  /**
   * Return whether or not we're at the end. If the front slot is the last slot, we're done. Also, if everything
   * is already at its max, we're done.
   *
   * @param frontSlot The first non-zero slot.
   * @return Whether or not the iterator is at its end.
   */
  private boolean atEnd(int frontSlot) {
    if (frontSlot == lastSlot) {
      return true;
    }
    for (int i = frontSlot; i <= lastSlot; i++) {
      if (counts[i] < maxCounts[i]) {
        return false;
      }
    }
    return true;
  }

  private int leftMostOccupiedSlot() {
    for (int i = 0; i <= lastSlot; i++) {
      if (counts[i] > 0) {
        return i;
      }
    }
    // shouldn't ever happen
    throw new IllegalStateException("invalid number of items");
  }

  /**
   * Starting at the 2 slot from the right, move left and check if items in that slot can be moved to the right.
   *
   * @return index of the right most movable slot, or the last slot if nothing is movable.
   */
  private int rightMostMovableSlot() {
    for (int i = lastSlot - 1; i >= 0; i--) {
      if (slotIsMovable(i)) {
        return i;
      }
    }
    // indicates nothing is movable.
    return -1;
  }

  /**
   * Check every slot to the right to see if items can be moved to them, returning true is one of them can take another
   * item and false if not.
   *
   * @param slot Slot to check.
   * @return True if an item can be taken from the slot and moved to the right, false otherwise.
   */
  private boolean slotIsMovable(int slot) {
    if (counts[slot] < 1) {
      return false;
    }
    for (int i = slot + 1; i <= lastSlot; i++) {
      if (counts[i] < maxCounts[i]) {
        return true;
      }
    }
    return false;
  }

  // get total number of nodes accounted for.
  private int getTotalCount(int[] counts) {
    int sum = 0;
    for (int count : counts) {
      sum += count;
    }
    return sum;
  }
}
