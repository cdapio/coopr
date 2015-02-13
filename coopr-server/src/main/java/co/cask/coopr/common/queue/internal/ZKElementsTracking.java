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
import co.cask.coopr.common.zookeeper.ZKClientExt;
import co.cask.coopr.common.zookeeper.lib.ReentrantDistributedLock;
import co.cask.coopr.common.zookeeper.lib.Serializer;
import co.cask.coopr.common.zookeeper.lib.SynchronizedZKMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import org.apache.twill.zookeeper.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;


/**
 * Simple implementation that uses ZK to store element data.
 * <p/>
 * Given that queue is not going to be big, at max will contain hundreds or thousands of elements *for simplicity* we
 * use {@link Map} backed by ZK - underneath:
 * <p/>
 * Again, for simplicity we use *single* lock and wrap all methods with it.
 */
public class ZKElementsTracking implements ElementsTracking {
  private static final Logger LOG = LoggerFactory.getLogger(ZKElementsTracking.class);
  // Moves to the top of the queue. Since we reset priority when we start consuming, it is safe to use 0L here
  // See {@link #getCurrentHighestPriority()} for more info.
  private static final long HIGHEST_PRIORITY = 0L;
  private static final String NO_CONSUMER_ASSIGNED = "";
  private static final EntrySerializer ENTRY_SERIALIZER = new EntrySerializer();

  private final ThreadLocal<Lock> globalLock;
  private final Map<String, Entry> queueElements;

  public ZKElementsTracking(final ZKClient zkClient, final String basePath)  {
    String queuePath = basePath + "/queue";
    Futures.getUnchecked(ZKClientExt.ensureExists(zkClient, queuePath));
    this.queueElements = new SynchronizedZKMap<Entry>(zkClient, queuePath + "/map", ENTRY_SERIALIZER);

    this.globalLock = new ThreadLocal<Lock>() {
      @Override
      protected Lock initialValue() {
        return new ReentrantDistributedLock(zkClient, basePath);
      }
    };
  }

