package com.continuuity.loom.macro.eval;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.macro.IncompleteClusterException;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to an ip address on the specified node.
 */
public class IPSelfEvaluator implements Evaluator {
  private final String ipType;

  public IPSelfEvaluator(String ipType) {
    this.ipType = ipType;
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    String ip = node.getProperties().getIPAddress(ipType);
    if (ip == null) {
      throw new IncompleteClusterException("node has no ip for macro expansion.");
    }
    return Lists.newArrayList(ip);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IPSelfEvaluator that = (IPSelfEvaluator) o;

    return Objects.equal(ipType, that.ipType);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ipType);
  }
}
