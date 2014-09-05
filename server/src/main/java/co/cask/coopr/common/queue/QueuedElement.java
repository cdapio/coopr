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

/**
 * Represents an Element that is queued.
 */
public interface QueuedElement {
  /**
   * Get the queued {@link Element}.
   *
   * @return The queued {@link Element}.
   */
  Element getElement();

  /**
   * Get the timestamp in milliseconds of the last time a status update was performed on the element.
   *
   * @return Timestamp in milliseconds of the last time a status update was performed on the element.
   */
  long getStatusTime();

  /**
   * Get the consumer id of the consumer that has taken this element.
   *
   * @return consumerId of the consumer that has taken this element. Will be empty if not being consumed.
   */
  String getConsumerId();
}
