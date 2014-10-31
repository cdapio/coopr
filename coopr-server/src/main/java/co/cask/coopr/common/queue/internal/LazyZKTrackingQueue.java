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
import co.cask.coopr.common.queue.TrackingQueue;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.twill.zookeeper.ZKClient;

import java.util.Iterator;

/**
 * Tracking queue backed by zookeeper that does not initialize any state in zookeeper until some queue operation is
 * performed.
 */
public class LazyZKTrackingQueue implements TrackingQueue {
  private final ZKClient zkClient;
  private final String zkQueuePath;
  private TrackingQueue queue;

  public LazyZKTrackingQueue(ZKClient zkClient, String zkQueuePath) {
    this.zkClient = zkClient;
    this.zkQueuePath = zkQueuePath;
  }

  private synchronized TrackingQueue getQueue() {
    if (queue == null) {
      queue = new ElementsTrackingQueue(new ZKElementsTracking(zkClient, zkQueuePath));
    }
    return queue;
  }

  @Override
  public ListenableFuture<String> add(Element element) {
    return getQueue().add(element);
  }

  @Override
  public Element take(String consumerId) {
    return getQueue().take(consumerId);
  }

  @Override
  public TrackingQueue.PossessionState recordProgress(String consumerId, String elementId,
                                                      TrackingQueue.ConsumingStatus status, String result) {
    return getQueue().recordProgress(consumerId, elementId, status, result);
  }

  @Override
  public boolean remove(String elementId) {
    return getQueue().remove(elementId);
  }

  @Override
  public boolean removeAll() {
    return getQueue().removeAll();
  }

  @Override
  public boolean toHighestPriority(String elementId) {
    return getQueue().toHighestPriority(elementId);
  }

  @Override
  public Iterator<QueuedElement> getQueued() {
    return getQueue().getQueued();
  }

  @Override
  public Iterator<QueuedElement> getBeingConsumed() {
    return getQueue().getBeingConsumed();
  }

  @Override
  public int size() {
    return getQueue().size();
  }
}
