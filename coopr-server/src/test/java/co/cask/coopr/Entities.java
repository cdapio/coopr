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
package co.cask.coopr;

import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.cluster.NodeProperties;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.FieldSchema;
import co.cask.coopr.spec.plugin.ParameterType;
import co.cask.coopr.spec.plugin.ParametersSpecification;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.plugin.ResourceTypeFormat;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.service.ServiceDependencies;
import co.cask.coopr.spec.service.ServiceStageDependencies;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.LayoutConstraint;
import co.cask.coopr.spec.template.LeaseDuration;
import co.cask.coopr.spec.template.ServiceConstraint;
import co.cask.coopr.spec.template.SizeConstraint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A bunch of example json strings for testing purposes.
 */
public class Entities {
  public static final String JOYENT = "joyent";
  public static final String RACKSPACE = "rackspace";
  public static final String OPENSTACK = "openstack";
  public static final Account USER_ACCOUNT = new Account("user1", "tenant1");
  public static final Account ADMIN_ACCOUNT = new Account(Constants.ADMIN_USER, "tenant1");

  public static class ProviderTypeExample {
    public static final ProviderType JOYENT =
      new ProviderType("joyent", null, "joyent provider type", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "joyent_username",
            FieldSchema.builder()
              .setLabel("user name")
              .setType("text")
              .setTip("your joyent username")
              .build(),
            "joyent_keyname",
            FieldSchema.builder()
              .setLabel("key name")
              .setType("text")
              .setTip("your joyent key name")
              .build(),
            "joyent_keyfile",
            FieldSchema.builder()
              .setLabel("path to key file")
              .setType("text")
              .setTip("path to your joyent key file")
              .build(),
            "joyent_version",
            FieldSchema.builder()
              .setLabel("version")
              .setType("text")
              .setTip("joyent version")
              .build()
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("joyent_username", "joyent_keyname", "joyent_keyfile", "joyent_version")
          )
        ),
        ParameterType.USER,
        ParametersSpecification.EMPTY_SPECIFICATION),
      ImmutableMap.<String, ResourceTypeSpecification>of(
        "keys", new ResourceTypeSpecification(ResourceTypeFormat.FILE, "0400")
      )
    );
    public static final ProviderType RACKSPACE =
      new ProviderType(
        "rackspace", null, "rackspace provider type", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "rackspace_username",
            FieldSchema.builder()
              .setLabel("user name")
              .setType("text")
              .setTip("your rackspace username")
              .setOverride(true)
              .build(),
            "rackspace_apikey",
            FieldSchema.builder()
              .setLabel("key name")
              .setType("text")
              .setTip("your rackspace key name")
              .setOverride(true)
              .build()
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("rackspace_username", "rackspace_apikey")
          )
        )),
        ImmutableMap.<String, ResourceTypeSpecification>of()
      );
    public static final ProviderType USER_RACKSPACE =
      new ProviderType("user-rackspace", null, "description", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.USER,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "rackspace_username",
            FieldSchema.builder()
              .setLabel("user name")
              .setType("text")
              .setTip("your rackspace username")
              .build(),
            "rackspace_apikey",
            FieldSchema.builder()
              .setLabel("key name")
              .setType("text")
              .setTip("your rackspace key name")
              .setSensitive(true)
              .build()
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("rackspace_username", "rackspace_apikey")
          )
        )),
        ImmutableMap.<String, ResourceTypeSpecification>of()
      );
  }

  public static class AutomatorTypeExample {
    public static final AutomatorType SHELL =
      new AutomatorType("shell", null, "shell automator", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "script",
            FieldSchema.builder()
              .setLabel("script")
              .setType("text")
              .setTip("path to script")
              .build(),
            "data",
            FieldSchema.builder()
              .setLabel("script arguments")
              .setType("text")
              .setTip("args")
              .setOptions(ImmutableSet.<String>of("opt1", "opt2"))
              .build()
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("script")
          )
        )),
        ImmutableMap.<String, ResourceTypeSpecification>of(
          "scripts", new ResourceTypeSpecification(ResourceTypeFormat.FILE, "755"),
          "data", new ResourceTypeSpecification(ResourceTypeFormat.ARCHIVE, null)
        )
      );
    public static final AutomatorType CHEF =
      new AutomatorType("chef-solo", null, "chef automator", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "recipe",
            FieldSchema.builder()
              .setLabel("chef recipe")
              .setType("text")
              .setTip("recipe name")
              .build(),
            "args",
            FieldSchema.builder()
              .setLabel("chef arguments")
              .setType("text")
              .setTip("args")
              .setOptions(ImmutableSet.<String>of("opt1", "opt2"))
              .build()
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("recipe")
          )
        )),
        ImmutableMap.<String, ResourceTypeSpecification>of(
          "cookbooks", new ResourceTypeSpecification(ResourceTypeFormat.ARCHIVE, null),
          "data_bags", new ResourceTypeSpecification(ResourceTypeFormat.ARCHIVE, null),
          "roles", new ResourceTypeSpecification(ResourceTypeFormat.FILE, "644")
        )
      );
    public static final AutomatorType PUPPET =
      new AutomatorType("puppet", null, "puppet automator", ImmutableMap.<ParameterType, ParametersSpecification>of(
        ParameterType.ADMIN,
        new ParametersSpecification(
          ImmutableMap.<String, FieldSchema>of(
            "manifest",
            FieldSchema.builder()
              .setLabel("puppet manifest")
              .setType("text")
              .setTip("manifest name")
              .build(),
            "args",
            FieldSchema.builder()
              .setLabel("puppet arguments")
              .setType("text")
              .setTip("args")
              .setOptions(ImmutableSet.<String>of("opt1", "opt2"))
              .build()
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("manifest")
          )
        )),
        ImmutableMap.<String, ResourceTypeSpecification>of(
          "modules", new ResourceTypeSpecification(ResourceTypeFormat.ARCHIVE, null),
          "manifests", new ResourceTypeSpecification(ResourceTypeFormat.FILE, "644")
        )
      );
  }

  public static class ProviderExample {
    public static final Provider JOYENT =
      new Provider("joyent", "Joyent Compute Service", Entities.JOYENT,
                   ImmutableMap.<String, Object>of(
                     "joyent_username", "EXAMPLE_USERNAME",
                     "joyent_keyname", "EXAMPLE_KEYNAME",
                     "joyent_keyfile", "/path/to/example.key",
                     "joyent_version", "~7.0"
                   ));
    public static final Provider RACKSPACE =
      new Provider("rackspace", "Rackspace Public Cloud", Entities.RACKSPACE,
                   ImmutableMap.<String, Object>of(
                     "rackspace_username", "EXAMPLE_USERNAME",
                     "rackspace_api_key", "EXAMPLE_API_KEY",
                     "some_boolean", true));
  }

  public static class HardwareTypeExample {
    public static final HardwareType SMALL =
      new HardwareType("small", "1 vCPU, 1 GB RAM, 30+ GB Disk",
                       ImmutableMap.<String, Map<String, String>>of(
                         "joyent", ImmutableMap.<String, String>of("flavor", "Small 1GB"),
                         "rackspace", ImmutableMap.<String, String>of("flavor", "3")
                       ));
    public static final HardwareType MEDIUM =
      new HardwareType("medium", "2+ vCPU, 4 GB RAM, 120+ GB Disk",
                       ImmutableMap.<String, Map<String, String>>of(
                         "joyent", ImmutableMap.<String, String>of("flavor", "Medium 4GB"),
                         "rackspace", ImmutableMap.<String, String>of("flavor", "5")
                       ));
    public static final HardwareType LARGE =
      new HardwareType("large", "4+ vCPU, 8 GB RAM, 240+ GB Disk",
                       ImmutableMap.<String, Map<String, String>>of(
                         "joyent", ImmutableMap.<String, String>of("flavor", "Large 8GB"),
                         "rackspace", ImmutableMap.<String, String>of("flavor", "6")
                       ));
  }

  public static class ImageTypeExample {
    public static final ImageType CENTOS_6 =
      new ImageType("centos6", "CentOS 6", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.<String, String>of("image", "325dbc5e-2b90-11e3-8a3e-bfdcb1582a8d"),
        "rackspace", ImmutableMap.<String, String>of("image", "f70ed7c7-b42e-4d77-83d8-40fa29825b85")
      ));
    public static final ImageType UBUNTU_12 =
      new ImageType("ubuntu12", "Ubuntu 12.04 LTS", ImmutableMap.<String, Map<String, String>>of(
        "joyent", ImmutableMap.<String, String>of("image", "d2ba0f30-bbe8-11e2-a9a2-6bc116856d85"),
        "rackspace", ImmutableMap.<String, String>of("image", "d45ed9c5-d6fc-4c9d-89ea-1b3ae1c83999")
      ));
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
                    new ServiceAction("chef-solo", TestHelper.actionMapOf("recipe[coopr_hosts::default]", null))
                  ));
    public static final Service NAMENODE =
      new Service("namenode", "Hadoop HDFS NameNode",
                  ImmutableSet.<String>of("hosts"),
                  ImmutableMap.<ProvisionerAction, ServiceAction>of(
                    ProvisionerAction.INSTALL,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[hadoop::hadoop_hdfs_namenode]", null)),
                    ProvisionerAction.INITIALIZE,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[hadoop_wrapper::hadoop_hdfs_namenode_init]", null)),
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[hadoop::default]", null)),
                    ProvisionerAction.START,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[coopr_service_runner::default]", "start data")),
                    ProvisionerAction.STOP,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[coopr_service_runner::default]", "stop data"))
                  ));
    public static final Service DATANODE =
      new Service("datanode", "Hadoop HDFS DataNode",
                  ImmutableSet.<String>of("hosts", "namenode"),
                  ImmutableMap.<ProvisionerAction, ServiceAction>of(
                    ProvisionerAction.INSTALL,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[hadoop::hadoop_hdfs_datanode]", null)),
                    ProvisionerAction.INITIALIZE,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[hadoop_wrapper::hadoop_hdfs_datanode_init]", null)),
                    ProvisionerAction.CONFIGURE,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[hadoop::default]", null)),
                    ProvisionerAction.START,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[coopr_service_runner::default]", "start data")),
                    ProvisionerAction.STOP,
                    new ServiceAction("chef-solo",
                                      TestHelper.actionMapOf("recipe[coopr_service_runner::default]", "stop data"))
                  ));
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

    public static final ClusterTemplate HDFS = ClusterTemplate.builder()
      .setName("hdfs")
      .setDescription("Hdfs cluster")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices(
            ServiceExample.HOSTS.getName(),
            ServiceExample.NAMENODE.getName(),
            ServiceExample.DATANODE.getName())
          .setProvider("joyent").build())
      .setCompatibilities(Compatibilities.builder().setServices(
        ServiceExample.HOSTS.getName(),
        ServiceExample.NAMENODE.getName(),
        ServiceExample.DATANODE.getName()).build())
      .setConstraints(new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          ServiceExample.NAMENODE.getName(),
          new ServiceConstraint(
            ImmutableSet.<String>of("large"),
            ImmutableSet.<String>of("centos6", "ubuntu12"),
            1, 1)
        ),
        new LayoutConstraint(
          null,
          ImmutableSet.<Set<String>>of(
            ImmutableSet.<String>of("namenode", "datanode")
          )
        ),
        SizeConstraint.EMPTY)).build();
    public static final ClusterTemplate REACTOR = ClusterTemplate.builder()
      .setName("hadoop-reactor")
      .setDescription("Hadoop cluster with reactor")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("hosts", "namenode", "datanode", "resourcemanager", "nodemanager",
                       "hbasemaster", "regionserver", "zookeeper", "reactor")
          .setProvider("joyent").build())
      .setCompatibilities(
        Compatibilities.builder().setServices(
          "hosts", "namenode", "datanode", "resourcemanager", "nodemanager",
          "hbasemaster", "regionserver", "zookeeper", "reactor").build())
      .setConstraints(new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          "namenode",
          new ServiceConstraint(
            ImmutableSet.<String>of("large"),
            ImmutableSet.<String>of("centos6", "ubuntu12"),
            1, 1)
        ),
        new LayoutConstraint(
          null,
          ImmutableSet.<Set<String>>of(
            ImmutableSet.<String>of("namenode", "datanode")
          )
        ),
        SizeConstraint.EMPTY))
      .setAdministration(new Administration(new LeaseDuration(10000, 900000, 1000))).build();
    public static final ClusterTemplate HADOOP_DISTRIBUTED = ClusterTemplate.builder()
      .setName("hadoop-distributed")
      .setDescription("Hadoop cluster without high-availability")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("firewall", "hosts", "datanode", "namenode", "nodemanager", "resourcemanager")
          .setProvider("rackspace")
          .setHardwaretype("medium")
          .setImagetype("ubuntu12")
          .build())
      .setCompatibilities(
        Compatibilities.builder()
          .setHardwaretypes("small", "medium", "large")
          .setImagetypes("centos6", "ubuntu12")
          .setServices("firewall", "hosts", "namenode", "secondarynamenode", "datanode",
                       "resourcemanager", "nodemanager", "zookeeper", "hbasemaster",
                       "regionserver", "hive-metastore", "mysql-server", "reactor").build())
      .setConstraints(new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          "namenode", new ServiceConstraint(null, null, 1, 1),
          "resourcemanager", new ServiceConstraint(null, null, 1, 1),
          "zookeeper", new ServiceConstraint(null, null, 1, 1),
          "hbasemaster", new ServiceConstraint(null, null, null, 1),
          "reactor", new ServiceConstraint(null, null, null, 1)
        ),
        new LayoutConstraint(
          ImmutableSet.<Set<String>>of(
            ImmutableSet.<String>of("datanode", "nodemanager", "regionserver")
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.<String>of("namenode", "secondarynamenode"),
            ImmutableSet.<String>of("resourcemanager", "nodemanager"),
            ImmutableSet.<String>of("namenode", "datanode"),
            ImmutableSet.<String>of("datanode", "mysql-server"),
            ImmutableSet.<String>of("datanode", "reactor"),
            ImmutableSet.<String>of("hbasemaster", "regionserver")
          )
        ),
        SizeConstraint.EMPTY))
      .setAdministration(new Administration(new LeaseDuration(0, 0, 0))).build();
    public static final ClusterTemplate REACTOR2 = ClusterTemplate.builder()
      .setName("reactor")
      .setDescription("Hadoop cluster without high-availability and with Continuuity Reactor")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices("firewall", "hosts", "hadoop-hdfs-datanode", "hadoop-hdfs-namenode",
                       "hadoop-yarn-nodemanager", "hadoop-yarn-resourcemanager",
                       "zookeeper-server", "hbase-master", "hbase-regionserver", "reactor")
          .setProvider("rackspace")
          .setHardwaretype("medium")
          .setImagetype("ubuntu12").build())
      .setCompatibilities(
        Compatibilities.builder()
          .setHardwaretypes("small", "medium", "large")
          .setImagetypes("centos6", "ubuntu12")
          .setServices("firewall", "hosts", "hadoop-hdfs-datanode", "hadoop-hdfs-namenode",
                       "hadoop-yarn-nodemanager", "hadoop-yarn-resourcemanager",
                       "zookeeper-server", "hbase-master", "hbase-regionserver",
                       "hive-metastore", "mysql-server", "reactor").build())
      .setConstraints(new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          "hadoop-hdfs-namenode", new ServiceConstraint(null, null, 1, 1),
          "hadoop-yarn-resourcemanager", new ServiceConstraint(null, null, 1, 1),
          "zookeeper-server", new ServiceConstraint(null, null, 1, 1),
          "hbase-master", new ServiceConstraint(null, null, null, 1),
          "reactor", new ServiceConstraint(null, null, null, 1)
        ),
        new LayoutConstraint(
          ImmutableSet.<Set<String>>of(
            ImmutableSet.<String>of("hadoop-hdfs-datanode", "hadoop-yarn-nodemanager", "hbase-regionserver")
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.<String>of("hadoop-hdfs-namenode", "hadoop-hdfs-secondarynamenode"),
            ImmutableSet.<String>of("hadoop-yarn-resourcemanager", "hadoop-yarn-nodemanager"),
            ImmutableSet.<String>of("hadoop-hdfs-namenode", "hadoop-hdfs-datanode"),
            ImmutableSet.<String>of("hadoop-hdfs-datanode", "mysql-server"),
            ImmutableSet.<String>of("hbase-master", "hbase-regionserver")
          )
        ),
        SizeConstraint.EMPTY)).build();
  }

  public static class ClusterExample {
    private static String node1 = "node1";
    private static String node2 = "node2";
    private static String clusterId = "2";
    public static Cluster createCluster() {
      return Cluster.builder()
        .setID(clusterId)
        .setAccount(USER_ACCOUNT)
        .setName("name")
        .setDescription("description")
        .setCreateTime(1234567890)
        .setProvider(ProviderExample.RACKSPACE)
        .setClusterTemplate(ClusterTemplateExample.HDFS)
        .setNodes(ImmutableSet.of(node1, node2))
        .setServices(ImmutableSet.of(
          ServiceExample.NAMENODE.getName(),
          ServiceExample.DATANODE.getName(),
          ServiceExample.HOSTS.getName()
        ))
        .build();
    }
    public static Node NODE1 =
      new Node(node1,
               clusterId,
               ImmutableSet.of(ServiceExample.NAMENODE, ServiceExample.HOSTS),
               NodeProperties.builder()
                 .setHardwaretype(HardwareTypeExample.LARGE.getName())
                 .setImagetype(ImageTypeExample.CENTOS_6.getName()).build());
    public static Node NODE2 =
      new Node(node2,
               clusterId,
               ImmutableSet.of(ServiceExample.DATANODE, ServiceExample.HOSTS),
               NodeProperties.builder()
                 .setHardwaretype(HardwareTypeExample.LARGE.getName())
                 .setImagetype(ImageTypeExample.CENTOS_6.getName()).build());
  }

  public static class NodeExample {
    private static String node1 = "node1";
    private static String node2 = "node2";
    private static String clusterId = "2";
    private static final String baseMockHostName = ".test.chi.intsm.net";
    public static Node NODE1 = createNode(node1, clusterId);
    public static Node NODE1_UPDATED = createNode(
      node1,
      clusterId,
      ImmutableSet.of(ServiceExample.DATANODE, ServiceExample.HOSTS),
      NodeProperties.builder()
        .setImagetype(ImageTypeExample.UBUNTU_12.getName())
        .setHardwaretype(HardwareTypeExample.MEDIUM.getName()).build());
    public static Set<Node> NODES = createMockNodes(2);

    private static Set<Node> createMockNodes(int numberOfNodes) {
      Set<Node> mockNodes = new HashSet<Node>();
      for (int i = 0; i < numberOfNodes; i++) {
        Node mockNode = createNode(i + baseMockHostName, Integer.toString(i));
        mockNodes.add(mockNode);
      }

      return mockNodes;
    }

    public static Node createNode(String id, String clusterId) {
      return createNode(id,
                        clusterId,
                        ImmutableSet.of(ServiceExample.DATANODE, ServiceExample.HOSTS),
                        NodeProperties.builder()
                          .setHardwaretype(HardwareTypeExample.LARGE.getName())
                          .setImagetype(ImageTypeExample.CENTOS_6.getName()).build());
    }

    public static Node createNode(String id, String clusterId, Set<Service> services, NodeProperties properties) {
      return new Node(id, clusterId, services, properties);
    }
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
