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
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Machine providers are instances of some {@link co.cask.coopr.spec.plugin.ProviderType}
 * like openstack, aws, rackspace, or joyent that can provision machines.
 * Providers are referenced by {@link ImageType} and {@link HardwareType}.
 */
public final class Provider extends BaseEntity {
  private final String providerType;
  private final Map<String, Object> provisionerFields;

  private Provider(BaseEntity.Builder baseBuilder,
                   String providerType, Map<String, Object> provisionerFields) {
    super(baseBuilder);
    Preconditions.checkArgument(providerType != null, "invalid provider type.");
    this.providerType = providerType;
    this.provisionerFields = provisionerFields == null ?
      Maps.<String, Object>newHashMap() : Maps.newHashMap(provisionerFields);
  }

  /**
   * Get the type of the provider.
   *
   * @return Type of the provider.
   */
  public String getProviderType() {
    return providerType;
  }

  /**
   * Get fields needed by provisioners for this provider.  This should include fields defined in the corresponding
   * {@link co.cask.coopr.spec.plugin.ProviderType}.
   *
   * @return Fields needed to provision machines from this provider.
   */
  public Map<String, Object> getProvisionerFields() {
    return provisionerFields;
  }

  /**
   * Add some fields to the provisioner fields. If the field already exists, its value will be overwritten.
   *
   * @param fields Fields to add to the provisioner fields.
  */
  public void addFields(Map<String, Object> fields) {
    if (fields != null) {
      provisionerFields.putAll(fields);
    }
  }

  /**
   * Get a builder for creating a provider.
   *
   * @return Builder for creating a provider.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating a provider.
   */
  public static class Builder extends BaseEntity.Builder<Provider> {
    private String providerType;
    private Map<String, Object> provisionerFields;

    public Builder setProviderType(String providerType) {
      this.providerType = providerType;
      return this;
    }

    public Builder setProvisionerFields(Map<String, Object> provisionerFields) {
      this.provisionerFields = provisionerFields;
      return this;
    }

    @Override
    public Provider build() {
      return new Provider(this, providerType, provisionerFields);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Provider)) {
      return false;
    }
    Provider other = (Provider) o;
    return super.equals(other) &&
      Objects.equal(providerType, other.providerType) &&
      Objects.equal(provisionerFields, other.provisionerFields);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), providerType, provisionerFields);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("providerType", providerType)
      .add("provisionerFields", provisionerFields)
      .toString();
  }
}
