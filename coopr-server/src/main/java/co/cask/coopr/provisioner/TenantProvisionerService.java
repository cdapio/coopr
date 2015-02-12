package co.cask.coopr.provisioner;

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueueGroup;
import co.cask.coopr.common.queue.QueueService;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.common.zookeeper.LockService;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import co.cask.coopr.provisioner.plugin.ResourceService;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.cluster.ClusterStoreView;
import co.cask.coopr.store.entity.EntityStoreService;
import co.cask.coopr.store.provisioner.ProvisionerStore;
import co.cask.coopr.store.tenant.TenantStore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * Service for managing provisioners.
 */
public class TenantProvisionerService {
  private static final Logger LOG  = LoggerFactory.getLogger(TenantProvisionerService.class);
  private final ProvisionerStore provisionerStore;
  private final TenantStore tenantStore;
  private final Lock tenantLock;
  private final long provisionerTimeoutSecs;
  private final TrackingQueue balanceQueue;
  private final ProvisionerRequestService provisionerRequestService;
  private final ClusterStoreService clusterStoreService;
  private final ResourceService resourceService;
  private final EntityStoreService entityStoreService;
  private final QueueService queueService;

  @Inject
  private TenantProvisionerService(ProvisionerStore provisionerStore,
                                   final TenantStore tenantStore,
                                   LockService lockService,
                                   @Named(Constants.Queue.WORKER_BALANCE) TrackingQueue balanceQueue,
                                   ClusterStoreService clusterStoreService,
                                   ProvisionerRequestService provisionerRequestService,
                                   ResourceService resourceService,
                                   EntityStoreService entityStoreService,
                                   QueueService queueService,
                                   Configuration conf) {
    this.provisionerStore = provisionerStore;
    this.tenantStore = tenantStore;
    this.provisionerRequestService = provisionerRequestService;
    this.clusterStoreService = clusterStoreService;
    this.resourceService = resourceService;
    this.entityStoreService = entityStoreService;
    // a single lock is used across all tenants and provisioners. This is so that a request modifies worker assignments
    // across multiple provisioners does not conflict with requests that modify worker capacity. For example, if a
    // tenant is added the same time a tenant is deleted, we don't want to be modifying the same provisioner at the
    // same time and cause conflicts. Similarly, if we're moving workers from one provisioner to another at the same
    // time as we're adding a tenant, we don't want to both add workers to the same provisioner at the same time.
    this.tenantLock = lockService.getTenantProvisionerLock();
    this.provisionerTimeoutSecs = conf.getLong(Constants.PROVISIONER_TIMEOUT_SECS);
    this.balanceQueue = balanceQueue;
    this.queueService = queueService;
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
    for (Provisioner provisioner : provisioners) {
      externalProvisioners.add(createExternalProvisioner(provisioner));
    }
    return externalProvisioners;
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
    return createExternalProvisioner(provisionerStore.getProvisioner(provisionerId));
  }

