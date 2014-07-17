package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBPut;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class SQLPluginResourceMetaStoreView implements PluginResourceMetaStoreView {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLPluginResourceMetaStoreView.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final String tenant;
  private final String pluginType;
  private final String pluginName;
  private final String resourceType;

  SQLPluginResourceMetaStoreView(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor,
                                 Account account, PluginResourceType pluginResourceType) {
    Preconditions.checkArgument(account.isAdmin(), "Must be admin to write to plugin meta store.");
    this.tenant = account.getTenantId();
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.pluginType = pluginResourceType.getPluginType().name();
    this.pluginName = pluginResourceType.getPluginName();
    this.resourceType = pluginResourceType.getResourceType();
  }

  @Override
  public boolean exists(PluginResourceMeta meta) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        return exists(conn, meta);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception checking existance of {} for tenant {}.", meta, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public void write(PluginResourceMeta meta) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut metaPut = new PluginResourceMetaDBPut(meta);
        metaPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception writing resource metadata {}.", meta, e);
      throw new IOException(e);
    }
  }

  @Override
  public void delete(PluginResourceMeta meta) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "DELETE FROM pluginMeta WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
        try {
          setConstantFields(statement);
          statement.setString(5, meta.getName());
          statement.setString(6, meta.getVersion());
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting resource metadata {}.", meta, e);
      throw new IOException(e);
    }
  }

  @Override
  public List<PluginResourceMeta> getAll() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, active FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=?");
        try {
          setConstantFields(statement);
          return getResourceMetaList(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting resources.", e);
      throw new IOException(e);
    }
  }

  @Override
  public List<PluginResourceMeta> getAllActive() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, active FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND active=true");
        try {
          setConstantFields(statement);
          return getResourceMetaList(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting resources.", e);
      throw new IOException(e);
    }
  }

  @Override
  public List<PluginResourceMeta> getAll(String resourceName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, active FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=?");
        try {
          setConstantFields(statement);
          statement.setString(5, resourceName);
          return getResourceMetaList(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting all metadata for resource {}.", resourceName, e);
      throw new IOException(e);
    }
  }

  @Override
  public PluginResourceMeta getActive(String resourceName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, active FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND active=true");
        try {
          setConstantFields(statement);
          statement.setString(5, resourceName);
          return getResourceMeta(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting active version of resource {}.", resourceName, e);
      throw new IOException(e);
    }
  }

  @Override
  public void activate(String resourceName, String version) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        deactivateActiveVersion(conn, resourceName);
        activateVersion(conn, resourceName, version);
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      if (conn != null) {
        LOG.error("Exception activating version {} of {}. Rolling back...", version, resourceName, e);
        try {
          conn.rollback();
        } catch (SQLException se) {
          LOG.error("Exception rolling back.", se);
        }
      }
      throw new IOException(e);
    }
  }

  @Override
  public void deactivate(String resourceName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        deactivateActiveVersion(conn, resourceName);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deactivating plugin resource {}.", resourceName, e);
      throw new IOException(e);
    }
  }

  private void deactivateActiveVersion(Connection conn, String resourceName) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET active=false WHERE " +
        "active=true AND tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=?");
    try {
      setConstantFields(statement);
      statement.setString(5, resourceName);
      statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private void activateVersion(Connection conn, String resourceName, String version) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET active=true WHERE " +
        "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
    try {
      setConstantFields(statement);
      statement.setString(5, resourceName);
      statement.setString(6, version);
      statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private boolean exists(Connection conn, PluginResourceMeta meta) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT name FROM pluginMeta WHERE " +
        "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
    try {
      statement.setString(1, tenant);
      statement.setString(2, pluginType);
      statement.setString(3, pluginName);
      statement.setString(4, resourceType);
      statement.setString(5, meta.getName());
      statement.setString(6, meta.getVersion());
      return dbQueryExecutor.hasResults(statement);
    } finally {
      statement.close();
    }
  }

  private PluginResourceMeta getResourceMeta(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      if (results.next()) {
        String name = results.getString(1);
        String version = results.getString(2);
        boolean isActive = results.getBoolean(3);
        return new PluginResourceMeta(name, version, isActive);
      }
      return null;
    } finally {
      results.close();
    }
  }

  private List<PluginResourceMeta> getResourceMetaList(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      List<PluginResourceMeta> output = Lists.newArrayList();
      while (results.next()) {
        String name = results.getString(1);
        String version = results.getString(2);
        boolean isActive = results.getBoolean(3);
        output.add(new PluginResourceMeta(name, version, isActive));
      }
      return ImmutableList.copyOf(output);
    } finally {
      results.close();
    }
  }

  private void setConstantFields(PreparedStatement statement) throws SQLException {
    statement.setString(1, tenant);
    statement.setString(2, pluginType);
    statement.setString(3, pluginName);
    statement.setString(4, resourceType);
  }

  private class PluginResourceMetaDBPut extends DBPut {
    private final PluginResourceMeta meta;

    private PluginResourceMetaDBPut(PluginResourceMeta meta) {
      this.meta = meta;
    }

    @Override
    protected PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE pluginMeta SET active=? WHERE " +
          "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
      statement.setBoolean(1, meta.isActive());
      statement.setString(2, tenant);
      statement.setString(3, pluginType);
      statement.setString(4, pluginName);
      statement.setString(5, resourceType);
      statement.setString(6, meta.getName());
      statement.setString(7, meta.getVersion());
      return statement;
    }

    @Override
    protected PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO pluginMeta (tenant_id, plugin_type, plugin_name, resource_type, name, version, active) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?)");
      statement.setString(1, tenant);
      statement.setString(2, pluginType);
      statement.setString(3, pluginName);
      statement.setString(4, resourceType);
      statement.setString(5, meta.getName());
      statement.setString(6, meta.getVersion());
      statement.setBoolean(7, meta.isActive());
      return statement;
    }
  }
}
