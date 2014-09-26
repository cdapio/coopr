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

import java.io.IOException;
import java.util.Set;

/**
 * A view of the node store as seen by a given account.
 */
public interface NodeStoreView {
  /**
   * Get an immutable set of all nodes in the store.
   * @return All nodes in the store.
   * @throws IOException if there was a problem getting the nodes.
   */
  Set<Node> getAllNodes() throws IOException;

  /**
   * Return whether or not the node with the given id exists or not, where existence is determined by whether or not
   * the node is in the store, and not by whether or not there is an active node with the given id.
   * @param nodeId Id of the node.
   * @return True if the node exists, False if not.
   */
  boolean nodeExists(String nodeId) throws IOException;

  /**
   * Get the node with the given id.
   * @param nodeId Id of the node to get.
   * @return The node with the given id, or null if none exists.
   * @throws java.io.IOException if there was a problem getting the node.
   */
  Node getNode(String nodeId) throws IOException;

  /**
   * Delete the node with the given id.
   * @param nodeId Id of the node to delete.
   * @throws IOException if there was a problem deleting the node.
   */
  void deleteNode(String nodeId) throws IllegalAccessException, IOException;

  /**
   * Write a node to the store using its id.
   * @param node Node to write.
   * @throws IOException if there was a problem writing the node.
   */
  void writeNode(Node node) throws IllegalAccessException, IOException;

  /**
   * Write a set of nodes to the store using its id.
   * @param nodes Nodes to write.
   * @throws IOException if there was a problem writing the nodes.
   */
  void writeNodes(Set<Node> nodes) throws IllegalAccessException, IOException;
}
