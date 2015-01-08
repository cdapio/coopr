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

import com.google.common.base.Objects;

/**
 * Uniquely identifies a mandatory partial template.
 */
public class MandatoryPartial {

  private final String name;
  private final int version;

  public MandatoryPartial(String name, int version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public int getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("version", version)
      .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MandatoryPartial other = (MandatoryPartial) obj;
    return Objects.equal(this.name, other.name) && Objects.equal(this.version, other.version);
  }
}