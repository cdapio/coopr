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
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * Tracks elements being consumed.
 */
public class ElementsTrackingQueue implements TrackingQueue {
  private static final Logger LOG = LoggerFactory.getLogger(ElementsTrackingQueue.class);
  private final ElementsTracking elementsTracking;

  private final Map<String, SettableFuture<String>> consumingResults;

  public ElementsTrackingQueue(ElementsTracking elementsTracking) {
    this.elementsTracking = elementsTracking;
    this.consumingResults = Maps.newHashMap();
  }

  @Override
  public ListenableFuture<String> add(Element element) {
    Preconditions.checkArgument(element != null, "element to add must not be null");
    SettableFuture<String> result = addConsumingResultToWaitFor(element.getId());
    if (!elementsTracking.addToQueue(element)) {
      result.setException(new RuntimeException("failed to add element to a queue " + element.toString()));
      stopWaitingForConsumingResult(element.getId());
    }

    return result;
  }

  @Override
  public Element take(String consumerId) {
    Preconditions.checkArgument(consumerId != null, "id of the consumer that takes element should not be null");
    return elementsTracking.startConsuming(consumerId);
  }

  @Override
  public PossessionState recordProgress(String consumerId, String elementId, ConsumingStatus status, String result) {
    Preconditions.checkArgument(consumerId != null, "id of the consumer that reports progress should not be null");
    Preconditions.checkArgument(elementId != null, "id of the element to report progress on should not be null");
    Preconditions.checkArgument(status != null, "reported status of the progress should not be null");
    if (status == ConsumingStatus.IN_PROGRESS) {
      if (elementsTracking.recordProgress(elementId, consumerId)) {
        return PossessionState.POSSESSES;
      } else {
        LOG.warn("Attempted to report consuming progress for element that doesn't belong to the consumer, " +
                   "elementId: " + elementId + ", reported status: " + status + ", reported by: " + consumerId);
        return PossessionState.NOT_POSSESSES;
      }
    }

    if (status == ConsumingStatus.FINISHED_SUCCESSFULLY) {
      if (elementsTracking.finishConsuming(elementId, consumerId)) {
        setConsumingResult(elementId, result);
        return PossessionState.POSSESSES;
      } else {
        LOG.warn("Attempted to report consuming progress for element that doesn't belong to the consumer, " +
                   "elementId: " + elementId + ", reported status: " + status + ", reported by: " + consumerId);
        return PossessionState.NOT_POSSESSES;
      }
    }

    if (status == ConsumingStatus.FAILED) {
      LOG.debug("Reported consuming failure for " +
                 "elementId: " + elementId + ", reported status: " + status + ", reported by: " + consumerId);

      if (elementsTracking.stopConsumingAndAddBackToQueue(elementId, consumerId)) {
        return PossessionState.POSSESSES;
      } else {
        LOG.warn("Attempted to report consuming progress for element that doesn't belong to the consumer, " +
                   "elementId: " + elementId + ", reported status: " + status + ", reported by: " + consumerId);
        return PossessionState.NOT_POSSESSES;
      }
    }

    throw new IllegalArgumentException("Unknown consuming status reported: " + status);
  }

  @Override
  public boolean remove(String elementId) {
    Preconditions.checkArgument(elementId != null, "id of the element to remove should not be null");
    return elementsTracking.remove(elementId);
  }

  @Override
  public boolean removeAll() {
    return elementsTracking.removeAll();
  }

  @Override
  public boolean toHighestPriority(String elementId) {
    Preconditions.checkArgument(elementId != null,
                                "id of the element to promote to highest priority should not be null");
    return elementsTracking.toHighestPriority(elementId);
  }

  @Override
  public Iterator<QueuedElement> getQueued() {
    return elementsTracking.getQueued().iterator();
  }

  @Override
  public Iterator<QueuedElement> getBeingConsumed() {
    return elementsTracking.getBeingConsumed().iterator();
  }

  @Override
  public int size() {
    return elementsTracking.size();
  }

  private synchronized SettableFuture<String> addConsumingResultToWaitFor(String elementId) {
    SettableFuture<String> futureResult = SettableFuture.create();
    consumingResults.put(elementId, futureResult);
    return futureResult;
  }

  private synchronized void stopWaitingForConsumingResult(String elementId) {
    SettableFuture<String> futureResult = consumingResults.remove(elementId);
  }

  private synchronized void setConsumingResult(String elementId, String result) {
    SettableFuture<String> futureResult = consumingResults.remove(elementId);
    if (futureResult != null) {
      futureResult.set(result);
    }
  }
}
