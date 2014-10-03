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
package co.cask.coopr.spec.plugin;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Plugin fields grouped based on the type of field it is. Currently only divided on whether the field is sensitive
 * or not, but may include other groups in the future.
 */
public class PluginFields {
  private final Map<String, Object> sensitive;
  private final Map<String, Object> nonsensitive;

  private PluginFields(ImmutableMap<String, Object> sensitive, ImmutableMap<String, Object> nonsensitive) {
    this.sensitive = sensitive;
    this.nonsensitive = nonsensitive;
  }

  /**
   * Get the immutable map of sensitive field keys to values.
   *
   * @return Immutable map of sensitive field keys to values
   */
  public Map<String, Object> getSensitive() {
    return sensitive;
  }


  /**
   * Get the immutable map of nonsensitive field keys to values.
   *
   * @return Immutable map of nonsensitive field keys to values
   */
  public Map<String, Object> getNonsensitive() {
    return nonsensitive;
  }

  /**
   * Get a builder for creating plugin fields.
   *
   * @return Builder for creating plugin fields.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating plugin fields.
   */
  public static class Builder {
    private ImmutableMap.Builder<String, Object> sensitive = ImmutableMap.builder();
    private ImmutableMap.Builder<String, Object> nonsensitive = ImmutableMap.builder();

    public Builder putSensitive(String key, Object val) {
      this.sensitive.put(key, val);
      return this;
    }

    public Builder putNonsensitive(String key, Object val) {
      this.nonsensitive.put(key, val);
      return this;
    }

    public PluginFields build() {
      return new PluginFields(sensitive.build(), nonsensitive.build());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PluginFields)) {
      return false;
    }

    PluginFields that = (PluginFields) o;

    return Objects.equal(sensitive, that.sensitive) && Objects.equal(nonsensitive, that.nonsensitive);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(sensitive, nonsensitive);
  }
}
