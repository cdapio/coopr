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

/**
 * A named entity that also has a link to an icon for the entity.
 */
public abstract class NamedIconEntity extends NamedEntity {
  protected final String icon;

  public NamedIconEntity(String name, String icon) {
    super(name);
    this.icon = icon;
  }

  /**
   * Get the link to the icon for the entity.
   *
   * @return Link to the icon for the entity.
   */
  public String getIcon() {
    return icon;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NamedIconEntity)) {
      return false;
    }

    NamedIconEntity that = (NamedIconEntity) o;

    return super.equals(that) && Objects.equal(icon, that.icon);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), icon);
  }
}
