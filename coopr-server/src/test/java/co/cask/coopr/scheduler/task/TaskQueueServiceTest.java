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

package co.cask.coopr.scheduler.task;

import co.cask.coopr.BaseTest;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueMetrics;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
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
    provisionerQueues = queueService.getQueueGroup(QueueType.PROVISIONER);
  }

  @After
  public void cleanupTest() {
    provisionerQueues.removeAll();
  }

  @Test
  public void testGetQueueMetrics() throws IOException {
    tenantStore.writeTenant(new Tenant("id1", new TenantSpecification("tenant1", 0, 10, 100)));
    tenantStore.writeTenant(new Tenant("id2", new TenantSpecification("tenant2", 0, 10, 100)));
    tenantStore.writeTenant(new Tenant("id3", new TenantSpecification("tenant3", 0, 10, 100)));
    tenantStore.writeTenant(new Tenant("id4", new TenantSpecification("tenant4", 0, 10, 100)));
    provisionerQueues.add("id1", new Element("task1"));
    provisionerQueues.add("id1", new Element("task2"));
    provisionerQueues.add("id1", new Element("task3"));
    provisionerQueues.take("id1", "consumer");
    provisionerQueues.add("id2", new Element("task4"));
    provisionerQueues.add("id3", new Element("task5"));
    provisionerQueues.take("id3", "consumer");
    provisionerQueues.add("id4", new Element("task6"));
    provisionerQueues.add("id4", new Element("task7"));
    provisionerQueues.take("id4", "consumer");

    Map<String, QueueMetrics> expected = Maps.newHashMap();
    expected.put("tenant1", new QueueMetrics(2, 1));
    expected.put("tenant2", new QueueMetrics(1, 0));
    expected.put("tenant3", new QueueMetrics(0, 1));
    expected.put("tenant4", new QueueMetrics(1, 1));
    expected.put(Constants.SUPERADMIN_TENANT, new QueueMetrics(0, 0));

    Assert.assertEquals(expected.get("tenant1"), service.getTaskQueueMetricsSnapshot("id1"));
    Assert.assertEquals(expected.get("tenant2"), service.getTaskQueueMetricsSnapshot("id2"));
    Assert.assertEquals(expected.get("tenant3"), service.getTaskQueueMetricsSnapshot("id3"));
    Assert.assertEquals(expected.get("tenant4"), service.getTaskQueueMetricsSnapshot("id4"));
    Assert.assertEquals(new QueueMetrics(0, 0), service.getTaskQueueMetricsSnapshot("non-id"));
    Assert.assertEquals(expected, service.getTaskQueueMetricsSnapshot());
  }
}
