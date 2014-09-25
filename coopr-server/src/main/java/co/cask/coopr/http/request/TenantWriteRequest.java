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
package co.cask.coopr.http.request;

import co.cask.coopr.spec.TenantSpecification;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * A request to add a tenant.
 */
public class TenantWriteRequest {
  private final TenantSpecification tenant;
  private final boolean bootstrap;

  public TenantWriteRequest(TenantSpecification tenant) {
    this(tenant, false);
  }

  public TenantWriteRequest(TenantSpecification tenant, Boolean bootstrap) {
    Preconditions.checkArgument(tenant != null, "Tenant specification must be given.");
    this.tenant = tenant;
    this.bootstrap = bootstrap == null ? false : bootstrap;
  }

  public TenantSpecification getTenant() {
    return tenant;
  }

  public boolean isBootstrap() {
    return bootstrap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TenantWriteRequest)) {
      return false;
    }

    TenantWriteRequest that = (TenantWriteRequest) o;

    return Objects.equal(tenant, that.tenant) && bootstrap == that.bootstrap;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(tenant, bootstrap);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("tenant", tenant)
      .add("bootstrap", bootstrap)
      .toString();
  }
}
