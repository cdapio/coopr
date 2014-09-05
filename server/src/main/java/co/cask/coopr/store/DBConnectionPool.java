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
package co.cask.coopr.store;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import com.google.inject.Inject;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connection pool for databases using JDBC.
 */
public class DBConnectionPool {
  private static final Logger LOG  = LoggerFactory.getLogger(DBConnectionPool.class);
  private final DataSource datasource;
  private final boolean isEmbeddedDerbyDB;

  @Inject
  private DBConnectionPool(Configuration conf) throws SQLException {
    String driverClass = conf.get(Constants.JDBC_DRIVER);
    String connectionString = conf.get(Constants.JDBC_CONNECTION_STRING);
    String dbUser = conf.get(Constants.DB_USER);
    String dbPassword = conf.get(Constants.DB_PASSWORD);
    String validationQuery = conf.get(Constants.DB_VALIDATION_QUERY);
    int maxConnections = conf.getInt(Constants.DB_MAX_ACTIVE_CONNECTIONS);

    if (driverClass == null || connectionString == null) {
      String localDataDir = conf.get(Constants.LOCAL_DATA_DIR);
      connectionString = "jdbc:derby:" + localDataDir + "/db/coopr;create=true";
      driverClass = Constants.EMBEDDED_DERBY_DRIVER;
      validationQuery = "VALUES 1";

      LOG.warn("{} or {} was not specified, defaulting to JDBC driver {} and connection string {}",
               Constants.JDBC_DRIVER, Constants.JDBC_CONNECTION_STRING, driverClass, connectionString);
    }

    if (dbPassword == null) {
      LOG.warn(Constants.DB_PASSWORD + " was not specified");
    }

    Properties properties = new Properties();
    properties.put("autoReconnect", "true");

    PoolProperties poolProperties = new PoolProperties();
    poolProperties.setUrl(connectionString);
    poolProperties.setDriverClassName(driverClass);
    poolProperties.setUsername(dbUser);
    poolProperties.setPassword(dbPassword);
    poolProperties.setJmxEnabled(true);
    poolProperties.setValidationQuery(validationQuery);

    if (validationQuery == null) {
      LOG.warn("JDBC validation query is null, no tests will be performed on JDBC connections.");
      poolProperties.setTestWhileIdle(false);
      poolProperties.setTestOnBorrow(false);
      poolProperties.setTestOnReturn(false);
    } else {
      poolProperties.setTestWhileIdle(false);
      poolProperties.setTestOnBorrow(true);
      poolProperties.setTestOnReturn(false);
    }

    poolProperties.setValidationInterval(30000);
    poolProperties.setTimeBetweenEvictionRunsMillis(30000);
    poolProperties.setMaxActive(maxConnections);
    poolProperties.setInitialSize(10 > maxConnections ? maxConnections : 10);
    poolProperties.setMaxWait(10000);
    poolProperties.setRemoveAbandonedTimeout(60);
    poolProperties.setMinEvictableIdleTimeMillis(30000);
    poolProperties.setMinIdle(10);
    poolProperties.setLogAbandoned(false);
    poolProperties.setRemoveAbandoned(true);
    poolProperties.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;" +
                                         "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
    poolProperties.setDbProperties(properties);

    this.datasource = new DataSource();
    datasource.setPoolProperties(poolProperties);
    this.isEmbeddedDerbyDB = driverClass.equals(Constants.EMBEDDED_DERBY_DRIVER);
  }

  /**
   * Returns whether or not the connection pool is connected to an embedded Derby db.
   *
   * @return true if connected to an embedded derby db, false if not.
   */
  public boolean isEmbeddedDerbyDB() {
    return isEmbeddedDerbyDB;
  }

  /**
   * Get a {@link Connection} from the pool with auto commit on.
   *
   * @return Connection from the pool with autocommit.
   * @throws SQLException
   */
  public Connection getConnection() throws SQLException {
    return getConnection(true);
  }

  /**
   * Get a {@link Connection} from the pool with auto commit set to the given value.
   *
   * @param autoCommit Whether or not autoCommit should be set.
   * @return Connection from the pool.
   * @throws SQLException
   */
  public Connection getConnection(boolean autoCommit) throws SQLException {
    Connection conn = datasource.getConnection();
    conn.setAutoCommit(autoCommit);
    return conn;
  }
}
