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


package co.cask.coopr.client;

import co.cask.coopr.client.model.HardwareTypeInfo;

import java.util.List;

/**
 * The client API for manage the hardware specifications.
 */
public interface HardwareTypeClient {

  /**
   * Retrieves the list of all the hardware types.
   *
   * @return List of {@link co.cask.coopr.client.model.HardwareTypeInfo} objects
   */
  List<HardwareTypeInfo> getAllHardwareTypes();

  /**
   * Retrieves details about a hardware type by the name.
   *
   * @return {@link co.cask.coopr.client.model.HardwareTypeInfo} object
   */
  HardwareTypeInfo getHardwareType(String name);

  /**
   * Deletes a hardware type by the name.
   */
  void deleteHardwareType(String name);
}
