package com.continuuity.loom.provisioner.mock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
 *
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
  private final CloseableHttpClient httpClient;
  private int counter;

  public MockProvisionerWorkerService(String provisionerId, String serverUrl,
                                      int capacity, long taskMs, long msBetweenTasks) {
    this.provisionerId = provisionerId;
    this.serverUrl = serverUrl;
    this.taskMs = taskMs;
    this.msBetweenTasks = msBetweenTasks;
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
                       workerExecutorService, taskMs, msBetweenTasks, httpClient);
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
