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
package com.continuuity.loom.store.entity;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBPut;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of {@link BaseSQLEntityStoreView} from the view of a tenant admin.
 */
public class SQLAdminEntityStoreView extends BaseSQLEntityStoreView {
  private final DBQueryExecutor dbQueryExecutor;

  SQLAdminEntityStoreView(Account account, DBConnectionPool dbConnectionPool,
                          DBQueryExecutor dbQueryExecutor, Gson gson) {
    super(account, dbConnectionPool, gson);
    this.dbQueryExecutor = dbQueryExecutor;
    Preconditions.checkArgument(account.isAdmin(), "Entity store only writeable by admins");
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, byte[] data) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut entityPut = new EntityDBPut(entityType, entityName, data);
        entityPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception writing entity of type " + entityType.name().toLowerCase()
                              + " of name " + entityName + accountErrorSnippet);
    }
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getDeleteStatement(conn, entityType, entityName);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception deleting entity of type " + entityType.name().toLowerCase()
                              + " of name " + entityName + accountErrorSnippet);
    }
  }

  private PreparedStatement getDeleteStatement(Connection conn, EntityType entityType,
                                               String entityName) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "DELETE FROM " + entityTypeId + "s WHERE name=? AND tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  private class EntityDBPut extends DBPut {
    private final EntityType entityType;
    private final String entityName;
    private final byte[] data;

    private EntityDBPut(EntityType entityType, String entityName, byte[] data) {
      this.entityType = entityType;
      this.entityName = entityName;
      this.data = data;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      String entityTypeId = entityType.getId();
      // immune to sql injection since it comes from the enum.
      String queryStr = "UPDATE " + entityTypeId + "s SET " + entityTypeId + "=? WHERE name=? AND tenant_id=?";
      PreparedStatement statement = conn.prepareStatement(queryStr);
      statement.setBytes(1, data);
      statement.setString(2, entityName);
      statement.setString(3, account.getTenantId());
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      String entityTypeId = entityType.getId();
      // immune to sql injection since it comes from the enum.
      String queryStr = "INSERT INTO " + entityTypeId + "s (name, tenant_id, " + entityTypeId + ") VALUES (?, ?, ?)";
      PreparedStatement statement = conn.prepareStatement(queryStr);
      statement.setString(1, entityName);
      statement.setString(2, account.getTenantId());
      statement.setBytes(3, data);
      return statement;
    }
  }
}
