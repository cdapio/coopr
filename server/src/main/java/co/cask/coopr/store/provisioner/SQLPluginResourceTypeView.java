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

package co.cask.coopr.store.provisioner;

import co.cask.coopr.account.Account;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBQueryExecutor;
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
 * SQL database backed implementation of {@link PluginResourceTypeView}. Stores all metadata in a single
 * database table.
 */
public class SQLPluginResourceTypeView implements PluginResourceTypeView {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLPluginResourceTypeView.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final String tenant;
  private final String pluginType;
  private final String pluginName;
  private final String resourceType;

  SQLPluginResourceTypeView(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor,
                            Account account, ResourceType resourceType) {
    Preconditions.checkArgument(account.isAdmin(), "Must be admin to write to plugin meta store.");
    this.tenant = account.getTenantId();
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.pluginType = resourceType.getPluginType().name();
    this.pluginName = resourceType.getPluginName();
    this.resourceType = resourceType.getTypeName();
  }

  @Override
  public boolean exists(String name) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name FROM pluginMeta WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND deleted=false");
        try {
          setConstantFields(statement);
          statement.setString(5, name);
          return dbQueryExecutor.hasResults(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception checking existance of resource {} for tenant {}.", name, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public boolean exists(String name, int version) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name FROM pluginMeta WHERE tenant_id=? AND plugin_type=? AND plugin_name=? " +
            "AND resource_type=? AND name=? AND version=? AND deleted=false");
        try {
          setConstantFields(statement);
          statement.setString(5, name);
          statement.setInt(6, version);
          return dbQueryExecutor.hasResults(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception checking existance of version {} of resource {} for tenant {}.",
                version, name, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public void add(ResourceMeta meta) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "INSERT INTO pluginMeta " +
            "(tenant_id, plugin_type, plugin_name, resource_type, name," +
            " version, slated, live, deleted, create_time, delete_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
          ResourceStatus status = meta.getStatus();
          setConstantFields(statement);
          statement.setString(5, meta.getName());
          statement.setInt(6, meta.getVersion());
          statement.setBoolean(7, status.isLiveAfterSync());
          statement.setBoolean(8, status.isLive());
          statement.setBoolean(9, false);
          statement.setTimestamp(10, DBHelper.getTimestamp(System.currentTimeMillis()));
          statement.setTimestamp(11, null);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
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
  public int getHighestVersion(String name) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT MAX(version) FROM pluginMeta WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=?");
        try {
          setConstantFields(statement);
          statement.setString(5, name);
          return dbQueryExecutor.getNum(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting highest version of resource {} for tenant {}.", name, tenant, e);
      throw new IOException(e);
    }
  }

  @Override
  public ResourceMeta get(String name, int version) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        return getMeta(conn, name, version);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting metadata of version {} of resource {} for tenant {}.",
                version, name, tenant, e);
      throw new IOException(e);
    }
  }

