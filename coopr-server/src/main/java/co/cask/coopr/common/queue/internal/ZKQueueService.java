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

import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.QueueType;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.apache.twill.zookeeper.ZKClient;

import java.util.Map;

/**
 * A service that returns zookeeper backed queue groups.
 */
public class ZKQueueService extends AbstractIdleService implements QueueService {
  private final Map<QueueType, QueueGroup> queueGroups;

  @Inject
  private ZKQueueService(ZKClient zkClient) {
    ImmutableMap.Builder<QueueType, QueueGroup> builder = ImmutableMap.builder();
    for (QueueType type : QueueType.GROUP_TYPES) {
      builder.put(type, new ZKQueueGroup(zkClient, type));
    }
    this.queueGroups = builder.build();
  }

  @Override
  public QueueGroup getQueueGroup(QueueType type) {
    return queueGroups.get(type);
  }

  @Override
  public Map<QueueType, QueueGroup> getAllQueueGroups() {
    return queueGroups;
  }

  @Override
  protected void startUp() throws Exception {
    for (QueueGroup queueGroup : queueGroups.values()) {
      queueGroup.startAndWait();
    }
  }

  @Override
  protected void shutDown() throws Exception {
    for (QueueGroup queueGroup : queueGroups.values()) {
      queueGroup.stopAndWait();
    }
  }
}
