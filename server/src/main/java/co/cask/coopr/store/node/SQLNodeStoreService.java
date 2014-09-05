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
package co.cask.coopr.store.node;

import co.cask.coopr.account.Account;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service for getting views of the node store. Getting a view is a lightweight operation as no data is preloaded.
 */
public class SQLNodeStoreService extends AbstractIdleService implements NodeStoreService {
  private static final Logger LOG = LoggerFactory.getLogger(SQLNodeStoreService.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final NodeStore nodeStore;

  @Inject
  public SQLNodeStoreService(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.nodeStore = new SQLNodeStore(dbConnectionPool, dbQueryExecutor);
  }

  @Override
  public NodeStoreView getView(final Account account) {
    if (account.isAdmin()) {
      return new SQLAdminNodeStoreView(dbConnectionPool, account, dbQueryExecutor);
    } else {
      return new SQLUserNodeStoreView(dbConnectionPool, account, dbQueryExecutor);
    }
  }

  @Override
  public NodeStore getSystemView() {
    return nodeStore;
  }

  // for unit tests only
  protected void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM nodes");
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
      LOG.warn("Initializing Derby DB... Tables are not optimized for performance.");

      boolean created = DBHelper.createDerbyTableIfNotExists("CREATE TABLE nodes ( " +
                                                             "cluster_id BIGINT, " +
                                                             "id VARCHAR(64), " +
                                                             "node BLOB, " +
                                                             "PRIMARY KEY (id) )", dbConnectionPool
                                                            );
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool, "nodes_cluster_index", "nodes", "cluster_id", "id");
      }
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }
}
