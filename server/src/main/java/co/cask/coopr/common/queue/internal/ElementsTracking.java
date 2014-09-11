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
package co.cask.coopr.common.queue.internal;

import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueuedElement;

import java.util.List;

/**
 * Defines collection used to track consumption of elements in a queue.
 */
public interface ElementsTracking {

  /**
   * Add an {@link Element} to the queue.
   *
   * @param element Element to add to the queue.
   * @return true if the element was added, false if not.
   */
  boolean addToQueue(Element element);

  /**
   * Start consuming an element from the queue.
   *
   * @param consumerId Id of the consumer.
   * @return Element to start consuming.
   */
  Element startConsuming(String consumerId);

  /**
   * Stop consuming an element and add it back to the queue to allow it to be consumed again. The operation may
   * fail if there is no element in the queue matching the element id, or if the element is being consumed by a
   * consumer other than the one given.
   *
   * @param elementId Id of the element to stop consuming.
   * @param consumerId Id of the consumer of the element.
   * @return true if the operation was successful, false if it failed.
   */
  boolean stopConsumingAndAddBackToQueue(String elementId, String consumerId);

  /**
   * Finish consuming an element, removing it from the queue. The operation may fail if there is no element in
   * the queue matching the element id, or if the element is being consumed by a consumer other than the one given.
   *
   * @param elementId Id of the element to finish consuming.
   * @param consumerId Id of the consumer of the element.
   * @return true if the operation was successful, false if it failed.
   */
  boolean finishConsuming(String elementId, String consumerId);

  /**
   * Record progress of an element in the queue. The operation may fail if there is no element in
   * the queue matching the element id, or if the element is being consumed by a consumer other than the one given.
   *
   * @param elementId Id of the element to record progress for.
   * @param consumerId Id of the consumer of the element.
   * @return true if progress was recorded, false if it failed.
   */
  boolean recordProgress(String elementId, String consumerId);

  /**
   * Walk through the elements that are being consumed, calling
   * {@link Walker#process(co.cask.coopr.common.queue.Element, String, long)} on each element.
   *
   * @param walker walker for processing each element being consumed.
   */
  void walkThruElementsBeingConsumed(Walker walker);

  /**
   * Remove an element from the queue, regardless of who the consumer is.
   *
   * @param elementId Id of the element to remove.
   * @return true if the element was removed or never existed to begin with, false if there was an exception removing.
   */
  boolean remove(String elementId);

  /**
   * Remove all items from the queue.
   *
   * @return true if all elements were removed, false if there was an exception removing.
   */
  boolean removeAll();

  /**
   * Sets the priority of the given element to the highest priority if the element exists and is not being consumed.
   *
   * @param elementId Id of the element whose priority should be changed.
   * @return true if successful, false if not.
   */
  boolean toHighestPriority(String elementId);

  /**
   * Get a list of elements in the queue that are not being consumed.
   *
   * @return List of elements in the queue that are not being consumed.
   */
  List<QueuedElement> getQueued();

  /**
   * Get a list of elements in the queue that are being consumed.
   *
   * @return List of elements in the queue that are being consumed.
   */
  List<QueuedElement> getBeingConsumed();

  /**
   * Get the size of the queue, which includes both elements that are being consumed and elements that are not being
   * consumed.
   *
   * @return size of the queue.
   */
  int size();

  /**
   * Walker interface to process Queue Element.
   */
  static interface Walker {
    /**
     * Determine whether something needs to happen to a queue element.
     *
     * @param element Element to process.
     * @param consumerId Id of the consumer of the element.
     * @param lastProgressReportTs Timestamp in milliseconds of last progress report on the element.
     * @return true if something should happen to the element, false if not.
     */
    boolean process(Element element, String consumerId, long lastProgressReportTs);
  }
}
