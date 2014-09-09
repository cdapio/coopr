package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.BaseTest;
import com.continuuity.loom.codec.json.guice.CodecModules;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.store.guice.TestStoreModule;
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
