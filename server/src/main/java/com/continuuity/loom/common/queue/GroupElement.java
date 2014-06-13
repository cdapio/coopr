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

/**
 * The element to be placed in the {@link com.continuuity.loom.common.queue.TrackingQueue}.
 */
public class GroupElement {
  private final String queueName;
  private final Element element;

  public GroupElement(String queueName, Element element) {
    this.queueName = queueName;
    this.element = element;
  }

  public String getQueueName() {
    return queueName;
  }

  public Element getElement() {
    return element;
  }
}
