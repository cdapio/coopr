package com.continuuity.loom.store.node;

import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

/**
 * A full view of the node store backed by a sql database.
 */
public class SQLNodeStore implements NodeStore {
  private static final Logger LOG = LoggerFactory.getLogger(SQLNodeStore.class);
  private final DBQueryExecutor dbQueryExecutor;
  private final DBConnectionPool dbConnectionPool;
  private final NodeStoreView systemView;

  public SQLNodeStore(final DBConnectionPool dbConnectionPool, final DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
    this.systemView = new SQLSystemNodeStoreView(dbConnectionPool, dbQueryExecutor);
  }

  @Override
  public Set<Node> getClusterNodes(final String clusterId) throws IOException {
    return null;
  }

  @Override
  public Set<Node> getAllNodes() throws IOException {
    return systemView.getAllNodes();
  }

  @Override
  public boolean nodeExists(final String nodeId) throws IOException {
    return systemView.nodeExists(nodeId);
  }

  @Override
  public Node getNode(final String nodeId) throws IOException {
    return systemView.getNode(nodeId);
  }

  @Override
  public void deleteNode(final String nodeId) throws IOException {
    systemView.deleteNode(nodeId);
  }

  @Override
  public void writeNode(final Node node) throws IllegalAccessException, IOException {
    systemView.writeNode(node);
  }

  @Override
  public void writeNodes(final Set<Node> nodes) throws IllegalAccessException, IOException {
    systemView.writeNodes(nodes);
  }
}
