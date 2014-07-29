/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.admin;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * A tenant contains the id, plus fields.
 */
public final class Tenant {
  private final String id;
  private final TenantSpecification specification;

  public Tenant(String id, TenantSpecification specification) {
    Preconditions.checkArgument(id != null, "Id must be specified.");
    Preconditions.checkArgument(specification != null, "Fields must be specified");
    this.id = id;
    this.specification = specification;
  }

  public String getId() {
    return id;
  }

  public TenantSpecification getSpecification() {
    return specification;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Tenant)) {
      return false;
    }
    Tenant other = (Tenant) o;
    return Objects.equal(id, other.id) &&
      Objects.equal(specification, other.specification);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, specification);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", id)
      .add("fields", specification)
      .toString();
  }
}
