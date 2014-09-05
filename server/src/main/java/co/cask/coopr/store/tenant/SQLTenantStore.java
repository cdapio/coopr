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
package co.cask.coopr.store.tenant;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBPut;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of {@link TenantStore} using a SQL database as the persistent store.
 */
public class SQLTenantStore extends AbstractIdleService implements TenantStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLTenantStore.class);

  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;
  private final ConcurrentMap<String, String> idToNameMap;

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement stmt = conn.prepareStatement("DELETE FROM tenants WHERE id<>?");
      try {
        stmt.setString(1, Constants.SUPERADMIN_TENANT);
        stmt.executeUpdate();
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
    this.idToNameMap = Maps.newConcurrentMap();
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
    // add superadmin if it doesn't exist
    Tenant superadminTenant = getTenantByName(Constants.SUPERADMIN_TENANT);
    if (superadminTenant == null) {
      writeTenant(Tenant.DEFAULT_SUPERADMIN);
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
        DBPut tenantPut = new TenantDBPut(tenant, dbQueryExecutor.toBytes(tenant, Tenant.class));
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

  /**
   * Get the name of the tenant with the given id. Id to name mapping is cached, so this will not involve an io
   * operation except for the first time it is called.
   *
   * @param id Id of the tenant to get the name for
   * @return Name of the tenant with the given id, or null if the tenant does not exist
   * @throws IOException
   */
  @Override
  public String getNameForId(String id) throws IOException {
    if (idToNameMap.containsKey(id)) {
      return idToNameMap.get(id);
    }
    String name = getNameFromDB(id);
    // if there is no tenant, return null
    if (name == null) {
      return null;
    }
    // cache the result since id to name will never change
    idToNameMap.put(id, name);
    return name;
  }

  private String getNameFromDB(String id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT name FROM tenants WHERE id=? AND deleted=false ");
        try {
          statement.setString(1, id);
          return dbQueryExecutor.getString(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting name of tenant {}", id);
      throw new IOException(e);
    }
  }

  private class TenantDBPut extends DBPut {
    private final Tenant tenant;
    private final byte[] tenantBytes;

    private TenantDBPut(Tenant tenant, byte[] tenantBytes) {
      this.tenant = tenant;
      this.tenantBytes = tenantBytes;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE tenants SET tenant=?, workers=? WHERE id=?");
      statement.setBytes(1, tenantBytes);
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
      statement.setBytes(4, tenantBytes);
      statement.setTimestamp(5, DBHelper.getTimestamp(System.currentTimeMillis()));
      return statement;
    }
  }
}
