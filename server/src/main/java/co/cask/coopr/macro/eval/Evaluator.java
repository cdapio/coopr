/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.macro.eval;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.macro.IncompleteClusterException;

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
