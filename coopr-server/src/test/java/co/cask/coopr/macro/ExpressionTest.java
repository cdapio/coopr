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
package co.cask.coopr.macro;

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.macro.eval.ClusterOwnerEvaluator;
import co.cask.coopr.macro.eval.HostSelfEvaluator;
import co.cask.coopr.macro.eval.HostServiceEvaluator;
import co.cask.coopr.macro.eval.IPSelfEvaluator;
import co.cask.coopr.macro.eval.IPServiceEvaluator;
import co.cask.coopr.macro.eval.ServiceInstanceEvaluator;
import co.cask.coopr.spec.service.Service;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Testing expression evaluation.
 */
public class ExpressionTest {
  private static final String IP_TYPE = "access_v4";
  static Cluster cluster;
  static Set<Node> clusterNodes;
  static Node node1;
  static Node node2;

  static {
    Service svc1 = Service.builder().setName("svc1").build();
    Service svc2 = Service.builder().setName("svc2").build();
    Service svc3 = Service.builder().setName("svc3").build();
    Node foo = new Node("foo", "1", ImmutableSet.of(svc1, svc2),
                        NodeProperties.builder()
                          .setHostname("oof")
                          .addIPAddress("access_v4", "9.7.8.4")
                          .setNodenum(0).build());
    Node bar = new Node("bar", "1", ImmutableSet.of(svc1),
                        NodeProperties.builder()
                          .setHostname("rab")
                          .addIPAddress("access_v4", "9.6.8.1")
                          .setNodenum(5).build());
    Node one = new Node("one", "1", ImmutableSet.of(svc1),
                        NodeProperties.builder()
                          .setHostname("eno")
                          .addIPAddress("access_v4", "9.1.3.4")
                          .setNodenum(8).build());
    Node two = new Node("two", "1", ImmutableSet.of(svc2),
                        NodeProperties.builder()
                          .setHostname("owt")
                          .setNodenum(9).build());
    Node thr = new Node("thr", "1", ImmutableSet.of(svc3),
                        NodeProperties.builder()
                          .addIPAddress("access_v4", "9.6.8.8")
                          .setNodenum(10).build());
    cluster = Cluster.builder()
      .setID("1")
      .setAccount(new Account("user", "tenant"))
      .setName("cluster")
      .setDescription("testy cluster")
      .setNodes(ImmutableSet.of(foo.getId(), bar.getId(), one.getId(), two.getId(), thr.getId()))
      .setServices(ImmutableSet.of(svc1.getName(), svc2.getName(), svc3.getName()))
      .build();
    clusterNodes = Sets.newTreeSet(Sets.newHashSet(foo, bar, one, two, thr));
    node1 = foo;
    node2 = bar;
  }

  @Test(expected = IncompleteClusterException.class)
  public void testNoHost() throws Exception {
    new Expression(new HostServiceEvaluator("svc3", null), null, null).evaluate(cluster, clusterNodes, node1);
  }

  @Test(expected = IncompleteClusterException.class)
  public void testNoIp() throws Exception {
    new Expression(new IPServiceEvaluator("svc2", IP_TYPE, null), null, null).evaluate(cluster, clusterNodes, node1);
  }

  @Test(expected = IncompleteClusterException.class)
  public void testOutOfBounds() throws Exception {
    new Expression(new IPServiceEvaluator("svc1", IP_TYPE, 3), null, null).evaluate(cluster, clusterNodes, node1);
  }

  @Test
  public void testNoService() throws Exception {
    Assert.assertNull(new Expression(
      new IPServiceEvaluator("svc4", IP_TYPE, null), null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testClusterOwner() throws Exception {
    Assert.assertEquals(
      cluster.getAccount().getUserId(),
      new Expression(new ClusterOwnerEvaluator(), null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testPlain() throws Exception {
    Assert.assertEquals(
      "9.6.8.1,9.7.8.4,9.1.3.4",
      new Expression(new IPServiceEvaluator("svc1", IP_TYPE, null), null, null)
        .evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testSelfServiceInstanceNum() throws Exception {
    Assert.assertEquals(
      "1",
      new Expression(new ServiceInstanceEvaluator("svc1"), null, null)
        .evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      "2",
      new Expression(new ServiceInstanceEvaluator("svc1"), null, null)
        .evaluate(cluster, clusterNodes, node2));
  }

  @Test
  public void testSelfServiceHost() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getHostname(),
      new Expression(new HostSelfEvaluator(), null, null)
        .evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getHostname(),
      new Expression(new HostSelfEvaluator(), null, null)
        .evaluate(cluster, clusterNodes, node2));
  }

  @Test
  public void testServiceInstanceHost() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getHostname(),
      new Expression(new HostServiceEvaluator("svc1", 0), null, null).evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getHostname(),
      new Expression(new HostServiceEvaluator("svc1", 1), null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testSelfServiceIp() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getIPAddress(IP_TYPE),
      new Expression(new IPSelfEvaluator(IP_TYPE), null, null)
        .evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getIPAddress(IP_TYPE),
      new Expression(new IPSelfEvaluator(IP_TYPE), null, null)
        .evaluate(cluster, clusterNodes, node2));
  }

  @Test
  public void testServiceIpInstance() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getIPAddress(IP_TYPE),
      new Expression(new IPServiceEvaluator("svc1", IP_TYPE, 0), null, null).evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getIPAddress(IP_TYPE),
      new Expression(new IPServiceEvaluator("svc1", IP_TYPE, 1), null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testFormatOnly() throws Exception {
    Assert.assertEquals(
      "oof:2181,owt:2181",
      new Expression(new HostServiceEvaluator("svc2", null), "$:2181", null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testJoinOnly() throws Exception {
    Assert.assertEquals(
      "rab-oof-eno",
      new Expression(new HostServiceEvaluator("svc1", null), null, "-").evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testFormatJoin() throws Exception {
    Assert.assertEquals(
      "oof:2181++owt:2181",
      new Expression(new HostServiceEvaluator("svc2", null), "$:2181", "++").evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testFormatting() {
    String[] cases = {
      "", "",
      "$", "foo",
      "$$", "$",
      "$$$", "$foo",
      "$ $", "foo foo",
      "$:2181", "foo:2181",
      "http://$/", "http://foo/"
    };
    for (int i = 0; i < cases.length; i += 2) {
      StringBuilder builder = new StringBuilder();
      new Expression(null, cases[i], null).format("foo", builder);
      Assert.assertEquals(cases[i + 1], builder.toString());
    }
  }
}
