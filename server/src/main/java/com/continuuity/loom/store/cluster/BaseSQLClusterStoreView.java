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
package com.continuuity.loom.store.cluster;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Base abstract class for {@link ClusterStoreView} using a SQL database as the persistent store.
 */
public abstract class BaseSQLClusterStoreView implements ClusterStoreView {
  private final DBConnectionPool dbConnectionPool;
  private static final JsonSerde codec = new JsonSerde();

  BaseSQLClusterStoreView(DBConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  abstract PreparedStatement getSelectAllClustersStatement(Connection conn) throws SQLException;

  abstract PreparedStatement getSelectClusterStatement(Connection conn, long id) throws SQLException;

  abstract boolean allowedToWrite(Cluster cluster);

  abstract PreparedStatement getSetClusterStatement(
    Connection conn, long id, Cluster cluster, ByteArrayInputStream clusterBytes) throws SQLException;

  abstract PreparedStatement getInsertClusterStatement(
    Connection conn, long id, Cluster cluster, ByteArrayInputStream clusterBytes) throws SQLException;

  abstract PreparedStatement getClusterExistsStatement(Connection conn, long id) throws SQLException;

  abstract PreparedStatement getDeleteClusterStatement(Connection conn, long id) throws SQLException;

  abstract PreparedStatement getSelectClusterJobsStatement(Connection conn, long id) throws SQLException;

  abstract PreparedStatement getSelectClusterNodesStatement(Connection conn, long id) throws SQLException;

  @Override
  public List<Cluster> getAllClusters() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectAllClustersStatement(conn);
        try {
          return DBQueryHelper.getQueryList(statement, Cluster.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting all clusters");
    }
  }

  @Override
  public Cluster getCluster(String clusterId) throws IOException {
    long clusterNum = Long.parseLong(clusterId);
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectClusterStatement(conn, clusterNum);
        try {
          return DBQueryHelper.getQueryItem(statement, Cluster.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting cluster " + clusterId, e);
    }
  }

  @Override
  public boolean clusterExists(String clusterId) throws IOException {
    long clusterNum = Long.parseLong(clusterId);
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getClusterExistsStatement(conn, clusterNum);
        try {
          ResultSet rs = statement.executeQuery();
          try {
            return rs.next();
          } finally {
            rs.close();
          }
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception checking existence of cluster " + clusterId, e);
    }
  }

  @Override
  public void writeCluster(Cluster cluster) throws IllegalAccessException, IOException {
    if (!allowedToWrite(cluster)) {
      throw new IllegalAccessException("Not allowed to write cluster " + cluster.getId());
    }
    long clusterNum = Long.parseLong(cluster.getId());
    // sticking with standard sql... this could be done in one step with replace, or with
    // insert ... on duplicate key update with mysql.
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement writeStatement;
        ByteArrayInputStream clusterBytes = new ByteArrayInputStream(codec.serialize(cluster, Cluster.class));
        if (clusterExists(cluster.getId())) {
          writeStatement = getSetClusterStatement(conn, clusterNum, cluster, clusterBytes);
        } else {
          writeStatement = getInsertClusterStatement(conn, clusterNum, cluster, clusterBytes);
        }

        // perform the update or insert
        try {
          writeStatement.executeUpdate();
        } finally {
          writeStatement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception writing cluster " + cluster.getId(), e);
    }
  }

  @Override
  public void deleteCluster(String clusterId) throws IOException {
    long clusterNum = Long.parseLong(clusterId);
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getDeleteClusterStatement(conn, clusterNum);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception deleting cluster " + clusterId, e);
    }
  }

  @Override
  public List<ClusterJob> getClusterJobs(String clusterId, int limit) throws IOException {
    long clusterNum = Long.parseLong(clusterId);
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectClusterJobsStatement(conn, clusterNum);

        try {
          return DBQueryHelper.getQueryList(statement, ClusterJob.class, limit);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting cluster jobs for cluster " + clusterId + " with limit " + limit, e);
    }
  }

  @Override
  public Set<Node> getClusterNodes(String clusterId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      long clusterNum = Long.parseLong(clusterId);
      try {
        PreparedStatement statement = getSelectClusterNodesStatement(conn, clusterNum);
        try {
          return DBQueryHelper.getQuerySet(statement, Node.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting nodes for cluster " + clusterId, e);
    }
  }
}
