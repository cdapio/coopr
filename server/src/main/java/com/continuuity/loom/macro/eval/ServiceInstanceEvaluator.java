package com.continuuity.loom.macro.eval;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.macro.IncompleteClusterException;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to be the instance number of the given node that contains the given service. For
 * example, if there are 3 zookeeper nodes in the cluster, %instance.self.service.zookeeper% will evaluate to 1
 * for the first node with zookeeper, 2 for the second node with zookeeper, and 3 for the last node with zookeeper.
 */
public class ServiceInstanceEvaluator extends ServiceEvaluator {

  public ServiceInstanceEvaluator(String serviceName) {
    super(serviceName);
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    int instanceNum = getServiceInstanceNum(node, sortServiceNodes(clusterNodes));
    if (instanceNum < 0) {
      return null;
    }
    return Lists.newArrayList(String.valueOf(instanceNum));
  }
}
