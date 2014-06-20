/**
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
package com.continuuity.test.input;

import com.continuuity.loom.codec.json.current.CurrentJsonSerde;
import com.continuuity.test.Constants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Read cluster files.
 */
public class ClusterReader {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterReader.class);

  private static final Gson GSON = new CurrentJsonSerde().getGson();
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final String URI = "/v1/loom/clusters/00000028";
  private static final List<String> KEYS = ImmutableList.of("00000139", "00000138", "00000135");
  private static final String CLUSTER_ID = "00000139";

  public JsonObject getCluster() throws Exception {
    try {
      JsonObject clusterDefinition =  GSON.fromJson(readCluster(Constants.CLUSTERDEF_FILE_NAME), JsonObject.class);
      return clusterDefinition.get(CLUSTER_ID).getAsJsonObject();
    } catch (JsonSyntaxException e) {
      LOG.error("Got exception while parsing JSON: ", e);
    }
    return null;
  }

  public JsonObject getCreateCluster() throws  Exception {
    try {
      JsonObject cluster = GSON.fromJson(readCluster(Constants.CLUSTER_CREATE_FILE_NAME), JsonObject.class);
      return cluster;
    } catch (JsonSyntaxException e) {
      LOG.error("Got exception while parsing JSON: ", e);
    }
    return null;
  }


  public Set<TestCluster> getClusters(String status) throws Exception {
    Set<TestCluster> testClusters = Sets.newHashSet();
    try {
      JsonObject clusters = GSON.fromJson(readCluster(Constants.CLUSTERS_FILE_NAME), JsonObject.class);
      for (String key : KEYS) {
        TestCluster cluster = parseCluster(clusters.get(key).getAsJsonObject(), status);
        if (cluster != null) {
          testClusters.add(cluster);
        }
      }
      return testClusters;
    } catch (JsonSyntaxException e) {
      LOG.error("Got exception while parsing JSON ", e);
    }
    return null;
  }

  private TestCluster parseCluster(JsonObject json, String status) {
    if (status.equalsIgnoreCase(json.get("status").getAsString())) {
      // Convert GMT to PST
      long tsPST = Long.parseLong(json.get("createTime").getAsString()) / 1000 + 8 * 60 * 60;
      String expectedDate = DATE_FORMAT.format(new Date(tsPST * 1000));

      return new TestCluster(json.get("name").getAsString(), json.get("id").getAsString(),
                                       expectedDate, json.get("clusterTemplate").getAsString(),
                                       Integer.parseInt(json.get("numNodes").getAsString()));
    }
    return null;
  }

  private String readCluster(String fileName) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    StringBuilder sb = new StringBuilder();
    String currentline;
    while ((currentline = br.readLine()) != null) {
      sb.append(currentline);
    }
    return sb.toString();
  }
}
