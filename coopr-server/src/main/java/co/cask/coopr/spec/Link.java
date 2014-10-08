/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.spec;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Specification to expose a link at the cluster or node level.
 */
public class Link {
  private final String label;
  private final String url;

  public Link(String label, String url) {
    Preconditions.checkArgument(url != null && !url.isEmpty(), "url must be specified");
    this.label = label;
    this.url = url;
  }

  public String getLabel() {
    return label;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Link)) {
      return false;
    }

    Link that = (Link) o;

    return Objects.equal(label, that.label) &&
      Objects.equal(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(label, url);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("label", label)
      .add("url", url)
      .toString();
  }
}
