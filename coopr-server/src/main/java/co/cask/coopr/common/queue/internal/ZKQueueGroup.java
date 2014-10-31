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
import co.cask.coopr.common.queue.GroupElement;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.QueuedElement;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.common.zookeeper.ZKClientExt;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.twill.zookeeper.NodeChildren;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKOperations;
import org.apache.zookeeper.KeeperException;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Implementation of a {@link QueueGroup} that uses queues built on zookeeper. Whenever a queue name is referenced in a
 * method, the queue is cached and the physical zookeeper queue is created if it does not already exist. Watches
 * zookeeper to make sure queues added or deleted by another instance of the group are reflected in this group.
 */
public class ZKQueueGroup extends AbstractIdleService implements QueueGroup {
  private final ZKClient zkClient;
  private final QueueType queueType;
  private final LoadingCache<String, TrackingQueue> queueMap;

  /**
   * Create a zookeeper queue group of the given type, using the given zookeeper client. Physical queues in the group
   * are namespaced by the namespace of the client plus the queue type and queue name. As such, if two queue groups
   * of the same type should not conflict, the namespace of their clients should not be the same.
   *
   * @param zkClient Client to use for zookeeper operations.
   * @param queueType Type of queues in the group.
   */
  ZKQueueGroup(final ZKClient zkClient, final QueueType queueType) {
    this.zkClient = zkClient;
    this.queueType = queueType;
    this.queueMap = CacheBuilder.newBuilder().build(
      new CacheLoader<String, TrackingQueue>() {
        @Override
        public TrackingQueue load(String queueName) throws Exception {
          return new LazyZKTrackingQueue(zkClient, getZKPathForQueue(queueName));
        }
      });
  }

  @Override
  public ListenableFuture<String> add(String queueName, Element element) {
    return queueMap.getUnchecked(queueName).add(element);
  }

  /**
   * Returns a live iterator that cycles through queues in the group in a round-robin fashion, returning an element from
   * the first queue that has one available. If all queues are cycled through once without an element, null is returned.
   *
   * @param consumerId Id of the consumer taking the element.
   * @return An element from a queue in the group, or null if none exists.
   */
  @Override
  public Iterator<GroupElement> takeIterator(String consumerId) {
    return new GroupElementIterator(consumerId);
  }

  @Override
  public Element take(String queueName, String consumerId) {
    return queueMap.getUnchecked(queueName).take(consumerId);
  }

  @Override
  public TrackingQueue.PossessionState recordProgress(String consumerId, String queueName, String elementId,
                                                      TrackingQueue.ConsumingStatus status, String result) {
    return queueMap.getUnchecked(queueName).recordProgress(consumerId, elementId, status, result);
  }

  @Override
  public boolean remove(String queueName, String elementId) {
    return queueMap.getUnchecked(queueName).remove(elementId);
  }

  @Override
  public boolean removeAll() {
    boolean allRemoved = true;
    for (TrackingQueue queue : queueMap.asMap().values()) {
      allRemoved = allRemoved && queue.removeAll();
    }
    return allRemoved;
  }

  @Override
  public boolean removeAll(String queueName) {
    return queueMap.getUnchecked(queueName).removeAll();
  }

  @Override
  public int size(String queueName) {
    return queueMap.getUnchecked(queueName).size();
  }

  @Override
  public Set<String> getQueueNames() {
    return queueMap.asMap().keySet();
  }

  @Override
  public Iterator<QueuedElement> getBeingConsumed(String queueName) {
    return queueMap.getUnchecked(queueName).getBeingConsumed();
  }

  @Override
  public Iterator<QueuedElement> getQueued(String queueName) {
    return queueMap.getUnchecked(queueName).getQueued();
  }

  @Override
  protected void startUp() throws Exception {
    Futures.getUnchecked(ZKClientExt.ensureExists(zkClient, queueType.getPath()));
    refreshQueues(Futures.getUnchecked(zkClient.getChildren(queueType.getPath())));
    ZKOperations.watchChildren(zkClient, queueType.getPath(), new ZKOperations.ChildrenCallback() {
      @Override
      public void updated(NodeChildren nodeChildren) {
        refreshQueues(nodeChildren);
      }
    });
  }

  @Override
  protected void shutDown() throws Exception {
    // no-op
  }

  private void refreshQueues(NodeChildren nodeChildren) {
    Set<String> queueNames = Sets.newHashSet(nodeChildren.getChildren());
    Set<String> existingQueues = queueMap.asMap().keySet();

    Set<String> toAdd = Sets.difference(queueNames, existingQueues);
    for (String queueName : toAdd) {
      queueMap.refresh(queueName);
    }

    Set<String> toRemove = Sets.difference(existingQueues, queueNames);
    for (String queueName : toRemove) {
      queueMap.invalidate(queueName);
    }
  }

  private class GroupElementIterator implements Iterator<GroupElement> {
    private final String consumerId;
    private GroupElement nextElement;
    private boolean foundElement = false;
    private Iterator<Map.Entry<String, TrackingQueue>> currentBatch;

    private GroupElementIterator(String consumerId) {
      this.consumerId = consumerId;
      this.currentBatch = queueMap.asMap().entrySet().iterator();
    }

    @Override
    public boolean hasNext() {
      if (foundElement) {
        return true;
      }
      nextElement = getNextElement();
      // if the current batch of queues was exhausted without finding an element, go through one more time to check
      // queues in the group that we haven't checked yet.
      if (nextElement == null) {
        this.currentBatch = queueMap.asMap().entrySet().iterator();
        nextElement = getNextElement();
      }
      foundElement = nextElement != null;
      return foundElement;
    }

    private GroupElement getNextElement() {
      while (currentBatch.hasNext()) {
        Map.Entry<String, TrackingQueue> currentQueueEntry = currentBatch.next();
        TrackingQueue queue = currentQueueEntry.getValue();
        String queueName = currentQueueEntry.getKey();
        Element element = queue.take(consumerId);
        if (element != null) {
          return new GroupElement(queueName, element);
        }
      }
      return null;
    }

    @Override
    public GroupElement next() {
      if (hasNext()) {
        foundElement = false;
        return nextElement;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private String getZKPathForQueue(String queueName) {
    return queueType.getPath() + "/" + queueName;
  }
}
