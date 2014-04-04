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
package com.continuuity.loom;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import java.util.Map;

/**
 *
 */
public class TestHelper {
  private static Gson GSON = new Gson();
  public static JsonObject takeTask(String loomUrl, String workerId) throws Exception{
    Map<String, String> properties = ImmutableMap.of("workerId", workerId);
    HttpPost httpPost = new HttpPost(String.format("%s/v1/loom/tasks/take", loomUrl));
    httpPost.setEntity(new StringEntity(GSON.toJson(properties)));

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpResponse response = httpClient.execute(httpPost);
    Assert.assertEquals(2, response.getStatusLine().getStatusCode() / 100);
    if (response.getEntity() == null) {
      return new JsonObject();
    }
    return GSON.fromJson(EntityUtils.toString(response.getEntity()), JsonElement.class).getAsJsonObject();
  }

  public static void finishTask(String loomUrl, JsonObject jsonObject) throws Exception {
    HttpPost httpPost = new HttpPost(String.format("%s/v1/loom/tasks/finish", loomUrl));
    httpPost.setEntity(new StringEntity(GSON.toJson(jsonObject)));

    DefaultHttpClient httpClient = new DefaultHttpClient();
    HttpResponse response = httpClient.execute(httpPost);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  public static JsonArray jsonArrayOf(String... values) {
    JsonArray output = new JsonArray();
    for (String value : values) {
      output.add(new JsonPrimitive(value));
    }
    return output;
  }
}
