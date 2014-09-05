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
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link java.util.Collection} backed by Zookeeper.
 *
 * @param <T> Type of object to keep in the collection.
 */
public class ZKCollection<T> implements Collection<T> {
  private final ZKClient zkClient;
  private final Serializer<T> serializer;
  private final AtomicReference<Collection<T>> currentView;

  public ZKCollection(ZKClient zkClient, String namespace, Serializer<T> serializer)
    throws ExecutionException, InterruptedException {
    this.zkClient = namespace == null ? zkClient : ZKClients.namespace(zkClient, namespace);
    this.serializer = serializer;
    this.currentView = new AtomicReference<Collection<T>>(Collections.<T>emptyList());
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
  public boolean contains(Object o) {
    return currentView.get().contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return currentView.get().iterator();
  }

  @Override
  public Object[] toArray() {
    return currentView.get().toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    return currentView.get().toArray(a);
  }

  public boolean add(T value) {
    try {
      // NOTE: we updating the currentView only so that this collection reflects the changes right away and hence its
      //       behavior is more user-friendly
      Collection<T> current = currentView.get();
      currentView.set(ImmutableList.<T>builder().addAll(current).add(value).build());
      return ZKClientExt.createOrSet(zkClient, getItemNodePath(), serializer.serialize(value),
                                     CreateMode.PERSISTENT_SEQUENTIAL).get().getPath() != null;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public boolean remove(Object o) {
    List<T> current = Lists.newArrayList(currentView.get());
    boolean removed = current.remove(o);
    if (removed) {
      currentView.set(ImmutableList.<T>builder().addAll(current).build());
    }

    // Hint: we can try to make removal more efficient if we keep map<nodeName->object> internally, or at least try to
    //       remove only when in-mem collection removed smth, but then we may face races...
    NodeChildren children = Futures.getUnchecked(ZKClientExt.getChildrenOrNull(zkClient, ""));
    if (children == null) {
      return false;
    }
    List<String> nodes = children.getChildren();
    for (String node : nodes) {
      byte[] data = Futures.getUnchecked(zkClient.getData(getNodePath(node))).getData();
      if (o.equals(serializer.deserialize(data))) {
        return Futures.getUnchecked(ZKClientExt.delete(zkClient, getNodePath(node), true)) != null;
      }
    }

    return removed;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return currentView.get().containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    // todo: implement efficiently
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    // todo: implement efficiently
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    // todo: implement efficiently
    throw new UnsupportedOperationException();
  }

  public void clear() {
    currentView.set(Collections.<T>emptyList());
    // Hint: again, we can try to make removal more efficient by cleaning only when in-mem collection cleaned smth,
    //       but then we may face races...
    NodeChildren nodeChildren = Futures.getUnchecked(zkClient.getChildren(""));
    List<ListenableFuture<String>> deleteFutures = Lists.newArrayList();
    for (String node : nodeChildren.getChildren()) {
      deleteFutures.add(ZKClientExt.delete(zkClient, getNodePath(node), true));
    }
    Futures.getUnchecked(Futures.allAsList(deleteFutures));
  }

  private void setExternalChangeWatcher()
    throws ExecutionException, InterruptedException {

    ZKOperations.watchChildren(zkClient, "", new ZKOperations.ChildrenCallback() {
      @Override
      public void updated(NodeChildren nodeChildren) {
        List<String> nodes = nodeChildren.getChildren();
        List<OperationFuture<NodeData>> dataFutures = Lists.newArrayList();
        for (String node : nodes) {
          dataFutures.add(zkClient.getData(getNodePath(node)));
        }

        final ListenableFuture<List<NodeData>> fetchFuture = Futures.successfulAsList(dataFutures);
        fetchFuture.addListener(new Runnable() {
          @Override
          public void run() {
            ImmutableList.Builder<T> builder = ImmutableList.builder();
            // fetchFuture is set by this time
            List<NodeData> nodesData = Futures.getUnchecked(fetchFuture);
            for (NodeData nodeData : nodesData) {
              builder.add(serializer.deserialize(nodeData.getData()));
            }

            currentView.set(builder.build());
          }
        }, Threads.SAME_THREAD_EXECUTOR);

      }
    });
  }

  private String getItemNodePath() {
    return "/item";
  }

  private String getNodePath(String nodeName) {
    return "/" + nodeName;
  }
}
