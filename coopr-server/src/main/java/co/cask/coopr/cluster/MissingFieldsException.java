/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.coopr.cluster;

import co.cask.coopr.spec.plugin.FieldSchema;

import java.util.List;
import java.util.Map;

/**
 * An exception indicating there are required fields that are missing.
 */
public class MissingFieldsException extends Exception {
  private final List<Map<String, FieldSchema>> missingFields;

  public MissingFieldsException(List<Map<String, FieldSchema>> missingFields) {
    this.missingFields = missingFields;
  }

  public List<Map<String, FieldSchema>> getMissingFields() {
    return missingFields;
  }
}
