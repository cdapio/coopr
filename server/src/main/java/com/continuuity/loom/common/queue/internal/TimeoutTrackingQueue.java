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
package com.continuuity.loom.common.queue.internal;

import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.QueuedElement;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Tracks elements being consumed and puts them back to queue if there's a certain time passed after last consumer's
 * progress report.
 */
public class TimeoutTrackingQueue implements TrackingQueue {
  private static final Logger LOG = LoggerFactory.getLogger(TimeoutTrackingQueue.class);
  private final ElementsTracking elementsTracking;

  private final Map<String, SettableFuture<String>> consumingResults;

  private long rescheduleAfterTimeout;
  private long intervalBetweenChecks;

  private ReschedulingThread thread;
  private boolean started = false;

  public TimeoutTrackingQueue(ElementsTracking elementsTracking, long intervalBetweenChecks,
                              long rescheduleAfterTimeout) {
    this.elementsTracking = elementsTracking;
    this.intervalBetweenChecks = intervalBetweenChecks;
    this.rescheduleAfterTimeout = rescheduleAfterTimeout;
    this.consumingResults = Maps.newHashMap();
  }

  public void start() {
    ReschedulingWalker reschedulingWalker = new ReschedulingWalker(rescheduleAfterTimeout);
    thread = new ReschedulingThread(reschedulingWalker, intervalBetweenChecks);
    // TODO: clean this up, handle retries
    //thread.start();
    started = true;
  }

  @Override
  public ListenableFuture<String> add(Element element) {
    Preconditions.checkArgument(element != null, "element to add must not be null");
    checkThatStarted();
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
    checkThatStarted();
    return elementsTracking.startConsuming(consumerId);
  }

  @Override
  public PossessionState recordProgress(String consumerId, String elementId, ConsumingStatus status, String result) {
    Preconditions.checkArgument(consumerId != null, "id of the consumer that reports progress should not be null");
    Preconditions.checkArgument(elementId != null, "id of the element to report progress on should not be null");
    Preconditions.checkArgument(status != null, "reported status of the progress should not be null");
    checkThatStarted();
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

  public void stop() {
    thread.finish();
    thread.interrupt();
  }

  private void checkThatStarted() {
    if (!started) {
      throw new IllegalStateException("TimeoutTrackingQueue is not started. Call start() method before using the " +
                                        "queue");
    }
  }

  private class ReschedulingThread extends Thread {
    private final Logger log = LoggerFactory.getLogger(ReschedulingThread.class);
    private final ReschedulingWalker reschedulingWalker;
    private final long intervalBetweenChecks;
    private volatile boolean finished = false;

    private ReschedulingThread(ReschedulingWalker reschedulingWalker, long intervalBetweenChecks) {
      super("ReschedulingThread");
      this.reschedulingWalker = reschedulingWalker;
      this.intervalBetweenChecks = intervalBetweenChecks;
    }

    public void finish() {
      finished = true;
    }

    @Override
    public void run() {
      while (!finished) {
        try {
          try {
            log.debug("sleeping for (ms): " + intervalBetweenChecks);
            Thread.sleep(intervalBetweenChecks);
          } catch (InterruptedException e) {
            log.error("sleep interrupted", e);
            interrupt();
            break;
          }
          log.debug("starting check...");
          elementsTracking.walkThruElementsBeingConsumed(reschedulingWalker);
          log.debug("check DONE");
        } catch (Throwable e) {
          log.error("Failed to walk thru elements being consumed", e);
          // Do NOT die at any circumstances!
        }
      }
    }
  }

  // TODO: Looks like ReschedulingWalker is no longer required, since we prefer taking care of rescheduling in the
  // TODO: application logic, so that we can do appropriate state changes in application.
  private class ReschedulingWalker implements ElementsTracking.Walker {
    private final Logger log = LoggerFactory.getLogger(ReschedulingWalker.class);
    private final long rescheduleAfterTimeout;

    private ReschedulingWalker(long rescheduleAfterTimeout) {
      this.rescheduleAfterTimeout = rescheduleAfterTimeout;
    }

    @Override
    public boolean process(Element element, String consumerId, long lastProgressReportTs) {
      long currentTime = System.currentTimeMillis();
      long sinceLastProgressReport = currentTime - lastProgressReportTs;
      if (sinceLastProgressReport > rescheduleAfterTimeout) {
        log.info("Rescheduling element since it's been more than " + rescheduleAfterTimeout +
                   " since any progress was reported on it. Last report time: " + lastProgressReportTs +
                   " (" + new Date(lastProgressReportTs) + ")" +
                   ", element id: " + element.getId());
        return true;
      }

      return false;
    }
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
