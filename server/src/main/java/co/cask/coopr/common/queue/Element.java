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
package co.cask.coopr.common.queue;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * The element to be placed in the {@link TrackingQueue}.
 */
public class Element {
  private String id;
  private String value;

  /**
   * Queue element with the given id and value.
   *
   * @param id Id of the element.
   * @param value Value of the element.
   */
  public Element(String id, String value) {
    this.id = id;
    this.value = value;
  }

  /**
   * Queue element with the given value and an automatically generated id.
   *
   * @param value Value of the element.
   */
  public Element(String value) {
    this(UUID.randomUUID().toString(), value);
  }

  /**
   * Id of the element.
   *
   * @return Id of the element.
   */
  public String getId() {
    return id;
  }

  /**
   * Value of the element.
   *
   * @return Value of the element.
   */
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    JsonObject object = new JsonObject();
    object.addProperty("id", id);
    object.addProperty("value", value);
    return object.toString();
  }
}
