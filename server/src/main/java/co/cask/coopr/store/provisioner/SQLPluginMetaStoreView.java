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
import co.cask.coopr.common.utils.ImmutablePair;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBQueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * View of the plugin metadata persistent store for a given account, backed by a SQL database.
 */
public class SQLPluginMetaStoreView implements PluginMetaStoreView {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLPluginMetaStoreView.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final Account account;
  private final String tenantId;

  public SQLPluginMetaStoreView(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor, Account account) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.account = account;
    this.tenantId = account.getTenantId();
  }

  @Override
  public PluginResourceTypeView getResourceTypeView(ResourceType type) {
    return new SQLPluginResourceTypeView(dbConnectionPool, dbQueryExecutor, account, type);
  }

  @Override
  public void syncResources(ResourceCollection resources) throws IOException {
    // set live to false for everything
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        // set everything to not live
        unsetLiveFlag(conn);
        // go through resources in the collection and set each one to live
        PreparedStatement statement = conn.prepareStatement(
          "UPDATE pluginMeta SET live=true WHERE tenant_id=? AND plugin_type=? AND plugin_name=? AND " +
            "resource_type=? AND name=? AND version=?");
        try {
          statement.setString(1, tenantId);
          for (Map.Entry<ImmutablePair<ResourceType, ResourceTypeSpecification>, ResourceMeta> entry :
            resources.getResources().entries()) {
            ResourceType type = entry.getKey().getFirst();
            ResourceMeta meta = entry.getValue();
            statement.setString(2, type.getPluginType().name());
            statement.setString(3, type.getPluginName());
            statement.setString(4, type.getTypeName());
            statement.setString(5, meta.getName());
            statement.setInt(6, meta.getVersion());
            statement.executeUpdate();
          }
        } finally {
          statement.close();
        }
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception syncing resources for tenant {}.", tenantId);
      if (conn != null) {
        try {
          LOG.info("Rolling back uncommitted changes during sync.");
          conn.rollback();
        } catch (SQLException e1) {
          LOG.error("Exception rolling back uncommited changes during sync.", e1);
        }
      }
      throw new IOException(e);
    }
  }

  @Override
  public int numResources() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT COUNT(*) FROM pluginMeta WHERE tenant_id=? AND deleted=false");
        try {
          statement.setString(1, tenantId);
          return dbQueryExecutor.getNum(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting number of resources in the metadata store.", e);
      throw new IOException(e);
    }
  }

  private void unsetLiveFlag(Connection conn) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE pluginMeta SET live=false WHERE tenant_id=? AND live=true");
    try {
      statement.setString(1, tenantId);
      statement.executeUpdate();
    } finally {
      statement.close();
    }
  }
}
