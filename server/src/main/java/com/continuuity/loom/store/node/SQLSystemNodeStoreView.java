package com.continuuity.loom.store.node;

import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryExecutor;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The node store as viewed by the system. The system can do anything to any object.
 */
public class SQLSystemNodeStoreView extends BaseSQLNodeStoreView {
  public SQLSystemNodeStoreView(final DBConnectionPool dbConnectionPool, final DBQueryExecutor dbQueryExecutor) {
    super(dbConnectionPool, dbQueryExecutor);
  }

  @Override
  PreparedStatement getSelectAllNodesStatement(final Connection conn) throws SQLException {
    return conn.prepareStatement("SELECT node FROM nodes");
  }

  @Override
  PreparedStatement getSelectNodeStatement(final Connection conn, final String id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT node FROM nodes WHERE id=?");
    statement.setString(1, id);
    return statement;
  }

  @Override
  PreparedStatement getDeleteNodeStatement(final Connection conn, final String id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("DELETE FROM nodes WHERE id=?");
    statement.setString(1, id);
    return statement;
  }

  @Override
  PreparedStatement getNodeExistsStatement(final Connection conn, final String id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement("SELECT id FROM nodes WHERE id=?");
    statement.setString(1, id);
    return statement;
  }

  @Override
  PreparedStatement getSetNodeStatement(final Connection conn, final Node node, final ByteArrayInputStream nodeBytes)
  throws SQLException {
    PreparedStatement statement = conn.prepareStatement("UPDATE nodes SET node=? WHERE id=?");
    statement.setBlob(1, nodeBytes);
    statement.setString(2, node.getId());
    return statement;
  }

  @Override
  PreparedStatement getInsertNodeStatement(final Connection conn, final Node node, final ByteArrayInputStream nodeBytes)
  throws SQLException {
    PreparedStatement statement = conn.prepareStatement("INSERT INTO nodes (id, cluster_id, node) VALUES (?, ?, ?)");
    statement.setString(1, node.getId());
    statement.setLong(2, Long.parseLong(node.getClusterId()));
    statement.setBlob(3, nodeBytes);
    return statement;
  }

  @Override
  boolean allowedToWrite(final Node node) {
    return true;
  }
}