  /**
   * Write the tenant to the store and balance the tenant workers across provisioners. Returns the id of the tenant
   * that was written.
   *
   * @param tenantSpecification Tenant to write
   * @throws IOException if there was an exception persisting the tenant
   * @throws CapacityException if there is not enough capacity to support all tenant workers
   * @throws QuotaException if a tenant quota would be violated by the change
   * @return Id of the written tenant
   */
  public String writeTenantSpecification(TenantSpecification tenantSpecification)
    throws IOException, CapacityException, QuotaException {
    tenantLock.lock();
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
      Tenant updatedTenant = new Tenant(id, tenantSpecification);
      // check if changing the tenant would cause the cluster or node quotas to be exceeded.
      if (!satisfiesTenantQuotas(updatedTenant, 0, 0)) {
        throw new QuotaException("Writing tenant would cause cluster or node quotas to be violated.");
      }

      balanceQueue.add(new Element(id));
      tenantStore.writeTenant(updatedTenant);
      return id;
    } finally {
      tenantLock.unlock();
    }
  }

  /**
   * Verify that tenant cluster and node quotas would not be exceeded if the given number of additional clusters and
   * nodes would be added.
   *
   * @param tenantId Id of the tenant to verify quotas for
   * @param additionalClusters Number of clusters that would be added
   * @param additionalNodes Number of nodes that would be added
   * @return true if the tenant quotas would be satisfied, false if they would be exceeded.
   */
  public boolean satisfiesTenantQuotas(String tenantId, int additionalClusters,
                                       int additionalNodes) throws IOException {
    Tenant tenant = tenantStore.getTenantByID(tenantId);
    // if there is no tenant there are no quotas to voilate
    if (tenant == null) {
      return true;
    }
    return satisfiesTenantQuotas(tenant, additionalClusters, additionalNodes);
  }

  /**
   * Verify that tenant cluster and node quotas would not be exceeded if the given number of additional clusters and
   * nodes would be added.
   *
   * @param tenant Tenant to verify quotas for
   * @param additionalClusters Number of clusters that would be added
   * @param additionalNodes Number of nodes that would be added
   * @return true if the tenant quotas would be satisfied, false if they would be exceeded.
   */
  public boolean satisfiesTenantQuotas(Tenant tenant, int additionalClusters, int additionalNodes) throws IOException {
    ClusterStoreView view = clusterStoreService.getView(new Account(Constants.ADMIN_USER, tenant.getId()));
    List<Cluster> nonTerminatedClusters = view.getNonTerminatedClusters();

    int numClusters = additionalClusters + nonTerminatedClusters.size();
    if (numClusters > tenant.getSpecification().getMaxClusters()) {
      return false;
    }

    int numNodes = additionalNodes;
    for (Cluster cluster : nonTerminatedClusters) {
      numNodes += cluster.getNodeIDs().size();
    }
    if (numNodes > tenant.getSpecification().getMaxNodes()) {
      return false;
    }

    return true;
  }

  /**
   * Delete the given tenant. A tenant must not have any assigned workers in order for deletion to be allowed.
   *
   * @param name Name of the tenant to delete
   * @throws IllegalStateException if the tenant has one or more assigned workers
   * @throws IOException if there was an exception persisting the deletion
   */
  public void deleteTenantByName(String name) throws IllegalStateException, IOException {
    tenantLock.lock();
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
      for (QueueGroup queueGroup : queueService.getAllQueueGroups().values()) {
        queueGroup.removeAll(tenant.getId());
      }
    } finally {
      tenantLock.unlock();
    }
  }

  /**
   * Delete the given provisioner, queueing a job to reassign its workers to different provisioners.
   *
   * @param provisionerId Id of the provisioner to delete
   * @throws IOException
   */
  public void deleteProvisioner(String provisionerId) throws IOException {
    tenantLock.lock();
    try {
      Provisioner provisioner = provisionerStore.getProvisioner(provisionerId);
      if (provisioner == null) {
        return;
      }

      deleteProvisioner(provisioner);
    } finally {
      tenantLock.unlock();
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
    tenantLock.lock();
    try {
      provisionerStore.writeProvisioner(provisioner);
      // rebalance tenants every time a provisioner registers itself
      for (Tenant tenant : tenantStore.getAllTenants()) {
        balanceQueue.add(new Element(tenant.getId()));
      }
    } finally {
      tenantLock.unlock();
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
    // lock across all tenants to protect against conflicts in setting worker counts for different tenants across
    // different provisioners
    tenantLock.lock();
    try {
      Tenant tenant = tenantStore.getTenantByID(tenantId);
      if (tenant == null) {
        return;
      }

      int diff = tenant.getSpecification().getWorkers() - provisionerStore.getNumAssignedWorkers(tenantId);
      if (diff < 0) {
        Account tenantAdmin = new Account(Constants.ADMIN_USER, tenantId);
        ResourceCollection liveResources = resourceService.getLiveResources(tenantAdmin);
        // too many workers assigned, remove some.
        int toRemove = 0 - diff;
        LOG.debug("Removing {} workers from tenant {}", toRemove, tenantId);
        removeWorkers(tenantId, toRemove, liveResources);
      } else if (diff > 0) {
        Account tenantAdmin = new Account(Constants.ADMIN_USER, tenantId);
        ResourceCollection liveResources = resourceService.getLiveResources(tenantAdmin);
        // not enough workers assigned, assign some more.
        LOG.debug("Adding {} workers to tenant {}", diff, tenantId);
        addWorkers(tenantId, diff, liveResources);
      }
    } finally {
      tenantLock.unlock();
    }
  }

  /**
   * Get a snapshot of what plugin resources are slated to be active on the provisioners that are running workers
   * for the given account, and push those resources to the provisioners.
   *
   * @param account Account to sync
   * @throws IOException
   */
  public void syncResources(Account account) throws IOException {
    // this will mean that a sync from one tenant will block a sync from another, but we need this because when
    // workers are being re-balanced, the live resource collection is sent to the provisioners. We don't want a
    // scenario where the live collection is read for rebalancing, a sync is called, and the sync and rebalance
    // fight over what resource versions should be live on the provisioners, resulting in inconsistent state.
    // Since syncs are uncommon, this should be ok...
    tenantLock.lock();
    try {
      ResourceCollection resources = resourceService.getResourcesToSync(account);

      // TODO: failures will cause inconsistencies between metadata state and provisioner state.
      // We can add an ability to block tasks for a given tenant from going out here, then make the calls to the
      // provisioners and update the metadata store, then unblock tasks for the tenant. That way if the sync fails,
      // tasks are not being taken so there is no ambiguity on what code is running. Maybe add a blockTakes call
      // to the queue group interface.

      // update tenant provisioners
      syncProvisionerResources(account.getTenantId(), resources);

      // update metadata store
      resourceService.syncResourceMeta(account, resources);

    } finally {
      tenantLock.unlock();
    }
  }

  private void syncProvisionerResources(String tenantId, ResourceCollection resourceCollection) throws IOException {
    for (Provisioner provisioner : provisionerStore.getTenantProvisioners(tenantId)) {
      if (!provisionerRequestService.putTenant(provisioner, tenantId, resourceCollection)) {
        LOG.error("Could not write resource metadata for tenant {} to provisioner {}. " +
                    "The provisioner appears broken, deleting it and rebalancing its tenant workers",
                  tenantId, provisioner.getId());
        deleteProvisioner(provisioner);
      }
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
    tenantLock.lock();
    try {
      Set<String> affectedTenants = Sets.newHashSet();
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
      tenantLock.unlock();
    }
  }

  public void bootstrapTenant(String tenantId) throws IOException, IllegalAccessException {
    Account account = new Account(Constants.ADMIN_USER, tenantId);

    LOG.debug("Bootstrapping entities");
    entityStoreService.copyEntities(Account.SUPERADMIN, account);
    // bootstrap plugin resources
    LOG.debug("Bootstrapping plugin resources");
    resourceService.bootstrapResources(account);
    LOG.debug("Syncing plugin resources");
    syncResources(account);
  }

  // TODO: abstract out to support different types of balancing policies
  // Currently a greedy approach, just remove from first available.
  private void removeWorkers(String tenantId, int numToRemove, ResourceCollection resources) throws IOException {
    // go through each provisioner, removing workers for the tenant until we've removed enough.
    for (Provisioner provisioner : provisionerStore.getTenantProvisioners(tenantId)) {
      int numRemoved = provisioner.tryRemoveTenantAssignments(tenantId, numToRemove);
      if (numRemoved > 0) {
        provisionerStore.writeProvisioner(provisioner);
        LOG.debug("Requesting provisioner {} to set workers to {} for tenant {} (removing {})",
                  provisioner.getId(), provisioner.getAssignedWorkers(tenantId), tenantId, numRemoved);
        if (provisionerRequestService.putTenant(provisioner, tenantId, resources)) {
          numToRemove -= numRemoved;
        } else {
          // request failed with retries. something is wrong with the provisioner, delete it and rebalance its workers
          // TODO: what if this fails?
          LOG.error("Could not set workers for tenant {} to provisioner {}. " +
                      "The provisioner appears broken, deleting it and rebalancing its tenant workers",
                    tenantId, provisioner.getId());
          deleteProvisioner(provisioner);
        }
      }
    }
  }

  // TODO: abstract out to support different types of balancing policies
  // Currently a greedy approach, just add to first available.
  private void addWorkers(String tenantId, int numToAdd, ResourceCollection resources)
    throws CapacityException, IOException {
    for (Provisioner provisioner : provisionerStore.getProvisionersWithFreeCapacity()) {
      if (numToAdd <= 0) {
        break;
      }
      int numAdded = provisioner.tryAddTenantAssignments(tenantId, numToAdd);
      if (numAdded > 0) {
        provisionerStore.writeProvisioner(provisioner);
        LOG.debug("Requesting provisioner {} to set workers to {} for tenant {} (adding {})",
                  provisioner.getId(), provisioner.getAssignedWorkers(tenantId), tenantId, numAdded);
        if (provisionerRequestService.putTenant(provisioner, tenantId, resources)) {
          numToAdd -= numAdded;
        } else {
          // request failed with retries. something is wrong with the provisioner, delete it and rebalance its workers.
          // Rebalancing will be queued, but will not be triggered until after this method finishes due to the
          // lock that is held.
          // TODO: what if this fails due to db failure or something of that sort?
          // should be ok as long as the tenant balance task is in the queue and retried.
          LOG.error("Could not set workers for tenant {} to provisioner {}. " +
                      "The provisioner appears broken, deleting it and rebalancing its tenant workers",
                    tenantId, provisioner.getId());
          deleteProvisioner(provisioner);
        }
      }
    }
    if (numToAdd > 0) {
      throw new CapacityException("Unable to add all " + numToAdd + " workers to tenant "
                                    + tenantId + " without exceeding worker capacity.");
    }
  }

  /**
   * Create a new Provisioner object where the tenant ids have been replaced with tenant names for external
   * consumption.
   *
   * @param provisioner Internal provisioner that uses tenant ids
   * @return New Provisioner where the tenant ids have been replaced with tenant names
   * @throws IOException
   */
  private Provisioner createExternalProvisioner(Provisioner provisioner) throws IOException {
    if (provisioner == null) {
      return null;
    }
    Map<String, Integer> nameUsage = Maps.newHashMap();
    for (Map.Entry<String, Integer> entry : provisioner.getUsage().entrySet()) {
      nameUsage.put(tenantIdToName(entry.getKey()), entry.getValue());
    }
    Map<String, Integer> nameAssignments = Maps.newHashMap();
    for (String tenantId : provisioner.getAssignedTenants()) {
      nameAssignments.put(tenantIdToName(tenantId), provisioner.getAssignedWorkers(tenantId));
    }
    return new Provisioner(provisioner.getId(), provisioner.getHost(), provisioner.getPort(),
                           provisioner.getCapacityTotal(), nameUsage, nameAssignments);
  }

  // id to name mapping is cached, so this mapping should not be expensive
  private String tenantIdToName(String id) throws IOException {
    String tenantName = tenantStore.getNameForId(id);
    if (tenantName == null) {
      LOG.warn("Could not map tenant id {} to a name, will use the id.");
      tenantName = id;
    }
    return tenantName;
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
