package co.cask.coopr.store.cluster;

import co.cask.coopr.cluster.Cluster;

import java.io.IOException;

/**
 * A view of the cluster store as seen by a given account.
 */
public interface ClusterStoreView extends ReadOnlyClusterStoreView {

  /**
   * Write a cluster to the store using its id.
   *
   * @param cluster Cluster to write.
   * @throws IOException if there was a problem writing the cluster.
   */
  void writeCluster(Cluster cluster) throws IllegalAccessException, IOException;

  /**
   * Delete the cluster that has the given id.
   *
   * @param clusterId Id of the cluster to delete.
   * @throws IOException if there was a problem deleting the cluster.
   */
  void deleteCluster(String clusterId) throws IOException;
}
