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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.twill.common.Threads;
import org.apache.twill.zookeeper.NodeChildren;
import org.apache.twill.zookeeper.NodeData;
import org.apache.twill.zookeeper.OperationFuture;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.apache.twill.zookeeper.ZKOperations;
import org.apache.zookeeper.CreateMode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link Map} backed by Zookeeper.
 * NOTE: it is not thread-safe and not multi-process safe. Consider using {@link ReentrantDistributedLock}
 * to guard against changes by other processes if needed.
 *
 * @param <T> Type of object to keep in the map values.
 */
public class ZKMap<T> implements Map<String, T> {
  private final ZKClient zkClient;
  private final Serializer<T> serializer;
  private final AtomicReference<Map<String, T>> currentView;
  private final Map<String, SettableFuture<T>> waitingForElements;

  public ZKMap(ZKClient zkClient, String namespace, Serializer<T> serializer)
    throws ExecutionException, InterruptedException {
    this.zkClient = namespace == null ? zkClient : ZKClients.namespace(zkClient, namespace);
    this.serializer = serializer;
    this.currentView = new AtomicReference<Map<String, T>>(Collections.<String, T>emptyMap());
    this.waitingForElements = Maps.newHashMap();
    setExternalChangeWatcher();
  }

  @Override
  public int size() {
    return currentView.get().size();
  }

  @Override
  public boolean isEmpty() {
    return currentView.get().isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return currentView.get().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return currentView.get().containsValue(value);
  }

  @Override
  public T get(Object key) {
    return currentView.get().get(key);
  }

  public ListenableFuture<T> getOrWait(String key) {
    if (currentView.get().containsKey(key)) {
      return Futures.immediateFuture(currentView.get().get(key));
    }
    SettableFuture<T> future = SettableFuture.create();
    waitingForElements.put(key, future);
    return future;
  }

  // note: may not return value and still delete smth from ZK if in-memory view is stale
  public T put(String key, T value) {
    Map<String, T> current = Maps.newHashMap(currentView.get());
    T result = current.put(key, value);
    currentView.set(ImmutableMap.<String, T>builder().putAll(current).build());
    // Note: we do delete and add new node with new data VS createOrSet() to avoid attaching watchers to every node
    String itemNodePath = getItemNodePath(key);
    Futures.getUnchecked(ZKClientExt.delete(zkClient, itemNodePath, true));
    Futures.getUnchecked(zkClient.create(itemNodePath, serializer.serialize(value), CreateMode.PERSISTENT));
    return result;
  }

  // note: we may return null even though we removed non-null element if the view in memory is stale. Which is OK
  @Override
  public T remove(Object key) {
    if (!(key instanceof String)) {
      throw new IllegalArgumentException("Expected key of type java.lang.String but was " +
                                           (key == null ? null : key.getClass()));
    }
    Map<String, T> current = Maps.newHashMap(currentView.get());
    T removed = current.remove(key);
    currentView.set(ImmutableMap.<String, T>builder().putAll(current).build());
    // note: we cannot only issue remove from zk if removed != null because even if removed == null this could mean
    //       the element was removed (and for other race-condition reasons)
    Futures.getUnchecked(ZKClientExt.delete(zkClient, getItemNodePath((String) key), true));
    return removed;
  }

  @Override
  public void putAll(Map<? extends String, ? extends T> m) {
    // todo: implement efficiently
    throw new UnsupportedOperationException();
  }

  public void clear() {
    currentView.set(Collections.<String, T>emptyMap());
    // Hint: again, we can try to make removal more efficient by cleaning only when in-mem collection cleaned smth,
    //       but then we may face races...
    NodeChildren nodeChildren = Futures.getUnchecked(zkClient.getChildren(""));
    List<ListenableFuture<String>> deleteFutures = Lists.newArrayList();
    for (String node : nodeChildren.getChildren()) {
      deleteFutures.add(ZKClientExt.delete(zkClient, getNodePath(node), true));
    }
    Futures.getUnchecked(Futures.allAsList(deleteFutures));
  }

  @Override
  public Set<String> keySet() {
    return currentView.get().keySet();
  }

  @Override
  public Collection<T> values() {
    return currentView.get().values();
  }

  @Override
  public Set<Entry<String, T>> entrySet() {
    return currentView.get().entrySet();
  }

  private void setExternalChangeWatcher()
    throws ExecutionException, InterruptedException {

    ZKOperations.watchChildren(zkClient, "", new ZKOperations.ChildrenCallback() {
      @Override
      public void updated(NodeChildren nodeChildren) {
        List<String> nodes = nodeChildren.getChildren();
        final Map<String, ListenableFuture<NodeData>> nodeAndDataFutures = Maps.newHashMap();
        List<OperationFuture<NodeData>> dataFutures = Lists.newArrayList();
        for (String node : nodes) {
          OperationFuture<NodeData> dataFuture = zkClient.getData(getNodePath(node));
          dataFutures.add(dataFuture);
          nodeAndDataFutures.put(node, dataFuture);
        }

        final ListenableFuture<List<NodeData>> fetchFuture = Futures.successfulAsList(dataFutures);
        fetchFuture.addListener(new Runnable() {
          @Override
          public void run() {
            ImmutableMap.Builder<String, T> builder = ImmutableMap.builder();
            for (Map.Entry<String, ListenableFuture<NodeData>> nodeAndData : nodeAndDataFutures.entrySet()) {
              T value = serializer.deserialize(Futures.getUnchecked(nodeAndData.getValue()).getData());
              builder.put(nodeAndData.getKey(), value);
            }

            currentView.set(builder.build());
            updateWaitingForElements();
          }
        }, Threads.SAME_THREAD_EXECUTOR);

      }
    });
  }

  private void updateWaitingForElements() {
    // lazy init: if nothing interesting happened, avoid doing redundant stuff
    Set<String> done = null;
    for (Map.Entry<String, SettableFuture<T>> waiting : waitingForElements.entrySet()) {
      T elem = currentView.get().get(waiting.getKey());
      if (elem != null) {
        if (done == null) {
          done = Sets.newHashSet();
        }
        waiting.getValue().set(elem);
        done.add(waiting.getKey());
      }
    }

    if (done == null) {
      return;
    }

    for (String key : done) {
      waitingForElements.remove(key);
    }
  }


  private String getItemNodePath(String key) {
    return getNodePath(key);
  }

  private String getNodePath(String nodeName) {
    return "/" + nodeName;
  }
}
