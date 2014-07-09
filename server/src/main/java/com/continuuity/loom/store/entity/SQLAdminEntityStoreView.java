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
import com.google.common.base.Preconditions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of {@link BaseSQLEntityStoreView} from the view of a tenant admin.
 */
public class SQLAdminEntityStoreView extends BaseSQLEntityStoreView {

  SQLAdminEntityStoreView(Account account, DBConnectionPool dbConnectionPool) {
    super(account, dbConnectionPool);
    Preconditions.checkArgument(account.isAdmin(), "Entity store only viewable by admins");
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, byte[] data) throws IOException {
    // sticking with standard sql... this could be done in one step with replace, or with
    // insert ... on duplicate key update with mysql.
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        // table name doesn't come from the user, ok to insert here
        PreparedStatement checkStatement = getSelectStatement(conn, entityType, entityName);
        PreparedStatement writeStatement;
        try {
          ResultSet rs = checkStatement.executeQuery();
          try {
            if (rs.next()) {
              // entity exists already, perform an update.
              writeStatement = getUpdateStatement(conn, entityType, entityName, data);
            } else {
              // entity does not exist, perform an insert.
              writeStatement = getInsertStatement(conn, entityType, entityName, data);
            }
          } finally {
            rs.close();
          }
          // perform the update or insert
          try {
            writeStatement.executeUpdate();
          } finally {
            writeStatement.close();
          }
        } finally {
          checkStatement.close();
        }
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

  private PreparedStatement getInsertStatement(Connection conn, EntityType entityType,
                                               String entityName, byte[] data) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "INSERT INTO " + entityTypeId + "s (name, tenant_id, " + entityTypeId + ") VALUES (?, ?, ?)";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    statement.setString(2, account.getTenantId());
    statement.setBlob(3, new ByteArrayInputStream(data));
    return statement;
  }

  private PreparedStatement getUpdateStatement(Connection conn, EntityType entityType,
                                               String entityName, byte[] data) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "UPDATE " + entityTypeId + "s SET " + entityTypeId + "=? WHERE name=? AND tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setBlob(1, new ByteArrayInputStream(data));
    statement.setString(2, entityName);
    statement.setString(3, account.getTenantId());
    return statement;
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
}
