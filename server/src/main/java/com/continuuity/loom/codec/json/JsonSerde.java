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
package com.continuuity.loom.codec.json;

import com.google.gson.Gson;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Class for serializing and deserializing objects to/from json, using gson.
 */
public interface JsonSerde {

  /**
   * Serialize the object of specified type.
   *
   * @param object Object to serialize.
   * @param type Type of the object to serialize.
   * @param <T> Object class.
   * @return serialized object.
   */
  public <T> byte[] serialize(T object, Type type);

  /**
   * Deserialize an object given a reader for the object and the type of the object.
   *
   * @param reader Reader for reading the object to deserialize.
   * @param type Type of the object to deserialize.
   * @param <T> Object class.
   * @return deserialized object.
   */
  public <T> T deserialize(Reader reader, Type type);
  /**
   * Deserialize an object given the object as a byte array and the type of the object.
   *
   * @param bytes Serialized object.
   * @param type Type of the object to deserialize.
   * @param <T> Object class.
   * @return deserialized object.
   */
  public <T> T deserialize(byte[] bytes, Type type);

  /**
   * Get the Gson used for serialization and deserialization.
   *
   * @return Gson used for serialization and deserialization.
   */
  public Gson getGson();
}
