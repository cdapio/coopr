package co.cask.coopr.store.cluster;

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.base.Preconditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * The cluster store as viewed by a tenant admin. A tenant admin can read, write, and delete any cluster
 * within the tenant.
 */
public class SQLAdminClusterStoreView extends BaseSQLClusterStoreView {
  private final String tenantId;

  public SQLAdminClusterStoreView(DBConnectionPool dbConnectionPool,
                                  Account account, DBQueryExecutor dbQueryExecutor) {
    super(dbConnectionPool, dbQueryExecutor);
    Preconditions.checkArgument(account.isAdmin(), "Cannot create admin view with a non-admin user.");
    this.tenantId = account.getTenantId();
  }

  @Override
  protected PreparedStatement getSelectAllClustersStatement(Connection conn) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters WHERE tenant_id=? ORDER BY create_time DESC");
    statement.setString(1, tenantId);
    return statement;
  }

  @Override
  protected PreparedStatement getSelectNonTerminatedClusters(Connection conn)
    throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters WHERE tenant_id=? AND status<>? ORDER BY create_time DESC");
    statement.setString(1, tenantId);
    statement.setString(2, Cluster.Status.TERMINATED.name());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT cluster FROM clusters WHERE id=? AND tenant_id=?");
    statement.setLong(1, id);
    statement.setString(2, tenantId);
    return statement;
  }

  @Override
  boolean allowedToWrite(Cluster cluster) {
    return this.tenantId.equals(cluster.getAccount().getTenantId());
  }

  @Override
  protected PreparedStatement getSetClusterStatement(
    Connection conn, long id, Cluster cluster, byte[] clusterBytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE clusters SET cluster=?, owner_id=?, tenant_id=?, status=?, expire_time=? WHERE id=? AND tenant_id=?");
    statement.setBytes(1, clusterBytes);
    statement.setString(2, cluster.getAccount().getUserId());
    statement.setString(3, cluster.getAccount().getTenantId());
    statement.setString(4, cluster.getStatus().name());
    statement.setTimestamp(5, DBHelper.getTimestamp(cluster.getExpireTime()));
    // where clause
    statement.setLong(6, id);
    statement.setString(7, tenantId);
    return statement;
  }

  @Override
  protected PreparedStatement getClusterExistsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT id FROM clusters WHERE id=? AND tenant_id=?");
    statement.setLong(1, id);
    statement.setString(2, tenantId);
    return statement;
  }

  @Override
  protected PreparedStatement getDeleteClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("DELETE FROM clusters WHERE id=? AND tenant_id=?");
    statement.setLong(1, id);
    statement.setString(2, tenantId);
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterJobsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT J.job FROM jobs J, clusters C " +
        "WHERE C.id=? AND C.tenant_id=? and J.cluster_id=C.id ORDER BY job_num DESC");
    statement.setLong(1, id);
    statement.setString(2, tenantId);
    return statement;
  }

  @Override
  PreparedStatement getSelectAllClusterJobsStatement(Connection conn) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT C.cluster, J.job FROM clusters C, jobs J WHERE " +
        "C.latest_job_num=J.job_num AND C.id=J.cluster_id AND C.tenant_id=? ORDER BY C.create_time DESC");
    statement.setString(1, tenantId);
    return statement;
  }

  @Override
  PreparedStatement getSelectAllClusterJobsStatement(Connection conn, Set<Cluster.Status> states) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT C.cluster, J.job FROM clusters C, jobs J WHERE C.latest_job_num=J.job_num AND C.id=J.cluster_id " +
        "AND C.tenant_id=? AND C.status IN " + DBHelper.createInString(states.size()) + " ORDER BY C.create_time DESC");
    statement.setString(1, tenantId);
    setInClause(statement, states, 2);
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterNodesStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT N.node FROM nodes N, clusters C WHERE C.id=? AND C.tenant_id=? AND N.cluster_id=C.id");
    statement.setLong(1, id);
    statement.setString(2, tenantId);
    return statement;
  }
}
