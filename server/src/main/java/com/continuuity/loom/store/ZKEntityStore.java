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
package com.continuuity.loom.store;

import com.continuuity.loom.common.zookeeper.ZKClientExt;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import org.apache.twill.zookeeper.NodeChildren;
import org.apache.twill.zookeeper.NodeData;
import org.apache.twill.zookeeper.ZKClient;
import org.apache.twill.zookeeper.ZKClients;
import org.apache.zookeeper.CreateMode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link BaseEntityStore} using zookeeper as the persistent store.  Entities are stored under
 * /store/[entity-type]s/[entity-name].
 */
public class ZKEntityStore extends BaseEntityStore {
  private final ZKClient zkClient;

  @Inject
  ZKEntityStore(ZKClient zkClient) {
    super();
    this.zkClient = ZKClients.namespace(zkClient, "/store");
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, byte[] data) {
    Futures.getUnchecked(
      ZKClientExt.createOrSet(zkClient, getNodePath(entityType.getId(), entityName), data, CreateMode.PERSISTENT));
  }

  @Override
  protected byte[] getEntity(EntityType entityType, String entityName) {
    NodeData data =
      Futures.getUnchecked(ZKClientExt.getDataOrNull(zkClient, getNodePath(entityType.getId(), entityName)));
    return (data == null) ? null : data.getData();
  }

  @Override
  protected <T> Collection<T> getAllEntities(EntityType entityType, Function<byte[], T> transform) {
    NodeChildren children =
      Futures.getUnchecked(ZKClientExt.getChildrenOrNull(zkClient, "/" + entityType.getId() + "s"));
    if (children == null) {
      return Collections.EMPTY_LIST;
    }
    List<String> childPaths = children.getChildren();
    List<T> entities = Lists.newArrayListWithCapacity(childPaths.size());
    for (String childPath : childPaths) {
      byte[] entityBytes = getEntity(entityType, childPath);
      if (entityBytes != null) {
        entities.add(transform.apply(entityBytes));
      }
    }
    return entities;
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName) {
    Futures.getUnchecked(ZKClientExt.delete(zkClient, getNodePath(entityType.getId(), entityName), true));
  }

  private String getNodePath(String entityType, String entityName) {
    StringBuilder sb = new StringBuilder();
    sb.append("/");
    sb.append(entityType);
    sb.append("s/");
    sb.append(entityName);
    return sb.toString();
  }
}
