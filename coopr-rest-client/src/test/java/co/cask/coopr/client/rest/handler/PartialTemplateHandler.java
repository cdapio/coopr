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
import co.cask.coopr.spec.template.PartialTemplate;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Partial Templates test handler.
 */
public class PartialTemplateHandler extends AbstractAdminHandler<PartialTemplate> {

  private static final String GET_ALL_URL = "/v2/partialtemplates";

  @Override
  public List<PartialTemplate> getAll() {
    return Lists.newArrayList(Entities.PartialTemplateExample.TEST_PARTIAL1,
                              Entities.PartialTemplateExample.TEST_PARTIAL2);
  }

  @Override
  public PartialTemplate getSingle() {
    return  Entities.PartialTemplateExample.TEST_PARTIAL1;
  }

  @Override
  public String getAllURL() {
    return GET_ALL_URL;
  }
}
