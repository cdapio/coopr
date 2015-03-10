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
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.store.DBConnectionPool;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Base implementation of {@link BaseEntityStoreView} using a sql database as the persistent store.
 */
public abstract class BaseSQLEntityStoreView extends BaseEntityStoreView {
  protected final Account account;
  protected final DBConnectionPool dbConnectionPool;
  protected final String accountErrorSnippet;

  BaseSQLEntityStoreView(Account account, DBConnectionPool dbConnectionPool, Gson gson) {
    super(gson);
    this.account = account;
    this.dbConnectionPool = dbConnectionPool;
    this.accountErrorSnippet = " from tenant " + account.getTenantId();
  }

  @Override
  protected int getVersion(EntityType entityType, String entityName) throws IOException {
    try {
      Integer version = 1;
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectMaxVersionStatement(conn, entityType, entityName);
        try {
          ResultSet rs = statement.executeQuery();
          try {
            if (rs.next()) {
              version += rs.getInt(1);
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
      return version;
    } catch (SQLException e) {
      throw new IOException("Exception getting highest version of entity of type " + entityType.name().toLowerCase()
                              + " of name " + entityName + accountErrorSnippet);
    }
  }

  @Override
  protected byte[] getEntity(EntityType entityType, String entityName, int entityVersion) throws IOException {
    try {
      byte[] entityBytes = null;
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectStatement(conn, entityType, entityName, entityVersion);
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
    } catch (SQLException e) {
      throw new IOException("Exception getting entity of type " + entityType.name().toLowerCase()
                              + " of name " + entityName + " of version " + entityVersion  + accountErrorSnippet);
    }
  }

  @Override
  protected <T> Collection<T> getAllLatestEntities(EntityType entityType,
                                                   Function<byte[], T> transform) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      List<T> entities = Lists.newLinkedList();
      try {
        PreparedStatement statement = getSelectAllLatestStatement(conn, entityType);
        try {
          ResultSet rs = statement.executeQuery();
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
    } catch (SQLException e) {
      throw new IOException("Exception getting all entities of type "
                              + entityType.name().toLowerCase() + accountErrorSnippet);
    }
  }

  protected PreparedStatement getSelectMaxVersionStatement(Connection conn, EntityType entityType,
                                                           String entityName) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since everything is an enum or constant
    String query = "SELECT MAX(version) FROM " + entityTypeId + "s WHERE name=? AND tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(query);
    statement.setString(1, entityName);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  protected PreparedStatement getSelectStatement(Connection conn, EntityType entityType,
                                                 String entityName, int entityVersion) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since everything is an enum or constant
    StringBuilder queryStr = new StringBuilder();
    queryStr.append("SELECT ");
    queryStr.append(entityTypeId);
    queryStr.append(" FROM ");
    queryStr.append(entityTypeId);
    queryStr.append("s WHERE name=? AND tenant_id=? AND version=");
    if (entityVersion == Constants.FIND_MAX_VERSION) {
      queryStr.append(String.format("(SELECT MAX(version) FROM %ss WHERE name=? AND tenant_id=?)", entityTypeId));
    } else {
      queryStr.append("?");
    }
    String tenantId = (entityType == EntityType.AUTOMATOR_TYPE || entityType == EntityType.PROVIDER_TYPE) ?
      Constants.SUPERADMIN_TENANT : account.getTenantId();
    PreparedStatement statement = conn.prepareStatement(queryStr.toString());
    statement.setString(1, entityName);
    statement.setString(2, tenantId);
    if (entityVersion == Constants.FIND_MAX_VERSION) {
      statement.setString(3, entityName);
      statement.setString(4, tenantId);
    } else {
      statement.setInt(3, entityVersion);
    }
    return statement;
  }

  private PreparedStatement getSelectAllLatestStatement(Connection conn, EntityType entityType) throws SQLException {
    StringBuilder queryBuilder = new StringBuilder();
    // TODO: COOPR-684 - Handle latest version querying more robustly and efficiently
    queryBuilder.append("SELECT t.").append(entityType.getBlobColumn()).append(", t.name, t.version");
    queryBuilder.append(" FROM ").append(entityType.getTableName()).append(" t");
    queryBuilder.append(" INNER JOIN(");
    queryBuilder.append("   SELECT name, MAX(version) version");
    queryBuilder.append("   FROM ").append(entityType.getTableName());
    queryBuilder.append("   WHERE tenant_id=?");
    queryBuilder.append("   GROUP BY name");
    queryBuilder.append(" ) ss on t.name = ss.name AND t.version = ss.version");
    queryBuilder.append(" WHERE t.tenant_id=?");
    String queryString = queryBuilder.toString();

    // TODO: remove once types are defined through server instead of through provisioner
    // automator and provider types are constant across tenants and defined only in the superadmin tenant.
    String tenantId = (entityType == EntityType.AUTOMATOR_TYPE || entityType == EntityType.PROVIDER_TYPE) ?
      Constants.SUPERADMIN_TENANT : account.getTenantId();

    PreparedStatement statement = conn.prepareStatement(queryString);
    statement.setString(1, tenantId);
    statement.setString(2, tenantId);
    return statement;
  }
}
