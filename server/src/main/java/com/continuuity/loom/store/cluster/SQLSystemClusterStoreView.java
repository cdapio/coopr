package com.continuuity.loom.store.cluster;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The cluster store as viewed by the system. The system can do anything to any object.
 */
public class SQLSystemClusterStoreView extends BaseSQLClusterStoreView {

  public SQLSystemClusterStoreView(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    super(dbConnectionPool, dbQueryExecutor);
  }

  @Override
  protected PreparedStatement getSelectAllClustersStatement(Connection conn) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters ORDER BY create_time DESC");
    return statement;
  }

  @Override
  protected PreparedStatement getSelectNonTerminatedClusters(Connection conn)
    throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters WHERE status<>? ORDER BY create_time DESC");
    statement.setString(1, Cluster.Status.TERMINATED.name());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT cluster FROM clusters WHERE id=?");
    statement.setLong(1, id);
    return statement;
  }

  @Override
  boolean allowedToWrite(Cluster cluster) {
    return true;
  }

  @Override
  protected PreparedStatement getSetClusterStatement(
    Connection conn, long id, Cluster cluster, ByteArrayInputStream clusterBytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE clusters SET cluster=?, owner_id=?, tenant_id=?, status=?, expire_time=? WHERE id=?");
    statement.setBlob(1, clusterBytes);
    statement.setString(2, cluster.getAccount().getUserId());
    statement.setString(3, cluster.getAccount().getTenantId());
    statement.setString(4, cluster.getStatus().name());
    statement.setTimestamp(5, DBHelper.getTimestamp(cluster.getExpireTime()));
    // where clause
    statement.setLong(6, id);
    return statement;
  }

  @Override
  protected PreparedStatement getClusterExistsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT id FROM clusters WHERE id=?");
    statement.setLong(1, id);
    return statement;
  }

  @Override
  protected PreparedStatement getDeleteClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("DELETE FROM clusters WHERE id=?");
    statement.setLong(1, id);
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterJobsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement =
      conn.prepareStatement("SELECT job FROM jobs WHERE cluster_id=? ORDER BY job_num DESC");
    statement.setLong(1, id);
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterNodesStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT node FROM nodes WHERE cluster_id=?");
    statement.setLong(1, id);
    return statement;
  }
}
