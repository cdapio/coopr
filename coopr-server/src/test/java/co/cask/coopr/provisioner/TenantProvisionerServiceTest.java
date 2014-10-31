package co.cask.coopr.provisioner;

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.queue.Element;
import co.cask.coopr.common.queue.QueueType;
import co.cask.coopr.common.queue.TrackingQueue;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class TenantProvisionerServiceTest extends BaseTest {
  private static TenantProvisionerService service;
  private static MockProvisionerRequestService provisionerRequestService;

  @BeforeClass
  public static void setupTestClass() {
    service = injector.getInstance(TenantProvisionerService.class);
    provisionerRequestService = injector.getInstance(MockProvisionerRequestService.class);
  }

  @Before
  public void setupTest() {
    provisionerRequestService.reset();
  }

  @Test(expected = QuotaException.class)
  public void testExceptionWhenQuotaExceeded() throws Exception {
    // 2 cluster quota, 5 node quota
    Tenant tenant = new Tenant("id123", new TenantSpecification("tenantX", 50, 2, 5));
    tenantStore.writeTenant(tenant);
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    service.writeProvisioner(provisioner);

    // write a cluster with 2 nodes, quotas should be fine
    Account account = new Account("user1", tenant.getId());
    Cluster cluster = Cluster.builder()
      .setID("104")
      .setAccount(account)
      .setName("example-hdfs-delete")
      .setProvider(Entities.ProviderExample.RACKSPACE)
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setNodes(ImmutableSet.of("node1", "node2"))
      .setServices(ImmutableSet.of("s1", "s2"))
      .build();
    clusterStoreService.getView(account).writeCluster(cluster);

    // quotas should be fine with 1 more cluster
    Assert.assertTrue(service.satisfiesTenantQuotas(tenant, 1, 0));
    // quotas should not be fine with 2 more clusters
    Assert.assertFalse(service.satisfiesTenantQuotas(tenant, 2, 0));
    // quotas should be fine with 3 more nodes
    Assert.assertTrue(service.satisfiesTenantQuotas(tenant, 0, 3));
    // quotas should not be fine with 4 more nodes
    Assert.assertFalse(service.satisfiesTenantQuotas(tenant, 0, 4));

    // lowering the quotas should throw the exception
    service.writeTenantSpecification(
      new TenantSpecification(tenant.getSpecification().getName(), tenant.getSpecification().getWorkers(), 1, 1));
  }

  @Test
  public void testAddWorkersToProvisioner() throws Exception {
    Tenant tenant = new Tenant("id123", new TenantSpecification("tenantX", 50, 10, 100));
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    service.writeProvisioner(provisioner);
    tenantStore.writeTenant(tenant);

    service.rebalanceTenantWorkers(tenant.getId());
    Provisioner actual = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(tenant.getSpecification().getWorkers(),
                        actual.getAssignedWorkers(tenant.getSpecification().getName()));
  }

  @Test
  public void testRemoveWorkersFromProvisioner() throws Exception {
    Tenant tenant = new Tenant("id123", new TenantSpecification("tenantX", 10, 10, 100));
    Provisioner provisioner =
      new Provisioner("p1", "host", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(tenant.getId(), tenant.getSpecification().getWorkers()));
    service.writeProvisioner(provisioner);
    tenantStore.writeTenant(tenant);

    service.rebalanceTenantWorkers(tenant.getId());
    Provisioner actual = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(tenant.getSpecification().getWorkers(),
                        // tenant ids get replaced with names
                        actual.getAssignedWorkers(tenant.getSpecification().getName()));
  }

  @Test
  public void testDeadProvisionerGetsDeletedDuringAddWorkers() throws Exception {
    Tenant tenant = new Tenant("id123", new TenantSpecification("tenantX", 110, 10, 100));
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null, null);
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    tenantStore.writeTenant(tenant);

    provisionerRequestService.addDeadProvisioner(provisioner1.getId());

    try {
      service.rebalanceTenantWorkers(tenant.getId());
    } catch (CapacityException e) {
      // expected
    }
    // provisioner should have been deleted
    Assert.assertNull(service.getProvisioner(provisioner1.getId()));
  }

  @Test
  public void testDeadProvisionerGetsDeletedDuringRemoveWorkers() throws Exception {
    Tenant tenant = new Tenant("id123", new TenantSpecification("tenantX", 40, 10, 100));
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null,
                                               ImmutableMap.<String, Integer>of(tenant.getId(), 50));
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    tenantStore.writeTenant(tenant);

    provisionerRequestService.addDeadProvisioner(provisioner1.getId());

    try {
      service.rebalanceTenantWorkers(tenant.getId());
    } catch (CapacityException e) {
      // expected
    }
    // provisioner1 should have been deleted
    Assert.assertNull(service.getProvisioner(provisioner1.getId()));

    // normally the tenant would have been queued for rebalancing.  Here we'll just call it directly.
    service.rebalanceTenantWorkers(tenant.getId());
    // workers should have been transferred to provisioner2
    Provisioner p2 = service.getProvisioner(provisioner2.getId());
    Assert.assertEquals(tenant.getSpecification().getWorkers(),
                        // tenant ids should have been replaced with names
                        p2.getAssignedWorkers(tenant.getSpecification().getName()));
  }

  @Test
  public void testDeleteTenant() throws Exception {
    Tenant tenant1 = new Tenant("tenant1", new TenantSpecification("tenant1", 0, 10, 100));
    Tenant tenant2 = new Tenant("tenant2", new TenantSpecification("tenant2", 10, 10, 100));
    Provisioner provisioner1 =
      new Provisioner("p1", "host1", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(
                        tenant2.getId(), 5));
    Provisioner provisioner2 =
      new Provisioner("p2", "host2", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(
                        tenant2.getId(), 5));
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    tenantStore.writeTenant(tenant1);
    tenantStore.writeTenant(tenant2);

    service.deleteTenantByName(tenant1.getId());
    Provisioner actualProvisioner1 = service.getProvisioner(provisioner1.getId());
    Provisioner actualProvisioner2 = service.getProvisioner(provisioner2.getId());
    Assert.assertTrue(!actualProvisioner1.getAssignedTenants().contains(tenant1.getId()));
    Assert.assertTrue(!actualProvisioner2.getAssignedTenants().contains(tenant1.getId()));
  }

  @Test(expected = IllegalStateException.class)
  public void testDeleteTenantWithAssignedWorkersThrowsException() throws Exception {
    Tenant tenant1 = new Tenant("tenant1", new TenantSpecification("tenant1", 10, 10, 100));
    Provisioner provisioner1 =
      new Provisioner("p1", "host1", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(
                        tenant1.getId(), 10));
    service.writeProvisioner(provisioner1);
    tenantStore.writeTenant(tenant1);
    service.deleteTenantByName(tenant1.getId());
  }

  @Test
  public void testHeartbeatChanges() throws Exception {
    Tenant tenant = new Tenant("tenant1", new TenantSpecification("tenant1", 10, 10, 100));
    Provisioner provisioner =
      new Provisioner("p1", "host1", 12345, 100,
                      ImmutableMap.<String, Integer>of(tenant.getId(), 5),
                      ImmutableMap.<String, Integer>of(tenant.getId(), 10));
    service.writeProvisioner(provisioner);
    tenantStore.writeTenant(tenant);

    ProvisionerHeartbeat heartbeat1 = new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of(tenant.getId(), 5));
    ProvisionerHeartbeat heartbeat2 = new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of(tenant.getId(), 10));
    ProvisionerHeartbeat heartbeat3 = new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of());

    service.handleHeartbeat(provisioner.getId(), heartbeat1);
    Provisioner actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(5, actualProvisioner.getLiveWorkers(tenant.getId()));

    service.handleHeartbeat(provisioner.getId(), heartbeat2);
    actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(10, actualProvisioner.getLiveWorkers(tenant.getId()));

    service.handleHeartbeat(provisioner.getId(), heartbeat3);
    actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertEquals(0, actualProvisioner.getLiveWorkers(tenant.getId()));
    Assert.assertTrue(actualProvisioner.getLiveTenants().isEmpty());
  }

  @Test
  public void testHeartbeatAfterDeleteTenant() throws Exception {
    Tenant tenant = new Tenant("tenant1", new TenantSpecification("tenant1", 0, 10, 100));
    Provisioner provisioner =
      new Provisioner("p1", "host1", 12345, 100, ImmutableMap.<String, Integer>of(tenant.getId(), 5), null);
    service.writeProvisioner(provisioner);
    tenantStore.writeTenant(tenant);

    service.deleteTenantByName(tenant.getId());
    service.handleHeartbeat(provisioner.getId(), new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of()));
    Provisioner actualProvisioner = service.getProvisioner(provisioner.getId());
    Assert.assertTrue(actualProvisioner.getAssignedTenants().isEmpty());
    Assert.assertTrue(actualProvisioner.getLiveTenants().isEmpty());
  }

  @Test(expected = MissingEntityException.class)
  public void testHeartbeatForNonexistantProvisionerThrowsException() throws Exception {
    service.handleHeartbeat("id123", new ProvisionerHeartbeat(ImmutableMap.<String, Integer>of()));
  }
}
