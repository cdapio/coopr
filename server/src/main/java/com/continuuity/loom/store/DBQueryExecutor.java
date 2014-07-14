package com.continuuity.loom.store;

import com.continuuity.loom.cluster.Cluster;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
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
 * Executes prepared statements against databases while taking care of serialization/deserialization of blobs.
 */
public final class DBQueryExecutor {
  private final Gson gson;

  @Inject
  private DBQueryExecutor(Gson gson) {
    this.gson = gson;
  }

  /**
   * Queries the store for a set of items, deserializing the items and returning an immutable set of them. If no items
   * exist, the set will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution. Will be closed by this method.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @return
   * @throws java.sql.SQLException
   */
  public <T> ImmutableSet<T> getQuerySet(PreparedStatement statement, Class<T> clazz) throws SQLException {
    try {
      ResultSet rs = statement.executeQuery();
      try {
        Set<T> results = Sets.newHashSet();
        while (rs.next()) {
          Blob blob = rs.getBlob(1);
          results.add(deserializeBlob(blob, clazz));
        }
        return ImmutableSet.copyOf(results);
      } finally {
        rs.close();
      }
    } finally {
      statement.close();
    }
  }

  public <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz) throws SQLException {
    return getQueryList(statement, clazz, Integer.MAX_VALUE);
  }

  /**
   * Queries the store for a list of items, deserializing the items and returning an immutable list of them. If no items
   * exist, the list will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @param limit Max number of items to get.
   * @return Immutable list of objects that were queried for.
   * @throws java.sql.SQLException
   */
  public <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz, int limit) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      List<T> results = Lists.newArrayList();
      int numResults = 0;
      int actualLimit = limit < 0 ? Integer.MAX_VALUE : limit;
      while (rs.next() && numResults < actualLimit) {
        Blob blob = rs.getBlob(1);
        results.add(deserializeBlob(blob, clazz));
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
   * @param <T> Type of the item being queried.
   * @return
   * @throws java.sql.SQLException
   */
  public <T> T getQueryItem(PreparedStatement statement, Class<T> clazz) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      if (rs.next()) {
        Blob blob = rs.getBlob(1);
        return deserializeBlob(blob, clazz);
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
   * @throws java.sql.SQLException
   */
  public boolean hasResults(PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      return rs.next();
    } finally {
      rs.close();
    }
  }

  public <T> T deserializeBlob(Blob blob, Class<T> clazz) throws SQLException {
    Reader reader = new InputStreamReader(blob.getBinaryStream(), Charsets.UTF_8);
    T object;
    try {
      object = gson.fromJson(reader, clazz);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return object;
  }

  public <T> ByteArrayInputStream toByteStream(T object, Type type) {
    return new ByteArrayInputStream(gson.toJson(object, type).getBytes(Charsets.UTF_8));
  }
}
