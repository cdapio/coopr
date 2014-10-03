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

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.codec.json.AbstractCodec;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.template.ClusterTemplate;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Codec for serializing/deserializing a {@link Cluster}. Used to make sure required fields are present.
 */
public class ClusterCodec extends AbstractCodec<Cluster> {

  @Override
  public Cluster deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    return Cluster.builder()
      .setID(context.<String>deserialize(jsonObj.get("id"), String.class))
      .setName(context.<String>deserialize(jsonObj.get("name"), String.class))
      .setDescription(context.<String>deserialize(jsonObj.get("description"), String.class))
      .setAccount(context.<Account>deserialize(jsonObj.get("account"), Account.class))
      .setCreateTime(jsonObj.get("createTime").getAsLong())
      .setExpireTime(context.<Long>deserialize(jsonObj.get("expireTime"), Long.class))
      .setProvider(context.<Provider>deserialize(jsonObj.get("provider"), Provider.class))
      .setClusterTemplate(context.<ClusterTemplate>deserialize(jsonObj.get("clusterTemplate"), ClusterTemplate.class))
      .setNodes(context.<Set<String>>deserialize(jsonObj.get("nodes"), new TypeToken<Set<String>>() {}.getType()))
      .setServices(context.<Set<String>>deserialize(jsonObj.get("services"), new TypeToken<Set<String>>() {}.getType()))
      .setLatestJobID(context.<String>deserialize(jsonObj.get("latestJobId"), String.class))
      .setStatus(context.<Cluster.Status>deserialize(jsonObj.get("status"), Cluster.Status.class))
      .setConfig(context.<JsonObject>deserialize(jsonObj.get("config"), JsonObject.class))
      .build();
  }

  @Override
  public JsonElement serialize(Cluster src, Type typeOfSrc, JsonSerializationContext context) {
    return serializeCluster(src, context);
  }

  public static JsonObject serializeCluster(Cluster cluster, JsonSerializationContext context) {

    JsonObject jsonObj = new JsonObject();

    jsonObj.add("id", context.serialize(cluster.getId()));
    jsonObj.add("name", context.serialize(cluster.getName()));
    jsonObj.add("description", context.serialize(cluster.getDescription()));
    jsonObj.add("account", context.serialize(cluster.getAccount()));
    jsonObj.add("createTime", context.serialize(cluster.getCreateTime()));
    jsonObj.add("expireTime", context.serialize(cluster.getExpireTime()));
    jsonObj.add("provider", context.serialize(cluster.getProvider()));
    jsonObj.add("clusterTemplate", context.serialize(cluster.getClusterTemplate()));
    jsonObj.add("nodes", context.serialize(cluster.getNodeIDs()));
    jsonObj.add("services", context.serialize(cluster.getServices()));
    jsonObj.add("latestJobId", context.serialize(cluster.getLatestJobId()));
    jsonObj.add("status", context.serialize(cluster.getStatus()));
    jsonObj.add("config", context.serialize(cluster.getConfig()));

    return jsonObj;
  }
}
