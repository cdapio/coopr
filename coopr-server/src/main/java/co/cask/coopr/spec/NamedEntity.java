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
package co.cask.coopr.spec;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.regex.Pattern;

/**
 * A named entity has a name that must only consist of alphanumeric characters, underscore, period, and dash.
 */
public class NamedEntity {
  private static final Pattern whitelist = Pattern.compile("[\\w\\.-]+");
  protected final String name;

  public NamedEntity(String name) {
    Preconditions.checkArgument(name != null && name.length() > 0 && whitelist.matcher(name).matches(),
                                "invalid name. Name must be specified and consist of only [a-zA-Z0-9_.-].");
    Preconditions.checkArgument(name.length() < 256, "name must not exceed 255 characters.");
    this.name = name;
  }

  /**
   * Get the name of the entity.
   *
   * @return Name of the entity.
   */
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NamedEntity that = (NamedEntity) o;

    return Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .toString();
  }
}
