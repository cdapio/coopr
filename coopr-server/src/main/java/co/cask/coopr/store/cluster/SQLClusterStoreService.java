package co.cask.coopr.store.cluster;

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
 * Service for getting views of the cluster store. Getting a view is a lightweight operation as no data is preloaded.
 */
public class SQLClusterStoreService extends AbstractIdleService implements ClusterStoreService {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLClusterStoreService.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final ClusterStore clusterStore;

  @Inject
  public SQLClusterStoreService(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.clusterStore = new SQLClusterStore(dbConnectionPool, dbQueryExecutor);
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

      boolean created = DBHelper.createDerbyTableIfNotExists("CREATE TABLE clusters ( " +
                                                               "id BIGINT, " +
                                                               "owner_id VARCHAR(255), " +
                                                               "tenant_id VARCHAR(255), " +
                                                               "name VARCHAR(255), " +
                                                               "create_time TIMESTAMP, " +
                                                               "expire_time TIMESTAMP, " +
                                                               "status VARCHAR(32), " +
                                                               "latest_job_num BIGINT, " +
                                                               "cluster BLOB, " +
                                                               "PRIMARY KEY (id) )",
                                                             dbConnectionPool);
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool,
                                  "clusters_account_index", "clusters", "tenant_id", "owner_id", "id");
        DBHelper.createDerbyIndex(dbConnectionPool, "clusters_ctime_index", "clusters", "create_time");
        DBHelper.createDerbyIndex(dbConnectionPool, "clusters_status_index", "clusters", "status");
      }

      created = DBHelper.createDerbyTableIfNotExists("CREATE TABLE jobs ( " +
                                                       "cluster_id BIGINT, " +
                                                       "job_num BIGINT, " +
                                                       "status VARCHAR(32), " +
                                                       "create_time TIMESTAMP, " +
                                                       "job BLOB, " +
                                                       "PRIMARY KEY (job_num, cluster_id) )",
                                                     dbConnectionPool);
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool, "jobs_ctime_index", "jobs", "create_time");
        DBHelper.createDerbyIndex(dbConnectionPool, "jobs_status_index", "jobs", "status");
      }

      created = DBHelper.createDerbyTableIfNotExists("CREATE TABLE tasks ( " +
                                                       "cluster_id BIGINT, " +
                                                       "job_num BIGINT, " +
                                                       "task_num BIGINT, " +
                                                       "submit_time TIMESTAMP, " +
                                                       "status_time TIMESTAMP, " +
                                                       "status VARCHAR(32), " +
                                                       "type VARCHAR(64), " +
                                                       "cluster_template_name VARCHAR(255), " +
                                                       "user_id VARCHAR(255), " +
                                                       "tenant_id VARCHAR(64), " +
                                                       "task BLOB, " +
                                                       "PRIMARY KEY (task_num, job_num, cluster_id) )",
                                                     dbConnectionPool);
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool, "tasks_status_time_index", "tasks", "status_time");
        DBHelper.createDerbyIndex(dbConnectionPool, "tasks_submit_time_index", "tasks", "submit_time");
        DBHelper.createDerbyIndex(dbConnectionPool, "tasks_status_index", "tasks", "status");
      }

      created = DBHelper.createDerbyTableIfNotExists("CREATE TABLE nodes ( " +
                                                       "cluster_id BIGINT, " +
                                                       "id VARCHAR(64), " +
                                                       "node BLOB, " +
                                                       "PRIMARY KEY (id) )",
                                                     dbConnectionPool);
      if (created) {
        DBHelper.createDerbyIndex(dbConnectionPool, "nodes_cluster_index", "nodes", "cluster_id", "id");
      }
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public ClusterStoreView getView(Account account) {
    if (account.isAdmin()) {
      return new SQLAdminClusterStoreView(dbConnectionPool, account, dbQueryExecutor);
    } else {
      return new SQLUserClusterStoreView(dbConnectionPool, account, dbQueryExecutor);
    }
  }

  @Override
  public ClusterStore getSystemView() {
    return clusterStore;
  }
}
