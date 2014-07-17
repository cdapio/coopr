package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.store.guice.StoreModules;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 *
 */
public class LocalFilePluginStoreTest extends PluginStoreTest {
  private static LocalFilePluginStore store;
  private static Configuration conf;
  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupMemoryPluginStoreTest() throws Exception {
    conf = Configuration.create();
    Injector injector = Guice.createInjector(
      new StoreModules(conf).getTestModule()
    );
    store = injector.getInstance(LocalFilePluginStore.class);
  }

  @Override
  PluginStore getInitializedStore() throws IOException {
    conf.set(Constants.LocalFilePluginStore.DATA_DIR, tmpFolder.newFolder().getAbsolutePath());
    store.initialize(conf);
    return store;
  }

  @Override
  void clearData() {
    // tmp folder will get deleted itself.
  }
}
