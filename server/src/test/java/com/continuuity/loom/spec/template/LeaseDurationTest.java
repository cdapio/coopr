package com.continuuity.loom.spec.template;

import com.continuuity.loom.layout.InvalidClusterException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LeaseDurationTest {

  @Test
  public void testCalcInitialLease() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration(300, 0, 0);
    Assert.assertEquals(200, leaseDuration.calcInitialLease(200));
  }

  @Test
  public void testInitialLeaseZeroIsInfinite() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration(0, 0, 0);
    Assert.assertEquals(Long.MAX_VALUE, leaseDuration.calcInitialLease(Long.MAX_VALUE));
  }

  @Test
  public void testCalcInitialLeaseNegativeOne() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration(300, 0, 0);
    Assert.assertEquals(300, leaseDuration.calcInitialLease(-1));
  }

  @Test(expected = InvalidClusterException.class)
  public void testInvalidNegativeRequestedLease() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration(0, 0, 0);
    leaseDuration.calcInitialLease(-2);
  }

  @Test(expected = InvalidClusterException.class)
  public void testRequestedLeaseTooBig() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration(300, 0, 0);
    leaseDuration.calcInitialLease(301);
  }
}
