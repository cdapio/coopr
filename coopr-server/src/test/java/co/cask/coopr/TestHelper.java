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
package co.cask.coopr;

import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.http.request.FinishTaskRequest;
import co.cask.coopr.http.request.TakeTaskRequest;
import co.cask.coopr.scheduler.task.SchedulableTask;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.inject.Guice;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 *
 */
public class TestHelper {
  private static final Gson GSON = Guice.createInjector(new CodecModules().getModule()).getInstance(Gson.class);
  public static final NodeProperties EMPTY_NODE_PROPERTIES = NodeProperties.builder().build();

  public static SchedulableTask takeTask(String serverUrl, TakeTaskRequest request) throws Exception {
    HttpPost httpPost = new HttpPost(String.format("%s/tasks/take", serverUrl));
    httpPost.setEntity(new StringEntity(GSON.toJson(request)));

    CloseableHttpClient httpClient = HttpClients.createDefault();
    CloseableHttpResponse response = httpClient.execute(httpPost);
    try {
      Assert.assertEquals(2, response.getStatusLine().getStatusCode() / 100);
      if (response.getEntity() == null) {
        return null;
      }
      return GSON.fromJson(EntityUtils.toString(response.getEntity()), SchedulableTask.class);
    } finally {
      response.close();
    }
  }

  public static void finishTask(String serverUrl, FinishTaskRequest finishRequest) throws Exception {
    HttpPost httpPost = new HttpPost(String.format("%s/tasks/finish", serverUrl));
    httpPost.setEntity(new StringEntity(GSON.toJson(finishRequest)));

    CloseableHttpClient httpClient = HttpClients.createDefault();
    CloseableHttpResponse response = httpClient.execute(httpPost);
    try {
      Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    } finally {
      response.close();
    }
  }

  public static JsonArray jsonArrayOf(String... values) {
    JsonArray output = new JsonArray();
    for (String value : values) {
      output.add(new JsonPrimitive(value));
    }
    return output;
  }

  public static Map<String, String> actionMapOf(String script, String data) {
    Map<String, String> out = Maps.newHashMap();
    if (script != null) {
      out.put("script", script);
    }
    if (data != null) {
      out.put("data", data);
    }
    return out;
  }

  /**
   * Retrieves a free port
   *
   * @return int port value
   * @throws IOException if an I/O error occurs when opening the socket.
   */
  public static int getFreePort() throws IOException {
    ServerSocket server = new ServerSocket(0);
    try {
      return server.getLocalPort();
    } finally {
      server.close();
    }
  }
}