  @Override
  public boolean addToQueue(Element element) {
    try {
      // we actually may need no lock here: we just adding new element (it is assumed that test adds unique
      // elems into queue)
      globalLock.get().lock();
      try {
        Entry entry = new Entry(element, getCurrentHighestPriority());
        queueElements.put(entry.element.getId(), entry);
        return true;
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during adding to queue", e);
      return false;
    }
  }

  @Override
  public Element startConsuming(String consumerId) {
    try {
      globalLock.get().lock();
      try {
        Entry entry = getNotStartedWithHighestPriority();
        if (entry == null) {
          return null;
        }
        entry.consumerId = consumerId;
        entry.lastProgressReportTs = System.currentTimeMillis();
        entry.priority = getCurrentHighestPriority();
        queueElements.put(entry.element.getId(), entry);
        return entry.element;
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during start consuming", e);
      return null;
    }
  }

  private Entry getNotStartedWithHighestPriority() throws Exception {
    Entry result = null;
    for (Entry entry : queueElements.values()) {
      // we are walking thru *all* element items, hence need to skip those that are in progress
      if (!NO_CONSUMER_ASSIGNED.equals(entry.consumerId)) {
        continue;
      }
      // choosing entry with highest priority
      if (result == null || result.priority > entry.priority) {
        result = entry;
      }
    }
    return result;
  }

  @Override
  public boolean stopConsumingAndAddBackToQueue(String elementId, String consumerId) {
    try {
      globalLock.get().lock();
      try {
        Entry entry = queueElements.get(elementId);

        if (entry == null || !consumerId.equals(entry.consumerId)) {
          return false;
        }
        stopAndReschedule(entry);

      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during stop & reschedule", e);
      // it is OK to leave it now in "in-progress" list - we'll attempt to reschedule it by timeout
    }
    // TODO: what if we failed?
    return true;
  }

  private void stopAndReschedule(Entry entry) throws Exception {
    entry.consumerId = NO_CONSUMER_ASSIGNED;
    entry.lastProgressReportTs = 0;
    queueElements.put(entry.element.getId(), entry);
  }

  @Override
  public boolean finishConsuming(String elementId, String consumerId) {
    try {
      globalLock.get().lock();
      try {
        Entry entry = queueElements.get(elementId);
        if (entry == null || !consumerId.equals(entry.consumerId)) {
          return false;
        }
        queueElements.remove(elementId);

      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during marking finishConsuming", e);
      // TODO: this is actually bad: we will try to do element again, even though it was done already :(
    }
    // TODO: what if we failed?
    return true;
  }

  @Override
  public boolean recordProgress(String elementId, String consumerId) {
    try {
      globalLock.get().lock();
      try {
        Entry entry = queueElements.get(elementId);
        if (entry == null || !consumerId.equals(entry.consumerId)) {
          return false;
        }
        entry.lastProgressReportTs = System.currentTimeMillis();
        queueElements.put(entry.element.getId(), entry);

      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during checking state", e);
      // should be OK not do anything: we are just reporting a progress...
    }
    // TODO: what if we failed?
    return true;
  }

  @Override
  public void walkThruElementsBeingConsumed(Walker walker) {
    try {
      globalLock.get().lock();
      try {
        for (Entry entry : queueElements.values()) {
          // we are walking thru *all* element items, hence need to skip those not in progress
          if (NO_CONSUMER_ASSIGNED.equals(entry.consumerId)) {
            continue;
          }
          boolean stopAndReschedule = walker.process(entry.element,
                                                     entry.consumerId,
                                                     entry.lastProgressReportTs);
          if (stopAndReschedule) {
            stopAndReschedule(entry);
          }
        }

      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during walking", e);
      // should be OK not do anything: we'll walk thru next time :)
    }
  }

  @Override
  public boolean remove(String elementId) {
    try {
      globalLock.get().lock();
      try {
        queueElements.remove(elementId);
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during removing element", e);
      return false;
    }
    return true;
  }

  @Override
  public boolean removeAll() {
    try {
      globalLock.get().lock();
      try {
        queueElements.clear();
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during cleanup of the queue", e);
      return false;
    }

    return true;
  }

  @Override
  public boolean toHighestPriority(String elementId) {
    try {
      globalLock.get().lock();
      try {
        Entry entry = queueElements.get(elementId);
        if (NO_CONSUMER_ASSIGNED.equals(entry.consumerId)) {
          entry.priority = HIGHEST_PRIORITY;
          queueElements.put(entry.element.getId(), entry);
        }
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during promoting element to highest priority", e);
      // should be OK not do anything: we'll walk thru next time :)
    }

    return true;
  }

  @Override
  public List<QueuedElement> getQueued() {
    List<QueuedElement> list = Lists.newArrayList();
    try {
      globalLock.get().lock();
      try {
        List<Entry> all = Lists.newArrayList(queueElements.values());
        // we want to return the list ordered by priority
        Collections.sort(all);
        for (Entry entry : all) {
          if (NO_CONSUMER_ASSIGNED.equals(entry.consumerId)) {
            list.add(entry);
          }
        }
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during getting queued elements", e);
    }

    return list;
  }

  @Override
  public List<QueuedElement> getBeingConsumed() {
    ImmutableList.Builder<QueuedElement> listBuilder = new ImmutableList.Builder<QueuedElement>();
    try {
      globalLock.get().lock();
      try {
        for (Entry entry : queueElements.values()) {
          if (!NO_CONSUMER_ASSIGNED.equals(entry.consumerId)) {
            listBuilder.add(entry);
          }
        }
      } finally {
        globalLock.get().unlock();
      }
    } catch (Exception e) {
      LOG.error("error during getting queued elements", e);
    }

    return listBuilder.build();
  }

  /**
   * @return the highest priority an element which is currently in the queue can have
   */
  private long getCurrentHighestPriority() {
    // We use "queued ts" or "last consume attempt start ts" as priority.
    // When element is started to being consumed we reset priority to current ts. It is OK that those that were queued
    // later may have higher priority than added earlier element that was attempted to be consumed. This helps to
    // prevent "bad items" (that always fail to be consumed for whatever reason) to block new ones from being consumed
    // by occupying all consumers.
    // At the same time those attempted to be consumed still remain quite high in the queue, which is what desired:
    // we want to retry consuming after the fail earlier.
    return System.currentTimeMillis();
  }

  static class Entry implements QueuedElement, Comparable<Entry> {
    Element element;
    long priority;
    // will be empty if it is not in progress
    String consumerId;
    long lastProgressReportTs;

    public Entry(Element element, long priority) {
      this(element, priority, 0L, NO_CONSUMER_ASSIGNED);
    }

    public Entry(Element element, long priority, long lastProgressReportTs, String consumerId) {
      this.element = element;
      this.priority = priority;
      this.consumerId = consumerId;
      this.lastProgressReportTs = lastProgressReportTs;
    }

    @Override
    public Element getElement() {
      return element;
    }

    @Override
    public long getStatusTime() {
      return lastProgressReportTs;
    }

    @Override
    public String getConsumerId() {
      return consumerId;
    }

    @Override
    public int compareTo(Entry o) {
      if (o.priority == priority) {
        return 0;
      } else {
        return priority > o.priority ? 1 : -1;
      }
    }
  }

  private static final class EntrySerializer implements Serializer<Entry> {
    private static final ThreadLocal<Gson> GSON = new ThreadLocal<Gson>() {
      @Override
      protected Gson initialValue() {
        return new Gson();
      }
    };

    @Override
    public byte[] serialize(@Nullable Entry entry) {
      if (entry == null) {
        return null;
      }
      return GSON.get().toJson(entry).getBytes();
    }

    @Override
    public Entry deserialize(@Nullable byte[] bytes) {
      if (bytes == null) {
        return null;
      }
      return GSON.get().fromJson(new String(bytes), Entry.class);
    }
  }

  @Override
  public int size() {
    return queueElements.size();
  }
}
