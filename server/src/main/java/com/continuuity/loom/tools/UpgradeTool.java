package com.continuuity.loom.tools;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.guice.CodecModules;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.conf.guice.ConfigurationModule;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBHelper;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.guice.StoreModule;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

/**
 * Tool for upgrading data from older versions of the system to the current version.
 */
public class UpgradeTool {
  private static final String NEW_TENANT_ID = Constants.SUPERADMIN_TENANT;

  public static void main(String[] args) throws Exception {
    Configuration conf = Configuration.create();
    Injector injector = Guice.createInjector(
      new ConfigurationModule(conf),
      new StoreModule(conf),
      new CodecModules().getUpgradeModule(NEW_TENANT_ID)
    );
    ClusterStoreService clusterStoreService = injector.getInstance(ClusterStoreService.class);
    clusterStoreService.startAndWait();
    DBConnectionPool dbConnectionPool = injector.getInstance(DBConnectionPool.class);
    alterTables(dbConnectionPool);
    migrateClusters(clusterStoreService);
    clusterStoreService.stopAndWait();
  }

  private static void migrateClusters(ClusterStoreService clusterStoreService) throws IOException,
    IllegalAccessException {
    System.out.println("migrating clusters");
    ClusterStore clusterStore = clusterStoreService.getSystemView();
    for (Cluster cluster : clusterStore.getAllClusters()) {
      clusterStore.writeCluster(cluster);
      for (Node node : clusterStore.getClusterNodes(cluster.getId())) {
        clusterStore.writeNode(node);
      }
    }
  }

  private static void alterTables(DBConnectionPool dbConnectionPool) throws SQLException {
    if (!dbConnectionPool.isEmbeddedDerbyDB()) {
      return;
    }
    Set<String> tables = ImmutableSet.of("providers", "hardwareTypes", "imageTypes", "services",
                                           "clusterTemplates", "automatorTypes", "providerTypes");
    Connection conn = null;
    try {
      // update entity tables
      conn = dbConnectionPool.getConnection();
      for (String table : tables) {
        System.out.println("adding tenant column to table " + table);
        performQuery(conn, String.format(
          "ALTER TABLE %s ADD COLUMN tenant_id VARCHAR(255) NOT NULL WITH DEFAULT '%s'", table, NEW_TENANT_ID));

        System.out.println("adding primary key of (tenant_id, name) to table " + table);
        performQuery(conn, "ALTER TABLE " + table + " ALTER name NOT NULL");
        performQuery(conn, "ALTER TABLE " + table + " ADD PRIMARY KEY (tenant_id, name)");
      }

      // update clusters table
      System.out.println("updating clusters table");
      performQuery(conn, String.format(
        "ALTER TABLE clusters ADD COLUMN tenant_id VARCHAR(255) NOT NULL WITH DEFAULT '%s'", NEW_TENANT_ID));

      System.out.println("adding index on clusters table");
      DBHelper.createDerbyIndex(dbConnectionPool,
                                "clusters_account_index", "clusters", "tenant_id", "owner_id", "id");
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
  }

  private static void performQuery(Connection conn, String query) throws SQLException {
    Statement statement = conn.createStatement();
    try {
      statement.executeUpdate(query);
    } finally {
      statement.close();
    }
  }
}
