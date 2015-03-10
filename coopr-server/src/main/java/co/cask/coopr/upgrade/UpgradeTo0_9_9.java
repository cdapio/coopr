/*
 * Copyright © 2015 Cask Data, Inc.
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
package co.cask.coopr.upgrade;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.SQLClusterStoreService;
import co.cask.coopr.store.guice.StoreModule;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Upgrades database to work with Coopr 0.9.9.
 */
public class UpgradeTo0_9_9 {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeTo0_9_9.class);
  private static final Gson GSON = new Gson();

  private final DBConnectionPool dbConnectionPool;
  private final ClusterStore clusterStore;

  @Inject
  public UpgradeTo0_9_9(DBConnectionPool dbConnectionPool, SQLClusterStoreService sqlClusterStoreService) {
    this.dbConnectionPool = dbConnectionPool;
    this.clusterStore = sqlClusterStoreService.getSystemView();
  }

  public void run() throws IOException, SQLException {
    denormalizeTasksTable();
  }

  private int getTableRowCount(Connection conn, String table) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
    try {
      ResultSet rs = statement.executeQuery();
      try {
        if (rs.next()) {
          return rs.getInt(1);
        }
      } finally {
        rs.close();
      }
    } finally {
      statement.close();
    }

    throw new IllegalStateException();
  }

  private void denormalizeTasksTable() throws IOException, SQLException {
    LOG.info("Denormalizing tasks table: type, cluster_template_name, user_id, and tenant_id columns");

    LOG.info("Fetching all clusters");
    List<Cluster> clusters = clusterStore.getAllClusters();
    Map<Long, Cluster> clustersById = Maps.newHashMap();
    for (Cluster cluster : clusters) {
      clustersById.put(Long.parseLong(cluster.getId()), cluster);
    }

    for (Cluster cluster : clusters) {
      LOG.info("Denormalizing tasks for cluster {}", cluster.getId());
      Connection conn = dbConnectionPool.getConnection();
      try {
        denormalizeClusterTasks(conn, cluster);
      } finally {
        conn.close();
      }
    }
  }

  private void denormalizeClusterTasks(Connection conn, Cluster cluster) throws SQLException {
    try {
      long clusterId = Long.parseLong(cluster.getId());

      // populate tasks
      List<Task> tasks = Lists.newArrayList();
      PreparedStatement selectTasksBatch = conn.prepareStatement(
        "SELECT * FROM tasks WHERE cluster_id = " + clusterId);
      try {
        ResultSet rs = selectTasksBatch.executeQuery();
        while (rs.next()) {
          long taskNum = rs.getLong("task_num");
          long jobNum = rs.getLong("job_num");
          Reader reader = new InputStreamReader(rs.getBlob("task").getBinaryStream(), Charsets.UTF_8);
          JsonObject blob = GSON.fromJson(reader, JsonObject.class);
          String taskName = blob.get("taskName").getAsString();
          tasks.add(new Task(taskNum, jobNum, taskName));
        }
      } finally {
        selectTasksBatch.close();
      }

      String updateSql = "UPDATE tasks SET" +
        " type = ?," + // 1: type
        " cluster_template_name = ?," + // 2: cluster_template_name
        " user_id = ?," + // 3: user_id
        " tenant_id = ?" + // 4: tenant_id
        // 5: task_num, 6: job_num, 7: cluster_id
        " WHERE task_num = ? AND job_num = ? AND cluster_id = ?";
      PreparedStatement updateStatement = conn.prepareStatement(updateSql);
      try {
        for (Task task : tasks) {
          long taskNum = task.getTaskNum();
          long jobNum = task.getJobNum();
          String taskName = task.getTaskName();

          ProvisionerAction type = ProvisionerAction.valueOf(taskName.toUpperCase());
          updateStatement.setString(1, type.name()); // 1: type
          updateStatement.setString(2, cluster.getClusterTemplate().getName()); // 2: cluster_template_name
          updateStatement.setString(3, cluster.getAccount().getUserId()); // 3: user_id
          updateStatement.setString(4, cluster.getAccount().getTenantId()); // 4: tenant_id
          updateStatement.setLong(5, taskNum); // 5: task_num
          updateStatement.setLong(6, jobNum); // 6: job_num
          updateStatement.setLong(7, clusterId); // 7: cluster_id
          updateStatement.addBatch();
        }
        updateStatement.executeBatch();
      } finally {
        updateStatement.close();
      }
    } finally {
      conn.close();
    }
  }

  public static void main(String[] args) throws ClassNotFoundException {
    final Configuration configuration = Configuration.create();
    String jdbcConnectionString = configuration.get(Constants.JDBC_CONNECTION_STRING);
    if (jdbcConnectionString == null) {
      LOG.error("Missing property '{}' in coopr-site.xml", Constants.JDBC_CONNECTION_STRING);
      System.exit(1);
    }

    Injector injector = Guice.createInjector(
      new AbstractModule() {
        @Override
        protected void configure() {
          bind(Configuration.class).toInstance(configuration);
        }
      },
      new StoreModule(configuration));

    UpgradeTo0_9_9 upgrade = injector.getInstance(UpgradeTo0_9_9.class);
    try {
      upgrade.run();
    } catch (Exception e) {
      LOG.error("Error running upgrade", e);
    }
  }

  /**
   *
   */
  private static final class Task {

    private final long taskNum;
    private final long jobNum;
    private final String taskName;

    private Task(long taskNum, long jobNum, String taskName) {
      this.taskNum = taskNum;
      this.jobNum = jobNum;
      this.taskName = taskName;
    }

    public long getTaskNum() {
      return taskNum;
    }

    public long getJobNum() {
      return jobNum;
    }

    public String getTaskName() {
      return taskName;
    }
  }

}
