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
package co.cask.coopr.scheduler.action;

import co.cask.coopr.scheduler.dag.TaskDag;
import co.cask.coopr.scheduler.dag.TaskNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

/**
 * Test TaskDag.
 */
public class TaskDagTest {
  @Test
  public void testDagSingleHost() {
    TaskDag taskDag = new TaskDag();

    taskDag.addDependency(new TaskNode("1", "provision", ""), new TaskNode("1", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "install", "datanode"));
    taskDag.addDependency(new TaskNode("1", "install", "datanode"), new TaskNode("1", "configure", "datanode"));
    taskDag.addDependency(new TaskNode("1", "configure", "datanode"), new TaskNode("1", "initialize", "datanode"));
    taskDag.addDependency(new TaskNode("1", "initialize", "datanode"), new TaskNode("1", "start", "datanode"));

    taskDag.addDependency(new TaskNode("1", "provision", ""), new TaskNode("1", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "install", "region_server"));
    taskDag.addDependency(new TaskNode("1", "install", "region_server"),
                          new TaskNode("1", "configure", "region_server"));
    taskDag.addDependency(new TaskNode("1", "configure", "region_server"),
                          new TaskNode("1", "initialize", "region_server"));
    taskDag.addDependency(new TaskNode("1", "initialize", "region_server"),
                          new TaskNode("1", "start", "region_server"));

    taskDag.addDependency(new TaskNode("1", "initialize", "datanode"),
                          new TaskNode("1", "initialize", "region_server"));  // cross dependency
    taskDag.addDependency(new TaskNode("1", "initialize", "datanode"),
                          new TaskNode("1", "start", "region_server"));  // cross dependency
    taskDag.addDependency(new TaskNode("1", "start", "datanode"),
                          new TaskNode("1", "initialize", "region_server"));  // cross dependency

    List<Set<TaskNode>> actual = taskDag.linearize();

    List<ImmutableSet<TaskNode>> expected =
      ImmutableList.of(
        ImmutableSet.of(new TaskNode("1", "provision", "")),
        ImmutableSet.of(new TaskNode("1", "bootstrap", "")),
        ImmutableSet.of(new TaskNode("1", "install", "datanode"), new TaskNode("1", "install", "region_server")),
        ImmutableSet.of(new TaskNode("1", "configure", "datanode"), new TaskNode("1", "configure", "region_server")),
        ImmutableSet.of(new TaskNode("1", "initialize", "datanode")),
        ImmutableSet.of(new TaskNode("1", "start", "datanode")),
        ImmutableSet.of(new TaskNode("1", "initialize", "region_server")),
        ImmutableSet.of(new TaskNode("1", "start", "region_server"))
    );

    //noinspection AssertEqualsBetweenInconvertibleTypes
    Assert.assertEquals(expected, actual);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoDag() {
    TaskDag taskDag = new TaskDag();

    taskDag.addDependency(new TaskNode("1", "provision", ""), new TaskNode("1", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "install", "datanode"));
    taskDag.addDependency(new TaskNode("1", "install", "datanode"), new TaskNode("1", "configure", "datanode"));
    taskDag.addDependency(new TaskNode("1", "configure", "datanode"), new TaskNode("1", "start", "datanode"));

    taskDag.addDependency(new TaskNode("1", "provision", ""), new TaskNode("1", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "bootstrap", ""));    // cycle
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "install", "region_server"));
    taskDag.addDependency(new TaskNode("1", "install", "region_server"),
                          new TaskNode("1", "configure", "region_server"));
    taskDag.addDependency(new TaskNode("1", "start", "datanode"),
                          new TaskNode("1", "configure", "region_server"));  // cross dependency
    taskDag.addDependency(new TaskNode("1", "configure", "region_server"), new TaskNode("1", "start", "region_server"));

    taskDag.linearize();  // Should throw exception as there is a cycle.
  }

  @Test
  public void testDagMultipleHosts() {
    TaskDag taskDag = new TaskDag();

    // First host
    taskDag.addDependency(new TaskNode("1", "provision", ""), new TaskNode("1", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "install", "datanode"));
    taskDag.addDependency(new TaskNode("1", "install", "datanode"), new TaskNode("1", "configure", "datanode"));
    taskDag.addDependency(new TaskNode("1", "configure", "datanode"), new TaskNode("1", "start", "datanode"));

    taskDag.addDependency(new TaskNode("1", "provision", ""), new TaskNode("1", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("1", "bootstrap", ""), new TaskNode("1", "install", "region_server"));
    taskDag.addDependency(new TaskNode("1", "install", "region_server"),
                          new TaskNode("1", "configure", "region_server"));
    taskDag.addDependency(new TaskNode("1", "start", "datanode"), new TaskNode("1", "configure", "region_server"));
    taskDag.addDependency(new TaskNode("1", "configure", "region_server"), new TaskNode("1", "start", "region_server"));

    // Second host
    taskDag.addDependency(new TaskNode("2", "provision", ""), new TaskNode("2", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("2", "bootstrap", ""), new TaskNode("2", "install", "namenode"));
    taskDag.addDependency(new TaskNode("2", "install", "namenode"), new TaskNode("2", "configure", "namenode"));
    taskDag.addDependency(new TaskNode("2", "configure", "namenode"), new TaskNode("2", "start", "namenode"));
    taskDag.addDependency(new TaskNode("1", "start", "datanode"),
                          new TaskNode("2", "start", "namenode"));             // cross dependency

    taskDag.addDependency(new TaskNode("2", "provision", ""), new TaskNode("2", "bootstrap", ""));
    taskDag.addDependency(new TaskNode("2", "bootstrap", ""), new TaskNode("2", "install", "hbase_master"));
    taskDag.addDependency(new TaskNode("2", "install", "hbase_master"),
                          new TaskNode("2", "configure", "hbase_master"));
    taskDag.addDependency(new TaskNode("1", "start", "datanode"),
                          new TaskNode("2", "configure", "hbase_master"));     // cross dependency
    taskDag.addDependency(new TaskNode("2", "start", "namenode"),
                          new TaskNode("2", "configure", "hbase_master"));     // cross dependency
    taskDag.addDependency(new TaskNode("2", "configure", "hbase_master"), new TaskNode("2", "start", "hbase_master"));

    List<Set<TaskNode>> actual = taskDag.linearize();

    List<ImmutableSet<TaskNode>> expected =
      ImmutableList.of(
        ImmutableSet.of(new TaskNode("1", "provision", ""), new TaskNode("2", "provision", "")),
        ImmutableSet.of(new TaskNode("1", "bootstrap", ""), new TaskNode("2", "bootstrap", "")),
        ImmutableSet.of(new TaskNode("1", "install", "datanode"), new TaskNode("1", "install", "region_server"),
                        new TaskNode("2", "install", "namenode"), new TaskNode("2", "install", "hbase_master")),
        ImmutableSet.of(new TaskNode("1", "configure", "datanode"), new TaskNode("2", "configure", "namenode")),
        ImmutableSet.of(new TaskNode("1", "start", "datanode")),
        ImmutableSet.of(new TaskNode("1", "configure", "region_server"), new TaskNode("2", "start", "namenode")),
        ImmutableSet.of(new TaskNode("1", "start", "region_server"), new TaskNode("2", "configure", "hbase_master")),
        ImmutableSet.of(new TaskNode("2", "start", "hbase_master"))
      );

    //noinspection AssertEqualsBetweenInconvertibleTypes
    Assert.assertEquals(expected, actual);
  }
}
