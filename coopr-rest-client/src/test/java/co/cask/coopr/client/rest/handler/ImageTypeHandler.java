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
import co.cask.coopr.spec.ImageType;
import com.google.common.collect.Lists;

import java.util.List;

public class ImageTypeHandler extends AbstractAdminHandler<ImageType> {

  private static final String GET_ALL_URL = "/v2/imagetypes";

  @Override
  public List<ImageType> getAll() {
    return Lists.newArrayList(Entities.ImageTypeExample.CENTOS_6, Entities.ImageTypeExample.UBUNTU_12);
  }

  @Override
  public ImageType getSingle() {
    return Entities.ImageTypeExample.CENTOS_6;
  }

  @Override
  public String getAllURL() {
    return GET_ALL_URL;
  }
}
