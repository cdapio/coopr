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
import co.cask.coopr.macro.eval.Evaluator;
import com.google.common.base.Objects;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An expression represents a single macro. It has a type and a name that are used to lookup the substitute for the
 * macro. Currently, the substitute is either the list of ip addresses or the list of host names of the nodes that the
 * named service runs on. By default, the substitute is returned as a comma-separated list. The list separator can be
 * overridden by passing a different one to the constructor. Also, optionally each element in the substitute can be
 * formatted according to a format string. The actual element is inserted by replacing the $ in the format string. The
 * $ sign can be escaped by preceding it with another $.
 */
public class Expression {
  private static final String DEFAULT_SEPARATOR = ",";
  private static final char PLACEHOLDER = '$';
  private final Evaluator evaluator;
  private final String format;
  private final String separator;

  /**
   * Constructor with all argument.
   *
   * @param evaluator type of expression that knows how to evaluate the expression parts.
   * @param format the format string.
   * @param separator the list separator.
   */
  public Expression(Evaluator evaluator, @Nullable String format, @Nullable String separator) {
    this.evaluator = evaluator;
    this.format = format;
    this.separator = separator == null ? DEFAULT_SEPARATOR : separator;
  }

  /**
   * Evaluate the expression for a given cluster. Looks up the service name in the cluster to find all nodes that run
   * the service, then formats and joins all results into a string.
   *
   * @param cluster the cluster to evaluate for.
   * @param clusterNodes the nodes of the cluster to evaluate for.
   * @param node the node of the cluster to evaluate the expression for.
   * @return the replacement string for the expression, or null if the service required for replacement is not in
   *         the cluster.
   * @throws IncompleteClusterException if a node is missing the property that is required for the lookup type.
   */
  public String evaluate(Cluster cluster, Set<Node> clusterNodes, Node node) throws IncompleteClusterException {
    StringBuilder builder = new StringBuilder();

    List<String> parts = evaluator.evaluate(cluster, clusterNodes, node);
    if (parts == null) {
      return null;
    }
    if (!parts.isEmpty()) {
      format(parts.get(0), builder);
      for (int i = 1; i < parts.size(); i++) {
        builder.append(separator);
        format(parts.get(i), builder);
      }
    }
    return builder.toString();
  }

  /**
   * Apply the format string to a substitute string and append it to a string builder.
   *
   * @param raw the original substitute.
   * @param builder the string builder to append.
   */
  void format(String raw, StringBuilder builder) {
    if (format == null) {
      builder.append(raw);
      return;
    }
    for (int i = 0; i < format.length(); i++) {
      if (PLACEHOLDER == format.charAt(i)) {
        if (i + 1 < format.length() && PLACEHOLDER == format.charAt(i + 1)) {
          // escaped
          builder.append(PLACEHOLDER);
          i++; // move past second placeholder
        } else {
          builder.append(raw);
        }
      } else {
        builder.append(format.charAt(i));
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Expression that = (Expression) o;

    return Objects.equal(evaluator, that.evaluator) &&
      Objects.equal(format, that.format) &&
      Objects.equal(separator, that.separator);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(evaluator, format, separator);
  }
}
