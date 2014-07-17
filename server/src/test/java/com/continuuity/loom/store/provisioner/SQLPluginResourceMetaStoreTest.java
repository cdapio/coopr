package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.store.guice.StoreModules;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;

/**
 *
 */
public class SQLPluginResourceMetaStoreTest extends PluginResourceMetaStoreTest {
  private static SQLPluginResourceMetaStoreService service;

  @BeforeClass
  public static void setupTestClass() throws Exception {
    Configuration conf = Configuration.create();
    conf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    conf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    Injector injector = Guice.createInjector(
      new ConfigurationModule(conf),
      new StoreModules(conf).getTestModule()
    );
    service = injector.getInstance(SQLPluginResourceMetaStoreService.class);
    service.startAndWait();
  }

  @Override
  PluginResourceMetaStoreService getPluginResourceMetaStoreService() throws Exception {
    return service;
  }

  @Override
  void clearData() throws Exception {
    service.clearData();
  }
}
