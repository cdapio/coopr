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

import java.util.Map;

/**
 * Image type defines different images to use on on clusters, and include information needed by provisioners to
 * provision machines from different providers.
 */
public final class ImageType extends BaseEntity {
  private final Map<String, Map<String, String>> providerMap;

  private ImageType(BaseEntity.Builder baseBuilder, Map<String, Map<String, String>> providerMap) {
    super(baseBuilder);
    this.providerMap = providerMap;
  }

  /**
   * Get the provider map for this image type.  The provider map has provider names as its keys, and a map of
   * any key values needed by the provisioners given that provider.  For example, all cloud
   * providers require the 'image' key, which specifies which provider-specific image to use.
   *
   * @return Mapping of provider to data needed when provisioning nodes from the provider.
   */
  public Map<String, Map<String, String>> getProviderMap() {
    return providerMap;
  }

  /**
   * Get a builder for creating a image type.
   *
   * @return Builder for creating a image type.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating a image type.
   */
  public static class Builder extends BaseEntity.Builder<ImageType> {
    private Map<String, Map<String, String>> providerMap;

    public Builder setProviderMap(Map<String, Map<String, String>> providerMap) {
      this.providerMap = providerMap;
      return this;
    }

    @Override
    public ImageType build() {
      return new ImageType(this, providerMap);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ImageType)) {
      return false;
    }
    ImageType other = (ImageType) o;
    return super.equals(other) &&
      providerMap.equals(other.providerMap);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), providerMap);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("providerMap", providerMap)
      .toString();
  }
}
