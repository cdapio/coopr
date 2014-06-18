package com.continuuity.loom.store.cluster;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.scheduler.task.ClusterJob;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.JobId;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class SQLClusterStore implements ClusterStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLClusterStore.class);
  private static final JsonSerde CODEC = new JsonSerde();
  private final DBConnectionPool dbConnectionPool;
  private final ClusterStoreView systemView;

  SQLClusterStore(DBConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
    this.systemView = new SQLSystemClusterStoreView(dbConnectionPool);
  }

  @Override
  public ClusterJob getClusterJob(JobId jobId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT job FROM jobs WHERE job_num=? AND cluster_id=?");
        statement.setLong(1, jobId.getJobNum());
        statement.setLong(2, Long.parseLong(jobId.getClusterId()));
        try {
          return DBQueryHelper.getQueryItem(statement, ClusterJob.class);
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
              writeStatement.setBlob(1, new ByteArrayInputStream(CODEC.serialize(clusterJob, ClusterJob.class)));
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
              writeStatement.setTimestamp(4, DBQueryHelper.getTimestamp(System.currentTimeMillis()));
              writeStatement.setBlob(5, new ByteArrayInputStream(CODEC.serialize(clusterJob, ClusterJob.class)));
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
      throw new IOException(e);
    }
  }

  @Override
  public void deleteClusterJob(JobId jobId) throws IOException {
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
        statement.setLong(1, taskId.getTaskNum());
        statement.setLong(2, Long.parseLong(taskId.getClusterId()));
        statement.setLong(3, taskId.getJobNum());
        try {
          return DBQueryHelper.getQueryItem(statement, ClusterTask.class);
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
              writeStatement.setBlob(1, new ByteArrayInputStream(CODEC.serialize(clusterTask, ClusterTask.class)));
              writeStatement.setString(2, clusterTask.getStatus().name());
              writeStatement.setTimestamp(3, DBQueryHelper.getTimestamp(clusterTask.getSubmitTime()));
              writeStatement.setTimestamp(4, DBQueryHelper.getTimestamp(clusterTask.getStatusTime()));
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
              writeStatement.setTimestamp(5, DBQueryHelper.getTimestamp(clusterTask.getSubmitTime()));
              writeStatement.setBlob(6, new ByteArrayInputStream(CODEC.serialize(clusterTask, ClusterTask.class)));
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
          return DBQueryHelper.getQueryItem(statement, Node.class);
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
    // sticking with standard sql... this could be done in one step with replace, or with
    // insert ... on duplicate key update with mysql.
    try {
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
              writeStatement.setBlob(1, new ByteArrayInputStream(CODEC.serialize(node, Node.class)));
              writeStatement.setString(2, node.getId());
            } else {
              // node does not exist, perform an insert.
              writeStatement = conn.prepareStatement(
                "INSERT INTO nodes (id, cluster_id, node) VALUES (?, ?, ?)");
              writeStatement.setString(1, node.getId());
              writeStatement.setLong(2, Long.parseLong(node.getClusterId()));
              writeStatement.setBlob(3, new ByteArrayInputStream(CODEC.serialize(node, Node.class)));
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
        statement.setTimestamp(2, DBQueryHelper.getTimestamp(timestamp));
        try {
          return DBQueryHelper.getQuerySet(statement, ClusterTask.class);
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
        statement.setTimestamp(3, DBQueryHelper.getTimestamp(timestamp));
        try {
          return DBQueryHelper.getQuerySet(statement, Cluster.class);
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

  private <T> T deserializeBlob(Blob blob, Class<T> clazz) throws SQLException {
    Reader reader = new InputStreamReader(blob.getBinaryStream(), Charsets.UTF_8);
    T object;
    try {
      object = CODEC.deserialize(reader, clazz);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return object;
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
}
