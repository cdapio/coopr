package com.continuuity.loom.store.cluster;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.base.Preconditions;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The cluster store as viewed by a tenant admin. A tenant admin can read, write, and delete any cluster
 * within the tenant.
 */
public class SQLAdminClusterStoreView extends BaseSQLClusterStoreView {
  private final Account account;

  public SQLAdminClusterStoreView(DBConnectionPool dbConnectionPool,
                                  Account account, DBQueryExecutor dbQueryExecutor) {
    super(dbConnectionPool, dbQueryExecutor);
    Preconditions.checkArgument(account.isAdmin(), "Cannot create admin view with a non-admin user.");
    this.account = account;
  }

  @Override
  protected PreparedStatement getSelectAllClustersStatement(Connection conn) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters WHERE tenant_id=? ORDER BY create_time DESC");
    statement.setString(1, account.getTenantId());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT cluster FROM clusters WHERE id=? AND tenant_id=?");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  @Override
  boolean allowedToWrite(Cluster cluster) {
    return this.account.getTenantId().equals(cluster.getAccount().getTenantId());
  }

  @Override
  protected PreparedStatement getSetClusterStatement(
    Connection conn, long id, Cluster cluster, ByteArrayInputStream clusterBytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE clusters SET cluster=?, owner_id=?, tenant_id=?, status=?, expire_time=? WHERE id=? AND tenant_id=?");
    statement.setBlob(1, clusterBytes);
    statement.setString(2, cluster.getAccount().getUserId());
    statement.setString(3, cluster.getAccount().getTenantId());
    statement.setString(4, cluster.getStatus().name());
    statement.setTimestamp(5, DBHelper.getTimestamp(cluster.getExpireTime()));
    // where clause
    statement.setLong(6, id);
    statement.setString(7, account.getTenantId());
    return statement;
  }

  @Override
  protected PreparedStatement getClusterExistsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT id FROM clusters WHERE id=? AND tenant_id=?");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  @Override
  protected PreparedStatement getDeleteClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("DELETE FROM clusters WHERE id=? AND tenant_id=?");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterJobsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT J.job FROM jobs J, clusters C " +
        "WHERE C.id=? AND C.tenant_id=? and J.cluster_id=C.id ORDER BY job_num DESC");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterNodesStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT N.node FROM nodes N, clusters C WHERE C.id=? AND C.tenant_id=? AND N.cluster_id=C.id");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    return statement;
  }
}
