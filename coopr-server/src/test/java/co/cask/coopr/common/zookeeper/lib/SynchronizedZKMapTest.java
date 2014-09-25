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
package co.cask.coopr.common.zookeeper.lib;

import co.cask.coopr.common.zookeeper.BaseZKTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class SynchronizedZKMapTest extends BaseZKTest {

  @Test(timeout = 10000)
  public void testBasics() throws ExecutionException, InterruptedException {
    final String path = "/foo/map";

    SynchronizedZKMap<String> map1 = new SynchronizedZKMap<String>(zkClient, path, Serializers.stringSerializer());
    SynchronizedZKMap<String> map2 = new SynchronizedZKMap<String>(zkClient, path, Serializers.stringSerializer());
    Assert.assertEquals(0, map1.size());
    Assert.assertEquals(0, map2.size());

    map2.remove("foo");
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(0, map1.size());
    Assert.assertEquals(0, map2.size());

    map1.put("key1", "value1");
    map2.put("key2", "value2");
    map1.put("key3", "value3");
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(3, map1.size());
    Assert.assertEquals("value1", map1.get("key1"));
    Assert.assertEquals("value2", map1.get("key2"));
    Assert.assertEquals("value3", map1.get("key3"));
    Assert.assertEquals(3, map2.size());
    Assert.assertEquals("value1", map2.get("key1"));
    Assert.assertEquals("value2", map2.get("key2"));
    Assert.assertEquals("value3", map2.get("key3"));

    map1.put("key2", "value2_m");
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(3, map1.size());
    Assert.assertEquals("value2_m", map1.get("key2"));
    Assert.assertEquals(3, map2.size());
    Assert.assertEquals("value2_m", map2.get("key2"));

    map2.remove("key2");
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(2, map1.size());
    Assert.assertNull(map1.get("key2"));
    Assert.assertEquals(2, map2.size());
    Assert.assertNull(map2.get("key2"));

    // removing non-existed should be ok
    map1.remove("key2");
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(2, map1.size());
    Assert.assertNull(map1.get("key2"));
    Assert.assertEquals(2, map2.size());
    Assert.assertNull(map2.get("key2"));

    map2.clear();
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(0, map1.size());
    Assert.assertNull(map1.get("key1"));
    Assert.assertEquals(0, map2.size());
    Assert.assertNull(map2.get("key1"));

    // checking that we can clear an empty map
    map1.clear();
    TimeUnit.MILLISECONDS.sleep(10);
    Assert.assertEquals(0, map1.size());
    Assert.assertEquals(0, map2.size());
  }

  @Test (timeout = 30000)
  public void testConcurrent() throws Exception {
    int workersCount = 6;
    Producer[] producers = new Producer[workersCount];
    Thread[] producerThreads = new Thread[workersCount];
    for (int i = 0; i < workersCount; i++) {
      SynchronizedZKMap<String> map = new SynchronizedZKMap<String>(zkClient, "/map", Serializers.stringSerializer());
      producers[i] = new Producer(map);
      producerThreads[i] = new Thread(producers[i]);
    }
    Consumer[] consumers = new Consumer[workersCount];
    Thread[] consumerThreads = new Thread[workersCount];
    for (int i = 0; i < workersCount; i++) {
      SynchronizedZKMap<String> map = new SynchronizedZKMap<String>(zkClient, "/map", Serializers.stringSerializer());
      consumers[i] = new Consumer(map);
      consumerThreads[i] = new Thread(consumers[i]);
    }

    for (int i = 0; i < producerThreads.length; i++) {
      producerThreads[i].start();
      consumerThreads[i].start();
    }

    int totalProduced = 0;
    int totalConsumed = 0;
    for (int i = 0; i < producerThreads.length; i++) {
      producerThreads[i].join();
      totalProduced += producers[i].produced;
      consumerThreads[i].join();
      totalConsumed += consumers[i].consumed;
    }

    Assert.assertTrue(totalProduced > 0);
    Assert.assertEquals(totalProduced, totalConsumed);
  }

  private static class Producer implements Runnable {
    private final Map<String, String> dest;
    private int produced = 0;

    private Producer(Map<String, String> dest) {
      this.dest = dest;
    }

    @Override
    public void run() {
      for (int i = 0; i < 10; i++) {
        dest.put(UUID.randomUUID().toString(), "foo");
        produced++;
      }
    }
  }

  private static class Consumer implements Runnable {
    private final Map<String, String> src;
    private int consumed = 0;

    private Consumer(Map<String, String> src) {
      this.src = src;
    }

    @Override
    public void run() {
      int retriesLeft = 100;

      while (retriesLeft > 0) {
        Set<String> vals = src.keySet();
        if (vals.size() == 0) {
          retriesLeft--;
          try {
            TimeUnit.MILLISECONDS.sleep(1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
          continue;
        }
        if (null != src.remove(vals.iterator().next())) {
          consumed++;
        }
      }
    }
  }
}
