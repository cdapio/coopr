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
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

public class PluginTestConstants {
  public static final String TEST_USER_ID = "test";
  public static final String TEST_TENANT_ID = "supertest";

  public static final String CHEF_PLUGIN = "chef";
  public static final String REACTOR_RESOURCE = "reactor";
  public static final String TEST_RESOURCE_TYPE = "testType";
  public static final String JOYENT_PLUGIN = "joyent";
  public static final String VERSION = "2";
  public static final String NOT_EXISISTING_PLUGIN =  "not_existing_plugin";
  public static final String NOT_EXISISTING_RESOURCE = "not_existing_resource";
  public static final List AUTOMATOR_LISTS  = Lists.newArrayList(Entities.AutomatorTypeExample.CHEF,
                                                                 Entities.AutomatorTypeExample.PUPPET);
  public static final List  PROVIDER_LISTS =  Lists.newArrayList(Entities.ProviderTypeExample.JOYENT,
                                                                 Entities.ProviderTypeExample.RACKSPACE);

  public static final AutomatorType AUTOMATOR_TYPE = Entities.AutomatorTypeExample.CHEF;
  public static final ProviderType PROVIDER_TYPE = Entities.ProviderTypeExample.JOYENT;
  public static final Map TYPE_RESOURCES = ImmutableMap.of(REACTOR_RESOURCE,
    ImmutableSet.of(new ResourceMeta(REACTOR_RESOURCE, 1, ResourceStatus.ACTIVE),
                    new ResourceMeta(REACTOR_RESOURCE, 2, ResourceStatus.ACTIVE)));

}
