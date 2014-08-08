package com.continuuity.loom.macro.eval;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.macro.IncompleteClusterException;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to the hostname of the specified node.
 */
public class HostSelfEvaluator implements Evaluator {

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    String hostname = node.getProperties().getHostname();
    if (hostname == null) {
      return null;
    }
    return Lists.newArrayList(node.getProperties().getHostname());
  }
}
