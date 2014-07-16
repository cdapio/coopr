package com.continuuity.loom.store.node;

import com.continuuity.loom.cluster.Node;

import java.io.IOException;
import java.util.Set;

/**
 * A view of the node store as seen by a given account.
 */
public interface NodeStoreView {
  /**
   * Get an immutable set of all nodes belonging to a specific cluster.
   * @param clusterId Id of the cluster whose nodes will be fetched.
   * @return Set of all nodes belonging to specified cluster. Empty if no cluster or cluster nodes exist.
   * @throws java.io.IOException if there was a problem getting the cluster nodes.
   */
  Set<Node> getClusterNodes(String clusterId) throws IOException;

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
  void deleteNode(String nodeId) throws IOException;

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
