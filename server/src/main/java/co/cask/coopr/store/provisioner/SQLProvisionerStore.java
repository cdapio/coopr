package co.cask.coopr.store.provisioner;

import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBPut;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;

/**
 * Implementation of the {@link ProvisionerStore} using a SQL database for persistent storage.  Maintains
 * two tables, one for storing provisioner information and another for maintaining an index from tenants to
 * provisioners.
 */
public class SQLProvisionerStore extends AbstractIdleService implements ProvisionerStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLProvisionerStore.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  @Inject
  private SQLProvisionerStore(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
  }

  // for unit tests only
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM provisioners");
      } finally {
        stmt.close();
      }
      stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM provisionerWorkers");
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
      DBHelper.createDerbyTableIfNotExists("CREATE TABLE provisioners (" +
                                             "id VARCHAR(255), " +
                                             "last_heartbeat TIMESTAMP, " +
                                             "capacity_total INTEGER, " +
                                             "capacity_free INTEGER, " +
                                             "provisioner BLOB, " +
                                             "PRIMARY KEY (id) )",
                                           dbConnectionPool);
      DBHelper.createDerbyIndex(dbConnectionPool, "provisioners_heartbeat_index", "provisioners", "last_heartbeat");
      DBHelper.createDerbyTableIfNotExists("CREATE TABLE provisionerWorkers (" +
                                             "provisioner_id VARCHAR(255), " +
                                             "tenant_id VARCHAR(255), " +
                                             "num_assigned INTEGER, " +
                                             "num_live INTEGER, " +
                                             "PRIMARY KEY (tenant_id, provisioner_id) )",
                                           dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public Collection<Provisioner> getAllProvisioners() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT provisioner FROM provisioners");
        try {
          return dbQueryExecutor.getQueryList(statement, Provisioner.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting all provisioners", e);
    }
  }

  @Override
  public Collection<Provisioner> getProvisionersWithFreeCapacity() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT provisioner FROM provisioners WHERE capacity_free > 0");
        try {
          return dbQueryExecutor.getQueryList(statement, Provisioner.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting all provisioners", e);
    }
  }

  @Override
  public Collection<Provisioner> getTimedOutProvisioners(long idleTimestamp) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT provisioner FROM provisioners WHERE last_heartbeat < ?");
        try {
          statement.setTimestamp(1, DBHelper.getTimestamp(idleTimestamp));
          return dbQueryExecutor.getQueryList(statement, Provisioner.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting all provisioners", e);
    }
  }

  @Override
  public Collection<Provisioner> getTenantProvisioners(String tenantId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT P.provisioner FROM provisioners P, provisionerWorkers W" +
            " WHERE W.tenant_id=? AND P.id=W.provisioner_id AND W.num_assigned > 0");
        try {
          statement.setString(1, tenantId);
          return dbQueryExecutor.getQueryList(statement, Provisioner.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting all provisioners", e);
    }
  }

  @Override
  public Provisioner getProvisioner(String id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT provisioner FROM provisioners WHERE id=?");
        statement.setString(1, id);
        try {
          return dbQueryExecutor.getQueryItem(statement, Provisioner.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting provisioner " + id, e);
    }
  }

  @Override
  public void writeProvisioner(Provisioner provisioner) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        DBPut provisionerPut =
          new ProvisionerDBPut(provisioner, dbQueryExecutor.toBytes(provisioner, Provisioner.class));
        provisionerPut.executePut(conn);
        writeProvisionerWorkers(conn, provisioner);
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception writing provisioner, will attempt to rollback.", e);
      if (conn != null) {
        try {
          conn.rollback();
        } catch (SQLException e1) {
          LOG.error("Exception rolling back failed provisioner write", e);
        }
      }
      throw new IOException("Exception writing cluster " + provisioner.getId(), e);
    }
  }

  @Override
  public void deleteProvisioner(String id) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        PreparedStatement statement = conn.prepareStatement("DELETE from provisioners WHERE id=?");
        try {
          statement.setString(1, id);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
        statement = conn.prepareStatement("DELETE from provisionerWorkers WHERE provisioner_id=?");
        try {
          statement.setString(1, id);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting provisioner {}", id, e);
      if (conn != null) {
        try {
          conn.rollback();
        } catch (SQLException e1) {
          LOG.error("Exception rolling back failed provisioner delete for provisioner {}", id, e1);
        }
      }
      throw new IOException("Exception deleting provisioner " + id, e);
    }
  }

  @Override
  public void setHeartbeat(String provisionerId, long ts) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("UPDATE provisioners SET last_heartbeat=? WHERE id=?");
        try {
          statement.setTimestamp(1, DBHelper.getTimestamp(ts));
          statement.setString(2, provisionerId);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception setting heartbeat time for provisioner " + provisionerId, e);
    }
  }

  @Override
  public int getFreeCapacity() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT SUM(capacity_free) FROM provisioners");
        try {
          return dbQueryExecutor.getNum(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting amount of free capacity", e);
    }
  }

  @Override
  public int getNumAssignedWorkers(String tenantID) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT SUM(num_assigned) FROM provisionerWorkers WHERE tenant_id=?");
        try {
          statement.setString(1, tenantID);
          return dbQueryExecutor.getNum(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting number of assigned workers for tenant " + tenantID, e);
    }
  }

  private void writeProvisionerWorkers(Connection conn, Provisioner provisioner) throws SQLException {
    // TODO: reduce i/o operations
    String provisionerId = provisioner.getId();
    Set<String> tenants = Sets.union(provisioner.getAssignedTenants(), provisioner.getLiveTenants());

    // tenant workers no longer in the provisioner should be deleted. Most of the time this should be empty.
    Set<String> prevTenants = getTenantsUsedByProvisioner(conn, provisionerId);
    Set<String> tenantsToDelete = Sets.difference(prevTenants, tenants);
    if (!tenantsToDelete.isEmpty()) {
      PreparedStatement deleteStatement = conn.prepareStatement(
        "DELETE FROM provisionerWorkers WHERE provisioner_id=? AND tenant_id=?");
      try {
        deleteStatement.setString(1, provisionerId);
        for (String tenantId : tenantsToDelete) {
          deleteStatement.setString(2, tenantId);
          deleteStatement.executeUpdate();
        }
      } finally {
        deleteStatement.close();
      }
    }

    // insert/update workers in the current state of the provisioner
    PreparedStatement updateStatement = conn.prepareStatement(
      "UPDATE provisionerWorkers SET num_assigned=?, num_live=? WHERE provisioner_id=? AND tenant_id=?");
    PreparedStatement insertStatement = conn.prepareStatement(
      "INSERT INTO provisionerWorkers (provisioner_id, tenant_id, num_assigned, num_live) " +
        "VALUES (?, ?, ?, ?)");
    try {
      updateStatement.setString(3, provisionerId);
      for (String tenant : tenants) {
        int assigned = provisioner.getAssignedWorkers(tenant);
        int live = provisioner.getLiveWorkers(tenant);
        updateStatement.setInt(1, assigned);
        updateStatement.setInt(2, live);
        updateStatement.setString(4, tenant);
        if (updateStatement.executeUpdate() == 0) {
          insertStatement.setString(1, provisionerId);
          insertStatement.setString(2, tenant);
          insertStatement.setInt(3, assigned);
          insertStatement.setInt(4, live);
          insertStatement.executeUpdate();
        }
      }
    } finally {
      try {
        updateStatement.close();
      } catch (SQLException e) {
        LOG.error("Exception closing update statement", e);
      }
      try {
        insertStatement.close();
      } catch (SQLException e) {
        LOG.error("Exception closing insert statement", e);
      }
    }
  }

  private Set<String> getTenantsUsedByProvisioner(Connection conn, String provisionerId) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT tenant_id FROM provisionerWorkers WHERE provisioner_id=?");
    try {
      statement.setString(1, provisionerId);
      ResultSet rs = statement.executeQuery();
      try {
        Set<String> results = Sets.newHashSet();
        while (rs.next()) {
          results.add(rs.getString(1));
        }
        return results;
      } finally {
        rs.close();
      }
    } finally {
      statement.close();
    }
  }

  private class ProvisionerDBPut extends DBPut {
    private final Provisioner provisioner;
    private final byte[] provisionerBytes;

    private ProvisionerDBPut(Provisioner provisioner, byte[] provisionerBytes) {
      this.provisioner = provisioner;
      this.provisionerBytes = provisionerBytes;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE provisioners SET capacity_total=?, capacity_free=?, provisioner=? WHERE id=?");
      statement.setInt(1, provisioner.getCapacityTotal());
      statement.setInt(2, provisioner.getCapacityFree());
      statement.setBytes(3, provisionerBytes);
      statement.setString(4, provisioner.getId());
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO provisioners (id, last_heartbeat, capacity_total, capacity_free, provisioner) " +
          "VALUES (?, ?, ?, ?, ?)");
      statement.setString(1, provisioner.getId());
      statement.setTimestamp(2, DBHelper.getTimestamp(System.currentTimeMillis()));
      statement.setInt(3, provisioner.getCapacityTotal());
      statement.setInt(4, provisioner.getCapacityFree());
      statement.setBytes(5, provisionerBytes);
      return statement;
    }
  }
}
