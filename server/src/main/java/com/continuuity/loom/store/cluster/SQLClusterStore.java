package com.continuuity.loom.store.cluster;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.DBPut;
import com.continuuity.loom.store.DBQueryExecutor;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A full view of the cluster store backed by a sql database.
 */
public class SQLClusterStore implements ClusterStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLClusterStore.class);
  private final DBQueryExecutor dbQueryExecutor;
  private final DBConnectionPool dbConnectionPool;
  private final ClusterStoreView systemView;

  SQLClusterStore(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.systemView = new SQLSystemClusterStoreView(dbConnectionPool, dbQueryExecutor);
  }

  @Override
  public ClusterJob getClusterJob(JobId jobId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT job FROM jobs WHERE job_num=? AND cluster_id=?");
        try {
          statement.setLong(1, jobId.getJobNum());
          statement.setLong(2, Long.parseLong(jobId.getClusterId()));
          return dbQueryExecutor.getQueryItem(statement, ClusterJob.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting cluster job {}", jobId, e);
      throw new IOException("Exception getting cluster job " + jobId, e);
    }
  }

  @Override
  public Map<JobId, ClusterJob> getClusterJobs(Set<JobId> jobIds, String tenantId) throws IOException {
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
              job = dbQueryExecutor.deserializeBlob(blob, ClusterJob.class);
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
      throw new IOException("Exception getting cluster jobs for multiple clusters", e);
    }
    return jobMap;
  }

  @Override
  public void writeClusterJob(ClusterJob clusterJob) throws IOException {
    JobId jobId = JobId.fromString(clusterJob.getJobId());
    long clusterId = Long.parseLong(jobId.getClusterId());
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        ByteArrayInputStream jobBytes = dbQueryExecutor.toByteStream(clusterJob, ClusterJob.class);
        DBPut jobPut = new ClusterJobDBPut(clusterJob, jobBytes, jobId, clusterId);
        jobPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void deleteClusterJob(JobId jobId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("DELETE FROM jobs WHERE job_num=? AND cluster_id=?");
        try {
          statement.setLong(1, jobId.getJobNum());
          statement.setLong(2, Long.parseLong(jobId.getClusterId()));
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public ClusterTask getClusterTask(TaskId taskId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("SELECT task FROM tasks WHERE task_num=? AND cluster_id=? AND job_num=?");
        try {
          statement.setLong(1, taskId.getTaskNum());
          statement.setLong(2, Long.parseLong(taskId.getClusterId()));
          statement.setLong(3, taskId.getJobNum());
          return dbQueryExecutor.getQueryItem(statement, ClusterTask.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting cluster task {}", taskId, e);
      throw new IOException("Exception getting cluster task " + taskId, e);
    }
  }

  @Override
  public void writeClusterTask(ClusterTask clusterTask) throws IOException {
    TaskId taskId = TaskId.fromString(clusterTask.getTaskId());
    long clusterId = Long.parseLong(taskId.getClusterId());
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        ByteArrayInputStream taskBytes = dbQueryExecutor.toByteStream(clusterTask, ClusterTask.class);
        DBPut taskPut = new ClusterTaskDBPut(clusterTask, taskBytes, taskId, clusterId);
        taskPut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void deleteClusterTask(TaskId taskId) throws IOException {
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
      throw new IOException(e);
    }
  }

  @Override
  public Node getNode(String nodeId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT node FROM nodes WHERE id=? ");
        statement.setString(1, nodeId);
        try {
          return dbQueryExecutor.getQueryItem(statement, Node.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void writeNode(Node node) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        ByteArrayInputStream nodeBytes = dbQueryExecutor.toByteStream(node, Node.class);
        DBPut nodePut = new NodeDBPut(node, nodeBytes);
        nodePut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void deleteNode(String nodeId) throws IOException {
    try {
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
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Set<ClusterTask> getRunningTasks(long timestamp) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("SELECT task FROM tasks WHERE status = ? AND submit_time < ?");
        statement.setString(1, ClusterTask.Status.IN_PROGRESS.name());
        statement.setTimestamp(2, DBHelper.getTimestamp(timestamp));
        try {
          return dbQueryExecutor.getQuerySet(statement, ClusterTask.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Set<Cluster> getExpiringClusters(long timestamp) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement =
          conn.prepareStatement("SELECT cluster FROM clusters WHERE status IN (?, ?) AND expire_time < ?");
        statement.setString(1, Cluster.Status.ACTIVE.name());
        statement.setString(2, Cluster.Status.INCOMPLETE.name());
        statement.setTimestamp(3, DBHelper.getTimestamp(timestamp));
        try {
          return dbQueryExecutor.getQuerySet(statement, Cluster.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<Cluster> getAllClusters() throws IOException {
    return systemView.getAllClusters();
  }

  @Override
  public Cluster getCluster(String clusterId) throws IOException {
    return systemView.getCluster(clusterId);
  }

  @Override
  public boolean clusterExists(String clusterId) throws IOException {
    return systemView.clusterExists(clusterId);
  }

  @Override
  public void writeCluster(Cluster cluster) throws IllegalAccessException, IOException {
    systemView.writeCluster(cluster);
  }

  @Override
  public void deleteCluster(String clusterId) throws IOException {
    systemView.deleteCluster(clusterId);
  }

  @Override
  public List<ClusterJob> getClusterJobs(String clusterId, int limit) throws IOException {
    return systemView.getClusterJobs(clusterId, limit);
  }

  @Override
  public Set<Node> getClusterNodes(String clusterId) throws IOException {
    return systemView.getClusterNodes(clusterId);
  }

  private class ClusterJobDBPut extends DBPut {
    private final ClusterJob clusterJob;
    private final ByteArrayInputStream jobBytes;
    private final JobId jobId;
    private final long clusterId;

    private ClusterJobDBPut(ClusterJob clusterJob, ByteArrayInputStream jobBytes, JobId jobId, long clusterId) {
      this.clusterJob = clusterJob;
      this.jobBytes = jobBytes;
      this.jobId = jobId;
      this.clusterId = clusterId;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement updateStatement =
        conn.prepareStatement("UPDATE jobs SET job=?, status=? WHERE job_num=? AND cluster_id=?");
      updateStatement.setBlob(1, jobBytes);
      updateStatement.setString(2, clusterJob.getJobStatus().name());
      updateStatement.setLong(3, jobId.getJobNum());
      updateStatement.setLong(4, clusterId);
      return updateStatement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO jobs (job_num, cluster_id, status, create_time, job) VALUES (?, ?, ?, ?, ?)");
      statement.setLong(1, jobId.getJobNum());
      statement.setLong(2, clusterId);
      statement.setString(3, clusterJob.getJobStatus().name());
      statement.setTimestamp(4, DBHelper.getTimestamp(System.currentTimeMillis()));
      statement.setBlob(5, jobBytes);
      return statement;
    }
  }

  private class ClusterTaskDBPut extends DBPut {
    private final ClusterTask clusterTask;
    private final ByteArrayInputStream taskBytes;
    private final TaskId taskId;
    private final long clusterId;

    private ClusterTaskDBPut(ClusterTask clusterTask, ByteArrayInputStream taskBytes, TaskId taskId, long clusterId) {
      this.clusterTask = clusterTask;
      this.taskBytes = taskBytes;
      this.taskId = taskId;
      this.clusterId = clusterId;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "UPDATE tasks SET task=?, status=?, submit_time=?, status_time=?" +
          " WHERE task_num=? AND job_num=? AND cluster_id=?");
      statement.setBlob(1, dbQueryExecutor.toByteStream(clusterTask, ClusterTask.class));
      statement.setString(2, clusterTask.getStatus().name());
      statement.setTimestamp(3, DBHelper.getTimestamp(clusterTask.getSubmitTime()));
      statement.setTimestamp(4, DBHelper.getTimestamp(clusterTask.getStatusTime()));
      statement.setLong(5, taskId.getTaskNum());
      statement.setLong(6, taskId.getJobNum());
      statement.setLong(7, clusterId);
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO tasks (task_num, job_num, cluster_id, status, submit_time, task)" +
          " VALUES (?, ?, ?, ?, ?, ?)");
      statement.setLong(1, taskId.getTaskNum());
      statement.setLong(2, taskId.getJobNum());
      statement.setLong(3, clusterId);
      statement.setString(4, clusterTask.getStatus().name());
      statement.setTimestamp(5, DBHelper.getTimestamp(clusterTask.getSubmitTime()));
      statement.setBlob(6, taskBytes);
      return statement;
    }
  }

  private class NodeDBPut extends DBPut {
    private final Node node;
    private final ByteArrayInputStream nodeBytes;

    private NodeDBPut(Node node, ByteArrayInputStream nodeBytes) {
      this.node = node;
      this.nodeBytes = nodeBytes;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement("UPDATE nodes SET node=? WHERE id=?");
      statement.setBlob(1, nodeBytes);
      statement.setString(2, node.getId());
      return statement;
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      PreparedStatement statement = conn.prepareStatement(
        "INSERT INTO nodes (id, cluster_id, node) VALUES (?, ?, ?)");
      statement.setString(1, node.getId());
      statement.setLong(2, Long.parseLong(node.getClusterId()));
      statement.setBlob(3, nodeBytes);
      return statement;
    }
  }
}
