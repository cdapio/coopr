package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public class SQLPluginResourceMetaStoreService extends AbstractIdleService implements PluginResourceMetaStoreService {
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  @Inject
  private SQLPluginResourceMetaStoreService(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
  }

  // for unit tests
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.executeUpdate("DELETE FROM pluginMeta");
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBHelper.createDerbyTableIfNotExists("CREATE TABLE pluginMeta (" +
                                             "tenant_id VARCHAR(255), " +
                                             "plugin_type VARCHAR(255), " +
                                             "plugin_name VARCHAR(255), " +
                                             "resource_type VARCHAR(255), " +
                                             "name VARCHAR(255), " +
                                             "version VARCHAR(255), " +
                                             "active BOOLEAN )",
                                           dbConnectionPool);
      DBHelper.createDerbyIndex(dbConnectionPool, "plugin_meta_index", "pluginMeta",
                                "tenant_id", "plugin_type", "plugin_name", "resource_type", "name", "version");
      DBHelper.createDerbyIndex(dbConnectionPool, "plugin_meta_active_index", "pluginMeta", "active");
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public PluginResourceMetaStoreView getView(Account account, PluginResourceType type) {
    Preconditions.checkArgument(account.isAdmin(), "Only an admin is allowed to access plugin information.");
    return new SQLPluginResourceMetaStoreView(dbConnectionPool, dbQueryExecutor, account, type);
  }
}
