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

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.store.DBConnectionPool;
import co.cask.coopr.store.DBQueryExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The node store as viewed by a tenant user. A tenant user can read, write, and delete any cluster
 * they own.
 */
public class SQLUserNodeStoreView extends BaseSQLNodeStoreView {
  private final Account account;

  SQLUserNodeStoreView(final DBConnectionPool dbConnectionPool, final Account account,
                       final DBQueryExecutor dbQueryExecutor) {
    super(dbConnectionPool, dbQueryExecutor);
    this.account = account;
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
  PreparedStatement getSetNodeStatement(final Connection conn, final Node node, final byte[] nodeBytes)
  throws SQLException {
    PreparedStatement statement = conn.prepareStatement("UPDATE nodes SET node=? WHERE id=?");
    statement.setBytes(1, nodeBytes);
    statement.setString(2, node.getId());
    return statement;
  }

  @Override
  PreparedStatement getInsertNodeStatement(final Connection conn, final Node node, final byte[] nodeBytes)
  throws SQLException {
    PreparedStatement statement = conn.prepareStatement("INSERT INTO nodes (id, cluster_id, node) VALUES (?, ?, ?)");
    statement.setString(1, node.getId());
    statement.setLong(2, Long.parseLong(node.getClusterId()));
    statement.setBytes(3, nodeBytes);
    return statement;
  }

  @Override
  boolean allowedToWrite(final Node node) {
    return true;
  }
}
