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
 * Evaluates a macro that expands to be a list of the ip addresses of nodes in the cluster that contain a given
 * service.
 */
public class IPServiceEvaluator extends ServiceEvaluator {
  private final String ipType;
  private final Integer instanceNum;

  public IPServiceEvaluator(String serviceName, String ipType, Integer instanceNum) {
    super(serviceName);
    this.ipType = ipType;
    this.instanceNum = instanceNum;
  }

  @Override
  public List<String> evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    List<String> output = Lists.newArrayList();
    if (instanceNum != null) {
      Node instanceNode = getNthServiceNode(clusterNodes, instanceNum);
      output.add(instanceNode.getProperties().getIPAddress(ipType));
    } else {
      // go through all nodes, looking for nodes with the service on it.
      for (Node clusterNode : clusterNodes) {
        for (Service service : clusterNode.getServices()) {
          // if the node has the service on it, add the relevant node property.
          if (serviceName.equals(service.getName())) {
            String ip = clusterNode.getProperties().getIPAddress(ipType);
            if (ip == null) {
              throw new IncompleteClusterException("node has no ip for macro expansion.");
            }
            output.add(ip);
          }
        }
      }
    }
    return output.isEmpty() ? null : output;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IPServiceEvaluator that = (IPServiceEvaluator) o;

    return Objects.equal(ipType, that.ipType) &&
      Objects.equal(instanceNum, that.instanceNum);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ipType, instanceNum);
  }
}
