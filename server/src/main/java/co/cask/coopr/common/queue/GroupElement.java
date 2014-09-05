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
 * An element taken from a {@link QueueGroup}, indicating which queue the element was taken from as well as the
 * element itself.
 */
public class GroupElement {
  private final String queueName;
  private final Element element;

  /**
   * Create a group element where the given element was taken from the given queue.
   *
   * @param queueName Name of the queue the element was taken from.
   * @param element Element that was taken.
   */
  public GroupElement(String queueName, Element element) {
    this.queueName = queueName;
    this.element = element;
  }

  /**
   * Get the name of the queue the element was taken from.
   *
   * @return Name of the queue the element was taken from.
   */
  public String getQueueName() {
    return queueName;
  }

  /**
   * Get the element that was taken.
   *
   * @return Element that was taken.
   */
  public Element getElement() {
    return element;
  }
}
