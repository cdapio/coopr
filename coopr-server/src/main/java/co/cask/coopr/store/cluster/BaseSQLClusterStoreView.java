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
package co.cask.coopr.store.cluster;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterSummary;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBHelper;
import co.cask.coopr.store.DBPut;
import co.cask.coopr.store.DBQueryExecutor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Base abstract class for {@link ClusterStoreView} using a SQL database as the persistent store.
 * TODO: find a way to consolidate common code in subclasses.
 */
public abstract class BaseSQLClusterStoreView implements ClusterStoreView {
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  BaseSQLClusterStoreView(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
  }

  abstract PreparedStatement getSelectAllClustersStatement(Connection conn) throws SQLException;

  abstract PreparedStatement getSelectNonTerminatedClusters(Connection conn) throws SQLException;

  abstract PreparedStatement getSelectClusterStatement(Connection conn, long id) throws SQLException;

  abstract boolean allowedToWrite(Cluster cluster);

  abstract PreparedStatement getSetClusterStatement(
    Connection conn, long id, Cluster cluster, byte[] clusterBytes) throws SQLException;

  abstract PreparedStatement getClusterExistsStatement(Connection conn, long id) throws SQLException;

  abstract PreparedStatement getDeleteClusterStatement(Connection conn, long id) throws SQLException;

  abstract PreparedStatement getSelectClusterJobsStatement(Connection conn, long id) throws SQLException;

  abstract PreparedStatement getSelectAllClusterJobsStatement(Connection conn) throws SQLException;

  abstract PreparedStatement getSelectAllClusterJobsStatement(
    Connection conn, Set<Cluster.Status> states) throws SQLException;

  abstract PreparedStatement getSelectClusterNodesStatement(Connection conn, long id) throws SQLException;

  @Override
  public List<Cluster> getAllClusters() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectAllClustersStatement(conn);
        try {
          return dbQueryExecutor.getQueryList(statement, Cluster.class);
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
  public List<ClusterSummary> getAllClusterSummaries() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectAllClusterJobsStatement(conn);
        try {
          return getSummaries(statement);
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
  public List<ClusterSummary> getAllClusterSummaries(Set<Cluster.Status> states) throws IOException {
    if (states == null || states.isEmpty()) {
      return getAllClusterSummaries();
    }
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectAllClusterJobsStatement(conn, states);
        try {
          return getSummaries(statement);
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
  public List<Cluster> getNonTerminatedClusters() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectNonTerminatedClusters(conn);
        try {
          return dbQueryExecutor.getQueryList(statement, Cluster.class);
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
          return dbQueryExecutor.getQueryItem(statement, Cluster.class);
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
          return dbQueryExecutor.hasResults(statement);
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
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        byte[] clusterBytes = dbQueryExecutor.toBytes(cluster, Cluster.class);
        DBPut clusterPut = new ClusterDBPut(clusterNum, cluster, clusterBytes);
        clusterPut.executePut(conn);
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
          return dbQueryExecutor.getQueryList(statement, ClusterJob.class, limit);
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
          return dbQueryExecutor.getQuerySet(statement, Node.class);
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

  private PreparedStatement getInsertClusterStatement(
    Connection conn, long id, Cluster cluster, byte[] clusterBytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "INSERT INTO  clusters (cluster, owner_id, tenant_id, status, expire_time," +
        " create_time, name, id, latest_job_num) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    statement.setBytes(1, clusterBytes);
    statement.setString(2, cluster.getAccount().getUserId());
    statement.setString(3, cluster.getAccount().getTenantId());
    statement.setString(4, cluster.getStatus().name());
    statement.setTimestamp(5, DBHelper.getTimestamp(cluster.getExpireTime()));
    statement.setTimestamp(6, DBHelper.getTimestamp(cluster.getCreateTime()));
    statement.setString(7, cluster.getName());
    statement.setLong(8, id);
    String latestJobStr = cluster.getLatestJobId();
    long latestJobNum = latestJobStr == null ? 0 : JobId.fromString(latestJobStr).getJobNum();
    statement.setLong(9, latestJobNum);
    return statement;
  }

  private List<ClusterSummary> getSummaries(PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      List<ClusterSummary> summaries = Lists.newArrayList();
      while (rs.next()) {
        Cluster cluster = dbQueryExecutor.deserializeBlob(rs.getBlob(1), Cluster.class);
        ClusterJob clusterJob = dbQueryExecutor.deserializeBlob(rs.getBlob(2), ClusterJob.class);
        summaries.add(new ClusterSummary(cluster, clusterJob));
      }
      return ImmutableList.copyOf(summaries);
    } finally {
      rs.close();
    }
  }

  private class ClusterDBPut extends DBPut {
    private final long clusterId;
    private final Cluster cluster;
    private final byte[] clusterBytes;

    private ClusterDBPut(long clusterId, Cluster cluster, byte[] clusterBytes) {
      this.clusterId = clusterId;
      this.cluster = cluster;
      this.clusterBytes = clusterBytes;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      return getSetClusterStatement(conn, clusterId, cluster, clusterBytes);
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      return getInsertClusterStatement(conn, clusterId, cluster, clusterBytes);
    }
  }

  protected void setInClause(PreparedStatement statement, Set<Cluster.Status> states, int startIndex)
    throws SQLException {
    int i = startIndex;
    for (Cluster.Status state : states) {
      statement.setString(i, state.name());
      i++;
    }
  }
}
