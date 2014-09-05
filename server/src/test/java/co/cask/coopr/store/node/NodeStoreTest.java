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

import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.conf.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

/**
 * Tests for getting and setting cluster objects.  Test classes for different types of stores must set the
 * protected store field before each test and make sure state is wiped out between tests.
 */
public abstract class NodeStoreTest {
  private static final Account tenant1_user1 = new Account("user1", "tenant1");
  private static final Account tenant1_admin = new Account(Constants.ADMIN_USER, "tenant1");
  protected static NodeStoreService nodeStoreService;
  protected static NodeStore systemView;

  public abstract void clearState() throws Exception;

  public abstract NodeStoreService getNodeStoreService() throws Exception;

  @Before
  public void setupTest() throws Exception {
    nodeStoreService = getNodeStoreService();
    systemView = nodeStoreService.getSystemView();
    clearState();
    Assert.assertEquals(0, systemView.getAllNodes().size());
  }

  @Test
  public void testWriteNodeAsSystem() throws Exception {
    Node node = Entities.NodeExample.NODE1;
    Assert.assertNull(systemView.getNode(node.getId()));
    systemView.writeNode(node);
    Assert.assertEquals(node, systemView.getNode(node.getId()));
  }

  @Test
  public void testGetAllNodesAsUser() throws Exception {
    NodeStoreView view = nodeStoreService.getView(tenant1_user1);
    assertGetStoreGetNode(view);
  }

  @Test
  public void testGetAllNodesAsAdmin() throws Exception {
    NodeStoreView view = nodeStoreService.getView(tenant1_admin);
    assertGetStoreGetNode(view);
  }

  @Test
  public void testGetAllNodesAsSystem() throws Exception {
    assertGetStoreGetNode(systemView);
  }

  @Test
  public void testGetStoreDeleteClusterAsAdmin() throws Exception {
    NodeStoreView view = nodeStoreService.getView(tenant1_admin);
    assertGetStoreDeleteNode(view, Entities.NodeExample.NODE1);
  }

  @Test
  public void testGetStoreDeleteClusterAsUser() throws Exception {
    NodeStoreView view = nodeStoreService.getView(tenant1_user1);
    assertGetStoreDeleteNode(view, Entities.NodeExample.NODE1);
  }

  @Test
  public void testGetStoreUpdateNodeAsUser() throws Exception {
    NodeStoreView view = nodeStoreService.getView(tenant1_user1);
    assertGetStoreUpdateNode(view, Entities.NodeExample.NODE1, Entities.NodeExample.NODE1_UPDATED);
  }

  @Test
  public void testGetStoreUpdateNodeAsAdmin() throws Exception {
    NodeStoreView view = nodeStoreService.getView(tenant1_admin);
    assertGetStoreUpdateNode(view, Entities.NodeExample.NODE1, Entities.NodeExample.NODE1_UPDATED);
  }

  @Test
  public void testGetStoreUpdateNodeAsSystem() throws Exception {
    assertGetStoreUpdateNode(systemView, Entities.NodeExample.NODE1, Entities.NodeExample.NODE1_UPDATED);
  }

  @Test
  public void testNodeExists() throws Exception {
    Node node = Entities.NodeExample.NODE1;
    systemView.writeNode(node);
    Assert.assertTrue(systemView.nodeExists(node.getId()));
  }

  @Test
  public void testGetStoreDeleteNodeAsSystem() throws Exception {
    assertGetStoreDeleteNode(systemView, Entities.NodeExample.NODE1);
  }

  private void assertNodeSetWritten(Set<Node> nodes) throws IOException {
    for (Node node : nodes) {
      assertNodeWritten(node);
    }
  }

  private void assertNodeWritten(Node node) throws IOException {
    Assert.assertTrue(systemView.nodeExists(node.getId()));
    Assert.assertEquals(node, systemView.getNode(node.getId()));
  }

  private void assertNodeSetAgainstNodeSet(Set<Node> leftNodes, Set<Node> rightNodes) {
    Assert.assertEquals(leftNodes, rightNodes);
  }

  private void assertGetStoreGetNode(NodeStoreView view) throws IOException, IllegalAccessException {
    Set<Node> mockNodes = Entities.NodeExample.NODES;
    view.writeNodes(mockNodes);
    assertNodeSetWritten(mockNodes);
    Set<Node> actualNodes = view.getAllNodes();
    assertNodeSetAgainstNodeSet(actualNodes, mockNodes);
  }

  private void assertGetStoreDeleteNode(NodeStoreView view, Node node) throws IOException, IllegalAccessException {
    String nodeId = node.getId();
    view.writeNode(node);
    assertNodeWritten(node);
    //  check overwrite
    view.writeNode(node);
    assertNodeWritten(node);

    view.deleteNode(nodeId);
    Assert.assertNull(view.getNode(nodeId));
  }

  private void assertGetStoreUpdateNode(NodeStoreView view, Node oldNode, Node updatedNode)
  throws IOException, IllegalAccessException {
    view.writeNode(oldNode);
    assertNodeWritten(oldNode);
    view.writeNode(updatedNode);
    assertNodeWritten(updatedNode);
    Node updatedNodeFromDatabase = view.getNode(oldNode.getId());
    Assert.assertNotEquals(oldNode.getProperties(), updatedNodeFromDatabase.getProperties());
  }
}
