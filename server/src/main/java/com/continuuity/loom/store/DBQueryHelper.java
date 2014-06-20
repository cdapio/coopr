package com.continuuity.loom.store;

import com.continuuity.loom.codec.json.JsonSerde;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;

import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

/**
 * Helper for executing sql statements for getting collections of items that should been deserialized.
 */
public final class DBQueryHelper {

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
          throw new SQLException(e);
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

  /**
   * Queries the store for a set of items, deserializing the items and returning an immutable set of them. If no items
   * exist, the set will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution. Will be closed by this method.
   * @param clazz Class of the items being queried.
   * @param codec Codec used to deserialize item.
   * @param <T> Type of the items being queried.
   * @return
   * @throws java.sql.SQLException
   */
  public static <T> ImmutableSet<T> getQuerySet(PreparedStatement statement,
                                                Class<T> clazz, JsonSerde codec) throws SQLException {
    try {
      ResultSet rs = statement.executeQuery();
      try {
        Set<T> results = Sets.newHashSet();
        while (rs.next()) {
          Blob blob = rs.getBlob(1);
          results.add(deserializeBlob(blob, clazz, codec));
        }
        return ImmutableSet.copyOf(results);
      } finally {
        rs.close();
      }
    } finally {
      statement.close();
    }
  }

  public static <T> ImmutableList<T> getQueryList(PreparedStatement statement,
                                                  Class<T> clazz, JsonSerde codec) throws SQLException {
    return getQueryList(statement, clazz, codec, Integer.MAX_VALUE);
  }

  /**
   * Queries the store for a list of items, deserializing the items and returning an immutable list of them. If no items
   * exist, the list will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried.
   * @param codec Codec used to deserialize item.
   * @param <T> Type of the items being queried.
   * @param limit Max number of items to get.
   * @return
   * @throws SQLException
   */
  public static <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz,
                                                  JsonSerde codec, int limit) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      List<T> results = Lists.newArrayList();
      int numResults = 0;
      int actualLimit = limit < 0 ? Integer.MAX_VALUE : limit;
      while (rs.next() && numResults < actualLimit) {
        Blob blob = rs.getBlob(1);
        results.add(deserializeBlob(blob, clazz, codec));
        numResults++;
      }
      return ImmutableList.copyOf(results);
    } finally {
      rs.close();
    }
  }

  /**
   * Queries the store for a single item, deserializing the item and returning it or null if the item does not exist.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the item being queried.
   * @param codec Codec used to deserialize item.
   * @param <T> Type of the item being queried.
   * @return
   * @throws SQLException
   */
  public static <T> T getQueryItem(PreparedStatement statement, Class<T> clazz, JsonSerde codec) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      if (rs.next()) {
        Blob blob = rs.getBlob(1);
        return deserializeBlob(blob, clazz, codec);
      } else {
        return null;
      }
    } finally {
      rs.close();
    }
  }

  /**
   * Performs the query and returns whether or not there are results.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @return True if the query has results, false if not.
   * @throws SQLException
   */
  public static boolean hasResults(PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      return rs.next();
    } finally {
      rs.close();
    }
  }

  public static <T> T deserializeBlob(Blob blob, Class<T> clazz, JsonSerde codec) throws SQLException {
    Reader reader = new InputStreamReader(blob.getBinaryStream(), Charsets.UTF_8);
    T object;
    try {
      object = codec.deserialize(reader, clazz);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return object;
  }

  // mysql will error if you give it a timestamp of 0...
  public static Timestamp getTimestamp(long ts) {
    return ts > 0 ? new Timestamp(ts) : null;
  }
}
