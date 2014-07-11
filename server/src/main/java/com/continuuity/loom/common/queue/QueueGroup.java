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

package com.continuuity.loom.common.queue;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Iterator;

/**
 * A group of {@link TrackingQueue TrackingQueues} that usually serve a similar purpose. Elements must be added to
 * a specific queue in the group, but elements can be taken either from a specific queue, or from some queue determined
 * by the underlying implementation, such as in a round-robin fashion.
 *
 * One example use case is if multiple queues are being used to manage tasks of some sort, with each queue holding
 * tasks for a specific tenant to ensure quality of service across tenants.
 */
public interface QueueGroup {

  /**
   * Adds the given elements to the specified queue.
   *
   * @param queueName Name of the queue to add the element to.
   * @param element Element to add to the queue.
   * @return future of the consuming element. It is set when consuming is reported as completed successfully.
   */
  ListenableFuture<String> add(String queueName, Element element);

  /**
   * Take an element from a queue in the group, or null if there are no elements to take.
   *
   * @param consumerId Id of the consumer taking the element.
   * @return Element from a queue in the group, or null if there are no elements to take.
   */
  GroupElement take(String consumerId);

  /**
   * Take an element from a specific queue in the group, or null if there are no elements to take.
   *
   * @param queueName Name of the queue to take an element from.
   * @param consumerId Id of the consumer taking the element.
   * @return Element from the queue, or null if there are no elements to take.
   */
  Element take(String queueName, String consumerId);

  /**
   * Records progress of consuming the given element from the given queue.
   *
   * @param consumerId Id of the consumer that took the element.
   * @param queueName Name of the queue the element came from.
   * @param elementId Id of the element to record progress from.
   * @param status Status of the element.
   * @param result Current value of the result of consuming the element.
   * @return State indicating whether or not the consumer still possesses the element. A consumer can lose possession
   *         if the underlying implementation timed out the consumption.
   */
  TrackingQueue.PossessionState recordProgress(String consumerId, String queueName, String elementId,
                                               TrackingQueue.ConsumingStatus status, String result);

  /**
   * Remove the specified element from the specified queue.
   *
   * @param queueName Queue to remove the element from.
   * @param elementId Id of the element to remove.
   * @return True if removed successfully or element was not present in the queue. False otherwise.
   */
  boolean remove(String queueName, String elementId);

  /**
   * Remove all elements from all queues in the group.
   *
   * @return True if all elements in all queues were removed successfully, false if not.
   */
  boolean removeAll();

  /**
   * Remove all elements from a specific queue in the group.
   *
   * @param queueName Queue to remove all items from.
   * @return True if all elements were removed successfully, false if not.
   */
  boolean removeAll(String queueName);

  /**
   * Remove the specified queue from the group. The queue itself may still exist, it is just no longer managed by this
   * group. This means elements in the queue will not be returned by the
   * {@link #take(String)} method. Referencing the queue again in any other method will add it back to the group.
   *
   * @param queueName Name of the queue to remove from the group.
   */
  void removeQueue(String queueName);

  /**
   * Get the size (both queued and being consumed) of a specific queue.
   *
   * @param queueName Name of the queue to get the size of.
   * @return Size of the specified queue.
   */
  int size(String queueName);

  /**
   * Get the name of all queues in the group.
   *
   * @return Name of all queues in the group.
   */
  Collection<String> getQueueNames();

  /**
   * Get all elements being consumed from a specific queue.
   *
   * @param queueName Queue to get elements being consumed from.
   * @return Iterator over all elements being consumed from the queue.
   */
  Iterator<QueuedElement> getBeingConsumed(String queueName);

  /**
   * Get all elements that are queued in the specific queue.
   *
   * @param queueName Queue to get queued elements from.
   * @return Iterator over all queued elements in the queue.
   */
  Iterator<QueuedElement> getQueued(String queueName);
}
