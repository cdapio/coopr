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
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
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

  public static class ProviderExample {
    public static final Provider JOYENT =
      new Provider("joyent", "Joyent Compute Service", Provider.Type.JOYENT,
                   ImmutableMap.<String, Map<String, String>>of(
                     "auth",
                     ImmutableMap.<String, String>of(
                       "joyent_username", "EXAMPLE_USERNAME",
                       "joyent_keyname", "EXAMPLE_KEYNAME",
                       "joyent_keyfile", "/path/to/example.key",
                       "joyent_version", "~7.0"
                     )
                   ));
    public static final String JOYENT_STRING =
      "{\n" +
      "  \"name\": \"joyent\",\n" +
      "  \"description\": \"Joyent Compute Service\",\n" +
      "  \"providertype\": \"joyent\",\n" +
      "  \"provisioner\": {\n" +
      "    \"auth\": {\n" +
      "      \"joyent_username\": \"EXAMPLE_USERNAME\",\n" +
      "      \"joyent_keyname\": \"EXAMPLE_KEYNAME\",\n" +
      "      \"joyent_keyfile\": \"/path/to/example.key\",\n" +
      "      \"joyent_version\": \"~7.0\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
    public static final JsonObject JOYENT_JSON = GSON.fromJson(JOYENT_STRING, JsonObject.class);
    public static final Provider RACKSPACE =
      new Provider("rackspace", "Rackspace Public Cloud", Provider.Type.RACKSPACE,
                   ImmutableMap.<String, Map<String, String>>of(
                     "auth",
                     ImmutableMap.<String, String>of(
                       "rackspace_username", "EXAMPLE_USERNAME",
                       "rackspace_api_key", "EXAMPLE_API_KEY"
                     )
                   ));
    public static final String RACKSPACE_STRING =
      "{\n" +
      "  \"name\": \"rackspace\",\n" +
      "  \"description\": \"Rackspace Public Cloud\",\n" +
      "  \"providertype\": \"rackspace\",\n" +
      "  \"provisioner\": {\n" +
      "    \"auth\": {\n" +
      "      \"rackspace_username\": \"EXAMPLE_USERNAME\",\n" +
      "      \"rackspace_api_key\": \"EXAMPLE_API_KEY\"\n" +
      "    }\n" +
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
                    ProvisionerAction.CONFIGURE, new ServiceAction("chef", "recipe[loom_hosts::default]", null)
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
      "        \"script\": \"recipe[loom_hosts::default]\"\n" +
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
                    new ServiceAction("chef", "recipe[hadoop::hadoop_hdfs_namenode]", null),
                    ProvisionerAction.INITIALIZE,
                    new ServiceAction("chef", "recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]", null),
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef", "recipe[hadoop::default]", null),
                    ProvisionerAction.START,
                    new ServiceAction("chef", "recipe[loom_service_runner::default]", "start data"),
                    ProvisionerAction.STOP,
                    new ServiceAction("chef", "recipe[loom_service_runner::default]", "stop data")
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
      "        \"script\": \"recipe[hadoop::hadoop_hdfs_namenode]\"\n" +
      "      },\n" +
      "      \"initialize\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]\"\n" +
      "      },\n" +
      "      \"configure\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[hadoop::default]\"\n" +
      "      },\n" +
      "      \"start\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "        \"data\": \"start data\"\n" +
      "      },\n" +
      "      \"stop\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "        \"data\": \"stop data\"\n" +
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
                    new ServiceAction("chef", "recipe[hadoop::hadoop_hdfs_datanode]", null),
                    ProvisionerAction.INITIALIZE,
                    new ServiceAction("chef", "recipe[hadoop_wrapper::hadoop_hdfs_datanode_init]", null),
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef", "recipe[hadoop::default]", null),
                    ProvisionerAction.START,
                    new ServiceAction("chef", "recipe[loom_service_runner::default]", "start data"),
                    ProvisionerAction.STOP,
                    new ServiceAction("chef", "recipe[loom_service_runner::default]", "stop data")
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
      "        \"script\": \"recipe[hadoop::hadoop_hdfs_datanode]\"\n" +
      "      },\n" +
      "      \"configure\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[hadoop::default]\"\n" +
      "      },\n" +
      "      \"start\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "        \"data\": \"start data\"\n" +
      "      },\n" +
      "      \"stop\": {\n" +
      "        \"type\": \"chef\",\n" +
      "        \"script\": \"recipe[loom_service_runner::default]\",\n" +
      "        \"data\": \"stop data\"\n" +
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
