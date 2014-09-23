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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.inject.Inject;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
   * exist, the set will be empty. Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @return Immutable set of items queried for.
   * @throws java.sql.SQLException
   */
  public <T> ImmutableSet<T> getQuerySet(PreparedStatement statement, Class<T> clazz) throws SQLException {
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
  }

  /**
   * Queries the store for a list of items, deserializing the items and returning an immutable list of them. If no items
   * exist, the list will be empty. Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @return Immutable list of objects that were queried for.
   * @throws java.sql.SQLException
   */
  public <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz) throws SQLException {
    return getQueryList(statement, clazz, Integer.MAX_VALUE);
  }

  /**
   * Queries the store for a list of at most limit items, deserializing the items and returning an immutable
   * list of them. If no items exist, the list will be empty. Statement passed in must be closed by the caller.
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
   * Queries the store for a list of at most limit items, deserializing the items and performing a transform before
   * before placing the transformed object into the output, finally returning an immutable list of transformed items.
   * If no items exist, the list will be empty. Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried in the statement.
   * @param transform Transform to perform to change the deserialized object in the table to the object in the output.
   * @param <F> Type of item being queried
   * @param <T> Type of item to return
   * @return Immutable list of transformed objects that were queried for.
   * @throws SQLException
   */
  public <F, T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<F> clazz,
                                              Function<F, T> transform) throws SQLException {
    return getQueryList(statement, clazz, transform, Integer.MAX_VALUE);
  }

  /**
   * Queries the store for a list of at most limit items, deserializing the items and performing a transform before
   * before placing the transformed object into the output, finally returning an immutable list of transformed items.
   * If no items exist, the list will be empty. Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried in the statement.
   * @param transform Transform to perform to change the deserialized object in the table to the object in the output.
   * @param limit Max number of items to get.
   * @param <F> Type of item being queried
   * @param <T> Type of item to return
   * @return Immutable list of transformed objects that were queried for.
   * @throws SQLException
   */
  public <F, T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<F> clazz,
                                              Function<F, T> transform, int limit) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      List<T> results = Lists.newArrayList();
      int numResults = 0;
      int actualLimit = limit < 0 ? Integer.MAX_VALUE : limit;
      while (rs.next() && numResults < actualLimit) {
        Blob blob = rs.getBlob(1);
        results.add(transform.apply(deserializeBlob(blob, clazz)));
        numResults++;
      }
      return ImmutableList.copyOf(results);
    } finally {
      rs.close();
    }
  }

  /**
   * Queries the store for a single item, deserializing the item and returning it or null if the item does not exist.
   * Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the item being queried.
   * @param <T> Type of the item being queried.
   * @return Item queried for, or null if none exists.
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
   * Queries for a single number, returning the value of the number or 0 if there are no results.
   * Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @return Result of the query, or 0 if no results.
   * @throws SQLException
   */
  public int getNum(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      if (!results.next()) {
        return 0;
      } else {
        return results.getInt(1);
      }
    } finally {
      results.close();
    }
  }

  /**
   * Queries for a single string, returning the value of the string or null if there are no results.
   * Statement passed in must be closed by the caller.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @return Result of the query, or null if no results.
   * @throws SQLException
   */
  public String getString(PreparedStatement statement) throws SQLException {
    ResultSet results = statement.executeQuery();
    try {
      if (!results.next()) {
        return null;
      } else {
        return results.getString(1);
      }
    } finally {
      results.close();
    }
  }

  /**
   * Performs the query and returns whether or not there are results. Statement passed in must be closed by the caller.
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

  /**
   * Deserialize a blob into an object. Assumes blob was serialized json.
   *
   * @param blob Blob to deserialize.
   * @param clazz Class of the object to deserialize the blob into.
   * @param <T> Type of the object to deserialize.
   * @return Deserialized object.
   * @throws SQLException
   */
  public <T> T deserializeBlob(Blob blob, Class<T> clazz) throws SQLException {
    if (blob == null) {
      return null;
    }
    Reader reader = new InputStreamReader(blob.getBinaryStream(), Charsets.UTF_8);
    T object;
    try {
      object = gson.fromJson(reader, clazz);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return object;
  }

  /**
   * Serialize the given object into json, then into bytes of that json.
   *
   * @param object Object to serialize.
   * @param type Type of the object to serialize.
   * @param <T> Type of the object to serialize.
   * @return Object as bytes.
   */
  public <T> byte[] toBytes(T object, Type type) {
    return gson.toJson(object, type).getBytes(Charsets.UTF_8);
  }
}
