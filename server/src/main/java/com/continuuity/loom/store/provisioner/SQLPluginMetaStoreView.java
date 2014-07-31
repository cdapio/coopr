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
import com.continuuity.loom.provisioner.plugin.ResourceType;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

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
  public void syncResourceTypes(Set<ResourceType> types) throws IOException {
    Connection conn = null;
    try {
      conn = dbConnectionPool.getConnection(false);
      try {
        syncResourceTypes(conn, types);
        conn.commit();
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception syncing resources of types {} for tenant {}.", types, tenantId);
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

  private void syncResourceTypes(Connection conn, Set<ResourceType> types) throws SQLException {
    // first set live to false for everything
    PreparedStatement deactivateStatement = conn.prepareStatement(
      "UPDATE pluginMeta SET live=false WHERE tenant_id=? AND plugin_type=? AND plugin_name=? " +
        "AND resource_type=? AND deleted=false AND slated=false AND live=true");
    deactivateStatement.setString(1, tenantId);

    // then set live to true for everything that is slated to be live
    PreparedStatement activateStatement = conn.prepareStatement(
      "UPDATE pluginMeta SET live=true WHERE tenant_id=? AND plugin_type=? AND plugin_name=? " +
        "AND resource_type=? AND deleted=false AND slated=true");
    activateStatement.setString(1, tenantId);

    for (ResourceType type : types) {
      deactivateStatement.setString(2, type.getPluginType().name());
      deactivateStatement.setString(3, type.getPluginName());
      deactivateStatement.setString(4, type.getTypeName());
      deactivateStatement.executeUpdate();

      activateStatement.setString(2, type.getPluginType().name());
      activateStatement.setString(3, type.getPluginName());
      activateStatement.setString(4, type.getTypeName());
      activateStatement.executeUpdate();
    }
  }
}
