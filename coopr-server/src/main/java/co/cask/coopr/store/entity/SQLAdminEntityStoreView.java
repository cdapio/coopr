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
package co.cask.coopr.store.entity;

import co.cask.coopr.account.Account;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBPut;
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

  SQLAdminEntityStoreView(Account account, DBConnectionPool dbConnectionPool, Gson gson) {
    super(account, dbConnectionPool, gson);
    Preconditions.checkArgument(account.isAdmin(), "Entity store only writable by admins");
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, int version, byte[] data) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut entityPut = new EntityDBPut(entityType, entityName, version, data);
        entityPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception writing entity of type " + entityType.name().toLowerCase()
                              + " with name " + entityName + accountErrorSnippet);
    }
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getDeleteStatementWithoutVersion(conn, entityType, entityName);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception deleting all versions of type " + entityType.name().toLowerCase()
                              + " with name " + entityName + accountErrorSnippet);
    }
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName, int entityVersion)
    throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getDeleteStatementWithVersion(conn, entityType, entityName, entityVersion);
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
                              + " with name " + entityName + " and version " + entityVersion + accountErrorSnippet);
    }
  }

  private PreparedStatement getDeleteStatementWithoutVersion(Connection conn, EntityType entityType,
                                                             String entityName) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "DELETE FROM " + entityTypeId + "s WHERE name=? AND tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  private PreparedStatement getDeleteStatementWithVersion(Connection conn, EntityType entityType,
                                                          String entityName, int entityVersion) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "DELETE FROM " + entityTypeId + "s WHERE name=? AND version=? AND tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    statement.setInt(2, entityVersion);
    statement.setString(3, account.getTenantId());
    return statement;
  }

  private class EntityDBPut extends DBPut {
    private final EntityType entityType;
    private final String entityName;
    private final int version;
    private final byte[] data;

    private EntityDBPut(EntityType entityType, String entityName, int version, byte[] data) {
      this.entityType = entityType;
      this.entityName = entityName;
      this.version = version;
      this.data = data;
    }

    public void executePut(Connection conn) throws SQLException {
      PreparedStatement insertStatement = createInsertStatement(conn);
      try {
        insertStatement.executeUpdate();
      } finally {
        insertStatement.close();
      }
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      throw new UnsupportedOperationException();
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      String entityTypeId = entityType.getId();
      // immune to sql injection since it comes from the enum.
      String queryStr = "INSERT INTO " + entityTypeId + "s (name, version, tenant_id, " + entityTypeId +
        ") VALUES (?, ?, ?, ?)";
      PreparedStatement statement = conn.prepareStatement(queryStr);
      statement.setString(1, entityName);
      statement.setInt(2, version);
      statement.setString(3, account.getTenantId());
      statement.setBytes(4, data);
      return statement;
    }
  }
}
