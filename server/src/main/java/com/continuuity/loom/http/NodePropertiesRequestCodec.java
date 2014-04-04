package com.continuuity.loom.http;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * Deserializes {@link NodePropertiesRequest}.
 */
public class NodePropertiesRequestCodec implements JsonDeserializer<NodePropertiesRequest> {

  @Override
  public NodePropertiesRequest deserialize(JsonElement json, Type type,
                                           JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();
    String clusterId = context.deserialize(jsonObj.get("clusterId"), String.class);
    Set<String> properties = context.deserialize(jsonObj.get("properties"),
                                                 new TypeToken<Set<String>>() {}.getType());
    Set<String> services = context.deserialize(jsonObj.get("services"),
                                               new TypeToken<Set<String>>() {}.getType());
    return new NodePropertiesRequest(clusterId, properties, services);
  }
}
