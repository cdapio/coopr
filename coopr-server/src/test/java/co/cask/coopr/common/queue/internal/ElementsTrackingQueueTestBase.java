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
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public abstract class ElementsTrackingQueueTestBase {
  protected abstract ElementsTrackingQueue getQueue() throws Exception;

  @Test
  public void testBasics() throws Exception {

    ElementsTrackingQueue queue = getQueue();
    ListenableFuture<String> workResult1 = queue.add(new Element("work1", "data1"));
    // we sleep a bit so that we make sure entries are added at different ts (which is used when prioritizing elements
    // in queue)
    Thread.sleep(1);
    queue.add(new Element("work2", "data2"));

    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));
    Iterator<QueuedElement> elems = queue.getQueued();
    // the one that added first has higher priority
    Assert.assertEquals("work1", elems.next().getElement().getId());
    Assert.assertEquals("work2", elems.next().getElement().getId());

    Element taken1 = queue.take("worker1");
    Assert.assertEquals(taken1.getId(), "work1");
    Assert.assertEquals(1, getQueuedCount(queue));
    Assert.assertEquals(1, Iterators.size(queue.getBeingConsumed()));
    Assert.assertEquals(1, Iterators.size(queue.getBeingConsumed()));
    Assert.assertEquals("work1", queue.getBeingConsumed().next().getElement().getId());

    Element taken2 = queue.take("worker2");
    Assert.assertEquals(0, getQueuedCount(queue));
    Assert.assertEquals(2, Iterators.size(queue.getBeingConsumed()));

    Assert.assertNull(queue.take("worker3"));

    Assert.assertEquals(TrackingQueue.PossessionState.POSSESSES,
                        queue.recordProgress("worker1", taken1.getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));
    Assert.assertEquals(TrackingQueue.PossessionState.POSSESSES,
                        queue.recordProgress("worker2", taken2.getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress("worker3", taken1.getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress("worker3", taken1.getId(), TrackingQueue.ConsumingStatus.FAILED, null));
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress("worker3", taken1.getId(),
                                             TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, null));

    // Adding another entry
    Thread.sleep(1);
    queue.add(new Element("work3", "data3"));
    Thread.sleep(1);

    // Faking failure: element should be rescheduled
    Assert.assertEquals(TrackingQueue.PossessionState.POSSESSES,
                        queue.recordProgress("worker2", taken2.getId(),
                                             TrackingQueue.ConsumingStatus.FAILED, null));
    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(1, Iterators.size(queue.getBeingConsumed()));
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress("worker2", taken2.getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));

    // Finishing element successfully
    Assert.assertEquals(TrackingQueue.PossessionState.POSSESSES,
                        queue.recordProgress("worker1", taken1.getId(),
                                             TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "result-55"));
    Assert.assertEquals("result-55", workResult1.get());
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress("worker1", taken1.getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));
    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));

    // Despite work3 was added after work2, it should be consumed last, as work2 was rescheduled after work3 was added
    // and therefore should have higher priority
    Element taken3 = queue.take("worker3");
    Assert.assertEquals(taken2.getId(), taken3.getId());

    Thread.sleep(1);

    // Faking failure: "work2" element should be rescheduled at higher priority than "work3"
    Assert.assertEquals(TrackingQueue.PossessionState.POSSESSES,
                        queue.recordProgress("worker3", taken3.getId(),
                                             TrackingQueue.ConsumingStatus.FAILED, null));

    Thread.sleep(1);

    // Promoting element "work3"
    queue.toHighestPriority("work3");
    Element taken4 = queue.take("worker4");
    Assert.assertEquals("work3", taken4.getId());

    Assert.assertEquals(1, getQueuedCount(queue));
    Assert.assertEquals(1, Iterators.size(queue.getBeingConsumed()));

    // Adding more elements
    queue.add(new Element("work4", "data4"));
    queue.add(new Element("work5", "data5"));
    Element taken5 = queue.take("worker1");
    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(2, Iterators.size(queue.getBeingConsumed()));

    // Removing element by element ID
    // we can remove element that is no longer in a queue, i.e. this should not cause error
    Assert.assertTrue(queue.remove(taken1.getId()));
    Assert.assertEquals(2, getQueuedCount(queue));
    Assert.assertEquals(2, Iterators.size(queue.getBeingConsumed()));

    // removing queued element
    Assert.assertTrue(queue.remove(queue.getQueued().next().getElement().getId()));
    Assert.assertEquals(1, getQueuedCount(queue));
    Assert.assertEquals(2, Iterators.size(queue.getBeingConsumed()));

    // removing element being consumed
    QueuedElement anyOfBeingConsumed = queue.getBeingConsumed().next();
    Assert.assertTrue(queue.remove(anyOfBeingConsumed.getElement().getId()));
    Assert.assertEquals(1, getQueuedCount(queue));
    Assert.assertEquals(1, Iterators.size(queue.getBeingConsumed()));

    // as it was removed we no longer possess it
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress(anyOfBeingConsumed.getConsumerId(),
                                             anyOfBeingConsumed.getElement().getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));

    // Removing all elements
    queue.removeAll();
    Assert.assertEquals(0, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));
    // as it was removed we no longer possess it
    Assert.assertEquals(TrackingQueue.PossessionState.NOT_POSSESSES,
                        queue.recordProgress("worker1", taken5.getId(),
                                             TrackingQueue.ConsumingStatus.IN_PROGRESS, null));

    Assert.assertNull(queue.take("worker6"));
  }

  @Test(timeout = 90000)
  public void testConcurrentAccess() throws Exception {
    final ElementsTrackingQueue queue = getQueue();

    ProducerThread[] producers = new ProducerThread[3];
    for (int i = 0; i < producers.length; i++) {
      producers[i] = new ProducerThread("producer" + i, queue);
    }

    ConsumerThread[] consumers = new ConsumerThread[6];
    for (int i = 0; i < consumers.length; i++) {
      consumers[i] = new ConsumerThread("consumer" + i, queue);
    }

    // starting workers
    for (Thread producer : producers) {
      producer.start();
    }

    for (Thread consumer : consumers) {
      consumer.start();
    }

    // waiting for them to complete
    for (Thread producer : producers) {
      producer.join();
    }

    for (Thread consumer : consumers) {
      consumer.join();
    }

    int producedCount = 0;
    int producedSum = 0;
    for (ProducerThread producer : producers) {
      producedCount += producer.producedCount;
      producedSum += producer.producedSum;
    }
    int consumedCount = 0;
    int consumedSum = 0;
    for (ConsumerThread consumer : consumers) {
      consumedCount += consumer.consumedCount;
      consumedSum += consumer.consumedSum;
    }

    Assert.assertEquals(0, getQueuedCount(queue));
    Assert.assertEquals(0, Iterators.size(queue.getBeingConsumed()));
    Assert.assertTrue(producedCount > 0);
    Assert.assertTrue(producedSum > 0);
    Assert.assertEquals(producedCount, consumedCount);
    Assert.assertEquals(producedSum, consumedSum);
  }

  private static class ProducerThread extends Thread {
    private final TrackingQueue queue;
    private final String producerName;
    private int producedCount;
    private int producedSum;

    private ProducerThread(String producerName, TrackingQueue queue) {
      this.producerName = producerName;
      this.queue = queue;
    }

    @Override
    public void run() {
      Map<String, ListenableFuture<String>> results = Maps.newHashMap();
      for (int i = 0; i < 60; i++) {
        Element element = new Element(producerName + "_" + i, String.valueOf(i));
        results.put(element.getId(), queue.add(element));
        System.out.println("produced: " + element.getId());
        producedCount++;
        producedSum += i;
        // to prevent all events to be added to queue very quickly at once (and queue is too big to loop thru it when
        // looking for next to consume)
        if (i % 10 == 9) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            // DO NOTHING
            System.out.println();
          }
        }
      }

      System.out.println("waiting for " + results.size() + " results");
      int received = 0;
      for (Map.Entry<String, ListenableFuture<String>> result : results.entrySet()) {
        try {
          String actual = result.getValue().get();
          System.out.println("received result #" + (received++) + " " + actual);
          Assert.assertEquals("result-" + result.getKey(), actual);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private static class ConsumerThread extends Thread {
    private final TrackingQueue queue;
    private final String consumerName;
    private int consumedCount;
    private int consumedSum;

    private ConsumerThread(String consumerName, TrackingQueue queue) {
      this.consumerName = consumerName;
      this.queue = queue;
    }

    @Override
    public void run() {
      int cycle = 0;
      int retries = 0;
      while (true) {
        cycle++;
        Element element = queue.take(consumerName);
        System.out.println("took: " + (element == null ? "null" : element.getId()));
        if (element == null) {
          System.out.println("failed to consume, retry: " + retries++);
          // done consuming
          if (retries > 12) {
            break;
          }
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            // DO NOTHING
          }
          continue;
        }
        retries = 0;

        // sometimes we fail
        if (cycle % 50 == 5) {
          queue.recordProgress(consumerName, element.getId(), TrackingQueue.ConsumingStatus.FAILED, null);
          System.out.println("failed consuming: " + element.getId());
          continue;
        }

        TrackingQueue.PossessionState state =
          queue.recordProgress(consumerName, element.getId(),
                               TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "result-" + element.getId());
        System.out.println("posesses: " + state.name() + ", elem: " + element.getId());

        if (TrackingQueue.PossessionState.POSSESSES == state) {
          System.out.println("consumed: " + element.getId());
          consumedCount++;
          consumedSum += Integer.valueOf(element.getValue());
        }
      }
    }
  }

  private static int getQueuedCount(TrackingQueue queue) {
    return Iterators.size(queue.getQueued());
  }
}
