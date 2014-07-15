package com.continuuity.loom.store;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
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
      DriverManager.getConnection("jdbc:derby:memory:loom;drop=true");
    } catch (SQLException e) {
      // this is normal when a drop happens
      if (!e.getSQLState().equals("08006")) {
        Throwables.propagate(e);
      }
    }
  }

  // mysql will error if you give it a timestamp of 0...
  public static Timestamp getTimestamp(long ts) {
    return ts > 0 ? new Timestamp(ts) : null;
  }
}
