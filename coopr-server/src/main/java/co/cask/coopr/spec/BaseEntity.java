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

import co.cask.coopr.common.conf.Constants;
import com.google.common.base.Objects;

/**
 * A base for entities that require a name and optionally support an icon, description, and label.
 */
public class BaseEntity extends NamedEntity {
  protected final String label;
  protected final String description;
  protected final String icon;
  protected int version;

  private BaseEntity(String name, String label, String description, String icon, int version) {
    super(name);
    this.label = label;
    this.description = description;
    this.icon = icon;
    this.version = version;
  }

  protected BaseEntity(Builder builder) {
    super(builder.name);
    this.label = builder.label;
    this.description = builder.description;
    this.icon = builder.icon;
    this.version = builder.version;
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
   * Retrieves the version of the entity.
   *
   * @return the version of the entity.
   */
  public int getVersion() {
    return version;
  }

  /**
   * Sets the version of the entity.
   *
   * @param version the version
   */
  public void setVersion(int version) {
    this.version = version;
  }

  /**
   * Create an admin entity from another admin entity.
   *
   * @param other entity to create from
   * @return admin entity created from the given entity
   */
  public static BaseEntity from(BaseEntity other) {
    return new BaseEntity(other.name, other.label, other.description, other.icon, other.version);
  }

  /**
   * Base builder for creating admin entities.
   */
  public abstract static class Builder<T extends BaseEntity> {
    protected String name;
    protected String label;
    protected String description;
    protected String icon;
    protected int version = Constants.DEFAULT_VERSION;

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

    public Builder<T> setVersion(int version) {
      this.version = version;
      return this;
    }

    public Builder<T> setBaseFields(String name, String label, String description, String icon, int version) {
      this.name = name;
      this.label = label;
      this.description = description;
      this.icon = icon;
      this.version = version;
      return this;
    }

    public abstract T build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseEntity)) {
      return false;
    }

    BaseEntity that = (BaseEntity) o;

    return super.equals(that) &&
      Objects.equal(label, that.label) &&
      Objects.equal(description, that.description) &&
      Objects.equal(icon, that.icon) &&
      Objects.equal(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), label, description, icon, version);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("label", label)
      .add("description", description)
      .add("icon", icon)
      .add("version", version)
      .toString();
  }
}
