package co.cask.coopr.provisioner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class ProvisionerTest {

  @Test
  public void testRemoveTenantAssignments() {
    Provisioner provisioner = new Provisioner(
      "id", "host", 12345, 100,
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 20),
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 25)
    );
    Assert.assertEquals(60, provisioner.getCapacityFree());
    Assert.assertEquals(ImmutableSet.of("t1", "t2", "t3"), ImmutableSet.copyOf(provisioner.getAssignedTenants()));

    provisioner.removeTenantAssignments("t1");
    Assert.assertEquals(ImmutableSet.of("t2", "t3"), ImmutableSet.copyOf(provisioner.getAssignedTenants()));
    Assert.assertEquals(65, provisioner.getCapacityFree());

    provisioner.removeTenantAssignments("t2");
    Assert.assertEquals(ImmutableSet.of("t3"), ImmutableSet.copyOf(provisioner.getAssignedTenants()));
    Assert.assertEquals(75, provisioner.getCapacityFree());

    provisioner.removeTenantAssignments("t3");
    Assert.assertTrue(provisioner.getAssignedTenants().isEmpty());
    Assert.assertEquals(100, provisioner.getCapacityFree());
  }

  @Test
  public void testGetAssignedWorkersWorksWithNonexistantTenant() {
    Provisioner provisioner = new Provisioner(
      "id", "host", 12345, 100,
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 20),
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 25)
    );
    Assert.assertEquals(0, provisioner.getAssignedWorkers("t4"));
  }

  @Test
  public void testGetLiveWorkersWorksWithNonexistantTenant() {
    Provisioner provisioner = new Provisioner(
      "id", "host", 12345, 100,
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 20),
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 25)
    );
    Assert.assertEquals(0, provisioner.getLiveWorkers("t4"));
  }

  @Test
  public void testTryAddTenantAssignments() {
    Provisioner provisioner = new Provisioner(
      "id", "host", 12345, 100,
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 20),
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 25)
    );

    Assert.assertEquals(10, provisioner.tryAddTenantAssignments("t4", 10));
    Assert.assertEquals(50, provisioner.getCapacityFree());
    Assert.assertEquals(5, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(10, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));
    Assert.assertEquals(10, provisioner.getAssignedWorkers("t4"));

    Assert.assertEquals(30, provisioner.tryAddTenantAssignments("t1", 30));
    Assert.assertEquals(20, provisioner.getCapacityFree());
    Assert.assertEquals(35, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(10, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));
    Assert.assertEquals(10, provisioner.getAssignedWorkers("t4"));

    // can only fit 100 workers, should add 20 of the 30 tried
    Assert.assertEquals(20, provisioner.tryAddTenantAssignments("t2", 30));
    Assert.assertEquals(0, provisioner.getCapacityFree());
    Assert.assertEquals(35, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(30, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));
    Assert.assertEquals(10, provisioner.getAssignedWorkers("t4"));
  }

  @Test
  public void testTryRemoveTenantAssignments() {
    Provisioner provisioner = new Provisioner(
      "id", "host", 12345, 100,
      ImmutableMap.<String, Integer>of("t1", 5, "t2", 10, "t3", 20),
      ImmutableMap.<String, Integer>of("t1", 25, "t2", 50, "t3", 25)
    );
    Assert.assertEquals(0, provisioner.getCapacityFree());
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(50, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));

    // removing a tenant that doesn't exist shouldn't do anything
    Assert.assertEquals(0, provisioner.tryRemoveTenantAssignments("t4", 10));
    Assert.assertEquals(0, provisioner.getCapacityFree());
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(50, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));

    // try removing 5 from tenant1, which should be fine
    Assert.assertEquals(5, provisioner.tryRemoveTenantAssignments("t1", 5));
    Assert.assertEquals(5, provisioner.getCapacityFree());
    Assert.assertEquals(20, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(50, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));

    // try removing 25 from tenant1, which should remove 20 since thats all thats left
    Assert.assertEquals(20, provisioner.tryRemoveTenantAssignments("t1", 25));
    Assert.assertEquals(25, provisioner.getCapacityFree());
    Assert.assertEquals(0, provisioner.getAssignedWorkers("t1"));
    Assert.assertEquals(50, provisioner.getAssignedWorkers("t2"));
    Assert.assertEquals(25, provisioner.getAssignedWorkers("t3"));
  }
}
