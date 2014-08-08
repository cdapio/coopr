package com.continuuity.loom.macro.eval;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.macro.IncompleteClusterException;

import java.util.List;
import java.util.Set;

/**
 * Evaluator for a specific type of expression, such as an IP expression or a hostname expression.
 */
public interface Evaluator {

  /**
   * Evaluate the macro expression on the given node of the given cluster, with the given cluster nodes.
   * Returns null if the macro does not expand to anything.
   *
   * @param cluster Cluster the macro is being expanded for.
   * @param clusterNodes Nodes in the cluster the macro is being expanded for.
   * @param node The cluster node that the macro is being expanded for.
   * @return Evaluated macro expression.
   * @throws IncompleteClusterException if the cluster does not contain the information required to evaluate the macro.
   */
  List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException;
}
