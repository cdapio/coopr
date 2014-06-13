package com.continuuity.loom.common.queue.internal;

import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.GroupElement;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.QueueType;
import com.continuuity.loom.common.queue.QueuedElement;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.utils.ImmutablePair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.twill.zookeeper.ZKClient;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ZKQueueGroup implements QueueGroup {
  private final ZKClient zkClient;
  private final QueueType queueType;
  private final List<ImmutablePair<String, TrackingQueue>> queueList;
  private final Map<String, TrackingQueue> queueMap;
  private int index;

  public ZKQueueGroup(final ZKClient zkClient, final QueueType queueType) {
    this.zkClient = zkClient;
    this.queueType = queueType;
    this.queueList = Lists.newArrayList();
    this.index = 0;
    this.queueMap = Maps.newHashMap();
  }

  @Override
  public ListenableFuture<String> add(String queueName, Element element) {
    return checkAndGetQueue(queueName).add(element);
  }

  @Override
  public synchronized GroupElement take(String consumerId) {
    int numQueues = queueList.size();
    if (numQueues == 0) {
      return null;
    }
    // go through all queues in a round robin fashion, returning the element from the first queue that is non-empty.
    int startIndex = index;
    do {
      ImmutablePair<String, TrackingQueue> queueInfo = queueList.get(index);
      index = index == (numQueues - 1) ? 0 : index + 1;
      Element element = queueInfo.getSecond().take(consumerId);
      if (element != null) {
        return new GroupElement(queueInfo.getFirst(), element);
      }
    } while (index != startIndex);
    return null;
  }

  @Override
  public Element take(String queueName, String consumerId) {
    return checkAndGetQueue(queueName).take(consumerId);
  }

  @Override
  public TrackingQueue.PossessionState recordProgress(String consumerId, String queueName, String elementId,
                                                      TrackingQueue.ConsumingStatus status, String result) {
    return checkAndGetQueue(queueName).recordProgress(consumerId, elementId, status, result);
  }

  @Override
  public boolean remove(String queueName, String elementId) {
    return checkAndGetQueue(queueName).remove(elementId);
  }

  @Override
  public boolean removeAll() {
    boolean allRemoved = true;
    for (TrackingQueue queue : queueMap.values()) {
      allRemoved = allRemoved && queue.removeAll();
    }
    return allRemoved;
  }

  @Override
  public boolean removeAll(String queueName) {
    return checkAndGetQueue(queueName).removeAll();
  }

  @Override
  public synchronized void hideQueue(String queueName) {
    if (queueMap.containsKey(queueName)) {
      TrackingQueue queue = queueMap.get(queueName);
      queueList.remove(ImmutablePair.of(queueName, queue));
      if (index > (queueList.size() - 1)) {
        index = 0;
      }
      queueMap.remove(queue);
    }
  }

  @Override
  public int size(String queueName) {
    return checkAndGetQueue(queueName).size();
  }

  @Override
  public Collection<String> getQueueNames() {
    return queueMap.keySet();
  }

  @Override
  public Iterator<QueuedElement> getBeingConsumed(String queueName) {
    return checkAndGetQueue(queueName).getBeingConsumed();
  }

  @Override
  public Iterator<QueuedElement> getQueued(String queueName) {
    return checkAndGetQueue(queueName).getQueued();
  }

  private TrackingQueue checkAndGetQueue(String queueName) {
    if (!queueMap.containsKey(queueName)) {
      addQueue(queueName);
    }
    return queueMap.get(queueName);
  }

  private synchronized void addQueue(String queueName) {
    if (!queueMap.containsKey(queueName)) {
      TrackingQueue queue =
        new ElementsTrackingQueue(new ZKElementsTracking(zkClient, queueType.getPath() + "/" + queueName));
      queueMap.put(queueName, queue);
      queueList.add(ImmutablePair.of(queueName, queue));
    }
  }
}
