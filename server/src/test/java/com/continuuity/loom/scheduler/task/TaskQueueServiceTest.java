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

package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.queue.QueueMetrics;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

/**
 *
 */
public class TaskQueueServiceTest extends BaseTest {
  private static TaskQueueService service;
  private static QueueGroup provisionerQueues;

  @BeforeClass
  public static void setupTestClass() {
    service = injector.getInstance(TaskQueueService.class);
    provisionerQueues = injector.getInstance(Key.get(QueueGroup.class, Names.named(Constants.Queue.PROVISIONER)));
  }

  @After
  public void cleanupTest() {
    provisionerQueues.removeAll();
  }

  @Test
  public void testGetQueueMetrics() {
    provisionerQueues.add("tenant1", new Element("task1"));
    provisionerQueues.add("tenant1", new Element("task2"));
    provisionerQueues.add("tenant1", new Element("task3"));
    provisionerQueues.take("tenant1", "consumer");
    provisionerQueues.add("tenant2", new Element("task4"));
    provisionerQueues.add("tenant3", new Element("task5"));
    provisionerQueues.take("tenant3", "consumer");
    provisionerQueues.add("tenant4", new Element("task6"));
    provisionerQueues.add("tenant4", new Element("task7"));
    provisionerQueues.take("tenant4", "consumer");

    Map<String, QueueMetrics> expected = Maps.newHashMap();
    expected.put("tenant1", new QueueMetrics(2, 1));
    Assert.assertEquals(expected, service.getTaskQueueMetricsSnapshot(Sets.newHashSet("tenant1")));

    expected.put("tenant2", new QueueMetrics(1, 0));
    Assert.assertEquals(expected, service.getTaskQueueMetricsSnapshot(Sets.newHashSet("tenant1", "tenant2")));

    expected.put("tenant3", new QueueMetrics(0, 1));
    Assert.assertEquals(expected,
                        service.getTaskQueueMetricsSnapshot(Sets.newHashSet("tenant1", "tenant2", "tenant3")));

    expected.put("tenant4", new QueueMetrics(1, 1));
    Assert.assertEquals(expected, service.getTaskQueueMetricsSnapshot());
  }
}
