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

package co.cask.coopr.test.cli;

import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.Entities;
import co.cask.coopr.test.Constants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;

public class AdminCommandsTest extends AbstractTest {

  @BeforeClass
  public static void beforeClass()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException {
    createCli(ADMIN_ACCOUNT);
  }

  @Test
  public void testListTemplates() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_TEMPLATES_COMMAND);
    checkCommandOutput(Arrays.asList(HADOOP_DISTRIBUTED_CLUSTER_TEMPLATE, REACTOR_CLUSTER_TEMPLATE,
                                     HDFS_CLUSTER_TEMPLATE));
  }

  @Test
  public void testGetClusterTemplate() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_TEMPLATE_COMMAND, REACTOR_CLUSTER_TEMPLATE.getName());
    execute(command);
    checkCommandOutput(REACTOR_CLUSTER_TEMPLATE);
  }

  @Test
  public void testGetClusterTemplateNotFound() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_TEMPLATE_COMMAND, "casandra");
    execute(command);
    checkError();
  }

  @Test
  public void testDeleteClusterTemplate() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_TEMPLATE_COMMAND, REACTOR_CLUSTER_TEMPLATE.getName());
    String deleteCommand = String.format(Constants.DELETE_TEMPLATE_COMMAND, REACTOR_CLUSTER_TEMPLATE.getName());

    execute(getCommand);
    checkCommandOutput(REACTOR_CLUSTER_TEMPLATE);

    OUTPUT_STREAM.reset();
    execute(deleteCommand);
    execute(getCommand);
    checkError();
  }

  @Test
  public void testListProviders() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_PROVIDERS_COMMAND);
    checkCommandOutput(Arrays.asList(Entities.ProviderExample.JOYENT, Entities.ProviderExample.RACKSPACE));
  }

  @Test
  public void testGetProvider() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_PROVIDER_COMMAND, Entities.JOYENT);
    execute(command);
    checkCommandOutput(Entities.ProviderExample.JOYENT);
  }

  @Test
  public void testGetProviderNotFound() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_PROVIDER_COMMAND, "test");
    execute(command);
    checkError();
  }

  @Test
  public void testDeleteProvider() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_PROVIDER_COMMAND, Entities.JOYENT);
    String deleteCommand = String.format(Constants.DELETE_PROVIDER_COMMAND, Entities.JOYENT);

    execute(getCommand);
    checkCommandOutput(Entities.ProviderExample.JOYENT);

    OUTPUT_STREAM.reset();
    execute(deleteCommand);
    execute(getCommand);
    checkError();
  }

  @Test
  public void testListServices() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_SERVICES_COMMAND);
    checkCommandOutput(Arrays.asList(Entities.ServiceExample.DATANODE, Entities.ServiceExample.HOSTS,
                                     Entities.ServiceExample.NAMENODE, ZOOKEEPER));
  }


  @Test
  public void testGetService() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_SERVICE_COMMAND, Entities.ServiceExample.NAMENODE.getName());
    execute(command);
    checkCommandOutput(Entities.ServiceExample.NAMENODE);
  }

  @Test
  public void testDeleteService() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_SERVICE_COMMAND, Entities.ServiceExample.HOSTS.getName());
    String deleteCommand = String.format(Constants.DELETE_SERVICE_COMMAND, Entities.ServiceExample.HOSTS.getName());

    execute(getCommand);
    checkCommandOutput(Entities.ServiceExample.HOSTS);

    OUTPUT_STREAM.reset();
    execute(deleteCommand);
    execute(getCommand);
    checkError();
  }

  @Test
  public void testListHardwareTypes() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_HARDWARE_TYPES_COMMAND);
    checkCommandOutput(Arrays.asList(Entities.HardwareTypeExample.LARGE, Entities.HardwareTypeExample.MEDIUM,
                                     Entities.HardwareTypeExample.SMALL));
  }

  @Test
  public void testGetHardwareType() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_HARDWARE_TYPE_COMMAND, Entities.HardwareTypeExample.LARGE.getName());
    execute(command);
    checkCommandOutput(Entities.HardwareTypeExample.LARGE);
  }

  @Test
  public void testDeleteHardwareType() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_HARDWARE_TYPE_COMMAND,
                                      Entities.HardwareTypeExample.SMALL.getName());
    String deleteCommand = String.format(Constants.DELETE_HARDWARE_TYPE_COMMAND,
                                         Entities.HardwareTypeExample.SMALL.getName());

    execute(getCommand);
    checkCommandOutput(Entities.HardwareTypeExample.SMALL);

    OUTPUT_STREAM.reset();
    execute(deleteCommand);
    execute(getCommand);
    checkError();
  }

  @Test
  public void testListImageTypes() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_IMAGE_TYPES_COMMAND);
    checkCommandOutput(Arrays.asList(Entities.ImageTypeExample.CENTOS_6, Entities.ImageTypeExample.UBUNTU_12));
  }

  @Test
  public void testGetImageType() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.GET_IMAGE_TYPE_COMMAND, Entities.ImageTypeExample.CENTOS_6.getName());
    execute(command);
    checkCommandOutput(Entities.ImageTypeExample.CENTOS_6);
  }

  @Test
  public void testDeleteImageType() throws InvalidCommandException, UnsupportedEncodingException {
    String getCommand = String.format(Constants.GET_IMAGE_TYPE_COMMAND, Entities.ImageTypeExample.CENTOS_6.getName());
    String deleteCommand = String.format(Constants.DELETE_IMAGE_TYPE_COMMAND, Entities.ImageTypeExample.CENTOS_6.getName());

    execute(getCommand);
    checkCommandOutput(Entities.ImageTypeExample.CENTOS_6);

    OUTPUT_STREAM.reset();
    execute(deleteCommand);
    execute(getCommand);
    checkError();
  }
}
