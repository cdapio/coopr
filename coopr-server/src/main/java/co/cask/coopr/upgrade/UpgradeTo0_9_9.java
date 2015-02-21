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
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

  private long batchSize;

  @Inject
  public UpgradeTo0_9_9(DBConnectionPool dbConnectionPool, SQLClusterStoreService sqlClusterStoreService) {
    this.dbConnectionPool = dbConnectionPool;
    this.clusterStore = sqlClusterStoreService.getSystemView();
  }

  public void run(long batchSize) throws IOException, SQLException {
    this.batchSize = batchSize;
    denormalizeTasksTable();
  }

  private void denormalizeTasksTable() throws IOException, SQLException {
    LOG.info("Denormalizing tasks table: type, cluster_template_name, user_id, and tenant_id columns");
    List<Cluster> clusters = clusterStore.getAllClusters();
    Map<Long, Cluster> clustersById = Maps.newHashMap();
    for (Cluster cluster : clusters) {
      clustersById.put(Long.parseLong(cluster.getId()), cluster);
    }

    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT * FROM tasks");
      try {
        ResultSet rs = statement.executeQuery();
        try {
          PreparedStatement updateStatement =
            conn.prepareStatement(
              "UPDATE tasks SET" +
                " type = ?," + // 1: type
                " cluster_template_name = ?," + // 2: cluster_template_name
                " user_id = ?," + // 3: user_id
                " tenant_id = ?" + // 4: tenant_id
                // 5: task_num, 6: job_num, 7: cluster_id
                " WHERE task_num = ? AND job_num = ? AND cluster_id = ?");
          try {
            long indexWithinCurrentBatch = 0;
            long updatesSent = 0;
            while (rs.next()) {
              long taskNum = rs.getLong("task_num");
              long jobNum = rs.getLong("job_num");
              long clusterId = rs.getLong("cluster_id");

              // 1: type
              Reader reader = new InputStreamReader(rs.getBlob("task").getBinaryStream(), Charsets.UTF_8);
              JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
              String taskName = jsonObject.get("taskName").getAsString();
              ProvisionerAction type = ProvisionerAction.valueOf(taskName.toUpperCase());
              updateStatement.setString(1, type.name());

              Cluster cluster = clustersById.get(clusterId);
              if (cluster != null) {
                updateStatement.setString(2, cluster.getClusterTemplate().getName()); // 2: cluster_template_name
                updateStatement.setString(3, cluster.getAccount().getUserId()); // 3: user_id
                updateStatement.setString(4, cluster.getAccount().getTenantId()); // 4: tenant_id
              } else {
                LOG.error("Task with id {} referenced a non-existent cluster with id {}", taskNum, clusterId);
                updateStatement.setString(2, null); // 2: cluster_template_name
                updateStatement.setString(3, null); // 3: user_id
                updateStatement.setString(4, null); // 4: tenant_id
              }

              // 5: task_num, 6: job_num, 7: cluster_id
              updateStatement.setLong(5, taskNum);
              updateStatement.setLong(6, jobNum);
              updateStatement.setLong(7, clusterId);

              updateStatement.addBatch();
              indexWithinCurrentBatch++;

              if (indexWithinCurrentBatch > batchSize) {
                updateStatement.executeBatch();
                updatesSent += (indexWithinCurrentBatch - 1);
                LOG.info("Successfully sent batch of {} updates", indexWithinCurrentBatch - 1);
                indexWithinCurrentBatch = 0;
              }
            }
            if (indexWithinCurrentBatch != 0) {
              updateStatement.executeBatch();
              updatesSent += (indexWithinCurrentBatch - 1);
              LOG.info("Successfully sent final batch of {} updates, for a total {} updates",
                       indexWithinCurrentBatch - 1, updatesSent);
            }
          } finally {
            updateStatement.close();
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
  }

  public static void main(String[] args) throws ClassNotFoundException {
    Options options = getOptions();
    CommandLineParser parser = new BasicParser();
    try {
      CommandLine command = parser.parse(options, args);
      long batchSize = Long.parseLong(command.getOptionValue("b", "25000"));

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
        upgrade.run(batchSize);
      } catch (Exception e) {
        LOG.error("Error running upgrade", e);
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      usage();
    }
  }

  private static Options getOptions() {
    Options options = new Options();

    OptionGroup optionalGroup = new OptionGroup();
    optionalGroup.setRequired(false);
    optionalGroup.addOption(new Option("b", "batch-size", true, "Batch size of DB updates. Defaults to 25000."));
    options.addOptionGroup(optionalGroup);

    return options;
  }

  private static void usage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("UpgradeTo0_9_9", getOptions());
    System.exit(0);
  }

}
