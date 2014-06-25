package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.provisioner.Provisioner;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.apache.twill.common.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the {@link ProvisionerStore} using a SQL database for persistent storage.  Maintains
 * two tables, one for storing provisioner information and another for maintaining an index from tenants to
 * provisioners.
 */
public class SQLProvisionerStore extends AbstractScheduledService implements ProvisionerStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLProvisionerStore.class);
  private final DBConnectionPool dbConnectionPool;
  private final JsonSerde codec;

  @Inject
  public SQLProvisionerStore(DBConnectionPool dbConnectionPool, JsonSerde codec) {
    this.dbConnectionPool = dbConnectionPool;
    this.codec = codec;
  }

  // for unit tests only
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection(true);
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
  protected void runOneIteration() throws Exception {
    cleanupWorkers();
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBQueryHelper.createDerbyTable("CREATE TABLE provisioners (" +
                                       "id VARCHAR(255), " +
                                       "last_heartbeat TIMESTAMP, " +
                                       "capacity_total INTEGER, " +
                                       "capacity_free INTEGER, " +
                                       "status VARCHAR(16), " +
                                       "provisioner BLOB, " +
                                       "PRIMARY KEY (id) )",
                                     dbConnectionPool);
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        Statement stmt = conn.createStatement();
        try {
          stmt.execute("CREATE INDEX provisioners_heartbeat_index ON provisioners (last_heartbeat)");
        } finally {
          stmt.close();
        }
      } finally {
        conn.close();
      }
      DBQueryHelper.createDerbyTable("CREATE TABLE provisionerWorkers (" +
                                       "provisioner_id VARCHAR(255), " +
                                       "tenant_id VARCHAR(255), " +
                                       "num_assigned INTEGER, " +
                                       "num_live INTEGER, " +
                                       "assign_time TIMESTAMP, " +
                                       "PRIMARY KEY (tenant_id, provisioner_id) )",
                                     dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  protected ScheduledExecutorService executor() {
    return Executors.newSingleThreadScheduledExecutor(Threads.createDaemonThreadFactory("sql-provisioner-store"));
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(1, 180, TimeUnit.SECONDS);
  }

  @Override
  public Collection<Provisioner> getAllProvisioners() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT provisioner FROM provisioners");
        try {
          return DBQueryHelper.getQueryList(statement, Provisioner.class);
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
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT provisioner FROM provisioners WHERE capacity_free > 0");
        try {
          return DBQueryHelper.getQueryList(statement, Provisioner.class);
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
  public Collection<Provisioner> getIdleProvisioners(long idleTimestamp) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT provisioner FROM provisioners WHERE last_heartbeat < ?");
        try {
          statement.setTimestamp(1, DBQueryHelper.getTimestamp(idleTimestamp));
          return DBQueryHelper.getQueryList(statement, Provisioner.class);
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
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT P.provisioner FROM provisioners P, provisionerWorkers W" +
            " WHERE W.tenant_id=? AND P.id=W.provisioner_id AND W.num_assigned > 0");
        try {
          statement.setString(1, tenantId);
          return DBQueryHelper.getQueryList(statement, Provisioner.class);
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
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT provisioner FROM provisioners WHERE id=?");
        statement.setString(1, id);
        try {
          return DBQueryHelper.getQueryItem(statement, Provisioner.class);
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
  public void unassignTenantProvisioners(String tenantId) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);

      // set num_assigned to 0 for all provisioners that have the tenant
      int numUnassigned = 0;
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE provisionerWorkers SET num_assigned=0 WHERE tenant_id=? AND num_assigned > 0");
      try {
        statement.setString(1, tenantId);
        numUnassigned = statement.executeUpdate();
      } finally {
        statement.close();
      }

      // if we unassigned anything, need to update the provisioners table too
      if (numUnassigned > 0) {
        Set <Provisioner> provisioners = getProvisionersForTenant(conn, tenantId);
        for (Provisioner provisioner : provisioners) {
          provisioner.removeTenantAssignments(tenantId);
          updateProvisioner(conn, provisioner, toByteStream(provisioner));
        }
      }

      conn.commit();
    } catch (SQLException e) {
      LOG.error("Exception deleting provisioners for tenant {}. Attempting rollback.", tenantId, e);
      if (conn != null) {
        try {
          conn.rollback();
        } catch (SQLException e1) {
          LOG.error("Exception rolling back failed deletion of provisioners for tenant {}", tenantId);
        }
      }
      throw new IOException("Exception deleting provisioners for tenant " + tenantId, e);
    }
  }

  @Override
  public void writeProvisioner(Provisioner provisioner) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      ByteArrayInputStream bytes = toByteStream(provisioner);
      try {
        // try updating first, if no rows were updated we need to do an insert
        if (updateProvisioner(conn, provisioner, bytes) == 0) {
          addProvisioner(conn, provisioner, bytes);
        }

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
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement("UPDATE provisioners SET last_heartbeat=? WHERE id=?");
        try {
          statement.setTimestamp(1, DBQueryHelper.getTimestamp(ts));
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
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT SUM(capacity_free) FROM provisioners");
        try {
          return DBQueryHelper.getNum(statement);
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
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT SUM(num_assigned) FROM provisionerWorkers WHERE tenant_id=?");
        try {
          statement.setString(1, tenantID);
          return DBQueryHelper.getNum(statement);
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

  // cleanup provisionerWorkers table. If assigned and live are both 0, no reason to keep them there.
  private void cleanupWorkers() {
    try {
      Connection conn = dbConnectionPool.getConnection(true);
      try {
        Statement statement = conn.createStatement();
        try {
          statement.executeUpdate("DELETE FROM provisionerWorkers WHERE num_assigned=0 AND num_live=0");
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception cleaning up provisionerWorkers table");
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
      "INSERT INTO provisionerWorkers (provisioner_id, tenant_id, num_assigned, num_live, assign_time) " +
        "VALUES (?, ?, ?, ?, ?)");
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
          insertStatement.setTimestamp(5, DBQueryHelper.getTimestamp(System.currentTimeMillis()));
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

  private int updateProvisioner(Connection conn, Provisioner provisioner,
                                ByteArrayInputStream bytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE provisioners SET capacity_total=?, capacity_free=?, status=?, provisioner=? WHERE id=?");
    try {
      statement.setInt(1, provisioner.getCapacityTotal());
      statement.setInt(2, provisioner.getCapacityFree());
      statement.setString(3, provisioner.getStatus().name());
      statement.setBlob(4, bytes);
      statement.setString(5, provisioner.getId());
      return statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private void addProvisioner(Connection conn, Provisioner provisioner,
                              ByteArrayInputStream bytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "INSERT INTO provisioners (id, last_heartbeat, capacity_total, capacity_free, status, provisioner) " +
        "VALUES (?, ?, ?, ?, ?, ?)");
    try {
      statement.setString(1, provisioner.getId());
      statement.setTimestamp(2, DBQueryHelper.getTimestamp(System.currentTimeMillis()));
      statement.setInt(3, provisioner.getCapacityTotal());
      statement.setInt(4, provisioner.getCapacityFree());
      statement.setString(5, provisioner.getStatus().name());
      statement.setBlob(6, bytes);
      statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private Set<Provisioner> getProvisionersForTenant(Connection conn, String tenantId) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT P.provisioner FROM provisioners P, provisionerWorkers W WHERE W.tenant_id=? AND W.provisioner_id=P.id");
    try {
      statement.setString(1, tenantId);
      return DBQueryHelper.getQuerySet(statement, Provisioner.class);
    } finally {
      statement.close();
    }
  }

  private Set<String> getTenantsUsedByProvisioner(Connection conn, String provisionerId) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT tenant_id FROM provisionerWorkers WHERE provisioner_id=?");
    try {
      statement.setString(1, provisionerId);
      try {
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
    } finally {
      statement.close();
    }
  }

  private ByteArrayInputStream toByteStream(Provisioner provisioner) {
    return new ByteArrayInputStream(codec.serialize(provisioner, Provisioner.class));
  }
}
