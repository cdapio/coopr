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
 * A base for admin defined entities, which all require a name and optionally support
 * an icon, description, and label.
 */
public abstract class BaseAdminEntity extends NamedEntity {
  protected final String label;
  protected final String description;
  protected final String icon;

  public BaseAdminEntity(Builder builder) {
    super(builder.name);
    this.label = builder.label;
    this.description = builder.description;
    this.icon = builder.icon;
  }

  /**
   * Get the label of the entity, or null if none exists.
   *
   * @return label of the entity, or null if none exists.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Get the description of the entity, or null if none exists.
   *
   * @return description of the entity, or null if none exists.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the link to the icon for the entity.
   *
   * @return Link to the icon for the entity.
   */
  public String getIcon() {
    return icon;
  }

  /**
   * Base builder for creating admin entities.
   */
  public abstract static class Builder<T extends BaseAdminEntity> {
    protected String name;
    protected String label;
    protected String description;
    protected String icon;

    public Builder<T> setName(String name) {
      this.name = name;
      return this;
    }

    public Builder<T> setLabel(String label) {
      this.label = label;
      return this;
    }

    public Builder<T> setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder<T> setIcon(String icon) {
      this.icon = icon;
      return this;
    }

    public Builder<T> setBaseFields(String name, String label, String description, String icon) {
      this.name = name;
      this.label = label;
      this.description = description;
      this.icon = icon;
      return this;
    }

    public abstract T build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseAdminEntity)) {
      return false;
    }

    BaseAdminEntity that = (BaseAdminEntity) o;

    return super.equals(that) &&
      Objects.equal(label, that.label) &&
      Objects.equal(description, that.description) &&
      Objects.equal(icon, that.icon);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), label, description, icon);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("label", label)
      .add("description", description)
      .add("icon", icon)
      .toString();
  }
}
