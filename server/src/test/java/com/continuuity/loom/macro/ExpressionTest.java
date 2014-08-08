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
package com.continuuity.loom.macro;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.cluster.NodeProperties;
import com.continuuity.loom.common.utils.ImmutablePair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static com.continuuity.loom.macro.Expression.Type.CLUSTER_OWNER;
import static com.continuuity.loom.macro.Expression.Type.HOST_OF_SERVICE;
import static com.continuuity.loom.macro.Expression.Type.IP_OF_SERVICE;

/**
 * Testing expression evaluation.
 */
public class ExpressionTest {

  static Cluster cluster;
  static Set<Node> clusterNodes;
  static Node node1;
  static Node node2;

  static {
    Service svc1 = new Service("svc1", "service 1", ImmutableSet.<String>of(),
                               ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Service svc2 = new Service("svc2", "service 2", ImmutableSet.<String>of(),
                               ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Service svc3 = new Service("svc3", "service 3", ImmutableSet.<String>of(),
                               ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Node foo = new Node("foo", "1", ImmutableSet.of(svc1, svc2),
                        NodeProperties.builder().setHostname("oof").setIpaddress("9.7.8.4").setNodenum(0).build());
    Node bar = new Node("bar", "1", ImmutableSet.of(svc1),
                        NodeProperties.builder().setHostname("rab").setIpaddress("9.6.8.1").setNodenum(5).build());
    Node one = new Node("one", "1", ImmutableSet.of(svc1),
                        NodeProperties.builder().setHostname("eno").setIpaddress("9.1.3.4").setNodenum(8).build());
    Node two = new Node("two", "1", ImmutableSet.of(svc2),
                        NodeProperties.builder().setHostname("owt").setNodenum(9).build());
    Node thr = new Node("thr", "1", ImmutableSet.of(svc3),
                        NodeProperties.builder().setIpaddress("9.6.8.8").setNodenum(10).build());
    cluster = new Cluster("1", new Account("user", "tenant"), "cluster", System.currentTimeMillis(), "testy cluster",
                          null, null, ImmutableSet.of(foo.getId(), bar.getId(), one.getId(), two.getId(), thr.getId()),
                          ImmutableSet.of(svc1.getName(), svc2.getName(), svc3.getName()));
    clusterNodes = Sets.newTreeSet(Sets.newHashSet(foo, bar, one, two, thr));
    node1 = foo;
    node2 = bar;
  }

  @Test(expected = IncompleteClusterException.class)
  public void testNoHost() throws Exception {
    new Expression(HOST_OF_SERVICE, "svc3", null, null, null).evaluate(cluster, clusterNodes, node1);
  }

  @Test(expected = IncompleteClusterException.class)
  public void testNoIp() throws Exception {
    new Expression(Expression.Type.IP_OF_SERVICE, "svc2", null, null, null).evaluate(cluster, clusterNodes, node1);
  }

  @Test(expected = IncompleteClusterException.class)
  public void testOutOfBounds() throws Exception {
    new Expression(Expression.Type.IP_OF_SERVICE, "svc1", null, null, 3).evaluate(cluster, clusterNodes, node1);
  }

  @Test
  public void testNoService() throws Exception {
    Assert.assertNull(
      new Expression(Expression.Type.IP_OF_SERVICE, "svc4", null, null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testClusterOwner() throws Exception {
    Assert.assertEquals(
      cluster.getAccount().getUserId(),
      new Expression(Expression.Type.CLUSTER_OWNER, null, null, null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testPlain() throws Exception {
    Assert.assertEquals(
      "9.6.8.1,9.7.8.4,9.1.3.4",
      new Expression(Expression.Type.IP_OF_SERVICE, "svc1", null, null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testSelfServiceInstanceNum() throws Exception {
    Assert.assertEquals(
      "1",
      new Expression(Expression.Type.SELF_INSTANCE_OF_SERVICE, "svc1", null, null, null)
        .evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      "2",
      new Expression(Expression.Type.SELF_INSTANCE_OF_SERVICE, "svc1", null, null, null)
        .evaluate(cluster, clusterNodes, node2));
  }

  @Test
  public void testSelfServiceHost() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getHostname(),
      new Expression(Expression.Type.SELF_HOST_OF_SERVICE, "svc1", null, null, null)
        .evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getHostname(),
      new Expression(Expression.Type.SELF_HOST_OF_SERVICE, "svc1", null, null, null)
        .evaluate(cluster, clusterNodes, node2));
  }

  @Test
  public void testServiceInstanceHost() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getHostname(),
      new Expression(Expression.Type.HOST_OF_SERVICE, "svc1", null, null, 0).evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getHostname(),
      new Expression(Expression.Type.HOST_OF_SERVICE, "svc1", null, null, 1).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testSelfServiceIp() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getIpaddress(),
      new Expression(Expression.Type.SELF_IP_OF_SERVICE, "svc1", null, null, null)
        .evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getIpaddress(),
      new Expression(Expression.Type.SELF_IP_OF_SERVICE, "svc1", null, null, null)
        .evaluate(cluster, clusterNodes, node2));
  }

  @Test
  public void testServiceIpInstance() throws Exception {
    Assert.assertEquals(
      node1.getProperties().getIpaddress(),
      new Expression(Expression.Type.IP_OF_SERVICE, "svc1", null, null, 0).evaluate(cluster, clusterNodes, node1));
    Assert.assertEquals(
      node2.getProperties().getIpaddress(),
      new Expression(Expression.Type.IP_OF_SERVICE, "svc1", null, null, 1).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testFormatOnly() throws Exception {
    Assert.assertEquals(
      "oof:2181,owt:2181",
      new Expression(HOST_OF_SERVICE, "svc2", "$:2181", null, null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testJoinOnly() throws Exception {
    Assert.assertEquals(
      "rab-oof-eno",
      new Expression(HOST_OF_SERVICE, "svc1", null, "-", null).evaluate(cluster, clusterNodes, node1));
  }

  @Test
  public void testFormatJoin() throws Exception {
    Assert.assertEquals(
      "oof:2181++owt:2181",
      new Expression(HOST_OF_SERVICE, "svc2", "$:2181", "++", null).evaluate(cluster, clusterNodes, node1));
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
      new Expression(null, null, cases[i], null, null).format("foo", builder);
      Assert.assertEquals(cases[i + 1], builder.toString());
    }
  }

  @Test
  public void testParseMacroName() throws SyntaxException {
    Assert.assertEquals(ImmutablePair.of(HOST_OF_SERVICE, "abc"), Expression.typeAndNameOf("host.service.abc"));
    Assert.assertEquals(ImmutablePair.of(IP_OF_SERVICE, "abc"), Expression.typeAndNameOf("ip.service.abc"));
    Assert.assertEquals(ImmutablePair.of(CLUSTER_OWNER, (String) null), Expression.typeAndNameOf("cluster.owner"));
    for (String macro : ImmutableList.of("", "host.service.", "IP_OF_SERVICE.abc", "SERVICE_BULLSHIT_abc")) {
      try {
        Expression.typeAndNameOf(macro);
        Assert.fail("'" + macro + "' should have thrown syntax exception.");
      } catch (SyntaxException e) {
        // expected
      }
    }
  }
}
