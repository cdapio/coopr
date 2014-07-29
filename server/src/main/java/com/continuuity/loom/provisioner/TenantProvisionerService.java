package com.continuuity.loom.provisioner;

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.admin.TenantSpecification;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.common.zookeeper.lib.ZKInterProcessReentrantLock;
import com.continuuity.loom.scheduler.task.MissingEntityException;
import com.continuuity.loom.store.provisioner.ProvisionerStore;
import com.continuuity.loom.store.tenant.TenantStore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.zookeeper.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing provisioners.
 */
public class TenantProvisionerService {
  private static final Logger LOG  = LoggerFactory.getLogger(TenantProvisionerService.class);
  private final ProvisionerStore provisionerStore;
  private final TenantStore tenantStore;
  private final ZKInterProcessReentrantLock lock;
  private final long provisionerTimeoutSecs;
  private final TrackingQueue balanceQueue;
  private final ProvisionerRequestService provisionerRequestService;
  private final LoadingCache<String, String> tenantIdNameMap;

  @Inject
  private TenantProvisionerService(ProvisionerStore provisionerStore,
                                   final TenantStore tenantStore,
                                   ZKClient zkClient,
                                   @Named(Constants.Queue.WORKER_BALANCE) TrackingQueue balanceQueue,
                                   ProvisionerRequestService provisionerRequestService,
                                   Configuration conf) {
    this.provisionerStore = provisionerStore;
    this.tenantStore = tenantStore;
    this.provisionerRequestService = provisionerRequestService;
    // a single lock is used across all tenants and provisioners. This is so that a request modifies worker assignments
    // across multiple provisioners does not conflict with requests that modify worker capacity. For example, if a
    // tenant is added the same time a tenant is deleted, we don't want to be modifying the same provisioner at the
    // same time and cause conflicts. Similarly, if we're moving workers from one provisioner to another at the same
    // time as we're adding a tenant, we don't want to both add workers to the same provisioner at the same time.
    this.lock = new ZKInterProcessReentrantLock(zkClient, Constants.TENANT_NAMESPACE);
    this.provisionerTimeoutSecs = conf.getLong(Constants.PROVISIONER_TIMEOUT_SECS);
    this.balanceQueue = balanceQueue;
    tenantIdNameMap = CacheBuilder.newBuilder().build(new CacheLoader<String, String>() {
      @Override
      public String load(String key) throws Exception {
        Tenant tenant = tenantStore.getTenantByID(key);
        if (tenant == null) {
          LOG.warn("Unable to find the name for tenant with id {}, it may have been deleted.", key);
          return key;
        }
        return tenant.getSpecification().getName();
      }
    });
  }

  /**
   * Get an unmodifiable collection of all tenant specifications.
   *
   * @return Unmodifiable collection of all tenant specifications
   * @throws IOException
   */
  public Collection<TenantSpecification> getAllTenantSpecifications() throws IOException {
    return tenantStore.getAllTenantSpecifications();
  }

  /**
   * Get a tenant specification by tenant name.
   *
   * @param name Name of the tenant to get
   * @return Tenant for the given name, or null if none exists
   * @throws IOException
   */
  public TenantSpecification getTenantSpecification(String name) throws IOException {
    Tenant tenant = tenantStore.getTenantByName(name);
    return tenant == null ? null : tenant.getSpecification();
  }

  /**
   * Get an immutable collection of all provisioners for external display, with tenant ids mapped to tenant names.
   *
   * @return Immutable collection of all provisioners for external display, with tenant ids mapped to tenant names
   * @throws IOException
   */
  public Collection<Provisioner> getAllProvisioners() throws IOException {
    Collection<Provisioner> provisioners = provisionerStore.getAllProvisioners();
    List<Provisioner> externalProvisioners = Lists.newArrayListWithCapacity(provisioners.size());
    try {
      for (Provisioner provisioner : provisioners) {
        externalProvisioners.add(Provisioner.swapIdsForNames(provisioner, tenantIdNameMap));
      }
      return externalProvisioners;
    } catch (ExecutionException e) {
      LOG.error("Exception mapping tenant ids to names.", e);
      throw new IOException(e);
    }
  }

  /**
   * Get the provisioner for the given id for external display, with tenant ids mapped to tenant names,
   * or null if none exists.
   *
   * @param provisionerId Id of the provisioner to get
   * @return Provisioner for the given id with tenant ids mapped to tenant names, or null if none exists
   * @throws IOException
   */
  public Provisioner getProvisioner(String provisionerId) throws IOException {
    try {
      return Provisioner.swapIdsForNames(provisionerStore.getProvisioner(provisionerId), tenantIdNameMap);
    } catch (ExecutionException e) {
      LOG.error("Exception mapping tenant ids to names.", e);
      throw new IOException(e);
    }
  }

  /**
   * Write the tenant to the store and balance the tenant workers across provisioners.
   *
   * @param tenantSpecification Tenant to write
   * @throws IOException if there was an exception persisting the tenant
   * @throws CapacityException if there is not enough capacity to support all tenant workers
   */
  public void writeTenantSpecification(TenantSpecification tenantSpecification) throws IOException, CapacityException {
    lock.acquire();
    try {
      Tenant prevTenant = tenantStore.getTenantByName(tenantSpecification.getName());
      String id;
      if (prevTenant == null) {
        // if we're adding a new tenant
        id = UUID.randomUUID().toString();
        checkCapacity(tenantSpecification.getWorkers());
      } else {
        // we're updating an existing tenant
        id = prevTenant.getId();
        checkCapacity(tenantSpecification.getWorkers() - prevTenant.getSpecification().getWorkers());
      }
      balanceQueue.add(new Element(id));
      tenantStore.writeTenant(new Tenant(id, tenantSpecification));
    } finally {
      lock.release();
    }
  }

