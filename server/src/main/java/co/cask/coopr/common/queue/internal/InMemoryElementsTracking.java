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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * In-memory implementation of {@link ElementsTracking}.
 */
public class InMemoryElementsTracking implements ElementsTracking {
  private PriorityQueue<PrioritizedElement> notStarted = new PriorityQueue<PrioritizedElement>();
  // elementId->elemBeingConsumed
  private Map<String, ElementBeingConsumed> inProgress = Maps.newHashMap();

  // Moves to the top of the queue. Since we reset priority when we start consuming, it is safe to use 0L here
  // See {@link #getCurrentHighestPriority()} for more info.
  private static final long HIGHEST_PRIORITY = 0L;

  @Override
  public synchronized boolean addToQueue(Element element) {
    return notStarted.add(new PrioritizedElement(element, getCurrentHighestPriority()));
  }

  @Override
  public synchronized Element startConsuming(String consumerId) {
    PrioritizedElement element = notStarted.poll();
    if (element == null) {
      return null;
    }
    inProgress.put(element.element.getId(), new ElementBeingConsumed(element.element, consumerId, element.priority));
    return element.element;
  }

  @Override
  public synchronized boolean stopConsumingAndAddBackToQueue(String elementId, String consumerId) {
    ElementBeingConsumed element = inProgress.get(elementId);
    if (element == null || !consumerId.equals(element.consumerId)) {
      return false;
    }
    stopAndReschedule(element);
    return true;
  }

  private synchronized void stopAndReschedule(ElementBeingConsumed element) {
    inProgress.remove(element.element.getId());
    notStarted.add(new PrioritizedElement(element.element, element.priority));
  }

  @Override
  public synchronized boolean finishConsuming(String elementId, String consumerId) {
    ElementBeingConsumed element = inProgress.get(elementId);
    if (element == null || !consumerId.equals(element.consumerId)) {
      return false;
    }
    inProgress.remove(elementId);
    return true;
  }

  @Override
  public synchronized boolean recordProgress(String elementId, String consumerId) {
    ElementBeingConsumed element = inProgress.get(elementId);
    if (element == null || !consumerId.equals(element.consumerId)) {
      return false;
    }
    element.lastProgressReportTs = System.currentTimeMillis();
    return true;
  }

  @Override
  public synchronized void walkThruElementsBeingConsumed(Walker walker) {
    List<ElementBeingConsumed> toBeStoppedAndRescheduled = Lists.newArrayList();
    for (ElementBeingConsumed element : inProgress.values()) {
      boolean stopAndReschedule = walker.process(element.element, element.consumerId, element.lastProgressReportTs);
      if (stopAndReschedule) {
        toBeStoppedAndRescheduled.add(element);
      }
    }

    for (ElementBeingConsumed element : toBeStoppedAndRescheduled) {
      stopAndReschedule(element);
    }
  }

  @Override
  public synchronized boolean remove(String elementId) {
    PrioritizedElement toRemove = null;
    for (PrioritizedElement elem : notStarted) {
      if (elementId.equals(elem.element.getId())) {
        toRemove = elem;
        break;
      }
    }
    if (toRemove != null) {
      notStarted.remove(toRemove);
    }
    inProgress.remove(elementId);
    return true;
  }

  @Override
  public synchronized boolean removeAll() {
    notStarted.clear();
    inProgress.clear();
    return true;
  }

  @Override
  public synchronized boolean toHighestPriority(String elementId) {
    // we adjust priority only in those not being consumed. If consuming fails for the element it will be promoted to
    // highest priority anyways
    PrioritizedElement toChange = null;
    for (PrioritizedElement elem : notStarted) {
      if (elementId.equals(elem.element.getId())) {
        toChange = elem;
        break;
      }
    }
    if (toChange != null) {
      notStarted.remove(toChange);
      toChange.priority = HIGHEST_PRIORITY;
      notStarted.add(toChange);
    }

    return true;
  }

  @Override
  public List<QueuedElement> getQueued() {
    List<QueuedElement> result = Lists.newArrayList();
    List<PrioritizedElement> list = new ArrayList<PrioritizedElement>(notStarted);
    Collections.sort(list);
    for (PrioritizedElement elem : list) {
      result.add(elem);
    }
    return result;
  }

  @Override
  public List<QueuedElement> getBeingConsumed() {
    ImmutableList.Builder<QueuedElement> listBuilder = new ImmutableList.Builder<QueuedElement>();
    listBuilder.addAll(inProgress.values());
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

  private static class PrioritizedElement implements QueuedElement, Comparable<PrioritizedElement> {
    private Element element;
    private long priority;

    private PrioritizedElement(Element element, long priority) {
      this.element = element;
      this.priority = priority;
    }

    @Override
    public Element getElement() {
      return element;
    }

    @Override
    public long getStatusTime() {
      return 0;
    }

    @Override
    public String getConsumerId() {
      return "";
    }

    @Override
    public int compareTo(PrioritizedElement o) {
      if (o.priority == priority) {
        return 0;
      } else {
        return priority > o.priority ? 1 : -1;
      }
    }
  }

  @Override
  public int size() {
    return notStarted.size() + inProgress.size();
  }

  private static class ElementBeingConsumed implements QueuedElement {
    private Element element;
    private String consumerId;
    private long priority;
    private long lastProgressReportTs;

    private ElementBeingConsumed(Element element, String consumerId, long priority) {
      this.element = element;
      this.consumerId = consumerId;
      this.priority = priority;
      this.lastProgressReportTs = System.currentTimeMillis();
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
  }
}
