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
package co.cask.coopr.macro;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A simple macro expander. Supported macros are:
 * <ul>
 *   <li>%cluster.owner% - owner id of the cluster</li>
 *   <li>%host.service.&ltservice name&gt% - comma separated list of hostnames of nodes with the service</li>
 *   <li>%ip.service.&ltservice name&gt% - comma separated list of ips of nodes with the service</li>
 *   <li>%num.service.&ltservice name&gt% - number of nodes with the service</li>
 *   <li>%instance.self.service.&ltservice name&gt% - service instance number of the specific node with the service</li>
 *   <li>%host.service.&ltservice name&gt[i]% - hostname of the i'th host with the service</li>
 *   <li>%ip.service.&ltservice name&gt[i]% - ip of the i'th host with the service</li>
 *   <li>%join(host.service.&ltservice name&gt,'&ltdelimiter&gt')% - join the given list with the given delimiter</li>
 *   <li>%join(map(host.service.&ltservice name&gt,'&ltformat string&gt'),'&ltdelimiter&gt')% - map each element
 *        in the list using the format string, replacing "$" in the string with the element. Then perform a join on
 *        the result.</li>
 * </ul>
 *
 * For example, if the string "%join(map(host.service.zookeeper, '$:2181'), ',')%/namespace" is a value in the config,
 * and if zookeeper is placed on nodes with hostnames hostA, hostB, and hostC, then the expander will expand the macro
 * to "hostA:2181,hostB:2181,hostC:2181/namespace". This is because the map function replaces the "$" in its format
 * string "$:2181" with each hostname, and the join function then joins them all with a comma.
 */
public final class Expander {

  /**
   * Given a text that may contain macros, validate the syntax of all macros.
   * @throws SyntaxException if a macro is not wellformed.
   */
  public static void validate(String textWithMacros) throws SyntaxException {
    try {
      expand(textWithMacros, null, null, null);
    } catch (IncompleteClusterException e) {
      // can never happen because expansion is skipped
    }
  }

  /**
   * Given a text that may contain macros, validate and expand all macros in the context of the given cluster nodes,
   * and on the specified node.
   * @param textWithMacros text that may contain macros.
   * @param cluster cluster to evaluate macros for.
   * @param nodes cluster nodes to evaluate macros for.
   * @param node cluster node to evaluate macros for.
   * @return text with any relevant macros expanded.
   * @throws SyntaxException if a macro is not wellformed.
   * @throws IncompleteClusterException if a macro cannot be expanded because the cluster lacks the information.
   */
  public static String expand(String textWithMacros, Cluster cluster, Set<Node> nodes, Node node)
    throws SyntaxException, IncompleteClusterException {
    int pos = 0;
    StringBuilder builder = nodes == null ? null : new StringBuilder();
    boolean expansionHappened = false;
    while (pos < textWithMacros.length()) {
      // find the first macro
      int pos1 = findNextPercent(textWithMacros, pos);
      if (pos1 >= 0) {
        int pos2 = findNextPercent(textWithMacros, pos1 + 1);
        if (pos2 >= 0) {
          // copy text up to macro
          if (builder != null) {
            builder.append(textWithMacros, pos, pos1);
          }
          // macro found
          String macro = textWithMacros.substring(pos1 + 1, pos2);
          // parse the macro
          Expression expression = new Parser(macro).parse();
          // if cluster is given, expand macro
          if (builder != null) {
            String expansion = expression.evaluate(cluster, nodes, node);
            if (expansion != null) {
              builder.append(expansion);
              expansionHappened = true;
            }
          }
          // move position past macro
          pos = pos2 + 1;
          continue;
        }
      }
      // no macro found, copy remaining text and quit
      if (builder != null && expansionHappened) {
        builder.append(textWithMacros, pos, textWithMacros.length());
      }
      break;
    }
    // only return a new string if actual expansion happened
    return expansionHappened ? builder.toString() : textWithMacros;
  }

  /**
   * Returns position of the next % character in the text, -1 if none found.
   */
  static int findNextPercent(String text, int pos) {
    while (true) {
      if (pos >= text.length()) {
        return -1;
      }
      if (text.charAt(pos) == '%') {
        if (pos + 1 < text.length() && text.charAt(pos + 1) == '%') {
          pos += 2;
          continue;
        } else {
          return pos;
        }
      }
      pos++;
    }
  }

  /**
   * Given a JSON tree, find the element specified by the path (or the root if path is null). In that subtree,
   * recursively traverse all elements, and expand all String typed right hand side values.  If a macro cannot be
   * expanded due to the cluster object missing certain data, that macro will be left unexpanded.
   *
   * @param json A JSON tree
   * @param path the path to expand under
   * @param cluster the cluster to use for expanding macros.
   * @param nodes the cluster nodes to use for expanding macros.
   * @param node the cluster node to use for expanding macros.
   * @return a new JSON tree if any expansion took place, and the original JSON tree otherwise.
   * @throws SyntaxException if a macro expression is ill-formed.
   * @throws IncompleteClusterException if the cluster does not have the meta data to expand all macros.
   */
  public static JsonElement expand(JsonElement json, @Nullable java.util.List<String> path, Cluster cluster,
                                   Set<Node> nodes, Node node) throws SyntaxException, IncompleteClusterException {

    // if path is given,
    if (path != null && !path.isEmpty()) {
      String first = path.get(0);
      if (json.isJsonObject()) {
        JsonObject object = json.getAsJsonObject();
        JsonElement json1 = object.get(first);
        if (json1 != null) {
          JsonElement expanded = expand(json1, path.subList(1, path.size()), cluster, nodes, node);
          if (expanded != json1) {
            // only construct new json object if actual expansion happened
            JsonObject object1 = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
              object1.add(entry.getKey(), entry.getKey().equals(first) ? expanded : entry.getValue());
            }
            return object1;
          }
        }
      }
      // path was given, but either no corresponding subtree was found or no expansion happened...
      return json;
    }

    if (json.isJsonPrimitive()) {
      JsonPrimitive primitive = json.getAsJsonPrimitive();
      if (primitive.isString()) {
        String value = primitive.getAsString();
        String expanded = expand(value, cluster, nodes, node);
        if (!expanded.equals(value)) {
          // only return a new json element if actual expansion happened
          return new JsonPrimitive(expanded);
        }
      }
    }

    if (json.isJsonArray()) {
      JsonArray array = json.getAsJsonArray();
      JsonArray array1 = new JsonArray();
      boolean expansionHappened = false;
      for (JsonElement element : array) {
        JsonElement expanded = expand(element, path, cluster, nodes, node);
        if (expanded != element) {
          expansionHappened = true;
        }
        array1.add(expanded);
      }
      // only return a new json array if actual expansion happened
      if (expansionHappened) {
        return array1;
      }
    }

    if (json.isJsonObject()) {
      JsonObject object = json.getAsJsonObject();
      JsonObject object1 = new JsonObject();
      boolean expansionHappened = false;
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
        JsonElement expanded = expand(entry.getValue(), path, cluster, nodes, node);
        if (expanded != entry.getValue()) {
          expansionHappened = true;
        }
        object1.add(entry.getKey(), expand(entry.getValue(), path, cluster, nodes, node));
      }
      if (expansionHappened) {
        return object1;
      }
    }

    return json;
  }

}
