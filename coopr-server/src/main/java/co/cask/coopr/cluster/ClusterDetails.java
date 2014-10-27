/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.cluster;

import co.cask.coopr.macro.Expander;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.spec.Link;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

/**
 * Full cluster details, including the cluster object itself, all nodes in the cluster, and the progress of the last
 * job performed on the cluster.
 */
public class ClusterDetails {
  private final Cluster cluster;
  private final List<Link> links;
  private final Set<Node> nodes;
  private final ClusterJobProgress progress;
  private final String message;

  public ClusterDetails(Cluster cluster, Set<Node> nodes, ClusterJob job) {
    this.cluster = cluster;
    this.nodes = ImmutableSet.copyOf(nodes);
    this.progress = new ClusterJobProgress(job);
    this.message = job.getStatusMessage();
    // get links from the cluster template, expanding any macros in them and populating the field
    ImmutableList.Builder linksBuilder = ImmutableList.builder();
    for (Link link : cluster.getClusterTemplate().getLinks()) {
      try {
        linksBuilder.add(new Link(link.getLabel(), Expander.expand(link.getUrl(), cluster, nodes, null)));
      } catch (Exception e) {
        // if we couldn't expand the macro, just use the original string
        linksBuilder.add(link);
      }
    }
    this.links = linksBuilder.build();
    for (Node node : this.nodes) {
      node.populateLinks(cluster, nodes);
    }
  }

  public ClusterDetails(Cluster cluster, List<Link> links, Set<Node> nodes, ClusterJobProgress progress,
                        String message) {
    this.cluster = cluster;
    this.links = links;
    this.nodes = nodes;
    this.progress = progress;
    this.message = message;
  }

  /**
   * Get the cluster.
   *
   * @return The cluster
   */
  public Cluster getCluster() {
    return cluster;
  }

  /**
   * Get an immutable set of all nodes in the cluster.
   *
   * @return Immutable set of all nodes in the cluster
   */
  public Set<Node> getNodes() {
    return nodes;
  }

  /**
   * Get the progress of the most recent job performed on the cluster, including any running job.
   *
   * @return Progress of the most recent job performed on the cluster, including any running job
   */
  public ClusterJobProgress getProgress() {
    return progress;
  }

  /**
   * Get the status message of the most recent job performed on the cluster, including any running job.
   *
   * @return Status message of the most recent job performed on the cluster, including any running job
   */
  public String getMessage() {
    return message;
  }

  /**
   * Get an immutable list of links that should be exposed as specified by the cluster template.
   *
   * @return Immutable list of links that should be exposed as specified by the cluster template
   */
  public List<Link> getLinks() {
    return links;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClusterDetails)) {
      return false;
    }

    ClusterDetails that = (ClusterDetails) o;
    return super.equals(that) &&
      Objects.equal(nodes, that.nodes) &&
      Objects.equal(progress, that.progress) &&
      Objects.equal(message, that.message) &&
      Objects.equal(links, that.links);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), nodes, progress, message, links);
  }
}
