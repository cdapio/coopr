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
package co.cask.coopr.provisioner.mock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.twill.common.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock service that will spin up and shut down mock workers for tenants.
 */
public class MockProvisionerWorkerService extends AbstractScheduledService {
  private static final Logger LOG = LoggerFactory.getLogger(MockProvisionerWorkerService.class);
  private final MockProvisionerTenantStore provisionerTenantStore = MockProvisionerTenantStore.getInstance();
  private final Multimap<String, MockWorker> tenantWorkers;
  private final ScheduledExecutorService workerExecutorService;
  private final String provisionerId;
  private final String serverUrl;
  private final long taskMs;
  private final long msBetweenTasks;
  private final int failureRate;
  private final CloseableHttpClient httpClient;
  private int counter;

  /**
   * Create a worker service for the given provisioner id that talks to the given server url for tasks and has the
   * given capacity. Each worker created will wait for the given amount of time between taking tasks and will finish
   * a taken task after the given amount of time.
   *
   * @param provisionerId Id of the provisioner
   * @param serverUrl URL of the server workers will communicate with
   * @param capacity Maximum number of workers that can be running at any given time
   * @param taskMs Time that workers should wait before finishing a task
   * @param msBetweenTasks Time workers should wait between finishing a task and taking another task
   * @param failureRate Percentage of the time that a worker should decide to fail the task it was given. 0 to 100.
   */
  public MockProvisionerWorkerService(String provisionerId, String serverUrl,
                                      int capacity, long taskMs, long msBetweenTasks, int failureRate) {
    this.provisionerId = provisionerId;
    this.serverUrl = serverUrl;
    this.taskMs = taskMs;
    this.msBetweenTasks = msBetweenTasks;
    this.failureRate = failureRate;
    this.workerExecutorService = Executors.newScheduledThreadPool(
      capacity,
      Threads.createDaemonThreadFactory("mock-worker-%d"));
    this.tenantWorkers = HashMultimap.create();
    this.counter = 0;

    this.httpClient = HttpClients.custom()
      .setMaxConnPerRoute(capacity + 1)
      .setMaxConnTotal(capacity + 1)
      .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
      .setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(false).setSoTimeout(2000).build())
      .build();
  }

  @Override
  protected void shutDown() throws Exception {
    LOG.info("stopping all workers...");
    for (MockWorker worker : tenantWorkers.values()) {
      LOG.info("stopping worker {} for tenant {}.", worker.getWorkerId(), worker.getTenantId());
      worker.stopAndWait();
    }
    LOG.info("stopped all workers.");
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(0, 10, TimeUnit.SECONDS);
  }

  @Override
  protected ScheduledExecutorService executor() {
    return Executors.newSingleThreadScheduledExecutor(
      Threads.createDaemonThreadFactory("mock-provisioner-worker-service"));
  }

  @Override
  protected void runOneIteration() throws Exception {
    for (String tenant : provisionerTenantStore.getAssignedTenants()) {
      int assigned = provisionerTenantStore.getAssignedWorkers(tenant);
      int live = provisionerTenantStore.getLiveWorkers(tenant);
      int diff = assigned - live;
      if (diff > 0) {
        addWorkers(tenant, diff);
      } else if (diff < 0) {
        removeWorkers(tenant, 0 - diff);
      }
      provisionerTenantStore.setLiveTenantWorkers(tenant, assigned);
    }
    Set<String> tenantsToDelete =
      Sets.difference(provisionerTenantStore.getLiveTenants(), provisionerTenantStore.getAssignedTenants());
    for (String tenantToDelete : tenantsToDelete) {
      LOG.info("Deleting tenant {}", tenantsToDelete);
      removeWorkers(tenantToDelete, provisionerTenantStore.getLiveWorkers(tenantToDelete));
      provisionerTenantStore.deleteTenant(tenantToDelete);
    }
  }

  private void addWorkers(String tenantId, int numToAdd) {
    LOG.info("Adding {} workers to tenant {}...", numToAdd, tenantId);
    for (int i = 0; i < numToAdd; i++) {
      MockWorker worker =
        new MockWorker(provisionerId, String.valueOf(counter), tenantId, serverUrl,
                       workerExecutorService, taskMs, msBetweenTasks, failureRate, httpClient);
      worker.startAndWait();
      tenantWorkers.put(tenantId, worker);
      counter++;
    }
    LOG.info("{} workers added to tenant {}.", numToAdd, tenantId);
  }

  private void removeWorkers(String tenantId, int numToRemove) {
    LOG.info("Removing {} workers from tenant {}.", numToRemove, tenantId);
    List<MockWorker> removed = Lists.newArrayListWithCapacity(numToRemove);
    Iterator<MockWorker> workers = tenantWorkers.get(tenantId).iterator();
    int numRemoved = 0;
    while (workers.hasNext() && numRemoved < numToRemove) {
      MockWorker worker = workers.next();
      worker.stopAndWait();
      removed.add(worker);
      numRemoved++;
    }
    tenantWorkers.get(tenantId).removeAll(removed);
    LOG.info("{} workers removed from tenant {}.", numToRemove, tenantId);
  }
}
