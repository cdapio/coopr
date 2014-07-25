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
package com.continuuity.loom.http;

import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Node;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class LoomNodeHandlerTest extends LoomServiceTestBase {
  @BeforeClass
  public static void init() {
  }

  protected static List<JsonObject> getJsonListFromResponse(HttpResponse response) throws IOException {
    Reader reader = getInputStreamReaderFromResponse(response);
    return gson.fromJson(reader, new TypeToken<List<JsonObject>>() {}.getType());
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
    postNode(USER1_HEADERS);
  }

  @Test
  public void testGetAllNodesAsUser() throws Exception {
    Node postedNode = postNode(USER1_HEADERS);
    HttpResponse response = doGet("/v1/loom/nodes", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    List<JsonObject> jsonObject = getJsonListFromResponse(response);
    Assert.assertEquals(1, jsonObject.size());
    String nodeId = jsonObject.get(0).get("id").getAsString();
    String clusterId = jsonObject.get(0).get("clusterId").getAsString();
    Assert.assertEquals(nodeId, postedNode.getId());
    Assert.assertEquals(clusterId, postedNode.getClusterId());
  }

  private Node postNode(Header[] headers) throws Exception {
    Node node = createNode();
    String nodeJsonString = gson.toJson(node);
    HttpResponse response = doPost("/v1/loom/nodes", nodeJsonString, headers);
    assertResponseStatus(response, HttpResponseStatus.CREATED);
    return node;
  }

  private Node createNode() {

    return new Node("id", "1234", new HashSet<Service>() {}, new HashMap<String, String>());
  }
}
