/*
 * Copyright 2012-2014, Continuuity, Inc.
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
 * SQL database backed implementation of {@link PluginResourceMetaStoreService}.
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
      boolean created = DBHelper.createDerbyTableIfNotExists("CREATE TABLE pluginMeta (" +
                                                               "tenant_id VARCHAR(255), " +
                                                               "plugin_type VARCHAR(255), " +
                                                               "plugin_name VARCHAR(255), " +
                                                               "resource_type VARCHAR(255), " +
                                                               "name VARCHAR(255), " +
                                                               "version VARCHAR(255), " +
                                                               "status VARCHAR(32))",
                                                             dbConnectionPool);
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool, "plugin_meta_index", "pluginMeta",
                                  "tenant_id", "plugin_type", "plugin_name", "resource_type", "name", "version");
        DBHelper.createDerbyIndex(dbConnectionPool, "plugin_meta_status_index", "pluginMeta", "status");
      }
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
