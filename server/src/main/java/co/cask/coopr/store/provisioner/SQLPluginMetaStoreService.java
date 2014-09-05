/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.store.provisioner;

import co.cask.coopr.account.Account;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL database backed implementation of {@link PluginMetaStoreService}.
 */
public class SQLPluginMetaStoreService extends AbstractIdleService implements PluginMetaStoreService {
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  @Inject
  private SQLPluginMetaStoreService(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
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
      boolean created = DBHelper.createDerbyTableIfNotExists(
        "CREATE TABLE pluginMeta (" +
          "tenant_id VARCHAR(255), " +
          "plugin_type VARCHAR(16), " +
          "plugin_name VARCHAR(255), " +
          "resource_type VARCHAR(255), " +
          "name VARCHAR(255), " +
          "version INTEGER, " +
          "live BOOLEAN, " +
          "slated BOOLEAN, " +
          "deleted BOOLEAN, " +
          "create_time TIMESTAMP," +
          "delete_time TIMESTAMP," +
          "PRIMARY KEY(tenant_id, plugin_type, plugin_name, resource_type, name, version) )",
        dbConnectionPool);
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool, "plugin_meta_index", "pluginMeta",
                                  "tenant_id", "plugin_type", "plugin_name", "resource_type", "name", "version");
      }
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public PluginMetaStoreView getAccountView(Account account) {
    Preconditions.checkArgument(account.isAdmin(), "Only an admin is allowed to access plugin information.");
    return new SQLPluginMetaStoreView(dbConnectionPool, dbQueryExecutor, account);
  }

  @Override
  public PluginResourceTypeView getResourceTypeView(Account account, ResourceType type) {
    return getAccountView(account).getResourceTypeView(type);
  }
}
