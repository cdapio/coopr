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
package co.cask.coopr.http;

import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.spec.service.Service;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class NodeHandlerTest extends ServiceTestBase {
  @BeforeClass
  public static void init() {
  }

  protected static List<JsonObject> getJsonListFromResponse(HttpResponse response) throws IOException {
    Reader reader = getInputStreamReaderFromResponse(response);
    List<JsonObject> result = gson.fromJson(reader, new TypeToken<List<JsonObject>>() {}.getType());
    reader.close();
    return result;
  }

  protected static JsonObject getJsonObjectFromResponse(HttpResponse response) throws IOException {
    Reader reader = getInputStreamReaderFromResponse(response);
    JsonObject object = gson.fromJson(reader, JsonObject.class);
    reader.close();
    return object;
  }

  protected static Reader getInputStreamReaderFromResponse(HttpResponse response) throws IOException {
    return new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
  }

  @After
  public void testCleanup() {
    // cleanup
    // TODO: Remove all nodes created
  }

  @Test
  public void testAddNodeAsUser() throws Exception {
    postNodes(1, USER1_HEADERS);
    List<JsonObject> nodes = getNodes(USER1_HEADERS);
    Assert.assertEquals(1, nodes.size());
  }

  @Test
  public void testGetAllNodesAsUser() throws Exception {
    Node postedNode = postNodes(1, USER1_HEADERS).get(0);
    List<JsonObject> nodes = getNodes(USER1_HEADERS);
    Assert.assertEquals(1, nodes.size());
    String nodeId = nodes.get(0).get("id").getAsString();
    String clusterId = nodes.get(0).get("clusterId").getAsString();
    Assert.assertEquals(nodeId, postedNode.getId());
    Assert.assertEquals(clusterId, postedNode.getClusterId());
  }

  @Test
  public void testDeleteNodeAsUser() throws Exception {
    Node node = postNodes(1, USER1_HEADERS).get(0);
    HttpResponse response = doDeleteExternalAPI("/nodes/" + node.getId(), USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.NO_CONTENT);
    List<JsonObject> nodes = getNodes(USER1_HEADERS);
    Assert.assertEquals(0, nodes.size());
  }

  @Test
  public void testUpdateNodeAsUser() throws Exception {
    Node node = postNodes(2, USER1_HEADERS).get(0);
    NodeProperties.Builder propertiesBuilder = NodeProperties.builder();
    propertiesBuilder.setHostname("my-updated-host");
    Node updatedNode = createNode(node.getId(),node.getClusterId(), node.getServices(), propertiesBuilder.build());
    String updatedNodeJsonString = getNodeAsJsonString(updatedNode);
    HttpResponse response = doPutExternalAPI("/nodes/" + node.getId(), updatedNodeJsonString, USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.NO_CONTENT);
    Node updatedNodeFromServer = convertNodeFromJson(getNode(USER1_HEADERS, updatedNode.getId()));
    Assert.assertEquals(node.getId(), updatedNodeFromServer.getId());
    Assert.assertNotEquals(node.getProperties(), updatedNodeFromServer.getProperties());
  }

  private List<JsonObject> getNodes(final Header[] headers) throws Exception {
    HttpResponse response = doGetExternalAPI("/nodes", headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    return getJsonListFromResponse(response);
  }

  private JsonObject getNode(final Header[] headers, final String nodeId) throws Exception {
    HttpResponse response = doGetExternalAPI("/nodes/" + nodeId, headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    return getJsonObjectFromResponse(response);
  }

  private List<Node> postNodes(int numberOfNodes, Header[] headers) throws Exception {
    List<Node> nodes = createNodes(numberOfNodes);
    for (Node node : nodes) {
      HttpResponse response = doPostExternalAPI("/nodes", gson.toJson(node), headers);
      assertResponseStatus(response, HttpResponseStatus.CREATED);
    }
    return nodes;
  }

  private String getNodeAsJsonString(final Node node) {
    return gson.toJson(node);
  }

  private List<Node> createNodes(int numberOfNodesToCreate) {
    List<Node> createdNodes = new ArrayList<Node>();
    for (int i = 0; i < numberOfNodesToCreate; i++) {
      createdNodes.add(createNode(Integer.toString(i)));
    }

    return createdNodes;
  }

  private Node createNode(String id, String clusterId, Set<Service> services, NodeProperties nodeProperties) {
    return new Node(id, clusterId, services, nodeProperties);
  }

  private Node createNode(String id) {
    return createNode(id, "1234", new HashSet<Service>(){}, NodeProperties.builder().build());
  }

  private Node convertNodeFromJson(JsonObject jsonNode) {
    return gson.fromJson(jsonNode, Node.class);
  }
}
