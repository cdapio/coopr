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
import co.cask.coopr.spec.template.ClusterTemplate;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Cluster Templates test handler.
 */
public class ClusterTemplateHandler extends AbstractAdminHandler<ClusterTemplate> {

  private static final String GET_ALL_URL = "/v2/clustertemplates";

  @Override
  public List<ClusterTemplate> getAll() {
    return Lists.newArrayList(Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED, Entities.ClusterTemplateExample.HDFS,
                              Entities.ClusterTemplateExample.REACTOR, Entities.ClusterTemplateExample.REACTOR2);
  }

  @Override
  public ClusterTemplate getSingle() {
    return  Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED;
  }

  @Override
  public String getAllURL() {
    return GET_ALL_URL;
  }
}
