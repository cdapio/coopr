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

import javax.annotation.Nullable;

/**
 * Serializes objects of type T into byte[] and back.
 *
 * @param <T> Type of object to serialize into byte[] and back.
 */
public interface Serializer<T> {
  /**
   * Serialize the value into bytes.
   *
   * @param value Object to serialize into bytes.
   * @return Serialized value.
   */
  byte[] serialize(@Nullable T value);

  /**
   * Deserialize the bytes into an object.
   *
   * @param serialized Serialized object.
   * @return Deserialized object.
   */
  T deserialize(@Nullable byte[] serialized);
}
