package com.continuuity.loom.provisioner;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.internal.TimeoutTrackingQueue;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Key;
import com.google.inject.name.Names;
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
    injector.getInstance(Key.get(TimeoutTrackingQueue.class, Names.named(Constants.Queue.WORKER_BALANCE))).start();
    service = injector.getInstance(TenantProvisionerService.class);
    provisionerRequestService = injector.getInstance(MockProvisionerRequestService.class);
  }

  @Before
  public void setupTest() {
    provisionerRequestService.reset();
  }

  @Test
  public void testAddWorkersToProvisioner() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 50, 10, 100);
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    service.writeProvisioner(provisioner);
    service.addTenant(tenant);

    service.rebalanceTenantWorkers(tenant.getId());
    Assert.assertEquals(tenant.getWorkers(),
                        service.getProvisioner(provisioner.getId()).getAssignedWorkers(tenant.getId()));
  }

  @Test
  public void testRemoveWorkersFromProvisioner() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 10, 10, 100);
    Provisioner provisioner =
      new Provisioner("p1", "host", 12345, 100, null,
                      ImmutableMap.<String, Integer>of(tenant.getId(), tenant.getWorkers()));
    service.writeProvisioner(provisioner);
    service.addTenant(tenant);

    service.rebalanceTenantWorkers(tenant.getId());
    Assert.assertEquals(tenant.getWorkers(),
                        service.getProvisioner(provisioner.getId()).getAssignedWorkers(tenant.getId()));
  }

  @Test
  public void testDeadProvisionerGetsDeletedDuringTenantDelete() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 50, 10, 100);
    Provisioner provisioner = new Provisioner("p1", "host1", 12345, 100, null,
                                              ImmutableMap.<String, Integer>of(tenant.getId(), tenant.getWorkers()));
    service.writeProvisioner(provisioner);
    service.addTenant(tenant);

    provisionerRequestService.addDeadProvisioner(provisioner.getId());

    service.deleteTenant(tenant.getId());
    // provisioner should have been deleted
    Assert.assertNull(service.getProvisioner(provisioner.getId()));
  }

  @Test
  public void testDeadProvisionerGetsDeletedDuringAddWorkers() throws Exception {
    Tenant tenant = new Tenant("tenantX", "id123", 110, 10, 100);
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null, null);
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    service.addTenant(tenant);

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
    Tenant tenant = new Tenant("tenantX", "id123", 40, 10, 100);
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null,
                                               ImmutableMap.<String, Integer>of(tenant.getId(), 50));
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    service.writeProvisioner(provisioner1);
    service.writeProvisioner(provisioner2);
    service.addTenant(tenant);

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
    Assert.assertEquals(tenant.getWorkers(), p2.getAssignedWorkers(tenant.getId()));
  }
}
