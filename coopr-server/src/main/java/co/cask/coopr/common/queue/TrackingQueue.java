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
package co.cask.coopr.common.queue;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Iterator;

/**
 * Queue that tracks consuming of the elements and may re-add elements that were attempted to be consumed.
 * E.g. when consumer fails to ack finish of processing of the element.
 * TODO: consider making generic, current implementation works only with Strings
 */
public interface TrackingQueue {
  /**
   * Adds element to the queue.
   *
   * @param element to add
   * @return future of the consuming result. It will be set when consuming is successfully reported to be completed
   */
  ListenableFuture<String> add(Element element);

  /**
   * Take next available element from the queue act on it.
   * <p>
   * After element is taken it is no longer available for others to take unless it is not placed back to queue.
   * Element can be placed back in the following cases:
   * <ul>
   *  <li>
   *   By invoking {@link #add(Element)} method. In this case element will
   *   be considered a new element never seen before.
   *   Element will not be removed from list of "in-progress" elements list automatically.
   *  </li>
   *  <li>
   *   By reporting {@link ConsumingStatus#FAILED} status via
   *   {@link #recordProgress(String, String, ConsumingStatus, String)}.
   *   Element will be rescheduled automatically and removed from the "in-progress" list.
   *  </li>
   *  <li>
   *   Element can be rescheduled automatically based on the implementation logic of subclasses.
   *  </li>
   * </ul>
   * </p>
   *
   * @param consumerId element consumer
   * @return next available element, or null if no element is available
   */
  Element take(String consumerId);

  /**
   * Records progress of consuming the element. If element no longer belongs to this consumer this will be noted in
   * returned possession state as {@link PossessionState#NOT_POSSESSES}. This may happen e.g. if {@link TrackingQueue}
   * implementation decided that this consumer is dead and put it back to queue.
   * <p>
   * If {@link ConsumingStatus#FAILED} status is reported,
   * element will be placed back to queue automatically
   * </p>
   * <p>
   * If {@link ConsumingStatus#FINISHED_SUCCESSFULLY} status is reported
   * the queue no longer tracks the element consuming status.
   * </p>
   *
   * @param consumerId consumer that is processing the element and reporting the progress
   * @param elementId  element to report progress on
   * @param status     status of the consuming.
   * @param result     current value of the result of consuming.
   * @return true if this consumer still possesses the element and can continue processing it.
   */
  PossessionState recordProgress(String consumerId, String elementId, ConsumingStatus status, String result);

  /**
   * Removes element from the queue by element id. If element was being consumed,
   * @param elementId id of the element to remove
   * @return true if removed successfully or element was not present in the queue, false otherwise
   */
  boolean remove(String elementId);

  /**
   * Removes all elements from the queue. See {@link #remove(String)} for more details.
   * @return true if all removed successfully, false otherwise
   */
  boolean removeAll();

  /**
   * Promotes element to the top of the queue.
   * @param elementId id of the element to promote
   * @return true if promoted successfully
   */
  boolean toHighestPriority(String elementId);

  /**
   * Get all {@link QueuedElement} in the queue that are not being consumed.
   *
   * @return an iterator over the queued (but not being consumed) elements in this queue. The iterator returns elements
   *         ordered in the way they are offered to be consumed starting with the top of the queue.
   */
  Iterator<QueuedElement> getQueued();

  /**
   * Get all {@link QueuedElement} that are being consumed.
   *
   * @return an iterator over the queue elements that are being consumed.
   */
  Iterator<QueuedElement> getBeingConsumed();

  /**
   * Get the size of the queue (both queued and being consumed).
   *
   * @return size of queue (both queued and being consumed).
   */
  int size();

  /**
   * Defines Tracking Queue Consuming Status.
   */
  static enum ConsumingStatus {
    IN_PROGRESS,
    FINISHED_SUCCESSFULLY,
    FAILED
  }

  /**
   * Defines PossessionState.
   */
  static enum PossessionState {
    POSSESSES,
    NOT_POSSESSES
  }
}
