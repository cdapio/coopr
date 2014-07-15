package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.codec.json.guice.CodecModules;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.store.guice.StoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.BeforeClass;

import java.sql.SQLException;

/**
 *
 */
public class SQLProvisionerStoreTest extends ProvisionerStoreTest {
  private static SQLProvisionerStore store;

  @BeforeClass
  public static void setupTestClass() throws SQLException {
    Configuration sqlConf = Configuration.create();
    sqlConf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    sqlConf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    Injector injector = Guice.createInjector(
      new ConfigurationModule(sqlConf),
      new StoreModule(),
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
