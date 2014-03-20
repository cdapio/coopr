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
package com.continuuity.loom.common.zookeeper.lib;

import com.continuuity.loom.common.zookeeper.BaseZKTest;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ZKInterProcessReentrantLockTest extends BaseZKTest {

  @Test(timeout = 10000)
  public void test() throws ExecutionException, InterruptedException {
    final String path = "/foo/lock";
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    final SettableFuture<Object> unblockFirst = SettableFuture.create();
    final SettableFuture<Object> unblockSecond = SettableFuture.create();
    final SettableFuture<Object> unblockThird = SettableFuture.create();

    final int[] lockOwner = new int[1];
    // 1st thread acquires the lock and waits for a signal before releases it.
    // 2nd & 3rd thread waits to acquire the lock
    // 2nd acquires after 1st releases it.
    // 3nd acquires after 2nd releases it.
    // Then 1st tries to acquire lock again, but has to wait before 3rd one releases it.

    executorService.submit(new NonSafeRunnable() {
      @Override
      public void notSafeRun() throws Exception {
        ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, path);
        lock.acquire();
        lockOwner[0] = 1;
        // waiting for command to start the whole thing
        unblockFirst.get();
        lock.release();
        TimeUnit.MILLISECONDS.sleep(300);
        lock.acquire();
        lockOwner[0] = 1;
        lock.release();
      }
    });

    // waiting for first to acquire the lock
    TimeUnit.MILLISECONDS.sleep(100);

    executorService.submit(new NonSafeRunnable() {
      @Override
      public void notSafeRun() throws Exception {
        ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, path);
        lock.acquire();
        lockOwner[0] = 2;
        // waiting for command to start the whole thing
        unblockSecond.get();
        lock.release();
      }
    });

    TimeUnit.MILLISECONDS.sleep(100);

    executorService.submit(new NonSafeRunnable() {
      @Override
      public void notSafeRun() throws Exception {
        ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, path);
        lock.acquire();
        lockOwner[0] = 3;
        // waiting for command to start the whole thing
        unblockSecond.get();
        lock.release();
      }
    });

    Assert.assertEquals(1, lockOwner[0]);

    unblockFirst.set(new Object());
    // waiting for next to acquire the lock
    TimeUnit.MILLISECONDS.sleep(100);
    Assert.assertEquals(2, lockOwner[0]);

    unblockSecond.set(new Object());
    // waiting for next to acquire the lock
    TimeUnit.MILLISECONDS.sleep(200);
    Assert.assertEquals(3, lockOwner[0]);

    unblockSecond.set(new Object());
    // waiting for next to acquire the lock
    TimeUnit.MILLISECONDS.sleep(300);
    Assert.assertEquals(1, lockOwner[0]);

    // checking that current thread can now acquire the lock
    ZKInterProcessReentrantLock lock = new ZKInterProcessReentrantLock(zkClient, path);
    lock.acquire();
    // checking that we can acquire the same lock multiple times
    lock.acquire();
    lock.acquire();
    lock.release();
    lock.acquire();
    lock.release();
  }

  private static abstract class NonSafeRunnable implements Runnable {
    @Override
    public void run() {
      try {
        notSafeRun();
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    public abstract void notSafeRun() throws Exception;
  }
}
