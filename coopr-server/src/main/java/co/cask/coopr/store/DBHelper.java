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

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Helper methods for performing database administrative operations.
 */
public final class DBHelper {

  /**
   * Try and create a table if it doesn't already exist. Returns whether or not the table was created.
   *
   * @param createString String for creating the table.
   * @param connectionPool Connection pool to use to create the table.
   * @return Whether or not a table was created.
   * @throws SQLException
   */
  public static boolean createDerbyTableIfNotExists(String createString, DBConnectionPool connectionPool)
    throws SQLException {
    Connection conn = connectionPool.getConnection();
    try {
      Statement statement = conn.createStatement();
      try {
        statement.executeUpdate(createString);
        return true;
      } catch (SQLException e) {
        // code for the table already exists in derby. Since there is no create if not exists, we check the state
        // and if the table already exists, we return false.  But if there was some other exception we'll throw that.
        if (!e.getSQLState().equals("X0Y32")) {
          throw e;
        }
        return false;
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Create an index on given columns in a table for an embedded derby table.
   *
   * @param dbConnectionPool Connection pool to use to create the index.
   * @param indexName Name of the index.
   * @param table Name of the table to create the index on.
   * @param columns Columns of the table to index.
   * @throws SQLException
   */
  public static void createDerbyIndex(DBConnectionPool dbConnectionPool,
                                      String indexName, String table, String... columns) throws SQLException {
    String statementStr = "CREATE INDEX " + indexName + " ON " + table + "(" + Joiner.on(',').join(columns) + ")";
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement statement = conn.createStatement();
      try {
        statement.executeUpdate(statementStr);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Drop all embedded derby dbs.
   */
  public static void dropDerbyDB() {
    try {
      DriverManager.getConnection("jdbc:derby:memory:coopr;drop=true");
    } catch (SQLException e) {
      // this is normal when a drop happens
      if (!e.getSQLState().equals("08006")) {
        Throwables.propagate(e);
      }
    }
  }

  /**
   * Get a {@link Timestamp} for the given timestamp in milliseconds. Returns null if the timestamp is less than 1,
   * as some databases will error if you try and use a timestamp of 0.
   *
   * @param ts timestamp to convert
   * @return A timestamp corresponding to the given input, or null if the input is invalid.
   */
  public static Timestamp getTimestamp(long ts) {
    return ts > 0 ? new Timestamp(ts) : null;
  }

  /**
   * Create a string for an IN clause in a sql query given the number of arguments that will be in the list.
   *
   * @param numArgs number of arguments in the IN clause
   * @return String representing the parameterized IN clause for a sql query.
   */
  public static String createInString(int numArgs) {
    if (numArgs < 1) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    for (int i = 0; i < numArgs - 1; i++) {
      builder.append("?,");
    }
    builder.append("?)");
    return builder.toString();
  }
}
