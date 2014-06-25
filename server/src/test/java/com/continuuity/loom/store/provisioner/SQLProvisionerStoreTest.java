package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.provisioner.Provisioner;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.entity.SQLEntityStoreService;
import com.continuuity.loom.store.guice.StoreModule;
import com.continuuity.loom.store.tenant.SQLTenantStore;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
      new StoreModule()
    );
    JsonSerde codec = new JsonSerde();
    DBConnectionPool dbConnectionPool = injector.getInstance(DBConnectionPool.class);
    store = new SQLProvisionerStore(dbConnectionPool, codec);
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
