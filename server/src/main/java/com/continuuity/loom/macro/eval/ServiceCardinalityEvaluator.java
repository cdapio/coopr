package com.continuuity.loom.macro.eval;

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.macro.IncompleteClusterException;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Evaluates a macro that expands to be the number of nodes in the cluster that contain a given service.
 */
public class ServiceCardinalityEvaluator implements Evaluator {
  private final String serviceName;

  public ServiceCardinalityEvaluator(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    int count = 0;
    for (Node clusterNode : clusterNodes) {
      for (Service service : clusterNode.getServices()) {
        if (serviceName.equals(service.getName())) {
          count++;
        }
      }
    }
    return Lists.newArrayList(String.valueOf(count));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceCardinalityEvaluator that = (ServiceCardinalityEvaluator) o;

    return Objects.equal(serviceName, that.serviceName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceName);
  }
}
