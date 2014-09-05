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

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * tests the macro expander.
 */
public class ExpanderTest {

  private static Set<Node> clusterNodes = ExpressionTest.clusterNodes;
  private static Cluster cluster = ExpressionTest.cluster;
  private static Node node1 = ExpressionTest.node1;
  private static Node node2 = ExpressionTest.node2;

  @Test
  public void testValidation() throws SyntaxException {
    Expander.validate("zookeeper.quorum = %join(map(host.service.zk,'$:2181'),',')%");
    Expander.validate("this %% is not a macro, but this is: %host.service.zk%");
  }

  @Test
  public void testExpansion() throws SyntaxException, IncompleteClusterException {
    Assert.assertEquals(
      "service one: rab:2181,oof:2181,eno:2181",
      Expander.expand("service one: %join(map(host.service.svc1,'$:2181'),',')%", cluster, clusterNodes, node1));
  }

  @Test
  public void testExpansionCreatesNewJson() throws SyntaxException, IncompleteClusterException {
    JsonObject input = new JsonObject();
    input.addProperty("host", "%host.service.svc1%");
    JsonObject output = Expander.expand(input, null, cluster, clusterNodes, node2).getAsJsonObject();
    Assert.assertEquals("rab,oof,eno", output.get("host").getAsString());
    Assert.assertEquals("%host.service.svc1%", input.get("host").getAsString());
  }

  @Test
  public void testJsonExpansion() throws SyntaxException, IncompleteClusterException {
    JsonElement json = new Gson().fromJson(jsonIn, JsonElement.class);
    JsonElement json1 = Expander.expand(json, ImmutableList.of("defaults", "config"), cluster, clusterNodes, node2);
    Assert.assertNotSame(json, json1);
    Assert.assertTrue(json1.toString().contains("hdfs://rab,oof,eno"));
    Assert.assertTrue(json1.toString().contains("quorum\":\"oof:2181,owt:2181\""));
    Assert.assertTrue(json1.toString().contains("server.1\":\"oof:2888:3888\""));
    Assert.assertTrue(json1.toString().contains("server.2\":\"rab:2888:3888\""));
    Assert.assertTrue(json1.toString().contains("server.3\":\"eno:2888:3888\""));
    Assert.assertTrue(json1.toString().contains("myid\":\"2\""));
    Assert.assertTrue(json1.toString().contains("quorum.size\":\"3\""));
    Assert.assertTrue(json1.toString().contains("email\":\"" + cluster.getAccount().getUserId() + "@company.net\""));
    // should not expand if the self macro is not for the node
    Assert.assertTrue(json1.toString().contains("dummyvar\":\"%instance.self.service.svc2%\""));
  }

  @Test
  public void testJsonExpansionSkipsUnexpandableMacros() throws SyntaxException, IncompleteClusterException {
    JsonObject input = new Gson().fromJson(jsonIn, JsonObject.class);
    input.addProperty("invalid-cluster-macro", "%host.service.svc4%");
    JsonElement expanded = Expander.expand(input, null, cluster, clusterNodes, node1);
    Assert.assertEquals("%host.service.svc4%", expanded.getAsJsonObject().get("invalid-cluster-macro").getAsString());
  }

  static String jsonIn = "{\n" +
    "  \"name\": \"hadoop\",\n" +
    "  \"description\": \"Hadoop cluster without high-availability\",\n" +
    "  \"defaults\": {\n" +
    "    \"services\": [\n" +
    "      \"hosts\",\n" +
    "      \"hadoop-hdfs-namenode\",\n" +
    "      \"hadoop-hdfs-datanode\",\n" +
    "      \"hadoop-yarn-resourcemanager\",\n" +
    "      \"hadoop-yarn-nodemanager\",\n" +
    "      \"zookeeper-server\",\n" +
    "      \"hbase-master\",\n" +
    "      \"hbase-regionserver\"\n" +
    "    ],\n" +
    "    \"provider\": \"rackspace\",\n" +
    "    \"hardwaretype\": \"medium\",\n" +
    "    \"imagetype\": \"ubuntu12\",\n" +
    "    \"config\": {\n" +
    "      \"hadoop\": {\n" +
    "        \"core_site\": {\n" +
    "          \"fs.defaultFS\": \"hdfs://%host.service.svc1%\"\n" +
    "        }\n" +
    "      },\n" +
    "      \"hbase\": {\n" +
    "        \"hbase_site\": {\n" +
    "          \"hbase.zookeeper.quorum\": \"%join(map(host.service.svc2,'$:2181'),',')%\",\n" +
    "          \"dummyvar\":\"%instance.self.service.svc2%\"\n" +
    "        }\n" +
    "      },\n" +
    "      \"zookeeper\": {\n" +
    "        \"myid\": \"%instance.self.service.svc1%\",\n" +
    "        \"quorum.size\": \"%num.service.svc1%\",\n" +
    "        \"zoocfg\": {\n" +
    "          \"server.1\": \"%host.service.svc1[0]%:2888:3888\",\n" +
    "          \"server.2\": \"%host.service.svc1[1]%:2888:3888\",\n" +
    "          \"server.3\": \"%host.service.svc1[2]%:2888:3888\"\n" +
    "        }\n" +
    "      },\n" +
    "      \"myapp\": {\n" +
    "        \"email\": \"%cluster.owner%@company.net\"\n" +
    "      }\n" +
    "    }\n" +
    "  }\n" +
    "}";

}
