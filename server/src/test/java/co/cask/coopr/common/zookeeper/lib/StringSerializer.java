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
package co.cask.coopr.common.zookeeper.lib;

import com.google.common.base.Charsets;

import javax.annotation.Nullable;

class StringSerializer implements Serializer<String> {
  @Override
  public byte[] serialize(@Nullable String value) {
    if (value == null) {
      return null;
    }
    return value.getBytes(Charsets.UTF_8);
  }

  @Override
  public String deserialize(@Nullable byte[] serialized) {
    if (serialized == null) {
      return null;
    }
    return new String(serialized, Charsets.UTF_8);
  }
}
