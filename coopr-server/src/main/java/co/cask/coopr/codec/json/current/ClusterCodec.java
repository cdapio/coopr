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

  private static final Type SET_STRING_TYPE = new TypeToken<Set<String>>() { }.getType();

  private static final String ID_KEY = "id";
  private static final String NAME_KEY = "name";
  private static final String DESCRIPTION_KEY = "description";
  private static final String ACCOUNT_KEY = "account";
  private static final String CREATE_TIME_KEY = "createTime";
  private static final String EXPIRE_TIME_KEY = "expireTime";
  private static final String PROVIDER_KEY = "provider";
  private static final String CLUSTER_TEMPLATE_KEY = "clusterTemplate";
  private static final String NODES_KEY = "nodes";
  private static final String SERVICES_KEY = "services";
  private static final String LATEST_JOB_ID_KEY = "latestJobId";
  private static final String STATUS_KEY = "status";
  private static final String CONFIG_KEY = "config";

  @Override
  public Cluster deserialize(JsonElement json, Type type, JsonDeserializationContext context)
    throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();

    return deserializeClusterWithoutNodes(jsonObj, context)
      .setNodes(context.<Set<String>>deserialize(jsonObj.get(NODES_KEY), SET_STRING_TYPE))
      .build();
  }

  @Override
  public JsonElement serialize(Cluster src, Type typeOfSrc, JsonSerializationContext context) {
    return serializeCluster(src, context);
  }

  public static JsonObject serializeCluster(Cluster cluster, JsonSerializationContext context) {

    JsonObject jsonObj = new JsonObject();

    jsonObj.add(ID_KEY, context.serialize(cluster.getId()));
    jsonObj.add(NAME_KEY, context.serialize(cluster.getName()));
    jsonObj.add(DESCRIPTION_KEY, context.serialize(cluster.getDescription()));
    jsonObj.add(ACCOUNT_KEY, context.serialize(cluster.getAccount()));
    jsonObj.add(CREATE_TIME_KEY, context.serialize(cluster.getCreateTime()));
    jsonObj.add(EXPIRE_TIME_KEY, context.serialize(cluster.getExpireTime()));
    jsonObj.add(PROVIDER_KEY, context.serialize(cluster.getProvider()));
    jsonObj.add(CLUSTER_TEMPLATE_KEY, context.serialize(cluster.getClusterTemplate()));
    jsonObj.add(NODES_KEY, context.serialize(cluster.getNodeIDs()));
    jsonObj.add(SERVICES_KEY, context.serialize(cluster.getServices()));
    jsonObj.add(LATEST_JOB_ID_KEY, context.serialize(cluster.getLatestJobId()));
    jsonObj.add(STATUS_KEY, context.serialize(cluster.getStatus()));
    jsonObj.add(CONFIG_KEY, context.serialize(cluster.getConfig()));

    return jsonObj;
  }

  public static Cluster deserializeCluster(JsonElement json, JsonDeserializationContext context) {
    return deserializeClusterWithoutNodes(json.getAsJsonObject(), context).build();
  }

  private static Cluster.Builder deserializeClusterWithoutNodes(JsonObject jsonObj,
                                                                JsonDeserializationContext context) {
    return Cluster.builder()
      .setID(context.<String>deserialize(jsonObj.get(ID_KEY), String.class))
      .setName(context.<String>deserialize(jsonObj.get(NAME_KEY), String.class))
      .setDescription(context.<String>deserialize(jsonObj.get(DESCRIPTION_KEY), String.class))
      .setAccount(context.<Account>deserialize(jsonObj.get(ACCOUNT_KEY), Account.class))
      .setCreateTime(jsonObj.get(CREATE_TIME_KEY).getAsLong())
      .setExpireTime(context.<Long>deserialize(jsonObj.get(EXPIRE_TIME_KEY), Long.class))
      .setProvider(context.<Provider>deserialize(jsonObj.get(PROVIDER_KEY), Provider.class))
      .setClusterTemplate(context.<ClusterTemplate>deserialize(jsonObj.get(CLUSTER_TEMPLATE_KEY),
                                                               ClusterTemplate.class))
      .setServices(context.<Set<String>>deserialize(jsonObj.get(SERVICES_KEY), SET_STRING_TYPE))
      .setLatestJobID(context.<String>deserialize(jsonObj.get(LATEST_JOB_ID_KEY), String.class))
      .setStatus(context.<Cluster.Status>deserialize(jsonObj.get(STATUS_KEY), Cluster.Status.class))
      .setConfig(context.<JsonObject>deserialize(jsonObj.get(CONFIG_KEY), JsonObject.class));
  }
}
