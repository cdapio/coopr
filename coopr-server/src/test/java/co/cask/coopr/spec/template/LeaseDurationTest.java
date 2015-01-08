package co.cask.coopr.spec.template;

import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.layout.InvalidClusterException;
import com.google.gson.Gson;
import com.google.inject.Guice;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LeaseDurationTest {

  private static final Gson GSON = Guice.createInjector(new CodecModules().getModule()).getInstance(Gson.class);

  @Test
  public void testLeaseDuration() {
    LeaseDuration leaseDuration = new LeaseDuration("7d", "min", "min");
    Assert.assertEquals(604800000, leaseDuration.getInitial());
    Assert.assertEquals(0, leaseDuration.getStep());
    leaseDuration = new LeaseDuration("3s", "min", "min");
    Assert.assertEquals(3000, leaseDuration.getInitial());
    leaseDuration = new LeaseDuration("5m", "min", "min");
    Assert.assertEquals(300000, leaseDuration.getInitial());
    leaseDuration = new LeaseDuration("10h", "min", "min");
    Assert.assertEquals(36000000, leaseDuration.getInitial());
    leaseDuration = new LeaseDuration("max", "min", "min");
    Assert.assertEquals(Long.MAX_VALUE, leaseDuration.getInitial());
  }

  @Test
  public void testLeaseDurationJsonRelativeTime() {
    LeaseDuration leaseDuration = new LeaseDuration("7d", "min", "min");
    Assert.assertEquals(leaseDuration,
                        GSON.fromJson("{\"initial\":\"7d\",\"max\":\"min\",\"step\":\"min\"}", LeaseDuration.class));
  }

  @Test
  public void testLeaseDurationJsonAbsoluteTime() {
    LeaseDuration leaseDuration = new LeaseDuration(123, 0, 1);
    Assert.assertEquals(leaseDuration, GSON.fromJson("{\"initial\":123,\"max\":0,\"step\":1}", LeaseDuration.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeArgStringConstructor() {
    new LeaseDuration("-15s", "min", "min");
  }

  @Test(expected = RuntimeException.class)
  public void testUnsupportedArgStringConstructor() {
    new LeaseDuration("1v", "min", "min");
  }

  @Test(expected = RuntimeException.class)
  public void testInvalidArgStringConstructor() {
    new LeaseDuration("invalidNumbers", "min", "min");
  }

  @Test
  public void testCalcInitialLease() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration("15m", "min", "min");
    Assert.assertEquals(2000, leaseDuration.calcInitialLease("2s"));
  }

  @Test
  public void testInitialLeaseZeroIsInfinite() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration("0", "0", "0");
    Assert.assertEquals(Long.MAX_VALUE, leaseDuration.calcInitialLease(Long.MAX_VALUE));
  }

  @Test
  public void testCalcInitialLeaseNegativeOne() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration("5m", "0", "0");
    Assert.assertEquals(300000, leaseDuration.calcInitialLease(-1));
  }

  @Test(expected = InvalidClusterException.class)
  public void testInvalidNegativeRequestedLease() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration("0", "0", "0");
    leaseDuration.calcInitialLease(-2);
  }

  @Test(expected = InvalidClusterException.class)
  public void testRequestedLeaseTooBig() throws InvalidClusterException {
    LeaseDuration leaseDuration = new LeaseDuration("5m", "0", "0");
    leaseDuration.calcInitialLease("6m");
  }
}
