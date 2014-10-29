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
package co.cask.coopr.codec.json.current;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.ClusterJobProgress;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.codec.json.AbstractCodec;
import co.cask.coopr.spec.Link;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Codec for serializing {@link co.cask.coopr.cluster.ClusterDetails}.
 */
public class ClusterDetailsCodec extends AbstractCodec<ClusterDetails> {

  private static final String NODES_KEY = "nodes";
  private static final String LINKS_KEY = "links";
  private static final String PROGRESS_JOB_KEY = "progress";
  private static final String MESSAGE_KEY = "message";
  private static final Type NODES_TYPE = new TypeToken<Set<Node>>() { }.getType();
  private static final Type LINKS_TYPE = new TypeToken<List<Link>>() { }.getType();

  @Override
  public JsonElement serialize(ClusterDetails src, Type typeOfSrc, JsonSerializationContext context) {
    // flattening the cluster object and adding the rest of the cluster details fields on top of the cluster's
    // fields. This weirdness is to preserve the cluster rest api, which was doing some weird combining of a cluster
    // and its nodes and its latest job.
    JsonObject jsonObj = ClusterCodec.serializeCluster(src.getCluster(), context);

    // node ids get overwritten by the full node objects
    jsonObj.add(NODES_KEY, context.serialize(src.getNodes()));
    jsonObj.add(LINKS_KEY, context.serialize(src.getLinks()));
    jsonObj.add(PROGRESS_JOB_KEY, context.serialize(src.getProgress()));
    jsonObj.add(MESSAGE_KEY, context.serialize(src.getMessage()));

    return jsonObj;
  }


  @Override
  public ClusterDetails deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException {

    JsonObject jsonObject = json.getAsJsonObject();

    Cluster cluster = ClusterCodec.deserializeCluster(json, context);

    Set<Node> nodes = context.deserialize(jsonObject.get(NODES_KEY), NODES_TYPE);
    List<Link> links = context.deserialize(jsonObject.get(LINKS_KEY), LINKS_TYPE);
    ClusterJobProgress progress = context.deserialize(jsonObject.get(PROGRESS_JOB_KEY), ClusterJobProgress.class);
    String msg = context.deserialize(jsonObject.get(MESSAGE_KEY), String.class);
    cluster.setNodes(getNodeIds(nodes));

    return new ClusterDetails(cluster, links, nodes, progress, msg);
  }

  /**
   * Retrieves {@link Set} of nodes ids.
   *
   * @param nodes the nodes
   * @return the {@link Set} of nodes ids
   */
  private Set<String> getNodeIds(Set<Node> nodes) {
    Set<String> nodeIds = new HashSet<String>();
    for (Node node : nodes) {
      nodeIds.add(node.getId());
    }
    return nodeIds;
  }
}
