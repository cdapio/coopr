package com.continuuity.loom.common.queue.internal;

import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.GroupElement;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.QueueType;
import com.continuuity.loom.common.queue.QueuedElement;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public abstract class QueueGroupTest {
  abstract QueueGroup getQueueGroup(QueueType type);

  @Test
  public void testOneQueueAddTakeWithQueueName() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    String queueName = "tenant1";
    String consumerId = "worker.0";
    queues.add(queueName, new Element("id", "val"));

    Element taken = queues.take(queueName, consumerId);
    Assert.assertEquals("id", taken.getId());
    Assert.assertEquals("val", taken.getValue());
  }

  @Test
  public void testMultiQueueTakeWithQueueName() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id1-1", "val"));
    queues.add("tenant1", new Element("id1-2", "val"));
    queues.add("tenant1", new Element("id1-3", "val"));
    queues.add("tenant2", new Element("id2-1", "val"));
    queues.add("tenant2", new Element("id2-2", "val"));
    queues.add("tenant2", new Element("id2-3", "val"));
    queues.add("tenant3", new Element("id3-1", "val"));
    queues.add("tenant4", new Element("id4-1", "val"));

    Element taken = queues.take("tenant1", "consumer");
    Assert.assertEquals("id1-1", taken.getId());

    taken = queues.take("tenant1", "consumer");
    Assert.assertEquals("id1-2", taken.getId());

    taken = queues.take("tenant2", "consumer");
    Assert.assertEquals("id2-1", taken.getId());

    taken = queues.take("tenant2", "consumer");
    Assert.assertEquals("id2-2", taken.getId());

    taken = queues.take("tenant2", "consumer");
    Assert.assertEquals("id2-3", taken.getId());

    taken = queues.take("tenant3", "consumer");
    Assert.assertEquals("id3-1", taken.getId());

    taken = queues.take("tenant4", "consumer");
    Assert.assertEquals("id4-1", taken.getId());

    Assert.assertNull(queues.take("tenant2", "consumer"));
    Assert.assertNull(queues.take("tenant3", "consumer"));
    Assert.assertNull(queues.take("tenant4", "consumer"));

    taken = queues.take("tenant1", "consumer");
    Assert.assertEquals("id1-3", taken.getId());
  }

  @Test
  public void testOneQueueAddTakeWithoutQueueName() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    String queueName = "tenant1";
    String consumerId = "worker.0";
    queues.add(queueName, new Element("id", "val"));

    GroupElement taken = queues.take(consumerId);
    Assert.assertEquals(queueName, taken.getQueueName());
    Assert.assertEquals("id", taken.getElement().getId());
    Assert.assertEquals("val", taken.getElement().getValue());
  }

  @Test
  public void testMultiQueueTakeWithoutQueueName() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id1", "val"));
    queues.add("tenant1", new Element("id2", "val"));
    queues.add("tenant1", new Element("id3", "val"));
    queues.add("tenant2", new Element("id1", "val"));
    queues.add("tenant2", new Element("id2", "val"));
    queues.add("tenant2", new Element("id3", "val"));
    queues.add("tenant2", new Element("id4", "val"));
    queues.add("tenant2", new Element("id5", "val"));

    GroupElement taken = queues.take("consumer");
    Assert.assertEquals("tenant1", taken.getQueueName());
    Assert.assertEquals("id1", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id1", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant1", taken.getQueueName());
    Assert.assertEquals("id2", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id2", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant1", taken.getQueueName());
    Assert.assertEquals("id3", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id3", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id4", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id5", taken.getElement().getId());

    Assert.assertNull(queues.take("consumer"));
  }


  @Test
  public void testOneQueueGetQueuedAndConsumed() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    String queueName = "tenant1";
    queues.add(queueName, new Element("id1", "val"));
    queues.add(queueName, new Element("id2", "val"));

    // check being consumed is correct
    Assert.assertEquals(ImmutableSet.<String>of(), getIds(queues.getBeingConsumed(queueName)));
    // check queued is correct.
    Assert.assertEquals(ImmutableSet.of("id1", "id2"), getIds(queues.getQueued(queueName)));

    // take one element
    queues.take(queueName, "consumer1");
    // check being consumed is correct
    Assert.assertEquals(ImmutableSet.of("id1"), getIds(queues.getBeingConsumed(queueName)));
    // check queued is correct.
    Assert.assertEquals(ImmutableSet.of("id2"), getIds(queues.getQueued(queueName)));

    // take next element
    queues.take(queueName, "consumer2");
    // check being consumed is correct
    Assert.assertEquals(ImmutableSet.of("id1", "id2"), getIds(queues.getBeingConsumed(queueName)));
    // check queued is correct.
    Assert.assertEquals(ImmutableSet.<String>of(), getIds(queues.getQueued(queueName)));

    // finish first element
    queues.recordProgress("consumer1", queueName, "id1", TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "result");
    // check being consumed is correct
    Assert.assertEquals(ImmutableSet.of("id2"), getIds(queues.getBeingConsumed(queueName)));
    // check queued is correct.
    Assert.assertEquals(ImmutableSet.<String>of(), getIds(queues.getQueued(queueName)));
  }

  @Test
  public void testGetQueueNames() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id", "val"));
    queues.take("tenant2", "consumer.0");
    queues.take("tenant3", "consumer.1");
    queues.removeAll("tenant4");
    queues.getBeingConsumed("tenant5");
    queues.getQueued("tenant6");

    ImmutableSet<String> expected = ImmutableSet.of("tenant1", "tenant2", "tenant3", "tenant4", "tenant5", "tenant6");
    ImmutableSet<String> actual = ImmutableSet.copyOf(queues.getQueueNames());
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testGetSize() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id1", "val"));
    queues.add("tenant1", new Element("id2", "val"));
    queues.add("tenant1", new Element("id3", "val"));
    queues.add("tenant1", new Element("id4", "val"));
    queues.add("tenant2", new Element("id1", "val"));
    queues.add("tenant2", new Element("id2", "val"));
    queues.add("tenant3", new Element("id1", "val"));

    Assert.assertEquals(4, queues.size("tenant1"));
    Assert.assertEquals(2, queues.size("tenant2"));
    Assert.assertEquals(1, queues.size("tenant3"));

    // size includes elements being consumed
    queues.take("tenant1", "consumer");
    Assert.assertEquals(4, queues.size("tenant1"));
    Assert.assertEquals(2, queues.size("tenant2"));
    Assert.assertEquals(1, queues.size("tenant3"));

    // size does not include elements that are finished being consumed
    queues.recordProgress("consumer", "tenant1", "id1", TrackingQueue.ConsumingStatus.FINISHED_SUCCESSFULLY, "result");
    Assert.assertEquals(3, queues.size("tenant1"));
    Assert.assertEquals(2, queues.size("tenant2"));
    Assert.assertEquals(1, queues.size("tenant3"));
  }

  @Test
  public void testRemoveAll() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id1", "val"));
    queues.add("tenant2", new Element("id2", "val"));
    queues.add("tenant3", new Element("id3", "val"));
    queues.add("tenant4", new Element("id4", "val"));

    Assert.assertTrue(queues.removeAll());
    Assert.assertNull(queues.take("consumer"));
    Assert.assertNull(queues.take("tenant1", "consumer"));
    Assert.assertNull(queues.take("tenant2", "consumer"));
    Assert.assertNull(queues.take("tenant3", "consumer"));
    Assert.assertNull(queues.take("tenant4", "consumer"));
  }

  @Test
  public void testRemoveAllForOneQueue() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id1", "val"));
    queues.add("tenant2", new Element("id2", "val"));
    queues.add("tenant3", new Element("id3", "val"));
    queues.add("tenant4", new Element("id4", "val"));

    Assert.assertTrue(queues.removeAll("tenant3"));
    Assert.assertNull(queues.take("tenant3", "consumer"));
    Assert.assertNotNull(queues.take("tenant1", "consumer"));
    Assert.assertNotNull(queues.take("tenant2", "consumer"));
    Assert.assertNotNull(queues.take("tenant4", "consumer"));
  }

  @Test
  public void testHideQueue() {
    QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    queues.add("tenant1", new Element("id1-1", "val"));
    queues.add("tenant1", new Element("id1-2", "val"));
    queues.add("tenant2", new Element("id2-1", "val"));
    queues.add("tenant2", new Element("id2-2", "val"));
    queues.add("tenant3", new Element("id3-1", "val"));
    queues.add("tenant3", new Element("id3-2", "val"));

    GroupElement taken = queues.take("consumer");
    Assert.assertEquals("tenant1", taken.getQueueName());
    Assert.assertEquals("id1-1", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id2-1", taken.getElement().getId());

    // hide queue3, should not get elements anymore until its referenced directly
    queues.hideQueue("tenant3");

    taken = queues.take("consumer");
    Assert.assertEquals("tenant1", taken.getQueueName());
    Assert.assertEquals("id1-2", taken.getElement().getId());

    taken = queues.take("consumer");
    Assert.assertEquals("tenant2", taken.getQueueName());
    Assert.assertEquals("id2-2", taken.getElement().getId());

    Assert.assertNull(queues.take("consumer"));
  }

  @Test(timeout = 20000)
  public void testConcurrentAddAndTake() throws InterruptedException {
    final QueueGroup queues = getQueueGroup(QueueType.PROVISIONER);
    final int addsPerThread = 20;
    final int numProducerThreads = 20;
    final int expectedNumElements = addsPerThread * numProducerThreads;
    final int numConsumerThreads = 5;
    final int numQueues = 10;
    final int numThreads = numProducerThreads + numConsumerThreads;
    final AtomicInteger numTaken = new AtomicInteger(0);
    // barrier so threads start acting at the same time
    final CyclicBarrier barrier = new CyclicBarrier(numThreads);
    // latch so we wait for all threads to finish before asserts
    final CountDownLatch latch = new CountDownLatch(numThreads);
    // map of element id -> queue name
    final Map<Integer, Integer> elements = new ConcurrentHashMap<Integer, Integer>();

    // each producer thread will write to a single queue, with the queue name being a number from 0 to numQueues - 1.
    // each producer writes element values of [producer num * addsPerThread, (producer num * addsPerThread + 1) - 1]
    // for ex, with 100 adds per thread and 5 queues, producer 0 writes 0-99 to queue 0, producer 1 writes 100-199 to
    // queue 1, producer 2 writes 200-299 to queue 2, etc.
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    for (int i = 0; i < numProducerThreads; i++) {
      final String queueName = String.valueOf(i % numQueues);
      final int producerNum = i;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
          } catch (Exception e) {
            Throwables.propagate(e);
          }
          for (int j = 0; j < addsPerThread; j++) {
            queues.add(queueName, new Element(String.valueOf(producerNum * addsPerThread + j)));
          }
          latch.countDown();
        }
      });
    }

    for (int i = 0; i < numConsumerThreads; i++) {
      final String consumerId = "consumer" + i;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
          } catch (Exception e) {
            Throwables.propagate(e);
          }
          while (numTaken.get() < expectedNumElements) {
            GroupElement taken = queues.take(consumerId);
            while (taken != null) {
              numTaken.getAndIncrement();
              elements.put(Integer.valueOf(taken.getElement().getValue()), Integer.valueOf(taken.getQueueName()));
              taken = queues.take(consumerId);
            }
            try {
              TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
              Throwables.propagate(e);
            }
          }
          latch.countDown();
        }
      });
    }

    latch.await();

    Assert.assertEquals(numQueues, queues.getQueueNames().size());

    for (Integer i = 0; i < expectedNumElements; i++) {
      Integer expectedQueue = (i / addsPerThread) % numQueues;
      Assert.assertEquals(expectedQueue, elements.get(i));
    }
  }

  private Set<String> getIds(Iterator<QueuedElement> iter) {
    Set<String> out = Sets.newHashSet();
    while (iter.hasNext()) {
      out.add(iter.next().getElement().getId());
    }
    return out;
  }
}