  @Override
  public void delete(String name, int version) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "UPDATE pluginMeta SET deleted=true, delete_time=? WHERE " +
            "tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=?");
        try {
          statement.setTimestamp(1, DBHelper.getTimestamp(System.currentTimeMillis()));
          setConstantFields(statement, 2);
          statement.setString(6, name);
          statement.setInt(7, version);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting version {} of resource {} for tenant {}.",
                version, name, tenant);
      throw new IOException(e);
    }
  }

  @Override
  public Map<String, Set<ResourceMeta>> getAll() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, slated, live FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND deleted=false");
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
  public Map<String, Set<ResourceMeta>> getAll(ResourceStatus status) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, slated, live FROM pluginMeta WHERE tenant_id=? AND plugin_type=? " +
            "AND plugin_name=? AND resource_type=? AND slated=? AND live=? AND deleted=false");
        try {
          setConstantFields(statement);
          statement.setBoolean(5, status.isLiveAfterSync());
          statement.setBoolean(6, status.isLive());
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
  public Set<ResourceMeta> getAll(String name) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, slated, live FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND deleted=false");
        try {
          setConstantFields(statement);
          statement.setString(5, name);
          return getResourceMetaSet(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting all metadata for resource {}.", name, e);
      throw new IOException(e);
    }
  }

  @Override
  public Set<ResourceMeta> getAll(String name, ResourceStatus status) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, slated, live FROM pluginMeta " +
            "WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND " +
            "resource_type=? AND name=? AND slated=? AND live=? AND deleted=false");
        try {
          setConstantFields(statement);
          statement.setString(5, name);
          statement.setBoolean(6, status.isLiveAfterSync());
          statement.setBoolean(7, status.isLive());
          return getResourceMetaSet(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting all metadata for resource {}.", name, e);
      throw new IOException(e);
    }
  }

  @Override
  public Set<ResourceMeta> getResourcesToSync() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, slated, live FROM pluginMeta WHERE tenant_id=? AND plugin_type=? AND " +
            "plugin_name=? AND resource_type=? AND slated=true AND deleted=false");
        try {
          setConstantFields(statement);
          return getResourceMetaSet(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting metadata of all resources slated to be active.", e);
      throw new IOException(e);
    }
  }

  @Override
  public Set<ResourceMeta> getLiveResources() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT name, version, slated, live FROM pluginMeta WHERE tenant_id=? AND plugin_type=? AND " +
            "plugin_name=? AND resource_type=? AND live=true AND deleted=false");
        try {
          setConstantFields(statement);
          return getResourceMetaSet(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting metadata of all resources slated to be active.", e);
      throw new IOException(e);
    }
  }

  @Override
  public void stage(String name, int version) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        // set the slated flag for this version to true
        // if it returns 0, no rows were updated, which means the resource does not exist
        if (slateVersion(conn, name, version) > 0) {
          // set slated to false for any other version except this one
          unslateResource(conn, name, version);
        }
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      if (conn != null) {
        LOG.error("Exception activating version {} of {}. Rolling back...", version, name, e);
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
  public void recall(String name, int version) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "UPDATE pluginMeta SET slated=false WHERE tenant_id=? AND plugin_type=? AND " +
            "plugin_name=? AND resource_type=? AND name=? AND version=?");
        try {
          setConstantFields(statement);
          statement.setString(5, name);
          statement.setInt(6, version);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deactivating plugin resource {}.", name, e);
      throw new IOException(e);
    }
  }

  // set the slated flag to false wherever its true, except for the given version,
  // returning the number of rows that were updated (should be 0 or 1)
  private int unslateResource(Connection conn, String name, int exceptVersion) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET slated=false WHERE tenant_id=? AND plugin_type=? " +
        "AND plugin_name=? AND resource_type=? AND name=? AND slated=true AND version<>?");
    try {
      setConstantFields(statement);
      statement.setString(5, name);
      statement.setInt(6, exceptVersion);
      return statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  // slate the given version, returning how many rows were updated (should be 0 or 1)
  private int slateVersion(Connection conn, String name, int version) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET slated=true WHERE tenant_id=? AND plugin_type=? " +
        "AND plugin_name=? AND resource_type=? AND name=? AND version=?");
    try {
      setConstantFields(statement);
      statement.setString(5, name);
      statement.setInt(6, version);
      return statement.executeUpdate();
    } finally {
      statement.close();
    }
  }

  private ResourceMeta getResourceMeta(PreparedStatement statement) throws SQLException {
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

  private Set<ResourceMeta> getResourceMetaSet(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      Set<ResourceMeta> output = Sets.newHashSet();
      while (results.next()) {
        output.add(metaFromResult(results));
      }
      return ImmutableSet.copyOf(output);
    } finally {
      results.close();
    }
  }

  private Map<String, Set<ResourceMeta>> getResourceMetaMap(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      Map<String, Set<ResourceMeta>> output = Maps.newHashMap();
      while (results.next()) {
        ResourceMeta meta = metaFromResult(results);
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

  private ResourceMeta getMeta(Connection conn, String name, int version) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT name, version, slated, live FROM pluginMeta WHERE tenant_id=? " +
        "AND plugin_type=? AND plugin_name=? AND resource_type=? AND name=? AND version=? AND deleted=false");
    try {
      setConstantFields(statement);
      statement.setString(5, name);
      statement.setInt(6, version);
      return getResourceMeta(statement);
    } finally {
      statement.close();
    }
  }

  private ResourceMeta metaFromResult(ResultSet results) throws SQLException {
    String name = results.getString(1);
    int version = results.getInt(2);
    boolean slated = results.getBoolean(3);
    boolean live = results.getBoolean(4);
    return new ResourceMeta(name, version, ResourceStatus.fromLiveFlags(live, slated));
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
}
