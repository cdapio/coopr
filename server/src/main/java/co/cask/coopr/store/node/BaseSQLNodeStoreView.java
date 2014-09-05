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
package co.cask.coopr.store.node;

import co.cask.coopr.cluster.Node;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBPut;
import co.cask.coopr.store.DBQueryExecutor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 * Base abstract class for {@link NodeStoreView} using a SQL database as the persistent store.
 */
public abstract class BaseSQLNodeStoreView implements NodeStoreView {
  private final DBConnectionPool dbConnectionPool;
  private final DBQueryExecutor dbQueryExecutor;

  BaseSQLNodeStoreView(DBConnectionPool dbConnectionPool, DBQueryExecutor dbQueryExecutor) {
    this.dbConnectionPool = dbConnectionPool;
    this.dbQueryExecutor = dbQueryExecutor;
  }

  abstract PreparedStatement getSelectAllNodesStatement(Connection conn) throws SQLException;

  abstract PreparedStatement getSelectNodeStatement(Connection conn, String id) throws SQLException;

  abstract PreparedStatement getDeleteNodeStatement(Connection conn, String id) throws SQLException;

  abstract PreparedStatement getNodeExistsStatement(Connection conn, String id) throws SQLException;

  abstract PreparedStatement getSetNodeStatement(Connection conn, Node node, byte[] nodeBytes)
  throws SQLException;

  abstract PreparedStatement getInsertNodeStatement(Connection conn, Node node, byte[] nodeBytes)
  throws SQLException;

  abstract boolean allowedToWrite(Node node);

  @Override
  public Set<Node> getAllNodes() throws IOException {

    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectAllNodesStatement(conn);
        try {
          return dbQueryExecutor.getQuerySet(statement, Node.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting all nodes");
    }
  }

  @Override
  public Node getNode(final String nodeId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectNodeStatement(conn, nodeId);
        try {
          return dbQueryExecutor.getQueryItem(statement, Node.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception getting nodes " + nodeId, e);
    }
  }

  @Override
  public void deleteNode(String nodeId) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getDeleteNodeStatement(conn, nodeId);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception deleting node " + nodeId, e);
    }
  }

  @Override
  public void writeNode(final Node node) throws IllegalAccessException, IOException {
    if (!allowedToWrite(node)) {
      throw new IllegalAccessException("Not allowed to write node " + node.getId());
    }

    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        byte[] nodeBytes = dbQueryExecutor.toBytes(node, Node.class);
        DBPut nodePut = new NodeDBPut(node, nodeBytes);
        nodePut.executePut(conn);
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception writing node " + node.getId(), e);
    }
  }

  @Override
  public void writeNodes(Set<Node> nodes) throws IllegalAccessException, IOException {
    for (Node node : nodes) {
      writeNode(node);
    }
  }

  @Override
  public boolean nodeExists(String nodeId) throws IOException {

    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getNodeExistsStatement(conn, nodeId);
        try {
          return dbQueryExecutor.hasResults(statement);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      throw new IOException("Exception checking existence of node` " + nodeId, e);
    }
  }

  private class NodeDBPut extends DBPut {
    private final Node node;
    private final byte[] nodeBytes;

    private NodeDBPut(Node node, byte[] nodeBytes) {
      this.node = node;
      this.nodeBytes = nodeBytes;
    }

    @Override
    public PreparedStatement createUpdateStatement(Connection conn) throws SQLException {
      return getSetNodeStatement(conn, node, nodeBytes);
    }

    @Override
    public PreparedStatement createInsertStatement(Connection conn) throws SQLException {
      return getInsertNodeStatement(conn, node, nodeBytes);
    }
  }
}
