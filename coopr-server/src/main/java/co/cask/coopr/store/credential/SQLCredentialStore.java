/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.store.credential;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBPut;
import com.google.inject.Inject;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL DB backed implementation of {@link EncryptedCredentialStore}.
 */
public class SQLCredentialStore extends EncryptedCredentialStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLCredentialStore.class);
  private final DBConnectionPool dbConnectionPool;

  @Inject
  private SQLCredentialStore(DBConnectionPool dbConnectionPool, Configuration conf)
    throws GeneralSecurityException, IOException, DecoderException {
    super(conf);
    this.dbConnectionPool = dbConnectionPool;
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBHelper.createDerbyTableIfNotExists(
        "CREATE TABLE sensitiveFields ( " +
          "tenant_id VARCHAR(64), " +
          "cluster_id VARCHAR(255), " +
          "fields BLOB )", dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  void setValue(String tenantId, String clusterId, byte[] val) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut profilePut = new FieldsDBPut(tenantId, clusterId, val);
        profilePut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception writing sensitive fields for tenant {} and cluster {}.", tenantId, clusterId, e);
      throw new IOException(e);
    }
  }

  @Override
  byte[] getValue(String tenantId, String clusterId) throws IOException {
    try {
      byte[] bytes = null;
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("SELECT fields FROM sensitiveFields WHERE tenant_id=? AND cluster_id=?");
        try {
          statement.setString(1, tenantId);
          statement.setString(2, clusterId);
          ResultSet rs = statement.executeQuery();
          try {
            if (rs.next()) {
              bytes = rs.getBytes(1);
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
      return bytes;
    } catch (SQLException e) {
      LOG.error("Exception getting sensitive fields for tenant {} and cluster {}.", tenantId, clusterId, e);
      throw new IOException(e);
    }
  }

  @Override
  public void wipe(String tenantId, String clusterId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("DELETE FROM sensitiveFields WHERE tenant_id=? AND cluster_id=?");
        try {
          statement.setString(1, tenantId);
          statement.setString(2, clusterId);
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting sensitive fields for tenant {} and cluster {}.", tenantId, clusterId, e);
      throw new IOException(e);
    }
  }

  @Override
  public void wipe() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM sensitiveFields");
        try {
          stmt.executeUpdate();
        } finally {
          stmt.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception wiping credential store.", e);
      throw new IOException(e);
    }
  }

  private class FieldsDBPut extends DBPut {
    private final String tenantId;
    private final String clusterId;
    private final byte[] fields;

    private FieldsDBPut(String tenantId, String clusterId, byte[] fields) {
      this.tenantId = tenantId;
      this.clusterId = clusterId;
      this.fields = fields;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement =
        conn.prepareStatement("UPDATE sensitiveFields SET fields=? WHERE tenant_id=? AND cluster_id=?");
      statement.setBytes(1, fields);
      statement.setString(2, tenantId);
      statement.setString(3, clusterId);
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO sensitiveFields (tenant_id, cluster_id, fields) VALUES (?, ?, ?)");
      statement.setString(1, tenantId);
      statement.setString(2, clusterId);
      statement.setBytes(3, fields);
      return statement;
    }
  }
}
