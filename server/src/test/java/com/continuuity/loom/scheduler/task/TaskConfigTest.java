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
package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.Entities;
import com.continuuity.loom.TestHelper;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.utils.ImmutablePair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 * Test TaskConfig.
 */
public class TaskConfigTest {
  private static final Gson GSON = new Gson();

  @Test
  public void testConfig() throws Exception {
    Map<String, String> providerConfig = ImmutableMap.of("openstack_api_url",
                                                         "http://1.2.3.4:5000/v2.0/tokens",
                                                         "openstack_username", "user");
    Provider provider = new Provider("openstack-central", "Default provider", Entities.OPENSTACK, providerConfig);

    JsonObject conf = Entities.ClusterTemplateExample.clusterConf;
    ClusterTemplate c1 = new ClusterTemplate(
      "hadoop-dev",
      "development hadoop cluster template",
      new ClusterDefaults(
        ImmutableSet.of("namenode", "datanode", "resourcemanager", "nodemanager"),
        "openstack1", null, null, null, conf),
      new Compatibilities(
        null,
        null,
        ImmutableSet.of("namenode", "datanode", "resourcemanager", "nodemanager", "hbasemaster", "regionserver")
      ),
      new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          "namenode",
          new ServiceConstraint(
            ImmutableSet.of("hardware1", "hardware3"),
            ImmutableSet.of("imageA", "imageB", "imageC"), 1, 1, 1, null),
          "datanode",
          new ServiceConstraint(
            ImmutableSet.of("hardware2"),
            ImmutableSet.of("imageA"), 1, 5, 1, new ImmutablePair<Integer, Integer>(1, 1))
        ),
        new LayoutConstraint(
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("datanode", "nodemanager"),
            ImmutableSet.of("namenode", "resourcemanager")
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("datanode", "namenode")
          )
        )
      ),
      null
    );

    Service s1 = new Service("datanode", "hadoop datanode", ImmutableSet.of("namenode"),
                             ImmutableMap.<ProvisionerAction, ServiceAction>of(
                               ProvisionerAction.INSTALL,
                               new ServiceAction(
                                 "chef", TestHelper.actionMapOf("install recipe", "{\"foo\": { \"bar\": \"baz\" } }")),
                               ProvisionerAction.REMOVE,
                               new ServiceAction("shell", TestHelper.actionMapOf("remove recipe", "arbitrary data"))
                             )
    );
    Service s2 = new Service("namenode", "hadoop namenode", ImmutableSet.of("hosts"),
                             ImmutableMap.<ProvisionerAction, ServiceAction>of(
                               ProvisionerAction.INSTALL,
                               new ServiceAction("chef", TestHelper.actionMapOf("install recipe", null)),
                               ProvisionerAction.REMOVE,
                               new ServiceAction("chef", TestHelper.actionMapOf("remove recipe", "arbitrary data")),
                               ProvisionerAction.CONFIGURE,
                               new ServiceAction("chef", TestHelper.actionMapOf("configure recipe", null))
                             )
    );
    Service s3 = new Service("hosts", "for managing /etc/hosts", ImmutableSet.<String>of(),
                             ImmutableMap.<ProvisionerAction, ServiceAction>of(
                               ProvisionerAction.CONFIGURE,
                               new ServiceAction("chef", TestHelper.actionMapOf("configure recipe", null))
                             )
    );

    Map<String, Node> nodeMap =
      ImmutableMap.of("node1",
                      new Node("node1", "1", ImmutableSet.of(s1, s2),
                               ImmutableMap.of("flavor", "5",
                                               "hostname", "node1.loom.continuuity.net",
                                               "bootstrap_keypair", "iad-root",
                                               "image", "f70ed7c7-b42e-4d77-83d8-40fa29825b85")),
                      "node2",
                      new Node("node2", "1", ImmutableSet.of(s2, s3),
                               ImmutableMap.of("flavor", "1",
                                               "hostname", "node2.loom.continuuity.net",
                                               "bootstrap_keypair", "iad-root",
                                               "image", "f70ed7c7-b42e-4d77-83d8-40fa29825b85")));

    Cluster defaultCluster = new Cluster("1", "user", "cluster1", System.currentTimeMillis(),
                                         "Test cluster", provider,
                                         c1, nodeMap.keySet(),
                                         ImmutableSet.of(s1.getName(), s2.getName(), s3.getName()));

    JsonObject actualDefaultConfig = TaskConfig.getConfig(defaultCluster, nodeMap.get("node1"), s1,
                                                          ProvisionerAction.INSTALL);

    Assert.assertEquals(GSON.fromJson(DEFAULT_CONFIG, JsonObject.class), actualDefaultConfig);

    // Test user config
    JsonObject userConf = new JsonObject();
    userConf.add("hadoop", conf.get("hadoop"));
    Cluster userCluster = new Cluster("1", "user", "cluster1", System.currentTimeMillis(),
                                      "Test cluster", provider,
                                      c1, nodeMap.keySet(),
                                      ImmutableSet.of(s1.getName(), s2.getName(), s3.getName()), userConf);

    JsonObject actualUserConfig = TaskConfig.getConfig(userCluster, nodeMap.get("node1"), s1,
                                                       ProvisionerAction.INSTALL);


    Assert.assertEquals(GSON.fromJson(USER_CONFIG, JsonObject.class), actualUserConfig);
  }

  private static final String DEFAULT_CONFIG =
    "{\n" +
      "  \"cluster\": {\n" +
      "    \"hadoop\": {\n" +
      "      \"core_site\": {\n" +
      "        \"fs.defaultFS\": \"hdfs://%host.service.hadoop-hdfs-namenode%\"\n" +
      "      },\n" +
      "      \"yarn_site\": {\n" +
      "        \"yarn.resourcemanager.hostname\": \"%host.service.hadoop-yarn-resourcemanager%\"\n" +
      "      }\n" +
      "    },\n" +
      "    \"hbase\": {\n" +
      "      \"hbase_site\": {\n" +
      "        \"hbase.rootdir\": \"hdfs://%host.service.hadoop-hdfs-namenode%/hbase\",\n" +
      "        \"hbase.zookeeper.quorum\": \"%join(map(host.service.zookeeper-server,\\u0027$:2181\\u0027),\\u0027,\\u0027)%\"\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"service\": {\n" +
      "    \"name\": \"datanode\",\n" +
      "    \"action\": {\n" +
      "      \"type\": \"chef\",\n" +
      "      \"fields\": {\n" +
      "        \"script\": \"install recipe\",\n" +
      "        \"data\": \"{\\\"foo\\\": { \\\"bar\\\": \\\"baz\\\" } }\"\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"flavor\": \"5\",\n" +
      "  \"hostname\": \"node1.loom.continuuity.net\",\n" +
      "  \"bootstrap_keypair\": \"iad-root\",\n" +
      "  \"image\": \"f70ed7c7-b42e-4d77-83d8-40fa29825b85\",\n" +
      "  \"automators\": [\"chef\", \"shell\"],\n" +
      "  \"provider\": {\n" +
      "    \"name\": \"openstack-central\",\n" +
      "    \"description\": \"Default provider\",\n" +
      "    \"providertype\": \"openstack\",\n" +
      "    \"provisioner\": {\n" +
      "      \"openstack_api_url\": \"http://1.2.3.4:5000/v2.0/tokens\",\n" +
      "      \"openstack_username\": \"user\"\n" +
      "    }\n" +
      "  }\n" +
      "}\n";

  private static final String USER_CONFIG =
    "{\n" +
      "  \"cluster\": {\n" +
      "    \"hadoop\": {\n" +
      "      \"core_site\": {\n" +
      "        \"fs.defaultFS\": \"hdfs://%host.service.hadoop-hdfs-namenode%\"\n" +
      "      },\n" +
      "      \"yarn_site\": {\n" +
      "        \"yarn.resourcemanager.hostname\": \"%host.service.hadoop-yarn-resourcemanager%\"\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"service\": {\n" +
      "    \"name\": \"datanode\",\n" +
      "    \"action\": {\n" +
      "      \"type\": \"chef\",\n" +
      "      \"fields\": {\n" +
      "        \"script\": \"install recipe\",\n" +
      "        \"data\": \"{\\\"foo\\\": { \\\"bar\\\": \\\"baz\\\" } }\"\n" +
      "      }\n" +
      "    }\n" +
      "  },\n" +
      "  \"flavor\": \"5\",\n" +
      "  \"hostname\": \"node1.loom.continuuity.net\",\n" +
      "  \"bootstrap_keypair\": \"iad-root\",\n" +
      "  \"image\": \"f70ed7c7-b42e-4d77-83d8-40fa29825b85\",\n" +
      "  \"automators\": [\"chef\", \"shell\"],\n" +
      "  \"provider\": {\n" +
      "    \"name\": \"openstack-central\",\n" +
      "    \"description\": \"Default provider\",\n" +
      "    \"providertype\": \"openstack\",\n" +
      "    \"provisioner\": {\n" +
      "      \"openstack_api_url\": \"http://1.2.3.4:5000/v2.0/tokens\",\n" +
      "      \"openstack_username\": \"user\"\n" +
      "    }\n" +
      "  }\n" +
      "}\n";
}
