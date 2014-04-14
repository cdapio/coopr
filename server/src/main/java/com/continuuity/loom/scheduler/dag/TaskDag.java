/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.scheduler.dag;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A DAG (directed acyclic graph) to linearize a set of dependent tasks.
 */
public class TaskDag {
  private static final Logger LOG = LoggerFactory.getLogger(TaskDag.class);

  // Nodes in the DAG
  private final Set<TaskNode> nodes;
  // Edges of the DAG
  private final SetMultimap<TaskNode, TaskNode> edges;

  public TaskDag() {
    this.nodes = Sets.newHashSet();
    this.edges = HashMultimap.create(100, 3);
  }

  /**
   * Defines a dependency between task fromNode to toNode.
   *
   * @param fromNode task that the toNode depends on.
   * @param toNode task that depends on the fromNode.
   */
  public void addDependency(TaskNode fromNode, TaskNode toNode) {
    nodes.add(fromNode);
    nodes.add(toNode);
    edges.put(fromNode, toNode);
  }

  /**
   * Add a node without any edges, meaning nothing depends on this node (yet).
   *
   * @param node task to add to the DAG.
   */
  public void addTaskNode(TaskNode node) {
    nodes.add(node);
  }

  /**
   * Linearize the DAG into a list of stages, where each stage is a set of tasks that can be executed in parallel, and
   * where each task in a stage can only be executed once all the tasks in the previous stage have successfully
   * completed.
   *
   * @return a list of set of actions that can be performed in order satisfying the dependencies.
   * The actions in each set can be run in parallel.
   */
  public List<Set<TaskNode>> linearize() {
    LOG.trace("Initial graph - {}", edges);

    List<Set<TaskNode>> linearizedNodes = Lists.newArrayList();
    ArrayListMultimap<TaskNode, TaskNode> copyEdges = ArrayListMultimap.create(edges);

    // nodes that have no edges are sources.  The initial set is actual nodes - edges.values(), but removal of
    // edges.values() happens right away in the loop so just setting it to nodes right here.
    Set<TaskNode> sources = Sets.newHashSet(nodes);
    Set<TaskNode> sinkNodes = Sets.newHashSet();
    do {
      sources.addAll(copyEdges.keySet());
      sources.removeAll(copyEdges.values());

      LOG.trace("SinkNodes - {}", sinkNodes);
      LOG.trace("Sources - {}", sources);
      if (sources.isEmpty()) {
        throw new IllegalStateException("No source nodes found, DAG not serializable");
      }

      Set<TaskNode> stageNodes = Sets.newHashSet(Iterables.concat(sinkNodes, sources));
      LOG.trace("Stage Nodes - {}", stageNodes);
      linearizedNodes.add(stageNodes);

      // Determine sink nodes if any
      sinkNodes.clear();
      Set<TaskNode> toNodes = Sets.newHashSet();
      for (TaskNode fromNode : sources) {
        toNodes.addAll(copyEdges.removeAll(fromNode));
      }

      // If the node is not a source, or a destination of any other source then they are sink nodes.
      toNodes.removeAll(copyEdges.keySet());
      toNodes.removeAll(copyEdges.values());
      if (!toNodes.isEmpty()) {
        sinkNodes.addAll(toNodes);
      }
      sources.clear();
    } while (!copyEdges.isEmpty());

    // Add final nodes
    LOG.trace("Final nodes - {}", sinkNodes);
    if (!sinkNodes.isEmpty()) {
      linearizedNodes.add(sinkNodes);
    }

    return linearizedNodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskDag other = (TaskDag) o;
    return Objects.equal(nodes, other.nodes) && Objects.equal(edges, other.edges);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nodes, edges);
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    Comparator<TaskNode> comparator = new TaskNodeComparator();
    TreeSet<TaskNode> nodes = Sets.newTreeSet(comparator);
    nodes.addAll(this.nodes);
    SetMultimap<TaskNode, TaskNode> edges = TreeMultimap.create(comparator, comparator);
    edges.putAll(this.edges);
    output.append("services:\n");
    for (TaskNode node : nodes) {
      output.append(node);
      output.append("\n");
    }
    output.append("edges:\n");
    for (TaskNode startNode : edges.keySet()) {
      output.append("  ");
      output.append(startNode);
      output.append("\n");
      for (TaskNode endNode : edges.get(startNode)) {
        output.append("    -> ");
        output.append(endNode);
        output.append("\n");
      }
    }
    return output.toString();
  }

  private class TaskNodeComparator implements Comparator<TaskNode> {
    @Override
    public int compare(TaskNode taskNode, TaskNode taskNode2) {
      int compare = taskNode.getTaskName().compareTo(taskNode2.getTaskName());
      if (compare != 0) {
        return compare;
      }
      compare = taskNode.getService().compareTo(taskNode2.getService());
      if (compare != 0) {
        return compare;
      }
      return taskNode.getHostId().compareTo(taskNode2.getHostId());
    }
  }
}
