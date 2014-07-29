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
package com.continuuity.loom.store.tenant;

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.admin.TenantSpecification;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.DBPut;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Implementation of {@link TenantStore} using a SQL database as the persistent store.
 */
public class SQLTenantStore extends AbstractIdleService implements TenantStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLTenantStore.class);

  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM tenants");
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Inject
  SQLTenantStore(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor)
    throws SQLException, ClassNotFoundException {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBHelper.createDerbyTableIfNotExists(
        "CREATE TABLE tenants ( " +
          "id VARCHAR(255), " +
          "name VARCHAR(255), " +
          "workers INT, " +
          "deleted BOOLEAN, " +
          "create_time TIMESTAMP, " +
          "delete_time TIMESTAMP, " +
          "tenant BLOB )", dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public Tenant getTenantByID(String id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT tenant FROM tenants WHERE id=? AND deleted=false");
        statement.setString(1, id);
        try {
          return dbQueryExecutor.getQueryItem(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting tenant with id {}", id, e);
      throw new IOException(e);
    }
  }

  @Override
  public Tenant getTenantByName(String name) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT tenant FROM tenants WHERE name=? AND deleted=false");
        statement.setString(1, name);
        try {
          return dbQueryExecutor.getQueryItem(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting tenant with name {}", name, e);
      throw new IOException(e);
    }
  }

  @Override
  public List<Tenant> getAllTenants() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT tenant FROM tenants WHERE deleted=false");
        try {
          return dbQueryExecutor.getQueryList(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting all tenants", e);
      throw new IOException(e);
    }
  }

  @Override
  public List<TenantSpecification> getAllTenantSpecifications() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT tenant FROM tenants WHERE deleted=false");
        try {
          return dbQueryExecutor.getQueryList(statement, Tenant.class,
                                              new Function<Tenant, TenantSpecification>() {
                                                @Override
                                                public TenantSpecification apply(Tenant input) {
                                                  return input.getSpecification();
                                                }
                                              });
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting all tenants", e);
      throw new IOException(e);
    }
  }

  @Override
  public void writeTenant(Tenant tenant) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut tenantPut = new TenantDBPut(tenant, dbQueryExecutor.toByteStream(tenant, Tenant.class));
        tenantPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception writing tenant {}", tenant);
      throw new IOException(e);
    }
  }

  @Override
  public void deleteTenantByName(String name) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "UPDATE tenants SET deleted=true, delete_time=? WHERE name=? ");
        statement.setTimestamp(1, DBHelper.getTimestamp(System.currentTimeMillis()));
        statement.setString(2, name);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting tenant {}", name);
      throw new IOException(e);
    }
  }

  private class TenantDBPut extends DBPut {
    private final Tenant tenant;
    private final ByteArrayInputStream tenantBytes;

    private TenantDBPut(Tenant tenant, ByteArrayInputStream tenantBytes) {
      this.tenant = tenant;
      this.tenantBytes = tenantBytes;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE tenants SET tenant=?, workers=? WHERE id=?");
      statement.setBlob(1, tenantBytes);
      statement.setInt(2, tenant.getSpecification().getWorkers());
      statement.setString(3, tenant.getId());
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO tenants (id, name, workers, tenant, create_time, delete_time, deleted) " +
          "VALUES (?, ?, ?, ?, ?, null, false)");
      statement.setString(1, tenant.getId());
      statement.setString(2, tenant.getSpecification().getName());
      statement.setInt(3, tenant.getSpecification().getWorkers());
      statement.setBlob(4, tenantBytes);
      statement.setTimestamp(5, DBHelper.getTimestamp(System.currentTimeMillis()));
      return statement;
    }
  }
}
