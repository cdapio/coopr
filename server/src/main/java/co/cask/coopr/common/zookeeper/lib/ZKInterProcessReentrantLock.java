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

import co.cask.coopr.common.zookeeper.ZKClientExt;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.twill.zookeeper.NodeChildren;
import org.apache.twill.zookeeper.OperationFuture;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

/**
 * A re-entrant mutual exclusion lock backed up by Zookeeper.
 * <b/>
 * Note: this class is not thread-safe. When using within same process every thread should have it's own instance of
 * the lock.
 */
public class ZKInterProcessReentrantLock {
  private final ZKClient zkClient;
  private final String path;
  private final String lockPath;
  // node that holds the lock, null if lock is not hold by us
  private String lockNode;

  // todo: consider implementing Locks class to create different types of locks
  // TODO: Do not expose ZKInterProcessReentrantLock directly, instead create a LockService that has a well
  // defined namespace.
  public ZKInterProcessReentrantLock(ZKClient zkClient, String path) {
    this.zkClient = zkClient;
    this.path = path;
    this.lockPath = path + "/lock";
    ZKClientExt.ensureExists(zkClient, path);
  }

  public void acquire() {
    if (isOwnerOfLock()) {
      return;
    }

    // The algo is the following:
    // 1) we add sequential ephemeral node
    // 2a) if added node is the first one in the list, we acquired the lock. Finish
    // 2b) if added node is not the first one, then add watch to the one before it to re-acquire when it is deleted.

    lockNode = Futures.getUnchecked(zkClient.create(lockPath, null, CreateMode.EPHEMERAL_SEQUENTIAL, true));
    NodeChildren nodeChildren = Futures.getUnchecked(zkClient.getChildren(path));
    List<String> children = nodeChildren.getChildren();
    Collections.sort(children);
    if (lockNode.equals(path + "/" + children.get(0))) {
      // we are the first to acquire the lock
      return;
    }

    final SettableFuture<Object> future = SettableFuture.create();
    boolean setWatcher = false;
    // add watch to the previous node
    Collections.reverse(children);
    for (String child : children) {
      child = path + "/" + child;
      if (child.compareTo(lockNode) < 0) {
        OperationFuture<Stat> exists = zkClient.exists(child, new Watcher() {
          @Override
          public void process(WatchedEvent event) {
            if (event.getType() == Event.EventType.NodeDeleted) {
              future.set(new Object());
            }
          }
        });
        // if it was deleted before we managed to add watcher, we need to add watcher to the current previous, hence
        // continue looping
        if (Futures.getUnchecked(exists) != null) {
          setWatcher = true;
          break;
        }
      }
    }

    if (!setWatcher) {
      // we are owners of a lock, just return
      return;
    }

    // wait for lock to be released by previous owner
    Futures.getUnchecked(future);
  }

  public boolean release() {
    if (lockNode == null) {
      return false;
    }
    // if we hold a lock, we release it by deleting the node
    // todo: check that we still hold the lock?
    Futures.getUnchecked(zkClient.delete(lockNode));
    return true;
  }

  private boolean isOwnerOfLock() {
    if (lockNode == null) {
      return false;
    }

    return Futures.getUnchecked(zkClient.exists(lockNode)) != null;
  }
}
