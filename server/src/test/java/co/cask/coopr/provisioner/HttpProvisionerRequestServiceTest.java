package co.cask.coopr.provisioner;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.guice.ConfigurationModule;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class HttpProvisionerRequestServiceTest {
  private static ProvisionerRequestService provisionerRequestService;
  private static StubProvisioner stubProvisioner;
  private static int port;
  private static String host;

  @BeforeClass
  public static void setupTestClass() {
    Configuration conf = Configuration.create();
    Injector injector = Guice.createInjector(
      new ConfigurationModule(conf)
    );
    provisionerRequestService = injector.getInstance(HttpProvisionerRequestService.class);
    stubProvisioner = new StubProvisioner();
    stubProvisioner.startAndWait();
    port = stubProvisioner.getBindAddress().getPort();
    host = stubProvisioner.getBindAddress().getHostName();
  }

  @AfterClass
  public static void cleanupTestClass() {
    stubProvisioner.stopAndWait();
  }

  @Before
  public void setupTest() {
    stubProvisioner.reset();
  }

  @Test
  public void testPutTenant() {
    Provisioner provisioner = new Provisioner("id", host, port, 100, null, null);
    for (int i = 0; i < 10; i++) {
      provisionerRequestService.putTenantWorkers(provisioner, "tenant" + i % 5);
      provisionerRequestService.putTenant(provisioner, "tenant" + i % 5, new ResourceCollection());
      provisionerRequestService.putTenantResources(provisioner, "tenant" + i % 5, new ResourceCollection());
    }
    for (int i = 0; i < 5; i++) {
      Assert.assertEquals(2, stubProvisioner.getPutCount("tenant" + i));
      Assert.assertEquals(2, stubProvisioner.getPutResourcesCount("tenant" + i));
      Assert.assertEquals(2, stubProvisioner.getPutWorkersCount("tenant" + i));
    }
  }

  @Test
  public void testDeleteTenant() {
    Provisioner provisioner = new Provisioner("id", host, port, 100, null, null);
    for (int i = 0; i < 10; i++) {
      provisionerRequestService.deleteTenant(provisioner, "tenant" + i % 5);
    }
    for (int i = 0; i < 5; i++) {
      Assert.assertEquals(2, stubProvisioner.getDeleteCount("tenant" + i));
    }
  }
}
