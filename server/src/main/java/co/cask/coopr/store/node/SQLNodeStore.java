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
import co.cask.coopr.store.DBQueryExecutor;

import java.io.IOException;
import java.util.Set;

/**
 * A full view of the node store backed by a sql database.
 */
public class SQLNodeStore implements NodeStore {
  private final NodeStoreView systemView;

  public SQLNodeStore(final DBConnectionPool dbConnectionPool, final DBQueryExecutor dbQueryExecutor) {
    this.systemView = new SQLSystemNodeStoreView(dbConnectionPool, dbQueryExecutor);
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
  public void deleteNode(final String nodeId) throws IOException, IllegalAccessException {
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
