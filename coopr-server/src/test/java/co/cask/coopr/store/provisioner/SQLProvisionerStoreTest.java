package co.cask.coopr.store.provisioner;

import co.cask.coopr.BaseTest;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.guice.ConfigurationModule;
import co.cask.coopr.store.guice.TestStoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;

/**
 *
 */
public class SQLProvisionerStoreTest extends ProvisionerStoreTest {
  private static SQLProvisionerStore store;

  @BeforeClass
  public static void setupTestClass() throws Exception {
    Configuration sqlConf = BaseTest.createTestConf();
    Injector injector = Guice.createInjector(
      new ConfigurationModule(sqlConf),
      new TestStoreModule(),
      new CodecModules().getModule()
    );
    store = injector.getInstance(SQLProvisionerStore.class);
    store.startAndWait();
  }

  @Override
  ProvisionerStore getProvisionerStore() {
    return store;
  }

  @Override
  void clearData() throws Exception {
    store.clearData();
  }
}
