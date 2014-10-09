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
package co.cask.coopr.scheduler.task;

import co.cask.coopr.TestHelper;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.ServiceTestBase;
import co.cask.coopr.spec.service.Service;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test NodeService
 */
public class NodeServiceTest extends ServiceTestBase {

  @Test
  public void testCreateHostname() {
    String clusterId = "00000001";
    Assert.assertEquals("i-am-a-cluster1-1002.local",
                        NodeService.createHostname("123i.am-a_clusTer", clusterId, 1002, null));
  }

  @Test
  public void testLongHostname() {
    String clusterId = "00000123";
    String domain = "dev.company.com";
    String labelSuffix = "123-1002";
    StringBuilder longName = new StringBuilder();
    for (int i = 0; i < 63 - labelSuffix.length(); i++) {
      longName.append("a");
    }
    String bigName = longName.toString();
    Assert.assertEquals(bigName + labelSuffix + "." + domain,
                        NodeService.createHostname(bigName + "bcde", clusterId, 1002, domain));

  }

  @Test
  public void testNodeMaxActions() throws Exception {
    conf.setInt(Constants.MAX_PER_NODE_NUM_ACTIONS, 3);
    conf.setInt(Constants.MAX_PER_NODE_LOG_LENGTH, 100);
    NodeService nodeService = injector.getInstance(NodeService.class);

    Node node = new Node("1", "1", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    Assert.assertTrue(node.getActions().isEmpty());

    for (int i = 0; i < 3; ++i) {
      Assert.assertEquals(i, node.getActions().size());

      nodeService.startAction(node, "taskId", "service" + i, "action" + i);
      nodeService.completeAction(node);

      Assert.assertEquals(i + 1, node.getActions().size());
    }

    nodeService.startAction(node, "taskId", "service3", "action3");
    nodeService.completeAction(node);

    Assert.assertEquals(3, node.getActions().size());
    Assert.assertEquals(Lists.newArrayList("service1", "service2", "service3"),
                        Lists.newArrayList(Iterables.transform(node.getActions(), new Function<Node.Action, String>() {
                          @Override
                          public String apply(Node.Action input) {
                            return input.getService();
                          }
                        })));
  }

  @Test
  public void testTruncateLog1() throws Exception {
    conf.setInt(Constants.MAX_PER_NODE_NUM_ACTIONS, 3);
    conf.setInt(Constants.MAX_PER_NODE_LOG_LENGTH, 8);
    NodeService nodeService = injector.getInstance(NodeService.class);

    Node node = new Node("1", "1", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    nodeService.startAction(node, "taskId", "service", "action");
    nodeService.failAction(node, "1234567890", "0987654321");

    Assert.assertEquals(1, node.getActions().size());
    Assert.assertEquals("[snipped]34567890", node.getActions().get(0).getStdout());
    Assert.assertEquals("[snipped]87654321", node.getActions().get(0).getStderr());
  }

  @Test
  public void testTruncateLog2() throws Exception {
    conf.setInt(Constants.MAX_PER_NODE_NUM_ACTIONS, 3);
    conf.setInt(Constants.MAX_PER_NODE_LOG_LENGTH, 10);
    NodeService nodeService = injector.getInstance(NodeService.class);

    Node node = new Node("1", "1", ImmutableSet.<Service>of(), TestHelper.EMPTY_NODE_PROPERTIES);
    nodeService.startAction(node, "taskId", "service", "action");
    nodeService.failAction(node, "1234567890", "0987654321");

    Assert.assertEquals(1, node.getActions().size());
    Assert.assertEquals("1234567890", node.getActions().get(0).getStdout());
    Assert.assertEquals("0987654321", node.getActions().get(0).getStderr());
  }

  @Test
  public void testTruncate() throws Exception {
    String shortStr = "this is a short string";
    Assert.assertEquals(shortStr, NodeService.truncateLog(shortStr, shortStr.length()));
    Assert.assertEquals("[snipped]short string", NodeService.truncateLog(shortStr, 12));

    String newLineString = "this \nis a string \nwith newlines";
    Assert.assertEquals(newLineString, NodeService.truncateLog(newLineString, newLineString.length() + 10));
    Assert.assertEquals("[snipped]ing \nwith newlines", NodeService.truncateLog(newLineString, 18));
    Assert.assertEquals("[snipped]is a string \nwith newlines", NodeService.truncateLog(newLineString, 26));
  }
}
