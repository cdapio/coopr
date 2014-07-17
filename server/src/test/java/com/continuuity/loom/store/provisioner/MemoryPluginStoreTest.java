package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.store.guice.StoreModules;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;

/**
 *
 */
public class MemoryPluginStoreTest extends PluginStoreTest {
  private static MemoryPluginStore store;
  private static Configuration conf;

  @BeforeClass
  public static void setupMemoryPluginStoreTest() throws Exception {
    conf = Configuration.create();
    Injector injector = Guice.createInjector(
      new ConfigurationModule(conf),
      new StoreModules(conf).getTestModule()
    );
    store = injector.getInstance(MemoryPluginStore.class);
  }

  @Override
  PluginStore getInitializedStore() {
    store.initialize(conf);
    return store;
  }

  @Override
  void clearData() {
    store.clearData();
  }
}
