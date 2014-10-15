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

package co.cask.coopr.client.rest.handler;

import co.cask.coopr.Entities;
import co.cask.coopr.spec.HardwareType;
import com.google.common.collect.Lists;

import java.util.List;

public class HardwareTypeHandler extends AbstractAdminHandler<HardwareType> {

  private static final String GET_ALL_URL = "/v2/hardwaretypes";

  @Override
  public List<HardwareType> getAll() {
    return Lists.newArrayList(Entities.HardwareTypeExample.LARGE, Entities.HardwareTypeExample.MEDIUM,
                              Entities.HardwareTypeExample.SMALL);
  }

  @Override
  public HardwareType getSingle() {
    return Entities.HardwareTypeExample.LARGE;
  }

  @Override
  public String getAllURL() {
    return GET_ALL_URL;
  }
}
