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

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 */
public class SlottedCombinationIteratorTest {

  @Test
  public void testHasNextDoesNotAdvanceStateNext() {
    Iterator<int[]> iter = new SlottedCombinationIterator(2, 2);
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(iter.hasNext());
    }
    iter.next();
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(iter.hasNext());
    }
    iter.next();
    for (int i = 0; i < 10; i++) {
      Assert.assertTrue(iter.hasNext());
    }
    iter.next();
    Assert.assertFalse(iter.hasNext());
  }

  @Test
  public void testTwoSlotsWithoutMaxCounts() {
    Iterator<int[]> iter = new SlottedCombinationIterator(2, 4);
    List<int[]> expected = ImmutableList.of(
      new int[]{4, 0}, new int[]{3, 1}, new int[]{2, 2}, new int[]{1, 3}, new int[]{0, 4}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testThreeSlotsWithoutMaxCounts() {
    Iterator<int[]> iter = new SlottedCombinationIterator(3, 3);
    List<int[]> expected = ImmutableList.of(
      new int[]{3, 0, 0},
      new int[]{2, 1, 0},
      new int[]{2, 0, 1},
      new int[]{1, 2, 0},
      new int[]{1, 1, 1},
      new int[]{1, 0, 2},
      new int[]{0, 3, 0},
      new int[]{0, 2, 1},
      new int[]{0, 1, 2},
      new int[]{0, 0, 3}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testThreeSlotsWithMaxCounts() {
    Iterator<int[]> iter = new SlottedCombinationIterator(3, 3, new int[]{5, 1, 5});
    List<int[]> expected = ImmutableList.of(
      new int[]{3, 0, 0},
      new int[]{2, 1, 0},
      new int[]{2, 0, 1},
      new int[]{1, 1, 1},
      new int[]{1, 0, 2},
      new int[]{0, 1, 2},
      new int[]{0, 0, 3}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testMaxCountZero() {
    Iterator<int[]> iter = new SlottedCombinationIterator(5, 2, new int[]{0, 2, 1, 0, 0});
    List<int[]> expected = ImmutableList.of(
      new int[]{0, 2, 0, 0, 0},
      new int[]{0, 1, 1, 0, 0}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testCarryAndSkipWithZero() {
    Iterator<int[]> iter = new SlottedCombinationIterator(4, 5, new int[]{100, 1, 2, 0});
    List<int[]> expected = ImmutableList.of(
      new int[]{5, 0, 0, 0},
      new int[]{4, 1, 0, 0},
      new int[]{4, 0, 1, 0},
      new int[]{3, 1, 1, 0},
      new int[]{3, 0, 2, 0},
      new int[]{2, 1, 2, 0}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testCarryAndSkipWithZeroAndInitialState() {
    Iterator<int[]> iter = new SlottedCombinationIterator(new int[]{3, 1, 1, 0}, new int[]{100, 1, 2, 0});
    List<int[]> expected = ImmutableList.of(
      new int[]{3, 1, 1, 0},
      new int[]{3, 0, 2, 0},
      new int[]{2, 1, 2, 0}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testInitialStateNotUsedEvenWhenInvalid() {
    Iterator<int[]> iter = new SlottedCombinationIterator(new int[]{5, 0}, new int[]{1, 5});
    List<int[]> expected = ImmutableList.of(
      new int[]{1, 4},
      new int[]{0, 5}
    );
    assertIteratorIsExpected(expected, iter);
  }

  @Test
  public void testInvalidInitialCombination() {
    // first slot normally starts out with 2, but because of max constraints it should start with 1.
    Iterator<int[]> iter = new SlottedCombinationIterator(2, 2, new int[]{1, 5});
    Assert.assertTrue(iter.hasNext());
    Assert.assertTrue(Arrays.equals(new int[]{1, 1}, iter.next()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidSlotsThrowsException() {
    new SlottedCombinationIterator(0, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNumItemsThrowsException() {
    new SlottedCombinationIterator(1, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxCountsBadLengthThrowsException() {
    new SlottedCombinationIterator(3, 5, new int[]{10, 5});
  }

  @Test(expected = NoSuchElementException.class)
  public void testThrowsExceptionAtEndOfIter() {
    Iterator<int[]> iter = new SlottedCombinationIterator(1, 1);
    iter.next();
    iter.next();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitialStateAndMaxCountsLengthMismatch() {
    new SlottedCombinationIterator(new int[]{5}, new int[]{10, 5});
  }

  private void assertIteratorIsExpected(List<int[]> expected, Iterator<int[]> iter) {
    for (int i = 0; i < expected.size(); i++) {
      Assert.assertTrue(iter.hasNext());
      Assert.assertTrue(Arrays.equals(expected.get(i), iter.next()));
    }
    Assert.assertFalse(iter.hasNext());
  }
}
