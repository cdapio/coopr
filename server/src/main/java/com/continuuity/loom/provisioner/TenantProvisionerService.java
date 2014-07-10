package com.continuuity.loom.provisioner;

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.continuuity.loom.store.provisioner.ProvisionerStore;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.common.Threads;
import org.apache.twill.zookeeper.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing provisioners.
 */
public class TenantProvisionerService extends AbstractScheduledService {
  private static final Logger LOG  = LoggerFactory.getLogger(TenantProvisionerService.class);
  private final ProvisionerStore provisionerStore;
  private final TenantStore tenantStore;
  private final ZKInterProcessReentrantLock lock;
  private final long provisionerTimeoutSecs;
  private final TrackingQueue balanceQueue;
  private final ProvisionerRequestService provisionerRequestService;

  @Inject
  private TenantProvisionerService(ProvisionerStore provisionerStore,
                                   TenantStore tenantStore,
                                   ZKClient zkClient,
                                   @Named(Constants.Queue.WORKER_BALANCE) TrackingQueue balanceQueue,
                                   ProvisionerRequestService provisionerRequestService,
                                   Configuration conf) {
    this.provisionerStore = provisionerStore;
    this.tenantStore = tenantStore;
    this.provisionerRequestService = provisionerRequestService;
    this.lock = new ZKInterProcessReentrantLock(zkClient, Constants.TENANT_NAMESPACE);
    this.provisionerTimeoutSecs = conf.getLong(Constants.PROVISIONER_TIMEOUT_SECS);
    this.balanceQueue = balanceQueue;
  }

  public Collection<Tenant> getAllTenants() throws IOException {
    return tenantStore.getAllTenants();
  }

  public Tenant getTenant(String tenantId) throws IOException {
    return tenantStore.getTenant(tenantId);
  }

  public Collection<Provisioner> getAllProvisioners() throws IOException {
    return provisionerStore.getAllProvisioners();
  }

  public Provisioner getProvisioner(String provisionerId) throws IOException {
    return provisionerStore.getProvisioner(provisionerId);
  }

  public void addTenant(Tenant tenant) throws IOException, CapacityException {
    lock.acquire();
    try {
      checkCapacity(tenant.getWorkers());
      balanceQueue.add(new Element(tenant.getId()));
      tenantStore.writeTenant(tenant);
    } finally {
      lock.release();
    }
  }

  public void updateTenant(Tenant tenant) throws IOException, CapacityException {
    lock.acquire();
    try {
      Tenant prevTenant = tenantStore.getTenant(tenant.getId());
      checkCapacity(tenant.getWorkers() - prevTenant.getWorkers());
      balanceQueue.add(new Element(tenant.getId()));
      tenantStore.writeTenant(tenant);
    } finally {
      lock.release();
    }
  }

  public void deleteTenant(String tenantId) throws IOException {
    lock.acquire();
    try {
      Collection<Provisioner> tenantProvisioners = provisionerStore.getTenantProvisioners(tenantId);
      provisionerStore.unassignTenantProvisioners(tenantId);

      // send requests to provisioners to delete tenant
      // if the db dies or something in between, the client making the request will get a 500.  No way to rollback,
      // so client would have to resend the delete when things are ok again.
      // TODO: what happens if we crash here? workaround would be to restart provisioners...
      for (Provisioner provisioner : tenantProvisioners) {
        LOG.debug("Requesting provisioner {} to delete tenant {}.", provisioner.getId(), tenantId);
        if (!provisionerRequestService.deleteTenant(provisioner, tenantId)) {
          // unable to complete request to delete tenant from this provisioner. Something is wrong with it, delete it
          // and rebalance its workers.
          LOG.error("Could not delete tenant {} from provisioner {}. The provisioner appears broken, deleting it and " +
                      "rebalancing its tenant workers", tenantId, provisioner.getId());
          deleteProvisioner(provisioner);
        }
      }
      tenantStore.deleteTenant(tenantId);
    } finally {
      lock.release();
    }
  }

  public void deleteProvisioner(String provisionerId) throws IOException {
    lock.acquire();
    try {
      Provisioner provisioner = provisionerStore.getProvisioner(provisionerId);
      if (provisioner == null) {
        return;
      }

      deleteProvisioner(provisioner);
    } finally {
      lock.release();
    }
  }

  public void handleHeartbeat(String provisionerId, ProvisionerHeartbeat heartbeat)
    throws IOException, MissingEntityException {
    lock.acquire();
    try {
      Provisioner provisioner = provisionerStore.getProvisioner(provisionerId);
      if (provisioner == null) {
        throw new MissingEntityException("Provisioner " + provisionerId + " not found.");
      }
      if (!provisioner.getUsage().equals(heartbeat.getUsage())) {
        provisioner.setUsage(heartbeat.getUsage());
        provisionerStore.writeProvisioner(provisioner);
      }
      provisionerStore.setHeartbeat(provisionerId, System.currentTimeMillis());
    } finally {
      lock.release();
    }
  }

  public void writeProvisioner(Provisioner provisioner) throws IOException {
    lock.acquire();
    try {
      // if a provisioner died earlier leaving us under capacity, and this provisioner adds capacity, we want to
      // rebalance the workers after writing the provisioner.
      boolean rebalanceWorkers = provisionerStore.getFreeCapacity() == 0;
      provisionerStore.writeProvisioner(provisioner);
      if (rebalanceWorkers) {
        for (Tenant tenant : tenantStore.getAllTenants()) {
          balanceQueue.add(new Element(tenant.getId()));
        }
      }
    } finally {
      lock.release();
    }
  }

