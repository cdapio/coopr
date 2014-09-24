package co.cask.coopr.macro.eval;

import co.cask.coopr.cluster.Node;
import co.cask.coopr.macro.IncompleteClusterException;
import co.cask.coopr.spec.service.Service;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Base class for evaluating service based macros.
 */
public abstract class ServiceEvaluator implements Evaluator {
  private static final NodeNumComparator NODE_NUM_COMPARATOR = new NodeNumComparator();
  protected final String serviceName;

  protected ServiceEvaluator(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Get the n'th node from the given cluster nodes that have the service.
   *
   * @param clusterNodes Nodes in the cluster.
   * @param n number of the node with the service on it.
   * @return N'th node on the cluster with the given service.
   * @throws IncompleteClusterException if there are not fewer than n + 1 nodes with the service
   */
  protected Node getNthServiceNode(Set<Node> clusterNodes, int n) throws IncompleteClusterException {
    List<Node> sortedNodes = sortServiceNodes(clusterNodes);
    if (n >= sortedNodes.size()) {
      throw new IncompleteClusterException("There are fewer than " + (n + 1) + " nodes with "
                                             + serviceName + " on the cluster");
    }
    return sortedNodes.get(n);
  }

  /**
   * Filter the given nodes to only include nodes with the service on it, and sort all those nodes by node number.
   *
   * @param clusterNodes Set of all cluster nodes.
   * @return Sorted list of nodes with the service on it.
   */
  protected List<Node> sortServiceNodes(Set<Node> clusterNodes) {
    List<Node> filteredNodes = nodesForService(clusterNodes);
    Collections.sort(filteredNodes, NODE_NUM_COMPARATOR);
    return filteredNodes;
  }

  /**
   * Find all nodes of a cluster that have a given service.
   */
  private List<Node> nodesForService(Set<Node> nodes) {
    List<Node> nodesFound = Lists.newArrayList();
    for (Node node : nodes) {
      for (Service service : node.getServices()) {
        if (service.getName().equals(serviceName)) {
          nodesFound.add(node);
          break; // done with this node
        }
      }
    }
    return nodesFound;
  }

  /**
   * Get the instance number of the node. For example, suppose there are 3 nodes with zookeeper
   * with node numbers 3, 7, and 17. The instance num of the node with node number 3 is 1, the instance num of the
   * node with node number 7 is 2, and the instance num of the node with node number 17 is 3.
   *
   * @param node Node to find the instance number for.
   * @param sortedNodeList List of nodes with the service on it, sorted by node number.
   * @return Instance number of the node.
   */
  protected int getServiceInstanceNum(Node node, List<Node> sortedNodeList) {
    int index = 1;
    int nodenum = node.getProperties().getNodenum();
    for (Node candidateNode : sortedNodeList) {
      if (candidateNode.getProperties().getNodenum() == nodenum) {
        return index;
      }
      index++;
    }
    return -1;
  }

  private static class NodeNumComparator implements Comparator<Node> {
    @Override
    public int compare(Node node, Node node2) {
      Integer nodenum1 = node.getProperties().getNodenum();
      Integer nodenum2 = node2.getProperties().getNodenum();
      return nodenum1.compareTo(nodenum2);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceEvaluator that = (ServiceEvaluator) o;

    return Objects.equal(serviceName, that.serviceName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceName);
  }
}
