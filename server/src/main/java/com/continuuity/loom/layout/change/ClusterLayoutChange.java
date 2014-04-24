package com.continuuity.loom.layout.change;

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.layout.ClusterLayout;

import java.util.Map;
import java.util.Set;

/**
 * Represents some change to a {@link ClusterLayout}, such as adding a service to some of the nodes. Caller is
 * responsible for calling the {@link #canApplyChange(com.continuuity.loom.layout.ClusterLayout)} method to validate
 * if a change can be applied to a layout before actually applying the change with
 * {@link #applyChange(com.continuuity.loom.layout.ClusterLayout)} or
 * {@link #applyChange(com.continuuity.loom.cluster.Cluster, java.util.Set, java.util.Map)}.
 */
public interface ClusterLayoutChange {

  /**
   * Returns whether or not the change can be applied to the given {@link ClusterLayout}. This is not for checking
   * if the resulting cluster layout would satisfy all its constraints, but for checking whether or not it is physically
   * possible to apply the change. For example, it is not physically possible to add a service to 10 nodes if the
   * cluster is only 5 nodes.
   *
   * @param layout Layout to check the legality of the change for.
   * @return True if the change can be applied, false if not.
   */
  boolean canApplyChange(ClusterLayout layout);

  /**
   * Apply the change to the given {@link ClusterLayout}. Returns a new layout object and does not modify the given
   * layout.
   *
   * @param layout Layout to apply the change to.
   * @return New layout object after the change.
   */
  ClusterLayout applyChange(ClusterLayout layout);

  /**
   * Apply the change to a set of nodes, returning nodes that have changed.
   *
   * @param cluster Cluster to apply the change to.
   * @param clusterNodes Nodes to apply the change to.
   * @param serviceMap Map of service name to {@link Service} for available services.
   * TODO: remove the need for the serviceMap
   */
  Set<Node> applyChange(Cluster cluster, Set<Node> clusterNodes, Map<String, Service> serviceMap);
}