  /**
   * Delete the given tenant. A tenant must not have any assigned workers in order for deletion to be allowed.
   *
   * @param name Name of the tenant to delete
   * @throws IllegalStateException if the tenant has one or more assigned workers
   * @throws IOException if there was an exception persisting the deletion
   */
  public void deleteTenantByName(String name) throws IllegalStateException, IOException {
    lock.acquire();
    try {
      Tenant tenant = tenantStore.getTenantByName(name);
      if (tenant == null) {
        return;
      }
      int numAssignedWorkers = provisionerStore.getNumAssignedWorkers(tenant.getId());
      if (numAssignedWorkers > 0) {
        throw new IllegalStateException("Tenant " + name + " still has " + numAssignedWorkers + " workers. " +
                                          "Cannot delete it until workers are set to 0.");
      }
      tenantStore.deleteTenantByName(name);
    } finally {
      lock.release();
    }
  }

  /**
   * Delete the given provisioner, queueing a job to reassign its workers to different provisioners.
   *
   * @param provisionerId Id of the provisioner to delete
   * @throws IOException
   */
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

  /**
   * Handle the heartbeat of a provisioner, updating the last heartbeat time of the provisioner and updating the number
   * of live workers running on the provisioner for each tenant it is responsible for.
   *
   * @param provisionerId Id of the provisioner that sent the heartbeat
   * @param heartbeat The heartbeat containing live worker information
   * @throws IOException if there was an exception persisting the data
   * @throws MissingEntityException if there is no provisioner for the given id
   */
  public void handleHeartbeat(String provisionerId, ProvisionerHeartbeat heartbeat)
    throws IOException, MissingEntityException {
    // no lock required here.  Simply getting a provisioner and writing worker usage. Would only expect one provisioner
    // to be calling this at a time, and even if it is calling it concurrently for some reason, only the usage can
    // change and for that its ok for one of them to win.
    Provisioner provisioner = provisionerStore.getProvisioner(provisionerId);
    if (provisioner == null) {
      throw new MissingEntityException("Provisioner " + provisionerId + " not found.");
    }
    if (!provisioner.getUsage().equals(heartbeat.getUsage())) {
      provisioner.setUsage(heartbeat.getUsage());
      provisionerStore.writeProvisioner(provisioner);
    }
    provisionerStore.setHeartbeat(provisionerId, System.currentTimeMillis());
  }

  /**
   * Write a provisioner, and queue jobs to rebalance workers for all tenants in the system.
   *
   * @param provisioner Provisioner to write
   * @throws IOException
   */
  public void writeProvisioner(Provisioner provisioner) throws IOException {
    lock.acquire();
    try {
      provisionerStore.writeProvisioner(provisioner);
      // rebalance tenants every time a provisioner registers itself
      for (Tenant tenant : tenantStore.getAllTenants()) {
        balanceQueue.add(new Element(tenant.getId()));
      }
    } finally {
      lock.release();
    }
  }

  /**
   * Rebalance workers for the tenant across the provisioners.
   *
   * @param tenantId Id of the tenant whose workers need to be rebalanced
   * @throws CapacityException if there is not enough capacity to rebalance tenant workers
   * @throws IOException if there was an exception persisting the worker rebalance
   */
  public void rebalanceTenantWorkers(String tenantId) throws IOException, CapacityException {
    lock.acquire();
    try {
      Tenant tenant = tenantStore.getTenantByID(tenantId);
      if (tenant == null) {
        return;
      }

      int diff = tenant.getSpecification().getWorkers() - provisionerStore.getNumAssignedWorkers(tenantId);
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

  /**
   * Delete and reassign workers for provisioners that have not sent a heartbeat since the given timestamp in
   * milliseconds.
   *
   * @param timeoutTs Timestamp in milliseconds to use as a cut off for deleting provisioners.
   * @throws IOException
   */
  public void timeoutProvisioners(long timeoutTs) throws IOException {
    lock.acquire();
    try {
      Set <String> affectedTenants = Sets.newHashSet();
      for (Provisioner provisioner : provisionerStore.getTimedOutProvisioners(timeoutTs)) {
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
      if (numToAdd <= 0) {
        break;
      }
      int numAdded = provisioner.tryAddTenantAssignments(tenantId, numToAdd);
      if (numAdded > 0) {
        provisionerStore.writeProvisioner(provisioner);
        LOG.debug("Requesting provisioner {} to set workers to {} for tenant {} (adding {})",
                  provisioner.getId(), provisioner.getAssignedWorkers(tenantId), tenantId, numAdded);
        if (provisionerRequestService.putTenant(provisioner, tenantId)) {
          numToAdd -= numAdded;
        } else {
          // request failed with retries. something is wrong with the provisioner, delete it and rebalance its workers.
          // Rebalancing will be queued, but will not be triggered until after this method finishes due to the
          // lock that is held.
          // TODO: what if this fails due to db failure or something of that sort?
          // should be ok as long as the tenant balance task is in the queue and retried.
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

  private void checkCapacity(int diff) throws IOException, CapacityException {
    if (diff > provisionerStore.getFreeCapacity()) {
      throw new CapacityException("Not enough capacity.");
    }
  }
}
