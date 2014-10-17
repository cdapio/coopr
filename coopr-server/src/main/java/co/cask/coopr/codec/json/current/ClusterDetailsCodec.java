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

import co.cask.coopr.cluster.ClusterDetails;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Codec for serializing {@link co.cask.coopr.cluster.ClusterDetails}.
 */
public class ClusterDetailsCodec implements JsonSerializer<ClusterDetails> {

  @Override
  public JsonElement serialize(ClusterDetails src, Type typeOfSrc, JsonSerializationContext context) {
    // flattening the cluster object and adding the rest of the cluster details fields on top of the cluster's
    // fields. This weirdness is to preserve the cluster rest api, which was doing some weird combining of a cluster
    // and its nodes and its latest job.
    JsonObject jsonObj = ClusterCodec.serializeCluster(src.getCluster(), context);

    // node ids get overwritten by the full node objects
    jsonObj.add("nodes", context.serialize(src.getNodes()));
    jsonObj.add("links", context.serialize(src.getLinks()));
    jsonObj.add("progress", context.serialize(src.getProgress()));
    jsonObj.add("message", context.serialize(src.getMessage()));

    return jsonObj;
  }
}
