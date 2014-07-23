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
import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.continuuity.loom.provisioner.PluginResourceStatus;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBPut;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * SQL database backed implementation of {@link PluginResourceMetaStoreView}. Stores all metadata in a single
 * database table.
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
  public boolean exists(String resourceName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name FROM pluginMeta WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=?");
        try {
          setConstantFields(statement);
          statement.setString(5, resourceName);
          return dbQueryExecutor.hasResults(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception checking existance of resource {} for tenant {}.", resourceName, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public boolean exists(String resourceName, String resourceVersion) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name FROM pluginMeta WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
        try {
          setConstantFields(statement);
          statement.setString(5, resourceName);
          statement.setString(6, resourceVersion);
          return dbQueryExecutor.hasResults(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception checking existance of version {} of resource {} for tenant {}.",
                resourceVersion, resourceName, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public void write(PluginResourceMeta meta) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut metaPut = new MetaDBPut(meta);
        metaPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception adding version {} of resource {} for tenant {}.",
                meta.getVersion(), meta.getName(), tenant, e);
      throw new IOException(e);
    }
  }

  @Override
  public PluginResourceMeta get(String resourceName, String resourceVersion) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        return getMeta(conn, resourceName, resourceVersion);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting metadata of version {} of resource {} for tenant {}.",
                resourceVersion, resourceName, tenant, e);
      throw new IOException(e);
    }
  }

  @Override
  public void delete(String resourceName, String resourceVersion) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "DELETE FROM pluginMeta WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
        try {
          setConstantFields(statement);
          statement.setString(5, resourceName);
          statement.setString(6, resourceVersion);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting version {} of resource {} for tenant {}.",
                resourceVersion, resourceName, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public Map<String, Set<PluginResourceMeta>> getAll() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT resource_id, name, version, status FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=?");
        try {
          setConstantFields(statement);
          return getResourceMetaMap(statement);
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
  public Map<String, Set<PluginResourceMeta>> getAll(PluginResourceStatus status) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT resource_id, name, version, status FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND status=?");
        try {
          setConstantFields(statement);
          statement.setString(5, status.name());
          return getResourceMetaMap(statement);
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
  public Set<PluginResourceMeta> getAll(String resourceName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT resource_id, name, version, status FROM pluginMeta " +
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
  public Set<PluginResourceMeta> getAll(String resourceName, PluginResourceStatus status) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT resource_id, name, version, status FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND status=?");
        try {
          setConstantFields(statement);
          statement.setString(5, resourceName);
          statement.setString(6, status.name());
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
  public void stage(String resourceName, String resourceVersion) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        PluginResourceMeta meta = getMeta(conn, resourceName, resourceVersion);
        if (meta == null) {
          return;
        }
        PluginResourceStatus status = meta.getStatus();
        if (status == PluginResourceStatus.INACTIVE) {
          // deactivate current staged version if there is one
          deactivateVersionsWithStatus(conn, resourceName, PluginResourceStatus.STAGED);
          // set status of this version to staged
          setStatus(conn, resourceName, resourceVersion, PluginResourceStatus.STAGED);
        } else if (status == PluginResourceStatus.UNSTAGED) {
          setStatus(conn, resourceName, resourceVersion, PluginResourceStatus.ACTIVE);
        }
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      if (conn != null) {
        LOG.error("Exception activating version {} of {}. Rolling back...", resourceVersion, resourceName, e);
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
  public void unstage(String resourceName, String resourceVersion) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PluginResourceMeta meta = getMeta(conn, resourceName, resourceVersion);
        PluginResourceStatus status = meta.getStatus();
        if (status == PluginResourceStatus.STAGED) {
          setStatus(conn, resourceName, resourceVersion, PluginResourceStatus.INACTIVE);
        } else if (status == PluginResourceStatus.ACTIVE) {
          setStatus(conn, resourceName, resourceVersion, PluginResourceStatus.UNSTAGED);
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deactivating plugin resource {}.", resourceName, e);
      throw new IOException(e);
    }
  }

  @Override
  public void activate(String resourceName) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        PluginResourceMeta meta = getStagedMeta(conn, resourceName);
        // if there is no staged version, don't do anything
        if (meta == null) {
          return;
        }
        // deactivate current active version
        deactivateVersionsWithStatus(conn, resourceName, PluginResourceStatus.ACTIVE);
        // activate staged version
        setStatus(conn, resourceName, meta.getVersion(), PluginResourceStatus.ACTIVE);
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception activating plugin resource {}.", resourceName, e);
      if (conn != null) {
        LOG.error("Rolling back changes...");
        try {
          conn.rollback();
        } catch (SQLException se) {
          LOG.error("Exception rolling back.", se);
        }
      }
      throw new IOException(e);
    }
  }

  private void deactivateVersionsWithStatus(Connection conn, String resourceName, PluginResourceStatus status)
    throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET status=? WHERE " +
        "status=? AND tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=?");
    try {
      statement.setString(1, PluginResourceStatus.INACTIVE.name());
      statement.setString(2, status.name());
      setConstantFields(statement, 3);
      statement.setString(7, resourceName);
      statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private void setStatus(Connection conn, String resourceName, String resourceVersion, PluginResourceStatus status)
    throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET status=? WHERE " +
        "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
    try {
      statement.setString(1, status.name());
      setConstantFields(statement, 2);
      statement.setString(6, resourceName);
      statement.setString(7, resourceVersion);
      statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private PluginResourceMeta getResourceMeta(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      if (results.next()) {
        return metaFromResult(results);
      }
      return null;
    } finally {
      results.close();
    }
  }

  private Set<PluginResourceMeta> getResourceMetaList(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      Set<PluginResourceMeta> output = Sets.newHashSet();
      while (results.next()) {
        output.add(metaFromResult(results));
      }
      return ImmutableSet.copyOf(output);
    } finally {
      results.close();
    }
  }

  private Map<String, Set<PluginResourceMeta>> getResourceMetaMap(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      Map<String, Set<PluginResourceMeta>> output = Maps.newHashMap();
      while (results.next()) {
        PluginResourceMeta meta = metaFromResult(results);
        String name = meta.getName();
        if (output.containsKey(name)) {
          output.get(name).add(meta);
        } else {
          output.put(name, Sets.newHashSet(meta));
        }
      }
      return ImmutableMap.copyOf(output);
    } finally {
      results.close();
    }
  }

  private PluginResourceMeta getStagedMeta(Connection conn, String resourceName) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT resource_id, name, version, status FROM pluginMeta " +
        "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND status=?");
    try {
      setConstantFields(statement);
      statement.setString(5, resourceName);
      statement.setString(6, PluginResourceStatus.STAGED.name());
      return getResourceMeta(statement);
    } finally {
      statement.close();
    }
  }

  private PluginResourceMeta getMeta(Connection conn, String resourceName, String resourceVersion) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT resource_id, name, version, status FROM pluginMeta " +
        "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
    try {
      setConstantFields(statement);
      statement.setString(5, resourceName);
      statement.setString(6, resourceVersion);
      return getResourceMeta(statement);
    } finally {
      statement.close();
    }
  }

  private PluginResourceMeta metaFromResult(ResultSet results) throws SQLException {
    String id = results.getString(1);
    String name = results.getString(2);
    String version = results.getString(3);
    PluginResourceStatus status = PluginResourceStatus.valueOf(results.getString(4));
    return PluginResourceMeta.fromExisting(id, name, version, status);
  }

  private void setConstantFields(PreparedStatement statement) throws SQLException {
    setConstantFields(statement, 1);
  }

  private void setConstantFields(PreparedStatement statement, int startIndex) throws SQLException {
    statement.setString(startIndex, tenant);
    statement.setString(startIndex + 1, pluginType);
    statement.setString(startIndex + 2, pluginName);
    statement.setString(startIndex + 3, resourceType);
  }

  private class MetaDBPut extends DBPut {
    private final PluginResourceMeta meta;

    private MetaDBPut(PluginResourceMeta meta) {
      this.meta = meta;
    }

    @Override
    protected PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE pluginMeta SET resource_id=?, status=? WHERE " +
          "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
      statement.setString(1, meta.getResourceId());
      statement.setString(2, meta.getStatus().name());
      setConstantFields(statement, 3);
      statement.setString(7, meta.getName());
      statement.setString(8, meta.getVersion());
      return statement;
    }

    @Override
    protected PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO pluginMeta " +
          "(tenant_id, plugin_type, plugin_name, resource_type, resource_id, name, version, status) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      setConstantFields(statement);
      statement.setString(5, meta.getResourceId());
      statement.setString(6, meta.getName());
      statement.setString(7, meta.getVersion());
      statement.setString(8, meta.getStatus().name());
      return statement;
    }
  }
}
