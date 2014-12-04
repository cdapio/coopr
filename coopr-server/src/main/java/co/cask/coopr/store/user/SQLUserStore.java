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
package co.cask.coopr.store.user;

import co.cask.coopr.account.Account;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBPut;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * SQL database backed implementation of {@link UserStore}.
 */
public class SQLUserStore extends AbstractIdleService implements UserStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLUserStore.class);
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  @Inject
  private SQLUserStore(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
  }

  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement stmt = conn.prepareStatement("DELETE FROM users");
      try {
        stmt.executeUpdate();
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBHelper.createDerbyTableIfNotExists(
        "CREATE TABLE users ( " +
          "user_id VARCHAR(256), " +
          "tenant_id VARCHAR(64), " +
          "profile BLOB )", dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public Map<String, Object> getProfile(Account account) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("SELECT profile FROM users WHERE user_id=? AND tenant_id=?");
        try {
          statement.setString(1, account.getUserId());
          statement.setString(2, account.getTenantId());
          return dbQueryExecutor.getQueryItem(statement, Map.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting profile for account {}.", account, e);
      throw new IOException(e);
    }
  }

  @Override
  public void writeProfile(Account account, Map<String, Object> profile) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        DBPut profilePut = new ProfileDBPut(account, profile);
        profilePut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception writing profile for account {}.", account, e);
      throw new IOException(e);
    }
  }

  @Override
  public void deleteProfile(Account account) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("UPDATE users SET profile=null WHERE user_id=? AND tenant_id=?");
        try {
          statement.setString(1, account.getUserId());
          statement.setString(2, account.getTenantId());
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting profile for account {}.", account, e);
      throw new IOException(e);
    }
  }

  private class ProfileDBPut extends DBPut {
    private final Account account;
    private final Map<String, Object> profile;

    private ProfileDBPut(Account account, Map<String, Object> profile) {
      this.account = account;
      this.profile = profile;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement("UPDATE users SET profile=? WHERE user_id=? AND tenant_id=?");
      statement.setBytes(1, dbQueryExecutor.toBytes(profile, new TypeToken<Map<String, Object>>() { }.getType()));
      statement.setString(2, account.getUserId());
      statement.setString(3, account.getTenantId());
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO users (user_id, tenant_id, profile) VALUES (?, ?, ?)");
      statement.setString(1, account.getUserId());
      statement.setString(2, account.getTenantId());
      statement.setBytes(3, dbQueryExecutor.toBytes(profile, new TypeToken<Map<String, Object>>() { }.getType()));
      return statement;
    }
  }
}
