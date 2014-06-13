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
 *
 */
public interface QueueGroup {

  ListenableFuture<String> add(String queueName, Element element);

  GroupElement take(String consumerId);

  Element take(String queueName, String consumerId);

  TrackingQueue.PossessionState recordProgress(String consumerId, String queueName, String elementId,
                                               TrackingQueue.ConsumingStatus status, String result);

  boolean remove(String queueName, String elementId);

  boolean removeAll();

  boolean removeAll(String queueName);

  void hideQueue(String queueName);

  int size(String queueName);

  Collection<String> getQueueNames();

  Iterator<QueuedElement> getBeingConsumed(String queueName);

  Iterator<QueuedElement> getQueued(String queueName);
}
