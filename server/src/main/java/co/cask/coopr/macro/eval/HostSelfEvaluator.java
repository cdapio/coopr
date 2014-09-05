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
import com.google.common.collect.ImmutableList;

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
    return ImmutableList.of(node.getProperties().getHostname());
  }
}
