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
package co.cask.coopr.spec.template;

import co.cask.coopr.spec.BaseEntity;
import co.cask.coopr.spec.Link;
import com.google.common.base.Objects;

import java.util.Set;

public final class PartialTemplate extends AbstractTemplate {

  private final boolean immutable;

  private PartialTemplate(BaseEntity.Builder baseBuilder, ClusterDefaults clusterDefaults,
                          Compatibilities compatibilities, Constraints constraints, Administration administration,
                          Set<Link> links, boolean immutable) {
    super(baseBuilder, clusterDefaults, compatibilities, constraints, administration, links);
    this.immutable = immutable;
  }

  /**
   * Get immutability of partial template. If this partial template is immutable it's attributes can't be overridden.
   *
   * @return true if template is immutable
   */
  public Boolean isImmutable() {
    return immutable;
  }

  /**
   * Get a builder for creating partial templates.
   *
   * @return Builder for creating partial templates.
   */
  public static PartialTemplate.Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating partial templates.
   */
  public static class Builder extends AbstractTemplate.Builder<PartialTemplate, Builder> {

    private boolean immutable;

    @Override
    protected PartialTemplate.Builder getThis() {
      return this;
    }

    public PartialTemplate build() {
      return new PartialTemplate(this, clusterDefaults, compatibilities, constraints, administration, links, immutable);
    }

    public Builder setImmutable(boolean immutable) {
      this.immutable = immutable;
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PartialTemplate)) {
      return false;
    }
    PartialTemplate other = (PartialTemplate) o;
    return super.equals(other) &&
      Objects.equal(immutable, other.immutable);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), compatibilities, constraints, administration, links);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("clusterDefaults", clusterDefaults)
      .add("constraints", constraints)
      .add("compatibilities", compatibilities)
      .add("administration", administration)
      .add("links", links)
      .add("immutable", immutable)
      .toString();
  }
}
