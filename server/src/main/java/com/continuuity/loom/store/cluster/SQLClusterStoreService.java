package com.continuuity.loom.store.cluster;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service for getting views of the cluster store. Getting a view is a lightweight operation as no data is preloaded.
 */
public class SQLClusterStoreService extends AbstractIdleService implements ClusterStoreService {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLClusterStoreService.class);
  private final DBConnectionPool dbConnectionPool;
  private final ClusterStore clusterStore;

  @Inject
  public SQLClusterStoreService(DBConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
    this.clusterStore = new SQLClusterStore(dbConnectionPool);
  }

  // for unit tests only
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM clusters");
        stmt = conn.createStatement();
        stmt.execute("DELETE FROM jobs");
        stmt = conn.createStatement();
        stmt.execute("DELETE FROM tasks");
        stmt = conn.createStatement();
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

      DBQueryHelper.createDerbyTable("CREATE TABLE clusters ( " +
                                       "id BIGINT, " +
                                       "owner_id VARCHAR(255), " +
                                       "tenant_id VARCHAR(255), " +
                                       "name VARCHAR(255), " +
                                       "create_time TIMESTAMP, " +
                                       "expire_time TIMESTAMP, " +
                                       "status VARCHAR(32), " +
                                       "cluster BLOB )",
                                     dbConnectionPool);
      DBQueryHelper.createDerbyTable("CREATE TABLE jobs ( cluster_id BIGINT, job_num BIGINT, status VARCHAR(32)," +
                                       " create_time TIMESTAMP, job BLOB)",
                                     dbConnectionPool);
      DBQueryHelper.createDerbyTable("CREATE TABLE tasks ( cluster_id BIGINT, job_num BIGINT, task_num BIGINT," +
                                       "submit_time TIMESTAMP, status_time TIMESTAMP, status VARCHAR(32), task BLOB )",
                                     dbConnectionPool);
      DBQueryHelper.createDerbyTable("CREATE TABLE nodes ( cluster_id BIGINT, id VARCHAR(64), node BLOB )",
                                     dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public ClusterStoreView getView(Account account) {
    if (account.isAdmin()) {
      return new SQLAdminClusterStoreView(dbConnectionPool, account);
    } else {
      return new SQLUserClusterStoreView(dbConnectionPool, account);
    }
  }

  @Override
  public ClusterStore getSystemView() {
    return clusterStore;
  }
}
