package co.cask.coopr.store.provisioner;

import co.cask.coopr.provisioner.Provisioner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public abstract class ProvisionerStoreTest {
  private final Provisioner provisioner1 = new Provisioner(
    "p1", "host1", 12345, 10,
    ImmutableMap.<String, Integer>of("tenantX", 5, "tenantY", 3),
    ImmutableMap.<String, Integer>of("tenantX", 5, "tenantY", 5)
  );
  private final Provisioner provisioner2 = new Provisioner(
    "p2", "host2", 12345, 100,
    ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 3, "tenantZ", 2),
    ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 5, "tenantZ", 2)
  );

  abstract ProvisionerStore getProvisionerStore();

  abstract void clearData() throws Exception;

  @Before
  public void setupTest() throws Exception {
    clearData();
  }

  @Test
  public void testWriteGetDeleteProvisioner() throws IOException {
    ProvisionerStore store = getProvisionerStore();
    String id = provisioner1.getId();
    Assert.assertNull(store.getProvisioner(id));

    store.writeProvisioner(provisioner1);
    Assert.assertEquals(provisioner1, store.getProvisioner(id));

    store.deleteProvisioner(id);
    Assert.assertNull(store.getProvisioner(id));
  }

  @Test
  public void testWriteWithTenantUpdate() throws IOException {
    ProvisionerStore store = getProvisionerStore();
    store.writeProvisioner(provisioner1);
    Assert.assertEquals(provisioner1, store.getProvisioner(provisioner1.getId()));

    Provisioner updatedProvisioner1 = new Provisioner(
      provisioner1.getId(), provisioner1.getHost(), provisioner1.getPort(), 100,
      ImmutableMap.<String, Integer>of("tenantA", 50, "tenantY", 5, "tenantZ", 0),
      ImmutableMap.<String, Integer>of("tenantA", 50, "tenantY", 5, "tenantZ", 1)
    );

    store.writeProvisioner(updatedProvisioner1);
    Assert.assertEquals(updatedProvisioner1, store.getProvisioner(provisioner1.getId()));
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantX"));
    Assert.assertEquals(44, store.getFreeCapacity());
    for (Provisioner provisioner : store.getTenantProvisioners("tenantX")) {
      Assert.assertFalse(provisioner.getId().equals(provisioner1.getId()));
    }
  }

  @Test
  public void testGetAllProvisioners() throws IOException {
    ProvisionerStore store = getProvisionerStore();
    Assert.assertTrue(store.getAllProvisioners().isEmpty());

    store.writeProvisioner(provisioner1);
    Assert.assertEquals(ImmutableSet.of(provisioner1), ImmutableSet.copyOf(store.getAllProvisioners()));

    store.writeProvisioner(provisioner2);
    Assert.assertEquals(ImmutableSet.of(provisioner1, provisioner2), ImmutableSet.copyOf(store.getAllProvisioners()));
  }

  @Test
  public void testGetIdleProvisioners() throws IOException {
    ProvisionerStore store = getProvisionerStore();

    store.writeProvisioner(provisioner1);
    store.writeProvisioner(provisioner2);
    store.setHeartbeat(provisioner1.getId(), 100L);
    store.setHeartbeat(provisioner2.getId(), 1000L);

    Assert.assertTrue(store.getTimedOutProvisioners(99L).isEmpty());
    Assert.assertEquals(ImmutableSet.of(provisioner1), ImmutableSet.copyOf(store.getTimedOutProvisioners(101L)));
    Assert.assertEquals(ImmutableSet.of(provisioner1, provisioner2),
                        ImmutableSet.copyOf(store.getTimedOutProvisioners(1001L)));
  }

  @Test
  public void testGetTenantProvisioners() throws IOException {
    ProvisionerStore store = getProvisionerStore();

    store.writeProvisioner(provisioner1);
    store.writeProvisioner(provisioner2);

    Assert.assertTrue(store.getTenantProvisioners("tenantB").isEmpty());
    Assert.assertEquals(ImmutableSet.of(provisioner2), ImmutableSet.copyOf(store.getTenantProvisioners("tenantA")));
    Assert.assertEquals(ImmutableSet.of(provisioner1), ImmutableSet.copyOf(store.getTenantProvisioners("tenantX")));
    Assert.assertEquals(ImmutableSet.of(provisioner2), ImmutableSet.copyOf(store.getTenantProvisioners("tenantZ")));
    Assert.assertEquals(ImmutableSet.of(provisioner1, provisioner2),
                        ImmutableSet.copyOf(store.getTenantProvisioners("tenantY")));
  }

  @Test
  public void testGetProvisionersWithFreeCapacity() throws IOException {
    ProvisionerStore store = getProvisionerStore();

    store.writeProvisioner(provisioner1);
    Assert.assertTrue(store.getProvisionersWithFreeCapacity().isEmpty());

    store.writeProvisioner(provisioner2);
    Assert.assertEquals(ImmutableSet.of(provisioner2), ImmutableSet.copyOf(store.getProvisionersWithFreeCapacity()));

    Provisioner updatedProvisioner1 = new Provisioner(
      provisioner1.getId(), provisioner1.getHost(), provisioner1.getPort(), provisioner1.getCapacityTotal(),
      ImmutableMap.<String, Integer>of("tenantX", 5),
      ImmutableMap.<String, Integer>of("tenantX", 5));

    store.writeProvisioner(updatedProvisioner1);
    Assert.assertEquals(ImmutableSet.of(updatedProvisioner1, provisioner2),
                        ImmutableSet.copyOf(store.getProvisionersWithFreeCapacity()));
  }

  @Test
  public void testGetFreeCapacity() throws IOException {
    ProvisionerStore store = getProvisionerStore();
    Assert.assertEquals(0, store.getFreeCapacity());

    store.writeProvisioner(provisioner1);
    store.writeProvisioner(provisioner2);
    Assert.assertEquals(88, store.getFreeCapacity());

    // add more workers
    Provisioner updatedProvisioner2 = new Provisioner(
      provisioner2.getId(), provisioner2.getHost(), provisioner2.getPort(), provisioner2.getCapacityTotal(),
      ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 3, "tenantZ", 2),
      ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 50, "tenantZ", 20));
    store.writeProvisioner(updatedProvisioner2);
    Assert.assertEquals(25, store.getFreeCapacity());

    // remove some workers
    updatedProvisioner2 = new Provisioner(
      provisioner2.getId(), provisioner2.getHost(), provisioner2.getPort(), provisioner2.getCapacityTotal(),
      ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 3, "tenantZ", 2),
      ImmutableMap.<String, Integer>of("tenantA", 5, "tenantZ", 20));
    store.writeProvisioner(updatedProvisioner2);
    Assert.assertEquals(75, store.getFreeCapacity());

    // delete a provisioner
    store.deleteProvisioner(provisioner2.getId());
    Assert.assertEquals(0, store.getFreeCapacity());

    // add a provisioner
    Provisioner provisioner3 = new Provisioner(
      "p3", "host3", 12345, 50,
      ImmutableMap.<String, Integer>of("tenantB", 10),
      ImmutableMap.<String, Integer>of("tenantB", 40));
    store.writeProvisioner(provisioner3);
    Assert.assertEquals(10, store.getFreeCapacity());
  }

  @Test
  public void testGetNumAssignedWorkers() throws IOException {
    ProvisionerStore store = getProvisionerStore();
    // should all start at 0
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantA"));
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantX"));
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantY"));
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantZ"));

    // add some provisioners
    store.writeProvisioner(provisioner1);
    store.writeProvisioner(provisioner2);
    // A is just in p1, X is just in p2, Y is in both, and Z is just in p2
    Assert.assertEquals(5, store.getNumAssignedWorkers("tenantA"));
    Assert.assertEquals(5, store.getNumAssignedWorkers("tenantX"));
    Assert.assertEquals(10, store.getNumAssignedWorkers("tenantY"));
    Assert.assertEquals(2, store.getNumAssignedWorkers("tenantZ"));

    // now try updating existing ones and adding a new one
    Provisioner provisioner3 = new Provisioner(
      "p3", "host3", 12345, 50,
      ImmutableMap.<String, Integer>of("tenantX", 10),
      ImmutableMap.<String, Integer>of("tenantX", 10, "tenantB", 40));
    Provisioner updatedProvisioner2 = new Provisioner(
      provisioner2.getId(), provisioner2.getHost(), provisioner2.getPort(), provisioner2.getCapacityTotal(),
      ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 3, "tenantZ", 2),
      ImmutableMap.<String, Integer>of("tenantA", 5, "tenantY", 50, "tenantZ", 20));
    store.writeProvisioner(provisioner3);
    store.writeProvisioner(updatedProvisioner2);

    Assert.assertEquals(5, store.getNumAssignedWorkers("tenantA"));
    Assert.assertEquals(40, store.getNumAssignedWorkers("tenantB"));
    Assert.assertEquals(15, store.getNumAssignedWorkers("tenantX"));
    Assert.assertEquals(55, store.getNumAssignedWorkers("tenantY"));
    Assert.assertEquals(20, store.getNumAssignedWorkers("tenantZ"));

    // now try deleting one
    store.deleteProvisioner(provisioner2.getId());
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantA"));
    Assert.assertEquals(40, store.getNumAssignedWorkers("tenantB"));
    Assert.assertEquals(15, store.getNumAssignedWorkers("tenantX"));
    Assert.assertEquals(5, store.getNumAssignedWorkers("tenantY"));
    Assert.assertEquals(0, store.getNumAssignedWorkers("tenantZ"));
  }

  @Test
  public void testWriteProvisionerIsTransactional() throws InterruptedException, IOException {
    final ProvisionerStore store = getProvisionerStore();
    final int writesPerThread = 10;
    String provisionerId = "p1";
    Provisioner pState1 = new Provisioner(
      provisionerId, "host1", 12345, 100,
      ImmutableMap.<String, Integer>of("t1-1", 1, "t1-2", 1, "t1-3", 1),
      ImmutableMap.<String, Integer>of("t1-1", 1, "t1-2", 1, "t1-3", 1)
    );

    Provisioner pState2 = new Provisioner(
      provisionerId, "host2", 12345, 100,
      ImmutableMap.<String, Integer>of("t2-1", 2, "t2-2", 2, "t2-3", 2),
      ImmutableMap.<String, Integer>of("t2-1", 2, "t2-2", 2, "t2-3", 2)
    );

    Provisioner pState3 = new Provisioner(
      provisionerId, "host3", 12345, 100,
      ImmutableMap.<String, Integer>of("t3-1", 3, "t3-2", 3, "t3-3", 3),
      ImmutableMap.<String, Integer>of("t3-1", 3, "t3-2", 3, "t3-3", 3)
    );
    List<Provisioner> provisionerStates = Lists.newArrayList(pState1, pState2, pState3);
    int numThreads = provisionerStates.size();
    final CyclicBarrier barrier = new CyclicBarrier(numThreads);
    final CountDownLatch latch = new CountDownLatch(numThreads);

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    for (final Provisioner provisioner : provisionerStates) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
          } catch (Exception e) {
            Throwables.propagate(e);
          }
          for (int j = 0; j < writesPerThread; j++) {
            try {
              store.writeProvisioner(provisioner);
            } catch (IOException e) {
              // some conflicts expected
            }
          }
          latch.countDown();
        }
      });
    }

    latch.await();

    // One of them should have won.
    Provisioner provisioner = store.getProvisioner(provisionerId);
    Assert.assertTrue(provisioner.equals(pState1) || provisioner.equals(pState2) || provisioner.equals(pState3));
    if (provisioner.equals(pState1)) {
      Assert.assertEquals(97, store.getFreeCapacity());
      for (String tenant : pState1.getAssignedTenants()) {
        Collection<Provisioner> tenantProvisioners = store.getTenantProvisioners(tenant);
        Assert.assertEquals(1, tenantProvisioners.size());
        Assert.assertEquals(provisioner, tenantProvisioners.iterator().next());
        Assert.assertEquals(1, store.getNumAssignedWorkers(tenant));
      }
    } else if (provisioner.equals(pState2)) {
      Assert.assertEquals(94, store.getFreeCapacity());
      for (String tenant : pState2.getAssignedTenants()) {
        Collection<Provisioner> tenantProvisioners = store.getTenantProvisioners(tenant);
        Assert.assertEquals(1, tenantProvisioners.size());
        Assert.assertEquals(provisioner, tenantProvisioners.iterator().next());
        Assert.assertEquals(2, store.getNumAssignedWorkers(tenant));
      }
    } else if (provisioner.equals(pState3)) {
      Assert.assertEquals(91, store.getFreeCapacity());
      for (String tenant : pState3.getAssignedTenants()) {
        Collection<Provisioner> tenantProvisioners = store.getTenantProvisioners(tenant);
        Assert.assertEquals(1, tenantProvisioners.size());
        Assert.assertEquals(provisioner, tenantProvisioners.iterator().next());
        Assert.assertEquals(3, store.getNumAssignedWorkers(tenant));
      }
    }
  }
}
