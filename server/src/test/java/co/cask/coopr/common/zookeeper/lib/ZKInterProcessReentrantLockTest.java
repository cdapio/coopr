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
package co.cask.coopr.common.zookeeper.lib;

import co.cask.coopr.common.zookeeper.BaseZKTest;
import co.cask.coopr.common.zookeeper.IdService;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ZKInterProcessReentrantLockTest extends BaseZKTest {

  @Test(timeout = 10000)
  public void testLockIsReentrant() {
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, "/foo/lock");
    lock.acquire();
    lock.acquire();
    lock.release();
    lock.acquire();
    lock.release();
  }

  @Test(timeout = 10000)
  public void testLock() throws InterruptedException {
    final int incrementsPerThread = 50;
    final int numThreads = 10;
    final CyclicBarrier barrier = new CyclicBarrier(numThreads);
    final CountDownLatch latch = new CountDownLatch(numThreads);
    final Counter counter = new Counter();
    final String lockPath = "/bar/lock";

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    for (int i = 0; i < numThreads; i++) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          // wait for all threads to get here
          try {
            barrier.await();
          } catch (Exception e) {
            Throwables.propagate(e);
          }
          // perform increments when holding the lock
          for (int j = 0; j < incrementsPerThread; j++) {
            ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, lockPath);
            lock.acquire();
            try {
              counter.setCount(counter.getCount() + 1);
            } finally {
              lock.release();
            }
          }
          // hit the latch when you're done
          latch.countDown();
        }
      });
    }

    latch.await();

    Assert.assertEquals(numThreads * incrementsPerThread, counter.getCount());
  }

  private class Counter {
    private int count = 0;

    private int getCount() {
      return count;
    }

    private void setCount(int count) {
      this.count = count;
    }
  }
}
