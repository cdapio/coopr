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

import co.cask.coopr.common.zookeeper.ZKClientExt;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.twill.zookeeper.NodeChildren;
import org.apache.twill.zookeeper.NodeData;
import org.apache.twill.zookeeper.OperationFuture;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.apache.zookeeper.CreateMode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Synchronized (across threads and different processes) implementation of {@link java.util.Map} backed
 * by Zookeeper.
 * <p/>
 * Does not permit null keys or values.
 *
 * @param <T> Type of object to keep in map values.
 */
public class SynchronizedZKMap<T> implements Map<String, T> {
  private static final String ENTRIES_PATH = "/entries";
  private static final String LOCK_PATH = "/lock";
  private final ZKClient zkClient;
  private final Serializer<T> serializer;

  private final Lock globalLock;
  private Map<String, T> currentView;
  private int currentViewVersion;

  public SynchronizedZKMap(ZKClient zkClient, String namespace, Serializer<T> serializer) {
    this(namespace == null ? zkClient : ZKClients.namespace(zkClient, namespace), serializer);
  }

  public SynchronizedZKMap(ZKClient zkClient, Serializer<T> serializer) {
    this.zkClient = zkClient;
    this.serializer = serializer;
    this.currentView = Maps.newHashMap();
    this.currentViewVersion = -1;
    this.globalLock = new ReentrantDistributedLock(zkClient, LOCK_PATH);
  }

  @Override
  public synchronized int size() {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.size();
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized boolean isEmpty() {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.isEmpty();
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized boolean containsKey(Object key) {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.containsKey(key);
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized boolean containsValue(Object value) {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.containsValue(value);
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized T get(Object key) {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.get(key);
    } finally {
      globalLock.unlock();
    }
  }

  // note: may not return value and still delete smth from ZK if in-memory view is stale
  public synchronized T put(String key, T value) {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return putInternal(key, value);
    } finally {
      globalLock.unlock();
    }
  }

  // note: we may return null even though we removed non-null element if the view in memory is stale. Which is OK
  @Override
  public synchronized T remove(Object key) {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return removeInternal(key);
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized void putAll(Map<? extends String, ? extends T> m) {
    // todo: implement efficiently
    throw new UnsupportedOperationException();
  }

  public synchronized void clear() {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      clearInternal();
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized Set<String> keySet() {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.keySet();
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized Collection<T> values() {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.values();
    } finally {
      globalLock.unlock();
    }
  }

  @Override
  public synchronized Set<Entry<String, T>> entrySet() {
    globalLock.lock();
    try {
      reloadCacheIfNeeded();
      return currentView.entrySet();
    } finally {
      globalLock.unlock();
    }
  }

  private T putInternal(String key, T value) {
    Map<String, T> current = Maps.newHashMap(currentView);
    T result = current.put(key, value);
    currentView = ImmutableMap.<String, T>builder().putAll(current).build();
    String itemNodePath = getItemNodePath(key);
    // Note: we do delete and add new node with new data VS createOrSet() so that cversion of children change (we depend
    //       on it when checking if the current in-memory view is stale)
    Futures.getUnchecked(ZKClientExt.delete(zkClient, itemNodePath, true));
    Futures.getUnchecked(zkClient.create(itemNodePath, serializer.serialize(value), CreateMode.PERSISTENT, true));
    return result;
  }

  private T removeInternal(Object key) {
    if (!(key instanceof String)) {
      throw new IllegalArgumentException("Expected key of type java.lang.String but was " +
                                           (key == null ? null : key.getClass()));
    }

    if (!currentView.containsKey(key)) {
      return null;
    }

    Map<String, T> current = Maps.newHashMap(currentView);
    T removed = current.remove(key);
    currentView = ImmutableMap.<String, T>builder().putAll(current).build();
    // note: we cannot only issue remove from zk if removed != null because even if removed == null this could mean
    //       the element was removed (and for other race-condition reasons)
    Futures.getUnchecked(ZKClientExt.delete(zkClient, getItemNodePath((String) key), true));

    return removed;
  }

  private void clearInternal() {
    if (currentView.size() > 0) {
      currentView = Collections.emptyMap();
      NodeChildren nodeChildren = Futures.getUnchecked(zkClient.getChildren(ENTRIES_PATH));
      List<ListenableFuture<String>> deleteFutures = Lists.newArrayList();
      for (String node : nodeChildren.getChildren()) {
        deleteFutures.add(ZKClientExt.delete(zkClient, getNodePath(node), true));
      }
      Futures.getUnchecked(Futures.allAsList(deleteFutures));
    }
  }

  private void reloadCacheIfNeeded() {
    NodeChildren nodeChildren = Futures.getUnchecked(ZKClientExt.getChildrenOrNull(zkClient, ENTRIES_PATH));
    if (nodeChildren == null) {
      if (currentView.size() > 0) {
        currentView = Collections.emptyMap();
      }

      return;
    }

    // we use children version to detect if we need to update local view
    int trueVersion = nodeChildren.getStat().getCversion();

    if (currentViewVersion == trueVersion) {
      return;
    }

    List<String> nodes = nodeChildren.getChildren();
    final Map<String, ListenableFuture<NodeData>> nodeAndDataFutures = Maps.newHashMap();
    List<OperationFuture<NodeData>> dataFutures = Lists.newArrayList();
    for (String node : nodes) {
      OperationFuture<NodeData> dataFuture = zkClient.getData(getNodePath(node));
      dataFutures.add(dataFuture);
      nodeAndDataFutures.put(node, dataFuture);
    }

    Futures.getUnchecked(Futures.successfulAsList(dataFutures));

    ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
    for (Entry<String, ListenableFuture<NodeData>> nodeAndData : nodeAndDataFutures.entrySet()) {
      T value = serializer.deserialize(Futures.getUnchecked(nodeAndData.getValue()).getData());
      builder.put(nodeAndData.getKey(), value);
    }

    currentView = builder.build();
    currentViewVersion = trueVersion;
  }


  private synchronized String getItemNodePath(String key) {
    return getNodePath(key);
  }

  private synchronized String getNodePath(String nodeName) {
    return ENTRIES_PATH + "/" + nodeName;
  }
}