  public void rebalanceTenantWorkers(String tenantId) throws IOException, CapacityException {
    lock.acquire();
    try {
      Tenant tenant = tenantStore.getTenant(tenantId);
      if (tenant == null) {
        return;
      }

      int diff = tenant.getWorkers() - provisionerStore.getNumAssignedWorkers(tenantId);
      if (diff < 0) {
        // too many workers assigned, remove some.
        int toRemove = 0 - diff;
        LOG.debug("Removing {} workers from tenant {}", toRemove, tenantId);
        removeWorkers(tenantId, toRemove);
      } else if (diff > 0) {
        // not enough workers assigned, assign some more.
        LOG.debug("Adding {} workers to tenant {}", diff, tenantId);
        addWorkers(tenantId, diff);
      }
    } finally {
      lock.release();
    }
  }

  // TODO: abstract out to support different types of balancing policies
  // Currently a greedy approach, just remove from first available.
  private void removeWorkers(String tenantId, int numToRemove) throws IOException {
    // go through each provisioner, removing workers for the tenant until we've removed enough.
    for (Provisioner provisioner : provisionerStore.getTenantProvisioners(tenantId)) {
      int numRemoved = provisioner.tryRemoveTenantAssignments(tenantId, numToRemove);
      if (numRemoved > 0) {
        provisionerStore.writeProvisioner(provisioner);
        LOG.debug("Requesting provisioner {} to set workers to {} for tenant {} (removing {})",
                  provisioner.getId(), provisioner.getAssignedWorkers(tenantId), tenantId, numRemoved);
        if (provisionerRequestService.putTenant(provisioner, tenantId)) {
          numToRemove -= numRemoved;
        } else {
          // request failed with retries. something is wrong with the provisioner, delete it and rebalance its workers
          // TODO: what if this fails?
          LOG.error("Could not write tenant {} to provisioner {}. The provisioner appears broken, deleting it and " +
                     "rebalancing its tenant workers", tenantId, provisioner.getId());
          deleteProvisioner(provisioner);
        }
      }
    }
  }

  // TODO: abstract out to support different types of balancing policies
  // Currently a greedy approach, just add to first available.
  private void addWorkers(String tenantId, int numToAdd) throws CapacityException, IOException {
    for (Provisioner provisioner : provisionerStore.getProvisionersWithFreeCapacity()) {
      int numAdded = provisioner.tryAddTenantAssignments(tenantId, numToAdd);
      if (numAdded > 0) {
        provisionerStore.writeProvisioner(provisioner);
        LOG.debug("Requesting provisioner {} to set workers to {} for tenant {} (adding {})",
                  provisioner.getId(), provisioner.getAssignedWorkers(tenantId), tenantId, numAdded);
        if (provisionerRequestService.putTenant(provisioner, tenantId)) {
          numToAdd -= numAdded;
        } else {
          // request failed with retries. something is wrong with the provisioner, delete it and rebalance its workers
          // TODO: what if this fails?
          LOG.error("Could not write tenant {} to provisioner {}. The provisioner appears broken, deleting it and " +
                     "rebalancing its tenant workers", tenantId, provisioner.getId());
          deleteProvisioner(provisioner);
        }
      }
    }
    if (numToAdd > 0) {
      throw new CapacityException("Unable to add all " + numToAdd + " workers to tenant "
                                    + tenantId + " without exceeding worker capacity.");
    }
  }

  private void deleteProvisioner(Provisioner provisioner) throws IOException {
    for (String tenant : provisioner.getAssignedTenants()) {
      balanceQueue.add(new Element(tenant));
    }
    provisionerStore.deleteProvisioner(provisioner.getId());
  }

  @Override
  protected void runOneIteration() throws Exception {
    lock.acquire();
    try {
      // time out provisioners that have not sent a heartbeat in a while
      long staleTime = System.currentTimeMillis() -
        TimeUnit.MILLISECONDS.convert(provisionerTimeoutSecs, TimeUnit.SECONDS);
      Set <String> affectedTenants = Sets.newHashSet();
      for (Provisioner provisioner : provisionerStore.getIdleProvisioners(staleTime)) {
        String provisionerId = provisioner.getId();
        LOG.error("provisioner {} has not sent a heartbeat in over {} seconds, deleting it...",
                 provisionerId, provisionerTimeoutSecs);
        provisionerStore.deleteProvisioner(provisioner.getId());
        affectedTenants.addAll(provisioner.getAssignedTenants());
      }
      for (String affectedTenant : affectedTenants) {
        balanceQueue.add(new Element(affectedTenant));
      }
    } finally {
      lock.release();
    }
  }

  @Override
  protected ScheduledExecutorService executor() {
    return Executors.newSingleThreadScheduledExecutor(Threads.createDaemonThreadFactory("tenant-provisioner-service"));
  }

  @Override
  protected Scheduler scheduler() {
    // if the server was down for a while, we don't want to time out provisioners right away but want to
    // give them a chance to get their heartbeats in.  So wait for a while before starting the timeout logic.
    return Scheduler.newFixedRateSchedule(provisionerTimeoutSecs, 60, TimeUnit.SECONDS);
  }

  private void checkCapacity(int diff) throws IOException, CapacityException {
    if (diff > provisionerStore.getFreeCapacity()) {
      throw new CapacityException("Not enough capacity.");
    }
  }
}
