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
package com.continuuity.loom.store;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskException;
import com.continuuity.loom.scheduler.task.TaskId;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.twill.zookeeper.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ClusterStore} using a SQL database as the persistent store.
 */
public class SQLClusterStore extends BaseClusterStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLClusterStore.class);
  private static final JsonSerde codec = new JsonSerde();

  private final DBConnectionPool dbConnectionPool;

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM clusters");
        stmt = conn.createStatement();
        stmt.execute("DELETE FROM jobs");
        stmt = conn.createStatement();
        stmt.execute("DELETE FROM tasks");
        stmt = conn.createStatement();
        stmt.execute("DELETE FROM nodes");
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Inject
  SQLClusterStore(ZKClient zkClient, DBConnectionPool dbConnectionPool,
                  @Named(Constants.ID_START_NUM) long startId,
                  @Named(Constants.ID_INCREMENT_BY) long incrementBy) throws SQLException, ClassNotFoundException {
    super(zkClient, startId, incrementBy);
    this.dbConnectionPool = dbConnectionPool;

    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      initDerbyDB();
    }
  }

  public void initDerbyDB() throws SQLException {
    LOG.warn("Initializing Derby DB... Tables are not optimized for performance.");

    createDerbyTable("CREATE TABLE clusters ( id BIGINT, owner_id VARCHAR(255), name VARCHAR(255)," +
                       " create_time TIMESTAMP, expire_time TIMESTAMP, status VARCHAR(32), cluster BLOB )");
    createDerbyTable("CREATE TABLE jobs ( cluster_id BIGINT, job_num BIGINT, status VARCHAR(32)," +
                       " create_time TIMESTAMP, job BLOB)");
    createDerbyTable("CREATE TABLE tasks ( cluster_id BIGINT, job_num BIGINT, task_num BIGINT, submit_time TIMESTAMP,"
                       + " status_time TIMESTAMP, status VARCHAR(32), task BLOB )");
    createDerbyTable("CREATE TABLE nodes ( cluster_id BIGINT, id VARCHAR(64), node BLOB )");
  }

  private void createDerbyTable(String createString) throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement statement = conn.createStatement();
      try {
        statement.executeUpdate(createString);
      } catch (SQLException e) {
        // code for the table already exists in derby.
        if (!e.getSQLState().equals("X0Y32")) {
          throw Throwables.propagate(e);
        }
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public List<Cluster> getAllClusters() throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT cluster FROM clusters ORDER BY create_time DESC");
      try {
        return getQueryList(statement, Cluster.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public List<Cluster> getAllClusters(String ownerId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement =
        conn.prepareStatement("SELECT cluster FROM clusters WHERE owner_id=? ORDER BY create_time DESC");
      statement.setString(1, ownerId);
      try {
        return getQueryList(statement, Cluster.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public Cluster getCluster(String clusterId) throws Exception {
    long clusterNum = Long.parseLong(clusterId);
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT cluster FROM clusters WHERE id=? ");
      statement.setLong(1, clusterNum);
      try {
        return getQueryItem(statement, Cluster.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public Cluster getCluster(String clusterId, String ownerId) throws Exception {
    long clusterNum = Long.parseLong(clusterId);
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT cluster FROM clusters WHERE id=? AND owner_id=?");
      statement.setLong(1, clusterNum);
      statement.setString(2, ownerId);
      try {
        return getQueryItem(statement, Cluster.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public boolean clusterExists(String clusterId) throws Exception {
    long clusterNum = Long.parseLong(clusterId);
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT id FROM clusters WHERE id=?");
      statement.setLong(1, clusterNum);
      try {
        return hasResults(statement);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public boolean clusterExists(String clusterId, String userId) throws Exception {
    long clusterNum = Long.parseLong(clusterId);
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT id FROM clusters WHERE id=? AND owner_id=?");
      statement.setLong(1, clusterNum);
      statement.setString(2, userId);
      try {
        return hasResults(statement);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public void writeCluster(Cluster cluster) throws Exception {
    // sticking with standard sql... this could be done in one step with replace, or with
    // insert ... on duplicate key update with mysql.
    long clusterNum = Long.parseLong(cluster.getId());
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement checkStatement = conn.prepareStatement("SELECT id FROM clusters WHERE id=?");
      checkStatement.setLong(1, clusterNum);
      PreparedStatement writeStatement;
      try {
        ResultSet rs = checkStatement.executeQuery();
        try {
          if (rs.next()) {
            // cluster exists already, perform an update.
            writeStatement = conn.prepareStatement(
              "UPDATE clusters SET cluster=?, owner_id=?, status=?, expire_time = ? WHERE id=?");
            writeStatement.setBlob(1, new ByteArrayInputStream(codec.serialize(cluster, Cluster.class)));
            writeStatement.setString(2, cluster.getOwnerId());
            writeStatement.setString(3, cluster.getStatus().name());
            writeStatement.setTimestamp(4, getTimestamp(cluster.getExpireTime()));
            writeStatement.setLong(5, clusterNum);
          } else {
            // cluster does not exist, perform an insert.
            writeStatement = conn.prepareStatement(
              "INSERT INTO clusters (id, owner_id, NAME, create_time, expire_time, status, cluster) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");
            writeStatement.setLong(1, clusterNum);
            writeStatement.setString(2, cluster.getOwnerId());
            writeStatement.setString(3, cluster.getName());
            writeStatement.setTimestamp(4, getTimestamp(cluster.getCreateTime()));
            writeStatement.setTimestamp(5, getTimestamp(cluster.getExpireTime()));
            writeStatement.setString(6, cluster.getStatus().name());
            writeStatement.setBlob(7, new ByteArrayInputStream(codec.serialize(cluster, Cluster.class)));
          }
        } finally {
          rs.close();
        }
        // perform the update or insert
        try {
          writeStatement.executeUpdate();
        } finally {
          writeStatement.close();
        }
      } finally {
        checkStatement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public void deleteCluster(String clusterId) throws Exception {
    long clusterNum = Long.parseLong(clusterId);
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("DELETE FROM clusters WHERE id=? ");
      statement.setLong(1, clusterNum);
      try {
        statement.executeUpdate();
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public ClusterJob getClusterJob(JobId jobId) throws TaskException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT job FROM jobs WHERE job_num=? AND cluster_id=?");
        statement.setLong(1, jobId.getJobNum());
        statement.setLong(2, Long.parseLong(jobId.getClusterId()));
        try {
          return getQueryItem(statement, ClusterJob.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting cluster job {}", jobId, e);
      throw new TaskException("Exception getting cluster job " + jobId, e);
    }
  }

  @Override
  public Map<JobId, ClusterJob> getClusterJobs(Set <JobId> jobIds) throws TaskException {

    Map<JobId, ClusterJob> jobMap = Maps.newHashMap();

    if (jobIds.isEmpty()) {
      return jobMap;
    }

    Map<Long, JobId> clusterIdToJobId = Maps.newHashMap();
    for (JobId jobId : jobIds) {
      clusterIdToJobId.put(Long.parseLong(jobId.getClusterId()), jobId);
    }

    ClusterJob job;
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {

        int bufferSize = jobIds.size();

        StringBuilder looperString = new StringBuilder(bufferSize * 3);
        for (int i = 0; i < bufferSize; i++) {
          looperString.append("?, ");
        }
        looperString.setLength(looperString.length() - 2);

        String preparedStatementString =
          "SELECT cluster_id, job FROM jobs WHERE job_num IN (" + looperString.toString() +
            ") AND cluster_id IN (" + looperString.toString() + ")";

        PreparedStatement statement = conn.prepareStatement(preparedStatementString);

        // TODO: This method currently has a limit of 10k items. Fix this using a table join
        int count = 1;
        for (JobId jobId : jobIds) {
          statement.setLong(count, jobId.getJobNum());
          statement.setLong(bufferSize + count, Long.parseLong(jobId.getClusterId()));
          count++;
        }

        try {
          ResultSet rs = statement.executeQuery();
          try {
            while (rs.next()) {
              long clusterId = rs.getLong(1);
              Blob blob = rs.getBlob(2);
              job = deserializeBlob(blob, ClusterJob.class);
              jobMap.put(clusterIdToJobId.get(clusterId), job);
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
    } catch (SQLException e) {
      LOG.error("Exception getting jobs for multiple clusters", e);
      throw new TaskException("Exception getting cluster jobs for multiple clusters", e);
    }
    return jobMap;
  }

  public List<ClusterJob> getClusterJobs(String clusterId, int limit) throws TaskException {
    long clusterNum = Long.parseLong(clusterId);
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
          "SELECT job FROM jobs WHERE cluster_id=? ORDER BY job_num DESC");
        statement.setLong(1, clusterNum);

        try {
          return getQueryList(statement, ClusterJob.class, limit);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting cluster jobs for cluster {} with limit {}", clusterId, limit, e);
      throw new TaskException("Exception getting cluster jobs for cluster " + clusterId);
    }
  }

  public List<ClusterJob> getClusterJobs(String clusterId, String ownerId, int limit) throws TaskException {
    long clusterNum = Long.parseLong(clusterId);
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement(
            "SELECT J.job FROM jobs J, clusters C WHERE J.cluster_id=C.id AND C.id=? AND C.owner_id=? " +
              "ORDER BY J.job_num DESC");
        statement.setLong(1, clusterNum);
        statement.setString(2, ownerId);

        try {
          return getQueryList(statement, ClusterJob.class, limit);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting cluster jobs for cluster {} with limit {}", clusterId, limit, e);
      throw new TaskException("Exception getting cluster jobs for cluster " + clusterId);
    }
  }

  @Override
  public void writeClusterJob(ClusterJob clusterJob) throws TaskException {
    JobId jobId = JobId.fromString(clusterJob.getJobId());
    long clusterId = Long.parseLong(jobId.getClusterId());
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement checkStatement =
          conn.prepareStatement("SELECT job_num FROM jobs WHERE job_num=? AND cluster_id=?");
        checkStatement.setLong(1, jobId.getJobNum());
        checkStatement.setLong(2, Long.parseLong(jobId.getClusterId()));
        PreparedStatement writeStatement;
        try {
          ResultSet rs = checkStatement.executeQuery();
          try {
            if (rs.next()) {
              // cluster exists already, perform an update.
              writeStatement =
                conn.prepareStatement("UPDATE jobs SET job=?, status=? WHERE job_num=? AND cluster_id=?");
              writeStatement.setBlob(1, new ByteArrayInputStream(codec.serialize(clusterJob, ClusterJob.class)));
              writeStatement.setString(2, clusterJob.getJobStatus().name());
              writeStatement.setLong(3, jobId.getJobNum());
              writeStatement.setLong(4, clusterId);
            } else {
              // cluster does not exist, perform an insert.
              writeStatement = conn.prepareStatement(
                "INSERT INTO jobs (job_num, cluster_id, status, create_time, job) VALUES (?, ?, ?, ?, ?)");
              writeStatement.setLong(1, jobId.getJobNum());
              writeStatement.setLong(2, clusterId);
              writeStatement.setString(3, clusterJob.getJobStatus().name());
              writeStatement.setTimestamp(4, getTimestamp(System.currentTimeMillis()));
              writeStatement.setBlob(5, new ByteArrayInputStream(codec.serialize(clusterJob, ClusterJob.class)));
            }
          } finally {
            rs.close();
          }
          // perform the update or insert
          try {
            writeStatement.executeUpdate();
          } finally {
            writeStatement.close();
          }
        } finally {
          checkStatement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new TaskException(e);
    }
  }

  @Override
  public void deleteClusterJob(JobId jobId) throws TaskException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("DELETE FROM jobs WHERE job_num=? AND cluster_id=?");
        statement.setLong(1, jobId.getJobNum());
        statement.setLong(2, Long.parseLong(jobId.getClusterId()));
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new TaskException(e);
    }
  }

  @Override
  public ClusterTask getClusterTask(TaskId taskId) throws TaskException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("SELECT task FROM tasks WHERE task_num=? AND cluster_id=? AND job_num=?");
        statement.setLong(1, taskId.getTaskNum());
        statement.setLong(2, Long.parseLong(taskId.getClusterId()));
        statement.setLong(3, taskId.getJobNum());
        try {
          return getQueryItem(statement, ClusterTask.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting cluster task {}", taskId, e);
      throw new TaskException("Exception getting cluster task " + taskId, e);
    }
  }

  @Override
  public void writeClusterTask(ClusterTask clusterTask) throws TaskException {
    TaskId taskId = TaskId.fromString(clusterTask.getTaskId());
    long clusterId = Long.parseLong(taskId.getClusterId());
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement checkStatement =
          conn.prepareStatement("SELECT task_num FROM tasks WHERE task_num=? AND job_num=? AND cluster_id=?");
        checkStatement.setLong(1, taskId.getTaskNum());
        checkStatement.setLong(2, taskId.getJobNum());
        checkStatement.setLong(3, clusterId);
        PreparedStatement writeStatement;
        try {
          ResultSet rs = checkStatement.executeQuery();
          try {
            if (rs.next()) {
              // task exists already, perform an update.
              writeStatement = conn.prepareStatement(
                "UPDATE tasks SET task=?, status=?, submit_time=?, status_time=?" +
                  " WHERE task_num=? AND job_num=? AND cluster_id=?");
              writeStatement.setBlob(1, new ByteArrayInputStream(codec.serialize(clusterTask, ClusterTask.class)));
              writeStatement.setString(2, clusterTask.getStatus().name());
              writeStatement.setTimestamp(3, getTimestamp(clusterTask.getSubmitTime()));
              writeStatement.setTimestamp(4, getTimestamp(clusterTask.getStatusTime()));
              writeStatement.setLong(5, taskId.getTaskNum());
              writeStatement.setLong(6, taskId.getJobNum());
              writeStatement.setLong(7, clusterId);
            } else {
              // task does not exist, perform an insert.
              writeStatement = conn.prepareStatement(
                "INSERT INTO tasks (task_num, job_num, cluster_id, status, submit_time, task)" +
                  " VALUES (?, ?, ?, ?, ?, ?)");
              writeStatement.setLong(1, taskId.getTaskNum());
              writeStatement.setLong(2, taskId.getJobNum());
              writeStatement.setLong(3, clusterId);
              writeStatement.setString(4, clusterTask.getStatus().name());
              writeStatement.setTimestamp(5, getTimestamp(clusterTask.getSubmitTime()));
              writeStatement.setBlob(6, new ByteArrayInputStream(codec.serialize(clusterTask, ClusterTask.class)));
            }
          } finally {
            rs.close();
          }
          // perform the update or insert
          try {
            writeStatement.executeUpdate();
          } finally {
            writeStatement.close();
          }
        } finally {
          checkStatement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new TaskException(e);
    }
  }

  @Override
  public void deleteClusterTask(TaskId taskId) throws TaskException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("DELETE FROM tasks WHERE task_num=? AND cluster_id=? AND job_num=?");
        statement.setLong(1, taskId.getTaskNum());
        statement.setLong(2, Long.parseLong(taskId.getClusterId()));
        statement.setLong(3, taskId.getJobNum());
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new TaskException(e);
    }
  }

  @Override
  public Node getNode(String nodeId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT node FROM nodes WHERE id=? ");
      statement.setString(1, nodeId);
      try {
        return getQueryItem(statement, Node.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public void writeNode(Node node) throws Exception {
    // sticking with standard sql... this could be done in one step with replace, or with
    // insert ... on duplicate key update with mysql.
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement checkStatement = conn.prepareStatement("SELECT id FROM nodes WHERE id=?");
      checkStatement.setString(1, node.getId());
      PreparedStatement writeStatement;
      try {
        ResultSet rs = checkStatement.executeQuery();
        try {
          if (rs.next()) {
            // node exists already, perform an update.
            writeStatement = conn.prepareStatement("UPDATE nodes SET node=? WHERE id=?");
            writeStatement.setBlob(1, new ByteArrayInputStream(codec.serialize(node, Node.class)));
            writeStatement.setString(2, node.getId());
          } else {
            // node does not exist, perform an insert.
            writeStatement = conn.prepareStatement(
              "INSERT INTO nodes (id, cluster_id, node) VALUES (?, ?, ?)");
            writeStatement.setString(1, node.getId());
            writeStatement.setLong(2, Long.parseLong(node.getClusterId()));
            writeStatement.setBlob(3, new ByteArrayInputStream(codec.serialize(node, Node.class)));
          }
        } finally {
          rs.close();
        }
        // perform the update or insert
        try {
          writeStatement.executeUpdate();
        } finally {
          writeStatement.close();
        }
      } finally {
        checkStatement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public void deleteNode(String nodeId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("DELETE FROM nodes WHERE id=? ");
      try {
        statement.setString(1, nodeId);
        statement.executeUpdate();
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public Set<Node> getClusterNodes(String clusterId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT node FROM nodes WHERE cluster_id=? ");
      statement.setLong(1, Long.parseLong(clusterId));
      try {
        return getQuerySet(statement, Node.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public Set<Node> getClusterNodes(String clusterId, String userId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement(
        "SELECT N.node FROM nodes N, clusters C WHERE C.id=? AND C.owner_id=? AND N.cluster_id=C.id");
      statement.setLong(1, Long.parseLong(clusterId));
      statement.setString(2, userId);
      try {
        return getQuerySet(statement, Node.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public Set<ClusterTask> getRunningTasks(long timestamp) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement =
        conn.prepareStatement("SELECT task FROM tasks WHERE status = ? AND submit_time < ?");
      statement.setString(1, ClusterTask.Status.IN_PROGRESS.name());
      statement.setTimestamp(2, getTimestamp(timestamp));
      try {
        return getQuerySet(statement, ClusterTask.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public Set<Cluster> getExpiringClusters(long timestamp) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement =
        conn.prepareStatement("SELECT cluster FROM clusters WHERE status IN (?, ?) AND expire_time < ?");
      statement.setString(1, Cluster.Status.ACTIVE.name());
      statement.setString(2, Cluster.Status.INCOMPLETE.name());
      statement.setTimestamp(3, getTimestamp(timestamp));
      try {
        return getQuerySet(statement, Cluster.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Queries the store for a set of items, deserializing the items and returning an immutable set of them. If no items
   * exist, the set will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution. Will be closed by this method.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @return
   * @throws SQLException
   */
  private <T> ImmutableSet<T> getQuerySet(PreparedStatement statement, Class<T> clazz) throws SQLException {
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


  private <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz) throws SQLException {
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
   * @return
   * @throws SQLException
   */
  private <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz, int limit)
    throws SQLException {
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
   * @throws SQLException
   */
  private <T> T getQueryItem(PreparedStatement statement, Class<T> clazz) throws SQLException {
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
   * @throws SQLException
   */
  private boolean hasResults(PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      return rs.next();
    } finally {
      rs.close();
    }
  }

  private <T> T deserializeBlob(Blob blob, Class<T> clazz) throws SQLException {
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
  private Timestamp getTimestamp(long ts) {
    return ts > 0 ? new Timestamp(ts) : null;
  }
}
