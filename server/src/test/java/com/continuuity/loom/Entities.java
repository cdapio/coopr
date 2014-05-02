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
package com.continuuity.loom;

import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.admin.AutomatorType;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.FieldSchema;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.admin.ParameterType;
import com.continuuity.loom.admin.ParametersSpecification;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProviderType;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * A bunch of example json strings for testing purposes.
 */
public class Entities {
  public static final Gson GSON = new Gson();
  public static final String JOYENT = "joyent";
  public static final String RACKSPACE = "rackspace";
  public static final String OPENSTACK = "openstack";

  public static class ProviderTypeExample {
    public static final ProviderType JOYENT =
      new ProviderType("joyent", "joyent provider type", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "joyent_username", new FieldSchema("user name", "text", "your joyent username", null, null, false),
            "joyent_keyname", new FieldSchema("key name", "text", "your joyent key name", null, null, false),
            "joyent_keyfile", new FieldSchema("path to key file", "text",
                                              "path to your joyent key file", null, null, false),
            "joyent_version", new FieldSchema("version", "text", "joyent version", null, null, false)
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("joyent_username", "joyent_keyname", "joyent_keyfile", "joyent_version")
          )
        ),
        ParameterType.USER,
        ParametersSpecification.EMPTY_SPECIFICATION
      ));
    public static final String JOYENT_STRING =
      "{\n" +
        "    \"name\": \"joyent\",\n" +
        "    \"description\": \"joyent provider type\",\n" +
        "    \"parameters\": {\n" +
        "        \"admin\": {\n" +
        "            \"fields\": {\n" +
        "                \"joyent_username\": {\n" +
        "                    \"label\": \"user name\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"your joyent username\",\n" +
        "                    \"override\": false\n" +
        "                },\n" +
        "                \"joyent_keyname\": {\n" +
        "                    \"label\": \"key name\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"your joyent key name\",\n" +
        "                    \"override\": false\n" +
        "                }\n," +
        "                \"joyent_keyfile\": {\n" +
        "                    \"label\": \"path to key file\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"path to your joyent key file\",\n" +
        "                    \"override\": false\n" +
        "                }\n," +
        "                \"joyent_version\": {\n" +
        "                    \"label\": \"version\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"joyent version\",\n" +
        "                    \"override\": false\n" +
        "                }\n" +
        "            },\n" +
        "            \"required\": [\n" +
        "                [\n" +
        "                    \"joyent_username\",\n" +
        "                    \"joyent_keyname\",\n" +
        "                    \"joyent_keyfile\",\n" +
        "                    \"joyent_version\"\n" +
        "                ]\n" +
        "            ]\n" +
        "        }\n" +
        "    }\n" +
        "}";
    public static final JsonObject JOYENT_JSON = GSON.fromJson(JOYENT_STRING, JsonObject.class);
    public static final ProviderType RACKSPACE =
      new ProviderType("rackspace", "rackspace provider type", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "rackspace_username", new FieldSchema("user name", "text", "your rackspace username", null, null, true),
            "rackspace_apikey", new FieldSchema("key name", "text", "your rackspace key name", null, null, true)
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("rackspace_username", "rackspace_apikey")
          )
        )));
    public static final String RACKSPACE_STRING =
      "{\n" +
        "    \"name\": \"rackspace\",\n" +
        "    \"description\": \"rackspace provider type\",\n" +
        "    \"parameters\": {\n" +
        "        \"admin\": {\n" +
        "            \"fields\": {\n" +
        "                \"rackspace_username\": {\n" +
        "                    \"label\": \"user name\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"your rackspace username\",\n" +
        "                    \"override\": false\n" +
        "                },\n" +
        "                \"rackspace_apikey\": {\n" +
        "                    \"label\": \"key name\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"your rackspace key name\",\n" +
        "                    \"override\": false\n" +
        "                }\n" +
        "            },\n" +
        "            \"required\": [\n" +
        "                [\n" +
        "                    \"rackspace_username\",\n" +
        "                    \"rackspace_apikey\"\n" +
        "                ]\n" +
        "            ]\n" +
        "        }\n" +
        "    }\n" +
        "}";
    public static final JsonObject RACKSPACE_JSON = GSON.fromJson(RACKSPACE_STRING, JsonObject.class);
    public static final ProviderType USER_RACKSPACE =
      new ProviderType("user-rackspace", "description", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.USER,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "rackspace_username", new FieldSchema("user name", "text", "your rackspace username", null, null, false),
            "rackspace_apikey", new FieldSchema("key name", "text", "your rackspace key name", null, null, false)
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("rackspace_username", "rackspace_apikey")
          )
        )
      ));
    public static final String USER_RACKSPACE_STRING =
      "{\n" +
        "    \"name\": \"user-rackspace\",\n" +
        "    \"description\": \"description\",\n" +
        "    \"parameters\": {\n" +
        "        \"admin\": {\n" +
        "            \"fields\": {\n" +
        "                \"rackspace_username\": {\n" +
        "                    \"label\": \"user name\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"your rackspace username\",\n" +
        "                    \"override\": true\n" +
        "                },\n" +
        "                \"rackspace_apikey\": {\n" +
        "                    \"label\": \"key name\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"your rackspace key name\",\n" +
        "                    \"override\": true\n" +
        "                }\n" +
        "            },\n" +
        "            \"required\": [\n" +
        "                [\n" +
        "                    \"rackspace_username\",\n" +
        "                    \"rackspace_apikey\"\n" +
        "                ]\n" +
        "            ]\n" +
        "        }\n" +
        "    }\n" +
        "}";
    public static final JsonObject USER_RACKSPACE_JSON = GSON.fromJson(USER_RACKSPACE_STRING, JsonObject.class);
  }

  public static class AutomatorTypeExample {
    public static final AutomatorType SHELL =
      new AutomatorType("shell", "shell automator", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "script",
            new FieldSchema("script", "text", "path to script", null, null, false),
            "data",
            new FieldSchema("script arguments", "text", "args", ImmutableSet.<String>of("opt1", "opt2"), null, false)
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("script")
          )
        )
      ));
    public static final String SHELL_STRING =
      "{\n" +
        "    \"name\": \"shell\",\n" +
        "    \"description\": \"shell automator\",\n" +
        "    \"parameters\": {\n" +
        "        \"admin\": {\n" +
        "            \"fields\": {\n" +
        "                \"script\": {\n" +
        "                    \"label\": \"script\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"path to script\",\n" +
        "                    \"override\": false\n" +
        "                },\n" +
        "                \"data\": {\n" +
        "                    \"label\": \"script arguments\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"args\",\n" +
        "                    \"options\": [ \"opt1\", \"opt2\" ],\n" +
        "                    \"override\": false\n" +
        "                }\n" +
        "            },\n" +
        "            \"required\": [\n" +
        "                [ \"script\" ]\n" +
        "            ]\n" +
        "        }\n" +
        "    }\n" +
        "}";
    public static final JsonObject SHELL_JSON = GSON.fromJson(SHELL_STRING, JsonObject.class);
    public static final AutomatorType CHEF =
      new AutomatorType("chef", "chef automator", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "recipe",
            new FieldSchema("chef recipe", "text", "recipe name", null, null, false),
            "args",
            new FieldSchema("chef arguments", "text", "args", ImmutableSet.<String>of("opt1", "opt2"), null, false)
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("recipe")
          )
        )
      ));
    public static final String CHEF_STRING =
      "{\n" +
        "    \"name\": \"chef\",\n" +
        "    \"description\": \"chef automator\",\n" +
        "    \"parameters\": {\n" +
        "        \"admin\": {\n" +
        "            \"fields\": {\n" +
        "                \"recipe\": {\n" +
        "                    \"label\": \"chef recipe\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"recipe name\",\n" +
        "                    \"override\": false\n" +
        "                },\n" +
        "                \"args\": {\n" +
        "                    \"label\": \"chef arguments\",\n" +
        "                    \"type\": \"text\",\n" +
        "                    \"tip\": \"args\",\n" +
        "                    \"options\": [ \"opt1\", \"opt2\" ],\n" +
        "                    \"override\": false\n" +
        "                }\n" +
        "            },\n" +
        "            \"required\": [\n" +
        "                [ \"recipe\" ]\n" +
        "            ]\n" +
        "        }\n" +
        "    }\n" +
        "}";
    public static final JsonObject CHEF_JSON = GSON.fromJson(CHEF_STRING, JsonObject.class);
    public static final AutomatorType PUPPET =
      new AutomatorType("puppet", "puppet automator", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "manifest",
            new FieldSchema("puppet manifest", "text", "manifest name", null, null, false),
            "args",
            new FieldSchema("puppet arguments", "text", "args", ImmutableSet.<String>of("opt1", "opt2"), null, false)
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("manifest")
          )
        )
      ));
  }

  public static class ProviderExample {
    public static final Provider JOYENT =
      new Provider("joyent", "Joyent Compute Service", Entities.JOYENT,
                   ImmutableMap.<String, String>of(
                     "joyent_username", "EXAMPLE_USERNAME",
                     "joyent_keyname", "EXAMPLE_KEYNAME",
                     "joyent_keyfile", "/path/to/example.key",
                     "joyent_version", "~7.0"
                   ));
    public static final String JOYENT_STRING =
      "{\n" +
      "  \"name\": \"joyent\",\n" +
      "  \"description\": \"Joyent Compute Service\",\n" +
      "  \"providertype\": \"joyent\",\n" +
      "  \"provisioner\": {\n" +
      "    \"joyent_username\": \"EXAMPLE_USERNAME\",\n" +
      "    \"joyent_keyname\": \"EXAMPLE_KEYNAME\",\n" +
      "    \"joyent_keyfile\": \"/path/to/example.key\",\n" +
      "    \"joyent_version\": \"~7.0\"\n" +
      "  }\n" +
      "}";
    public static final JsonObject JOYENT_JSON = GSON.fromJson(JOYENT_STRING, JsonObject.class);
    public static final Provider RACKSPACE =
      new Provider("rackspace", "Rackspace Public Cloud", Entities.RACKSPACE,
                   ImmutableMap.<String, String>of(
                     "rackspace_username", "EXAMPLE_USERNAME",
                     "rackspace_api_key", "EXAMPLE_API_KEY"));
    public static final String RACKSPACE_STRING =
      "{\n" +
      "  \"name\": \"rackspace\",\n" +
      "  \"description\": \"Rackspace Public Cloud\",\n" +
      "  \"providertype\": \"rackspace\",\n" +
      "  \"provisioner\": {\n" +
      "    \"rackspace_username\": \"EXAMPLE_USERNAME\",\n" +
      "    \"rackspace_api_key\": \"EXAMPLE_API_KEY\"\n" +
      "  }\n" +
      "}";
    public static final JsonObject RACKSPACE_JSON = GSON.fromJson(RACKSPACE_STRING, JsonObject.class);
  }

  public static class HardwareTypeExample {
    public static final HardwareType SMALL =
      new HardwareType("small", "1 vCPU, 1 GB RAM, 30+ GB Disk",
                       ImmutableMap.<String, Map<String, String>>of(
                         "joyent", ImmutableMap.<String, String>of("flavor", "Small 1GB"),
                         "rackspace", ImmutableMap.<String, String>of("flavor", "3")
                       ));
    public static final String SMALL_STRING =
      "{\n" +
      "  \"name\": \"small\",\n" +
      "  \"description\": \"1 vCPU, 1 GB RAM, 30+ GB Disk\",\n" +
      "  \"providermap\": {\n" +
      "    \"joyent\": {\n" +
      "      \"flavor\": \"Small 1GB\"\n" +
      "    },\n" +
      "    \"rackspace\": {\n" +
      "      \"flavor\": \"3\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject SMALL_JSON = GSON.fromJson(SMALL_STRING, JsonObject.class);
    public static final HardwareType MEDIUM =
      new HardwareType("medium", "2+ vCPU, 4 GB RAM, 120+ GB Disk",
                       ImmutableMap.<String, Map<String, String>>of(
                         "joyent", ImmutableMap.<String, String>of("flavor", "Medium 4GB"),
                         "rackspace", ImmutableMap.<String, String>of("flavor", "5")
                       ));
    public static final String MEDIUM_STRING =
      "{\n" +
      "  \"name\": \"medium\",\n" +
      "  \"description\": \"2+ vCPU, 4 GB RAM, 120+ GB Disk\",\n" +
      "  \"providermap\": {\n" +
      "    \"joyent\": {\n" +
      "      \"flavor\": \"Medium 4GB\"\n" +
      "    },\n" +
      "    \"rackspace\": {\n" +
      "      \"flavor\": \"5\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject MEDIUM_JSON = GSON.fromJson(MEDIUM_STRING, JsonObject.class);
    public static final HardwareType LARGE =
      new HardwareType("large", "4+ vCPU, 8 GB RAM, 240+ GB Disk",
                       ImmutableMap.<String, Map<String, String>>of(
                         "joyent", ImmutableMap.<String, String>of("flavor", "Large 8GB"),
                         "rackspace", ImmutableMap.<String, String>of("flavor", "6")
                       ));
    public static final String LARGE_STRING =
      "{\n" +
      "  \"name\": \"large\",\n" +
      "  \"description\": \"4+ vCPU, 8 GB RAM, 240+ GB Disk\",\n" +
      "  \"providermap\": {\n" +
      "    \"joyent\": {\n" +
      "      \"flavor\": \"Large 8GB\"\n" +
      "    },\n" +
      "    \"rackspace\": {\n" +
      "      \"flavor\": \"6\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject LARGE_JSON = GSON.fromJson(LARGE_STRING, JsonObject.class);
  }

  public static class ImageTypeExample {
    public static final ImageType CENTOS_6 =
      new ImageType("centos6", "CentOS 6", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.<String, String>of("image", "325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "rackspace", ImmutableMap.<String, String>of("image", "f70ed7c7-b42e-4d77-83d8-40fa29825b85")
      ));
    public static final String CENTOS_6_STRING =
      "{\n" +
      "  \"name\": \"centos6\",\n" +
      "  \"description\": \"CentOS 6\",\n" +
      "  \"providermap\": {\n" +
      "    \"joyent\": {\n" +
      "      \"image\": \"325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d\"\n" +
      "    },\n" +
      "    \"rackspace\": {\n" +
      "      \"image\": \"f70ed7c7-b42e-4d77-83d8-40fa29825b85\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject CENTOS_6_JSON = GSON.fromJson(CENTOS_6_STRING, JsonObject.class);
    public static final ImageType UBUNTU_12 =
      new ImageType("ubuntu12", "Ubuntu 12.04 LTS", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.<String, String>of("image", "d2ba0f30-bbe8-11e2-a9a2-6bc116856d85"),
        "rackspace", ImmutableMap.<String, String>of("image", "d45ed9c5-d6fc-4c9d-89ea-1b3ae1c83999")
      ));
    public static final String UBUNTU_12_STRING =
      "{\n" +
      "  \"name\": \"ubuntu12\",\n" +
      "  \"description\": \"Ubuntu 12.04 LTS\",\n" +
      "  \"providermap\": {\n" +
      "    \"joyent\": {\n" +
      "      \"image\": \"d2ba0f30-bbe8-11e2-a9a2-6bc116856d85\"\n" +
      "    },\n" +
      "    \"rackspace\": {\n" +
      "      \"image\": \"d45ed9c5-d6fc-4c9d-89ea-1b3ae1c83999\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject UBUNTU_12_JSON = GSON.fromJson(UBUNTU_12_STRING, JsonObject.class);
  }

  public static class ServiceExample {
    public static final Service HOSTS =
      new Service("hosts-1.0", "Manages /etc/hosts",
                  new ServiceDependencies(
                    ImmutableSet.<String>of("hosts"),
                    ImmutableSet.<String>of("hosts-1.1", "hosts-1.2"),
                    new ServiceStageDependencies(ImmutableSet.<String>of(), ImmutableSet.<String>of("base")),
                    new ServiceStageDependencies(ImmutableSet.<String>of(), ImmutableSet.<String>of("base"))
                  ),
                  ImmutableMap.<ProvisionerAction, ServiceAction>of(
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef", TestHelper.actionMapOf("recipe[loom_hosts::default]", null))
                  ));
    public static final String HOSTS_STRING =
      "{\n" +
      "  \"name\": \"hosts-1.0\",\n" +
      "  \"description\": \"Manages /etc/hosts\",\n" +
      "  \"dependencies\": {\n" +
      "    \"provides\": [ \"hosts\" ],\n" +
      "    \"conflicts\": [ \"hosts-1.1\", \"hosts-1.2\" ],\n" +
      "    \"install\": {\n" +
      "      \"requires\": [],\n" +
      "      \"uses\": [ \"base\" ]\n" +
      "     },\n" +
      "    \"runtime\": {\n" +
      "      \"requires\": [],\n" +
      "      \"uses\": [ \"base\" ]\n" +
      "     }\n" +
      "   },\n" +
      "  \"provisioner\": {\n" +
      "    \"actions\": {\n" +
      "      \"configure\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[loom_hosts::default]\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject HOSTS_JSON = GSON.fromJson(HOSTS_STRING, JsonObject.class);
    public static final Service NAMENODE =
      new Service("namenode", "Hadoop HDFS NameNode",
                  ImmutableSet.<String>of("hosts"),
                  ImmutableMap.<ProvisionerAction, ServiceAction>of(
                    ProvisionerAction.INSTALL,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[hadoop::hadoop_hdfs_namenode]", null)),
                    ProvisionerAction.INITIALIZE,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]", null)),
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[hadoop::default]", null)),
                    ProvisionerAction.START,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[loom_service_runner::default]", "start data")),
                    ProvisionerAction.STOP,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[loom_service_runner::default]", "stop data"))
                  ));
    public static final String NAMENODE_STRING =
      "{\n" +
      "  \"name\": \"namenode\",\n" +
      "  \"description\": \"Hadoop HDFS NameNode\",\n" +
      "  \"dependencies\": {\n" +
      "    \"provides\": [],\n" +
      "    \"conflicts\": [],\n" +
      "    \"install\": {\n" +
      "      \"requires\": [],\n" +
      "      \"uses\": []\n" +
      "     },\n" +
      "    \"runtime\": {\n" +
      "      \"requires\": [ \"hosts\" ],\n" +
      "      \"uses\": []\n" +
      "     }\n" +
      "   },\n" +
      "  \"provisioner\": {\n" +
      "    \"actions\": {\n" +
      "      \"install\": {\n" +
      "        \"type\":\"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[hadoop::hadoop_hdfs_namenode]\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"initialize\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"configure\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[hadoop::default]\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"start\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "          \"data\": \"start data\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"stop\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "          \"data\": \"stop data\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject NAMENODE_JSON = GSON.fromJson(NAMENODE_STRING, JsonObject.class);
    public static final Service DATANODE =
      new Service("datanode", "Hadoop HDFS DataNode",
                  ImmutableSet.<String>of("hosts", "namenode"),
                  ImmutableMap.<ProvisionerAction, ServiceAction>of(
                    ProvisionerAction.INSTALL,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[hadoop::hadoop_hdfs_datanode]", null)),
                    ProvisionerAction.INITIALIZE,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[hadoop_wrapper::hadoop_hdfs_datanode_init]", null)),
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[hadoop::default]", null)),
                    ProvisionerAction.START,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[loom_service_runner::default]", "start data")),
                    ProvisionerAction.STOP,
                    new ServiceAction("chef",
                                      TestHelper.actionMapOf("recipe[loom_service_runner::default]", "stop data"))
                  ));
    public static final String DATANODE_STRING =
      "{\n" +
      "  \"name\": \"datanode\",\n" +
      "  \"description\": \"Hadoop HDFS DataNode\",\n" +
      "  \"dependencies\": {\n" +
      "    \"provides\": [],\n" +
      "    \"conflicts\": [],\n" +
      "    \"install\": {\n" +
      "      \"requires\": [],\n" +
      "      \"uses\": []\n" +
      "     },\n" +
      "    \"runtime\": {\n" +
      "      \"requires\": [ \"hosts\", \"namenode\" ],\n" +
      "      \"uses\": []\n" +
      "     }\n" +
      "   },\n" +
      "  \"provisioner\": {\n" +
      "    \"actions\": {\n" +
      "      \"install\": {\n" +
      "        \"type\":\"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[hadoop::hadoop_hdfs_datanode]\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"configure\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[hadoop::default]\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"start\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "          \"data\": \"start data\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"stop\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"fields\": {\n" +
      "          \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "          \"data\": \"stop data\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject DATANODE_JSON = GSON.fromJson(DATANODE_STRING, JsonObject.class);
  }

  public static class ClusterTemplateExample {
    public static JsonObject clusterConf = new JsonObject();

    static {
      JsonObject hadoop = json("core_site", json("fs.defaultFS", "hdfs://%host.service.hadoop-hdfs-namenode%"));
      hadoop.add("yarn_site", json("yarn.resourcemanager.hostname", "%host.service.hadoop-yarn-resourcemanager%"));
      JsonObject hbase =
        json("hbase_site",
             json("hbase.rootdir", "hdfs://%host.service.hadoop-hdfs-namenode%/hbase",
                  "hbase.zookeeper.quorum", "%join(map(host.service.zookeeper-server,'$:2181'),',')%"));
      clusterConf.add("hadoop", hadoop);
      clusterConf.add("hbase", hbase);
    }

    public static final ClusterTemplate HDFS =
      new ClusterTemplate(
        "hdfs", "Hdfs cluster",
        new ClusterDefaults(
          ImmutableSet.<String>of("hosts", "namenode", "datanode"),
          "joyent",
          null, null, null, null
        ),
        new Compatibilities(null, null, ImmutableSet.<String>of("hosts", "namenode", "datanode")),
        new Constraints(
          ImmutableMap.<String, ServiceConstraint>of(
            "namenode",
            new ServiceConstraint(
              ImmutableSet.<String>of("large"),
              ImmutableSet.<String>of("centos6", "ubuntu12"),
              1, 1, 1, null)
          ),
          new LayoutConstraint(
            null,
            ImmutableSet.<Set<String>>of(
              ImmutableSet.<String>of("namenode", "datanode")
            )
          )
        ),
        null
      );
    public static final String HDFS_STRING =
      "{\n" +
      "  \"name\": \"hdfs\",\n" +
      "  \"description\": \"Hdfs cluster\",\n" +
      "  \"defaults\": {\n" +
      "    \"provider\": \"joyent\",\n" +
      "    \"services\": [\n" +
      "      \"hosts\",\n" +
      "      \"namenode\",\n" +
      "      \"datanode\"\n" +
      "    ],\n" +
      "    \"config\": {\n" +
      "      \"hadoop\": {\n" +
      "        \"core_site\": {\n" +
      "          \"fs.defaultFS\": \"hdfs://%host.service.hadoop-hdfs-namenode%\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"compatibility\": {\n" +
      "    \"services\": [\"hosts\", \"namenode\", \"datanode\"]\n" +
      "  },\n" +
      "  \"constraints\": {\n" +
      "    \"layout\": {\n" +
      "      \"mustcoexist\": [],\n" +
      "      \"cantcoexist\": [\n" +
      "        [ \"namenode\", \"datanode\" ]\n" +
      "      ]\n" +
      "    },\n" +
      "    \"services\": {\n" +
      "      \"namenode\": {\n" +
      "        \"hardwaretypes\": [\"large\"],\n" +
      "        \"imagetypes\": [\"centos6\", \"ubuntu12\"],\n" +
      "        \"quantities\": {\n" +
      "          \"min\": 1,\n" +
      "          \"max\": 1,\n" +
      "          \"stepSize\": 1\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"administration\":{\n" +
      "     \"leaseduration\":{\n" +
      "        \"initial\":0,\n" +
      "        \"max\":0,\n" +
      "        \"step\":0\n" +
      "     }\n" +
      "  }\n" +
      "}";
    public static final JsonObject HDFS_JSON = GSON.fromJson(HDFS_STRING, JsonObject.class);
    public static final ClusterTemplate REACTOR =
      new ClusterTemplate(
        "hadoop-reactor", "Hadoop cluster with reactor",
        new ClusterDefaults(
          ImmutableSet.<String>of("hosts", "namenode", "datanode", "resourcemanager", "nodemanager",
                                  "hbasemaster", "regionserver", "zookeeper", "reactor"),
          "joyent",
          null, null, null, null
        ),
        new Compatibilities(null, null,
                            ImmutableSet.<String>of("hosts", "namenode", "datanode", "resourcemanager", "nodemanager",
                                                    "hbasemaster", "regionserver", "zookeeper", "reactor")),
        new Constraints(
          ImmutableMap.<String, ServiceConstraint>of(
            "namenode",
            new ServiceConstraint(
              ImmutableSet.<String>of("large"),
              ImmutableSet.<String>of("centos6", "ubuntu12"),
              1, 1, 1, null)
          ),
          new LayoutConstraint(
            null,
            ImmutableSet.<Set<String>>of(
              ImmutableSet.<String>of("namenode", "datanode")
            )
          )
        ),
        new Administration(new LeaseDuration(10000, 900000, 1000))
      );
    public static final String REACTOR_STRING =
      "{\n" +
      "  \"name\": \"hadoop-reactor\",\n" +
      "  \"description\": \"Hadoop cluster with reactor\",\n" +
      "  \"defaults\": {\n" +
      "    \"services\": [\n" +
      "      \"hosts\",\n" +
      "      \"namenode\",\n" +
      "      \"datanode\",\n" +
      "      \"resourcemanager\",\n" +
      "      \"nodemanager\",\n" +
      "      \"hbasemaster\",\n" +
      "      \"regionserver\",\n" +
      "      \"zookeeper\",\n" +
      "      \"reactor\"\n" +
      "    ],\n" +
      "    \"provider\": \"joyent\",\n" +
      "    \"config\": {\n" +
      "      \"hadoop\": {\n" +
      "        \"core_site\": {\n" +
      "          \"fs.defaultFS\": \"hdfs://%host.service.hadoop-hdfs-namenode%\"\n" +
      "        },\n" +
      "        \"yarn_site\": {\n" +
      "          \"yarn.resourcemanager.hostname\": \"%host.service.hadoop-yarn-resourcemanager%\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"hbase\": {\n" +
      "        \"hbase_site\": {\n" +
      "          \"hbase.rootdir\": \"hdfs://%host.service.hadoop-hdfs-namenode%/hbase\",\n" +
      "          \"hbase.zookeeper.quorum\": \"%join(map(host.service.zookeeper-server,'$:2181'),',')%\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"compatibility\": {\n" +
      "    \"services\": [\"hosts\", \"namenode\", \"datanode\"]\n" +
      "  },\n" +
      "  \"constraints\": {\n" +
      "    \"layout\": {\n" +
      "      \"mustcoexist\": [\n" +
      "        [ \"namenode\", \"resourcemanager\", \"hbasemaster\" ],\n" +
      "        [ \"datanode\", \"nodemanager\", \"regionserver\" ]\n" +
      "      ],\n" +
      "      \"cantcoexist\": [\n" +
      "        [ \"namenode\", \"datanode\" ],\n" +
      "        [ \"namenode\", \"reactor\" ],\n" +
      "        [ \"namenode\", \"zookeeper\" ],\n" +
      "        [ \"datanode\", \"reactor\" ],\n" +
      "        [ \"datanode\", \"zookeeper\" ]\n" +
      "      ]\n" +
      "    },\n" +
      "    \"services\": {\n" +
      "      \"namenode\": {\n" +
      "        \"hardwaretypes\": [\"large\"],\n" +
      "        \"imagetypes\": [\"centos6\", \"ubuntu12\"],\n" +
      "        \"quantities\": {\n" +
      "          \"min\": 1,\n" +
      "          \"max\": 1,\n" +
      "          \"stepSize\": 1\n" +
      "        }\n" +
      "      },\n" +
      "      \"datanode\": {\n" +
      "        \"hardwaretypes\": [\"medium\"],\n" +
      "        \"imagetypes\": [\"centos6\", \"ubuntu12\"],\n" +
      "        \"quantities\": {\n" +
      "          \"min\": 1,\n" +
      "          \"max\": 50,\n" +
      "          \"stepSize\": 1\n" +
      "        }\n" +
      "      },\n" +
      "      \"zookeeper\": {\n" +
      "        \"hardwaretypes\": [\"small\"],\n" +
      "        \"imagetypes\": [\"centos6\", \"ubuntu12\"],\n" +
      "        \"quantities\": {\n" +
      "          \"min\": 1,\n" +
      "          \"max\": 5,\n" +
      "          \"stepSize\": 2\n" +
      "        }\n" +
      "      },\n" +
      "      \"reactor\": {\n" +
      "        \"hardwaretypes\": [\"small\", \"medium\"],\n" +
      "        \"imagetypes\": [\"centos6\", \"ubuntu12\"],\n" +
      "        \"quantities\": {\n" +
      "          \"min\": 1,\n" +
      "          \"max\": 5,\n" +
      "          \"stepSize\": 1\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"administration\":{\n" +
      "     \"leaseduration\":{\n" +
      "        \"initial\":10000,\n" +
      "        \"max\":900000,\n" +
      "        \"step\":1000\n" +
      "     }\n" +
      "  }\n" +
      "}";
    public static final JsonObject REACTOR_JSON = GSON.fromJson(REACTOR_STRING, JsonObject.class);
    public static final String HADOOP_DISTRIBUTED_STRING =
      "{\n" +
      "  \"name\": \"hadoop-distributed\",\n" +
      "  \"description\": \"Hadoop cluster without high-availability\",\n" +
      "  \"defaults\": {\n" +
      "    \"services\": [\n" +
      "      \"firewall\",\n" +
      "      \"hosts\",\n" +
      "      \"namenode\",\n" +
      "      \"datanode\",\n" +
      "      \"resourcemanager\",\n" +
      "      \"nodemanager\"\n" +
      "    ],\n" +
      "    \"provider\": \"rackspace\",\n" +
      "    \"hardwaretype\": \"medium\",\n" +
      "    \"imagetype\": \"ubuntu12\",\n" +
      "    \"config\": {\n" +
      "      \"hadoop\": {\n" +
      "        \"core_site\": {\n" +
      "          \"fs.defaultFS\": \"hdfs://%host.service.hadoop-hdfs-namenode%\"\n" +
      "        },\n" +
      "        \"hdfs_site\": {\n" +
      "          \"dfs.datanode.max.xcievers\": \"4096\"\n" +
      "        },\n" +
      "        \"mapred_site\": {\n" +
      "          \"mapreduce.framework.name\": \"yarn\"\n" +
      "        },\n" +
      "        \"yarn_site\": {\n" +
      "          \"yarn.resourcemanager.hostname\": \"%host.service.hadoop-yarn-resourcemanager%\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"hbase\": {\n" +
      "        \"hbase_site\": {\n" +
      "          \"hbase.rootdir\": \"hdfs://%host.service.hadoop-hdfs-namenode%/hbase\",\n" +
      "          \"hbase.cluster.distributed\": \"true\",\n" +
      "          \"hbase.zookeeper.quorum\": \"%join(map(host.service.zookeeper-server,'$:2181'),',')%\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"hive\": {\n" +
      "        \"hive_site\": {\n" +
      "          \"javax.jdo.option.ConnectionURL\": \"jdbc:mysql://%host.service" +
      ".mysql-server%:3306/hive?createDatabaseIfNotExist=true\",\n" +
      "          \"javax.jdo.option.ConnectionDriverName\": \"com.mysql.jdbc.Driver\",\n" +
      "          \"javax.jdo.option.ConnectionUserName\": \"dbuser\",\n" +
      "          \"javax.jdo.option.ConnectionPassword\": \"dbuserpassword\",\n" +
      "          \"hive.metastore.uris\": \"thrift://%host.service.hive-metastore%:9083\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"compatibility\": {\n" +
      "    \"hardwaretypes\": [\n" +
      "      \"small\",\n" +
      "      \"medium\",\n" +
      "      \"large\"\n" +
      "    ],\n" +
      "    \"imagetypes\": [\n" +
      "      \"centos6\",\n" +
      "      \"ubuntu12\"\n" +
      "    ],\n" +
      "    \"services\": [\n" +
      "      \"firewall\",\n" +
      "      \"hosts\",\n" +
      "      \"namenode\",\n" +
      "      \"secondarynamenode\",\n" +
      "      \"datanode\",\n" +
      "      \"resourcemanager\",\n" +
      "      \"nodemanager\",\n" +
      "      \"zookeeper\",\n" +
      "      \"hbasemaster\",\n" +
      "      \"regionserver\",\n" +
      "      \"hive-metastore\",\n" +
      "      \"mysql-server\",\n" +
      "      \"oozie\",\n" +
      "      \"reactor\"\n" +
      "    ]\n" +
      "  },\n" +
      "  \"constraints\": {\n" +
      "    \"layout\": {\n" +
      "      \"mustcoexist\": [\n" +
      "        [ \"datanode\", \"nodemanager\", \"regionserver\" ]\n" +
      "      ],\n" +
      "      \"cantcoexist\": [\n" +
      "        [ \"namenode\", \"secondarynamenode\" ],\n" +
      "        [ \"resourcemanager\", \"nodemanager\" ],\n" +
      "        [ \"namenode\", \"datanode\" ],\n" +
      "        [ \"datanode\", \"mysql-server\" ],\n" +
      "        [ \"datanode\", \"reactor\" ],\n" +
      "        [ \"hbasemaster\", \"regionserver\" ]\n" +
      "      ]\n" +
      "    },\n" +
      "    \"services\": {\n" +
      "      \"namenode\": {\n" +
      "        \"quantities\": {\n" +
      "          \"min\": \"1\",\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"secondarynamenode\": {\n" +
      "        \"quantities\": {\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"resourcemanager\": {\n" +
      "        \"quantities\": {\n" +
      "          \"min\": \"1\",\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"zookeeper\": {\n" +
      "        \"quantities\": {\n" +
      "          \"min\": \"1\",\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"hbasemaster\": {\n" +
      "        \"quantities\": {\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"mysql-server\": {\n" +
      "        \"quantities\": {\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      },\n" +
      "      \"reactor\": {\n" +
      "        \"quantities\": {\n" +
      "          \"max\": \"1\"\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"administration\":{\n" +
      "     \"leaseduration\":{\n" +
      "        \"initial\":0,\n" +
      "        \"max\":0,\n" +
      "        \"step\":0\n" +
      "     }\n" +
      "  }\n" +
      "}";
    public static final String REACTOR2_STRING =
      "{\n" +
        "  \"name\": \"reactor\",\n" +
        "  \"description\": \"Hadoop cluster without high-availability and Continuuity Reactor\",\n" +
        "  \"defaults\": {\n" +
        "    \"services\": [\n" +
        "      \"firewall\",\n" +
        "      \"hosts\",\n" +
        "      \"hadoop-hdfs-namenode\",\n" +
        "      \"hadoop-hdfs-datanode\",\n" +
        "      \"hadoop-yarn-resourcemanager\",\n" +
        "      \"hadoop-yarn-nodemanager\",\n" +
        "      \"zookeeper-server\",\n" +
        "      \"hbase-master\",\n" +
        "      \"hbase-regionserver\",\n" +
        "      \"reactor\"\n" +
        "    ],\n" +
        "    \"provider\": \"rackspace\",\n" +
        "    \"hardwaretype\": \"medium\",\n" +
        "    \"imagetype\": \"ubuntu12\",\n" +
        "    \"config\": {\n" +
        "      \"hadoop\": {\n" +
        "        \"core_site\": {\n" +
        "          \"fs.defaultFS\": \"hdfs://%host.service.hadoop-hdfs-namenode%\"\n" +
        "        },\n" +
        "        \"hdfs_site\": {\n" +
        "          \"dfs.datanode.max.xcievers\": \"4096\"\n" +
        "        },\n" +
        "        \"mapred_site\": {\n" +
        "          \"mapreduce.framework.name\": \"yarn\"\n" +
        "        },\n" +
        "        \"yarn_site\": {\n" +
        "          \"yarn.resourcemanager.hostname\": \"%host.service.hadoop-yarn-resourcemanager%\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"hbase\": {\n" +
        "        \"hbase_site\": {\n" +
        "          \"hbase.rootdir\": \"hdfs://%host.service.hadoop-hdfs-namenode%/hbase\",\n" +
        "          \"hbase.zookeeper.quorum\": \"%join(map(host.service.zookeeper-server,'$:2181'),',')%\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"hive\": {\n" +
        "        \"hive_site\": {\n" +
        "          \"javax.jdo.option.ConnectionURL\": \"jdbc:mysql://%host.service.mysql-server%:3306/hive?createDatabaseIfNotExist=true\",\n" +
        "          \"javax.jdo.option.ConnectionDriverName\": \"com.mysql.jdbc.Driver\",\n" +
        "          \"javax.jdo.option.ConnectionUserName\": \"dbuser\",\n" +
        "          \"javax.jdo.option.ConnectionPassword\": \"dbuserpassword\",\n" +
        "          \"hive.metastore.uris\": \"thrift://%host.service.hive-metastore%:9083\"\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"compatibility\": {\n" +
        "    \"hardwaretypes\": [\n" +
        "      \"small\",\n" +
        "      \"medium\",\n" +
        "      \"large\"\n" +
        "    ],\n" +
        "    \"imagetypes\": [\n" +
        "      \"centos6\",\n" +
        "      \"ubuntu12\"\n" +
        "    ],\n" +
        "    \"services\": [\n" +
        "      \"firewall\",\n" +
        "      \"hosts\",\n" +
        "      \"hadoop-hdfs-namenode\",\n" +
        "      \"hadoop-hdfs-secondarynamenode\",\n" +
        "      \"hadoop-hdfs-datanode\",\n" +
        "      \"hadoop-yarn-resourcemanager\",\n" +
        "      \"hadoop-yarn-nodemanager\",\n" +
        "      \"zookeeper-server\",\n" +
        "      \"hbase-master\",\n" +
        "      \"hbase-regionserver\",\n" +
        "      \"hive-metastore\",\n" +
        "      \"mysql-server\",\n" +
        "      \"oozie\",\n" +
        "      \"reactor\"\n" +
        "    ]\n" +
        "  },\n" +
        "  \"constraints\": {\n" +
        "    \"layout\": {\n" +
        "      \"mustcoexist\": [\n" +
        "        [ \"hadoop-hdfs-datanode\", \"hadoop-yarn-nodemanager\", \"hbase-regionserver\" ]\n" +
        "      ],\n" +
        "      \"cantcoexist\": [\n" +
        "        [ \"hadoop-hdfs-namenode\", \"hadoop-hdfs-secondarynamenode\" ],\n" +
        "        [ \"hadoop-yarn-resourcemanager\", \"hadoop-yarn-nodemanager\" ],\n" +
        "        [ \"hadoop-hdfs-namenode\", \"hadoop-hdfs-datanode\" ],\n" +
        "        [ \"hadoop-hdfs-datanode\", \"mysql-server\" ],\n" +
        "        [ \"hbase-master\", \"hbase-regionserver\" ]\n" +
        "      ]\n" +
        "    },\n" +
        "    \"services\": {\n" +
        "      \"hadoop-hdfs-namenode\": {\n" +
        "        \"quantities\": {\n" +
        "          \"min\": \"1\",\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"hadoop-hdfs-secondarynamenode\": {\n" +
        "        \"quantities\": {\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"hadoop-yarn-resourcemanager\": {\n" +
        "        \"quantities\": {\n" +
        "          \"min\": \"1\",\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"zookeeper-server\": {\n" +
        "        \"quantities\": {\n" +
        "          \"min\": \"1\",\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"hbase-master\": {\n" +
        "        \"quantities\": {\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"mysql-server\": {\n" +
        "        \"quantities\": {\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      },\n" +
        "      \"reactor\": {\n" +
        "        \"quantities\": {\n" +
        "          \"max\": \"1\"\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"administration\":{\n" +
        "     \"leaseduration\":{\n" +
        "        \"initial\":0,\n" +
        "        \"max\":0,\n" +
        "        \"step\":0\n" +
        "     }\n" +
        "  }\n" +
        "}";
  }

  public static class ClusterExample {
    private static String node1 = "node1";
    private static String node2 = "node2";
    public static Cluster CLUSTER =
      new Cluster("123", "user1", "name", 1234567890, "description",
                  ProviderExample.RACKSPACE, ClusterTemplateExample.HDFS,
                  ImmutableSet.of(node1, node2),
                  ImmutableSet.of(ServiceExample.NAMENODE.getName(), ServiceExample.DATANODE.getName()));
    public static Node NODE1 =
      new Node(node1,
               CLUSTER.getId(),
               ImmutableSet.of(ServiceExample.NAMENODE),
               ImmutableMap.<String, String>of());
    public static Node NODE2 =
      new Node(node2,
               CLUSTER.getId(),
               ImmutableSet.of(ServiceExample.DATANODE),
               ImmutableMap.<String, String>of());
  }

  protected static JsonObject json(String key, JsonObject val) {
    JsonObject out = new JsonObject();
    out.add(key, val);
    return out;
  }

  protected static JsonObject json(String... keyvals) {
    JsonObject out = new JsonObject();
    for (int i = 0; i < keyvals.length; i += 2) {
      out.addProperty(keyvals[i], keyvals[i + 1]);
    }
    return out;
  }
}
