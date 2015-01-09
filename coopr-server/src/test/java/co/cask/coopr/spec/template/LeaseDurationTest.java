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
    LeaseDuration leaseDuration = LeaseDuration.of("7d", "0", "0");
    Assert.assertEquals(604800000, leaseDuration.getInitial());
    Assert.assertEquals(0, leaseDuration.getStep());
    leaseDuration = LeaseDuration.of("3s", "0", "0");
    Assert.assertEquals(3000, leaseDuration.getInitial());
    leaseDuration = LeaseDuration.of("5m", "0", "0");
    Assert.assertEquals(300000, leaseDuration.getInitial());
    leaseDuration = LeaseDuration.of("10h", "0", "0");
    Assert.assertEquals(36000000, leaseDuration.getInitial());
    leaseDuration = LeaseDuration.of("0", "0", "0");
    Assert.assertEquals(0, leaseDuration.getInitial());
  }

  @Test
  public void testLeaseDurationJsonRelativeTime() {
    LeaseDuration leaseDuration = LeaseDuration.of("7d", "0", "0");
    Assert.assertEquals(leaseDuration,
                        GSON.fromJson("{\"initial\":\"7d\",\"max\":\"0\",\"step\":\"0\"}", LeaseDuration.class));
  }

  @Test
  public void testLeaseDurationJsonAbsoluteTime() {
    LeaseDuration leaseDuration = LeaseDuration.of(123, 0, 1);
    Assert.assertEquals(leaseDuration, GSON.fromJson("{\"initial\":123,\"max\":0,\"step\":1}", LeaseDuration.class));
  }

  @Test
  public void testLeaseDurationJsonMissingMax() {
    LeaseDuration leaseDuration = LeaseDuration.of(123, 0, 1);
    Assert.assertEquals(leaseDuration, GSON.fromJson("{\"initial\":123,\"step\":1}", LeaseDuration.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeArgStringConstructor() {
    LeaseDuration.of("-15s", "0", "0");
  }

  @Test(expected = RuntimeException.class)
  public void testUnsupportedArgStringConstructor() {
    LeaseDuration.of("1v", "0", "0");
  }

  @Test(expected = RuntimeException.class)
  public void testInvalidArgStringConstructor() {
    LeaseDuration.of("invalidNumbers", "0", "0");
  }

  @Test
  public void testCalcInitialLease() throws InvalidClusterException {
    LeaseDuration leaseDuration = LeaseDuration.of("15m", "0", "0");
    Assert.assertEquals(2000, leaseDuration.calcInitialLease("2s"));
  }

  @Test
  public void testInitialLeaseZeroIsInfinite() throws InvalidClusterException {
    LeaseDuration leaseDuration = LeaseDuration.of("0", "0", "0");
    Assert.assertEquals(Long.MAX_VALUE, leaseDuration.calcInitialLease(Long.MAX_VALUE));
  }

  @Test
  public void testCalcInitialLeaseNegativeOne() throws InvalidClusterException {
    LeaseDuration leaseDuration = LeaseDuration.of("5m", "0", "0");
    Assert.assertEquals(300000, leaseDuration.calcInitialLease(-1));
  }

  @Test(expected = InvalidClusterException.class)
  public void testInvalidNegativeRequestedLease() throws InvalidClusterException {
    LeaseDuration leaseDuration = LeaseDuration.of("0", "0", "0");
    leaseDuration.calcInitialLease(-2);
  }

  @Test(expected = InvalidClusterException.class)
  public void testRequestedLeaseTooBig() throws InvalidClusterException {
    LeaseDuration leaseDuration = LeaseDuration.of("5m", "0", "0");
    leaseDuration.calcInitialLease("6m");
  }
}
