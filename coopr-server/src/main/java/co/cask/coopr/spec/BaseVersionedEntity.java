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
 * A base for entities that require a name, version and optionally support an icon, description, and label.
 */
public class BaseVersionedEntity extends BaseEntity {

  protected int version;

  protected BaseVersionedEntity(BaseVersionedEntity.Builder baseBuilder) {
    super(baseBuilder);
    this.version = baseBuilder.version;
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
   * Base builder for creating admin versioned entities.
   */
  public abstract static class Builder<T extends BaseVersionedEntity> extends BaseEntity.Builder<T> {
    protected int version = Constants.DEFAULT_VERSION;

    public Builder setVersion(int version) {
      this.version = version;
      return this;
    }

    public Builder<T> setBaseFields(String name, String label, String description, String icon, int version) {
      super.setBaseFields(name, label, description, icon);
      this.version = version;
      return this;
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("version", version)
      .toString();
  }
}
