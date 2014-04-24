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
package com.continuuity.loom.layout;

import com.continuuity.loom.Entities;
import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.JsonSerde;
import com.google.common.base.Function;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class SolverTest extends BaseSolverTest {
  private static Solver solver;


  @Test(expected = IllegalArgumentException.class)
  public void testDependencyMissingThrowsException() throws Exception {
    Cluster cluster = new Cluster("123", "user1", "hadoop", System.currentTimeMillis(), "hadoop cluster",
                                  Entities.ProviderExample.RACKSPACE, reactorTemplate, ImmutableSet.<String>of(),
                                  ImmutableSet.of(namenode.getName(), datanode.getName()));
    solver.validateServicesToAdd(cluster, ImmutableSet.of(nodemanager.getName()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidServiceThrowsException() throws Exception {
    Cluster cluster = new Cluster("123", "user1", "hadoop", System.currentTimeMillis(), "hadoop cluster",
                                  Entities.ProviderExample.RACKSPACE, reactorTemplate, ImmutableSet.<String>of(),
                                  ImmutableSet.of(namenode.getName(), datanode.getName()));
    solver.validateServicesToAdd(cluster, ImmutableSet.of("fakeservice"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIncompatibleServiceThrowsException() throws Exception {
    Cluster cluster = new Cluster("123", "user1", "hadoop", System.currentTimeMillis(), "hadoop cluster",
                                  Entities.ProviderExample.RACKSPACE, reactorTemplate, ImmutableSet.<String>of(),
                                  ImmutableSet.of(namenode.getName(), datanode.getName()));
    solver.validateServicesToAdd(cluster, ImmutableSet.of(mysql.getName()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConflictingServicesThrowsException() throws Exception {
    Set<String> services = ImmutableSet.of("myapp-1");
    ClusterTemplate template = new ClusterTemplate(
      "name", "description",
      new ClusterDefaults(services, "joyent", null, null, null, Entities.ClusterTemplateExample.clusterConf),
      new Compatibilities(null, null, ImmutableSet.<String>of("myapp-1", "myapp-2")),
      Constraints.EMPTY_CONSTRAINTS,
      Administration.EMPTY_ADMINISTRATION
    );
    Service myapp1 = new Service("myapp-1",
                                 "my app v1",
                                 new ServiceDependencies(
                                   ImmutableSet.<String>of("myapp"),
                                   ImmutableSet.<String>of("myapp-2"),
                                   null, null),
                                 ImmutableMap.<ProvisionerAction, ServiceAction>of());
    Service myapp2 = new Service("myapp-2",
                                 "my app v2",
                                 new ServiceDependencies(
                                   ImmutableSet.<String>of("myapp"),
                                   ImmutableSet.<String>of("myapp-1"),
                                   null, null),
                                 ImmutableMap.<ProvisionerAction, ServiceAction>of());
    entityStore.writeService(myapp1);
    entityStore.writeService(myapp2);
    Cluster cluster = new Cluster("123", "user1", "cluster", System.currentTimeMillis(), "test cluster",
                                  Entities.ProviderExample.RACKSPACE, template, ImmutableSet.<String>of(),
                                  ImmutableSet.of(myapp1.getName()));
    solver.validateServicesToAdd(cluster, ImmutableSet.of(myapp2.getName()));
  }

  @Test
  public void testUsesDoesNotForceDependencyOnCluster() throws Exception {
    Set<String> services = ImmutableSet.of("service1");
    ClusterTemplate template = new ClusterTemplate(
      "name", "description",
      new ClusterDefaults(services, "joyent", null, null, null, Entities.ClusterTemplateExample.clusterConf),
      new Compatibilities(null, null, ImmutableSet.<String>of("service1", "service2", "service3")),
      Constraints.EMPTY_CONSTRAINTS,
      Administration.EMPTY_ADMINISTRATION
    );
    Service service1 = new Service("service1",
                                   "my service1",
                                   ServiceDependencies.EMPTY_SERVICE_DEPENDENCIES,
                                   ImmutableMap.<ProvisionerAction, ServiceAction>of());
    // service2 uses service 1 at install time
    Service service2 = new Service("service2",
                                   "my service2",
                                   new ServiceDependencies(
                                     null,
                                     null,
                                     new ServiceStageDependencies(
                                       ImmutableSet.<String>of(),
                                       ImmutableSet.<String>of("service1")
                                     ),
                                     null
                                   ),
                                   ImmutableMap.<ProvisionerAction, ServiceAction>of());
    // service 3 uses service 1 at runtime
    Service service3 = new Service("service3",
                                   "my service3",
                                   new ServiceDependencies(
                                     null,
                                     null,
                                     null,
                                     new ServiceStageDependencies(
                                       ImmutableSet.<String>of(),
                                       ImmutableSet.<String>of("service1")
                                     )
                                   ),
                                   ImmutableMap.<ProvisionerAction, ServiceAction>of());
    entityStore.writeService(service1);
    entityStore.writeService(service2);
    entityStore.writeService(service3);
    Cluster cluster = new Cluster("123", "user1", "cluster", System.currentTimeMillis(), "test cluster",
                                  Entities.ProviderExample.RACKSPACE, template, ImmutableSet.<String>of(),
                                  ImmutableSet.of(service1.getName()));
    solver.validateServicesToAdd(cluster, ImmutableSet.of(service2.getName(), service3.getName()));
  }


  @Test
  public void testEndToEnd() throws Exception {
    ClusterCreateRequest request =
      new ClusterCreateRequest("mycluster", "my reactor cluster", reactorTemplate.getName(),
                         5, null, null, null, null, 0L, null, null);
    Map<String, Node> nodes = solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);

    Multiset<Set<String>> serviceSetCounts = HashMultiset.create();
    for (Node node : nodes.values()) {
      Set<String> serviceNames = Sets.newHashSet(
        Iterables.transform(node.getServices(), new Function<Service, String>() {
          @Nullable
          @Override
          public String apply(@Nullable Service input) {
            return input.getName();
          }
        })
      );
      serviceSetCounts.add(serviceNames);
    }
    Assert.assertEquals(1, serviceSetCounts.count(ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")));
    Assert.assertEquals(3, serviceSetCounts.count(ImmutableSet.of("datanode", "nodemanager", "regionserver")));
    Assert.assertEquals(1, serviceSetCounts.count(ImmutableSet.of("reactor", "zookeeper")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnknownTemplateThrowsException() throws Exception {
    ClusterCreateRequest request =
      new ClusterCreateRequest("mycluster", "my cluster", "bad template name", 5, null, null, null, null, -1L, null, null);
    solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDisallowedServicesThrowsException() throws Exception {
    ClusterCreateRequest request =
      new ClusterCreateRequest("mycluster", "my cluster", reactorTemplate.getName(), 5, "joyent",
                         ImmutableSet.of("namenode", "datanode", "mysql", "httpd"), null, null, -1L, null, null);
    solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingServiceDependenciesThrowsException() throws Exception {
    ClusterCreateRequest request =
      new ClusterCreateRequest("mycluster", "my cluster", reactorTemplate.getName(), 5, "joyent",
                         ImmutableSet.of("reactor", "datanode"), null, null, -1L, null, null);
    solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoAvailableHardwareTypesThrowsException() throws Exception {
    ClusterCreateRequest request =
      new ClusterCreateRequest("mycluster", "my cluster", reactorTemplate.getName(), 5, "rackspace",
                         ImmutableSet.of("namenode", "datanode"), null, null, -1L, null, null);
    solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
  }

  @Test
  public void testRequiredTypes() throws Exception {
    ClusterTemplate simpleTemplate =
      new ClusterTemplate(
        "simple", "description",
        new ClusterDefaults(
          ImmutableSet.of("namenode", "datanode"),
          "joyent",
          "medium",
          "ubuntu12",
          null,
          new JsonObject()
        ),
        new Compatibilities(
          ImmutableSet.<String>of("small", "medium", "large-mem"),
          ImmutableSet.<String>of("centos6", "ubuntu12"),
          ImmutableSet.<String>of("namenode", "datanode")
        ),
        Constraints.EMPTY_CONSTRAINTS, null
      );
    entityStore.writeClusterTemplate(simpleTemplate);

    // check required hardware types
    ClusterCreateRequest request =
      new ClusterCreateRequest("abc", "desc", "simple", 5, "joyent", null, "medium", null, 0L, null, null);
    Map<String, Node> nodes = solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("medium", node.getProperties().get("hardwaretype").getAsString());
    }

    request = new ClusterCreateRequest("abc", "desc", "simple", 5, "joyent", null, "large-mem", null, 0L, null, null);
    nodes = solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("large-mem", node.getProperties().get("hardwaretype").getAsString());
    }

    // check required image types
    request = new ClusterCreateRequest("abc", "desc", "simple", 5, "joyent", null, null, "ubuntu12", 0L, null, null);
    nodes = solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("joyent-hash-of-ubuntu12", node.getProperties().get("image").getAsString());
    }

    // test both
    request = new ClusterCreateRequest("abc", "desc", "simple", 5, "joyent", null, "small", "centos6", 0L, null, null);
    nodes = solver.solveClusterNodes(
      new Cluster("1", "owner1", request.getName(), System.currentTimeMillis(), request.getDescription(),
                  null, null, ImmutableSet.<String>of(), ImmutableSet.<String>of()),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("small", node.getProperties().get("hardwaretype").getAsString());
      Assert.assertEquals("joyent-hash-of-centos6.4", node.getProperties().get("image").getAsString());
    }

    entityStore.deleteClusterTemplate(simpleTemplate.getName());
  }

  @Test
  public void testServiceConstraintsDontApplyWhenServiceNotOnCluster() throws Exception {
    ClusterTemplate template = new JsonSerde().getGson().fromJson(
      Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED_STRING, ClusterTemplate.class);
    Map<String, String> hwTypeMap = ImmutableMap.of("medium", "medium-flavor");
    Map<String, String> imgTypeMap = ImmutableMap.of("ubuntu12", "ubunut12-image");
    Set<String> services =
      ImmutableSet.of("firewall", "hosts", "namenode", "datanode", "nodemanager", "resourcemanager");
    Map<String, Service> serviceMap = Maps.newHashMap();
    for (String service : services) {
      serviceMap.put(service, new Service(service, "", Collections.EMPTY_SET, Collections.EMPTY_MAP));
    }

    Map<String, Node> nodes = Solver.solveConstraints("1", template, "name", 3,
                                                      hwTypeMap, imgTypeMap, services, serviceMap, null);
    Multiset<Set<String>> serviceSetCounts = HashMultiset.create();
    for (Map.Entry<String, Node> entry : nodes.entrySet()) {
      Node node = entry.getValue();
      Set<String> serviceNames = Sets.newHashSet(
        Iterables.transform(node.getServices(), new Function<Service, String>() {
          @Nullable
          @Override
          public String apply(@Nullable Service input) {
            return input.getName();
          }
        })
      );
      serviceSetCounts.add(serviceNames);
    }
    Assert.assertEquals(1, serviceSetCounts.count(ImmutableSet.of("hosts", "firewall", "namenode", "resourcemanager")));
    Assert.assertEquals(2, serviceSetCounts.count(ImmutableSet.of("hosts", "firewall", "datanode", "nodemanager")));
    Assert.assertEquals(3, nodes.size());
  }

  @Test
  public void testSolveReactor() throws Exception {
    Map<String, String> hwmap = ImmutableMap.of(
      "small", "flavor1",
      "medium", "flavor2",
      "large", "flavor3"
    );
    Map<String, String> imgmap = ImmutableMap.of(
      "centos6", "img1",
      "ubuntu12", "img2"
    );
    Set<String> services = reactorTemplate2.getClusterDefaults().getServices();
    Map<String, Service> serviceMap = Maps.newHashMap();
    for (String serviceName : services) {
      serviceMap.put(serviceName, new Service(serviceName, "", ImmutableSet.<String>of(),
                                              ImmutableMap.<ProvisionerAction, ServiceAction>of()));
    }
    Map<String, Node> nodes = Solver.solveConstraints("1", reactorTemplate2, "name", 200,
                                                      hwmap, imgmap, services, serviceMap, null);
    Multiset<Set<String>> serviceSetCounts = HashMultiset.create();
    for (Map.Entry<String, Node> entry : nodes.entrySet()) {
      Node node = entry.getValue();
      Set<String> serviceNames = Sets.newHashSet(
        Iterables.transform(node.getServices(), new Function<Service, String>() {
          @Nullable
          @Override
          public String apply(@Nullable Service input) {
            return input.getName();
          }
        })
      );
      serviceSetCounts.add(serviceNames);
    }
    Assert.assertEquals(200, nodes.size());
    Assert.assertEquals(198, serviceSetCounts.count(
      ImmutableSet.of("hosts", "firewall", "hadoop-hdfs-datanode", "hadoop-yarn-nodemanager", "hbase-regionserver")));
    Assert.assertEquals(1, serviceSetCounts.count(
      ImmutableSet.of("hosts", "firewall", "hadoop-hdfs-datanode", "hadoop-yarn-nodemanager", "hbase-regionserver",
                      "zookeeper-server", "reactor")));
    Assert.assertEquals(1, serviceSetCounts.count(
      ImmutableSet.of("hosts", "firewall", "hbase-master", "hadoop-hdfs-namenode",
                      "hadoop-yarn-resourcemanager")));
  }

  @BeforeClass
  public static void setup() throws Exception {
    solver = injector.getInstance(Solver.class);
  }
}
