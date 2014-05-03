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
package com.continuuity.loom.store;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link BaseEntityStore} using a sql database as the persistent store.
 */
public class SQLEntityStore extends BaseEntityStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLEntityStore.class);
  private final DBConnectionPool dbConnectionPool;

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      for (EntityType type : EntityType.values()) {
        Statement stmt = conn.createStatement();
        try {
          stmt.execute("DELETE FROM " + type.getId() + "s");
        } finally {
          stmt.close();
        }
      }
    } finally {
      conn.close();
    }
  }

  @Inject
  SQLEntityStore(DBConnectionPool dbConnectionPool) throws SQLException {
    super();
    this.dbConnectionPool = dbConnectionPool;
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      initDerbyDB();
    }
  }

  public void initDerbyDB() throws SQLException {
    LOG.warn("Initializing Derby DB... Tables are not optimized for performance.");

    for (EntityType entityType : EntityType.values()) {
      String entityName = entityType.getId();
      // immune to sql injection since it comes from the enum
      String createString = "CREATE TABLE " + entityName + "s ( name VARCHAR(255), " + entityName + " BLOB )";
      createDerbyTable(createString);
    }
  }

  private void createDerbyTable(String createString) throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement statement = conn.createStatement();
      try {
        statement.executeUpdate(createString);
      } catch (SQLException e) {
        // code for the table already exists in derby.
        if (!e.getSQLState().equals("X0Y32")) {
          throw Throwables.propagate(e);
        }
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, byte[] data) throws Exception {
    // sticking with standard sql... this could be done in one step with replace, or with
    // insert ... on duplicate key update with mysql.
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
  }

  @Override
  protected byte[] getEntity(EntityType entityType, String entityName) throws Exception {
    byte[] entityBytes = null;
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = getSelectStatement(conn, entityType, entityName);
      try {
        ResultSet rs = statement.executeQuery();
        try {
          if (rs.next()) {
            entityBytes = rs.getBytes(1);
          }
        } finally {
          rs.close();
        }
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
    return entityBytes;
  }

  @Override
  protected <T> Collection<T> getAllEntities(EntityType entityType, Function<byte[], T> transform) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    List<T> entities = Lists.newLinkedList();
    try {
      Statement statement = conn.createStatement();
      try {
        ResultSet rs = statement.executeQuery(getSelectAllStatement(entityType));
        try {
          while (rs.next()) {
            entities.add(transform.apply(rs.getBytes(1)));
          }
        } finally {
          rs.close();
        }
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
    return entities;
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName) throws Exception {
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
  }

  private PreparedStatement getSelectStatement(Connection conn, EntityType entityType,
                                               String entityName) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "SELECT " + entityTypeId + " FROM " + entityTypeId + "s WHERE name=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    return statement;
  }

  private PreparedStatement getInsertStatement(Connection conn, EntityType entityType,
                                               String entityName, byte[] data) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "INSERT INTO " + entityTypeId + "s (name, " + entityTypeId + ") VALUES (?, ?)";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    statement.setBlob(2, new ByteArrayInputStream(data));
    return statement;
  }

  private PreparedStatement getUpdateStatement(Connection conn, EntityType entityType,
                                               String entityName, byte[] data) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "UPDATE " + entityTypeId + "s SET " + entityTypeId + "=? WHERE name=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setBlob(1, new ByteArrayInputStream(data));
    statement.setString(2, entityName);
    return statement;
  }

  private PreparedStatement getDeleteStatement(Connection conn, EntityType entityType,
                                               String entityName) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "DELETE FROM " + entityTypeId + "s WHERE name=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    return statement;
  }

  private String getSelectAllStatement(EntityType entityType) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    return "SELECT " + entityTypeId + " FROM " + entityTypeId + "s";
  }
}
