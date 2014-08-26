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
import com.continuuity.loom.spec.template.Administration;
import com.continuuity.loom.spec.template.ClusterDefaults;
import com.continuuity.loom.spec.template.ClusterTemplate;
import com.continuuity.loom.spec.template.Compatibilities;
import com.continuuity.loom.spec.template.Constraints;
import com.continuuity.loom.spec.ProvisionerAction;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.spec.service.ServiceAction;
import com.continuuity.loom.spec.service.ServiceDependencies;
import com.continuuity.loom.spec.service.ServiceStageDependencies;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.http.request.ClusterCreateRequest;
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
    solver.validateServicesToAdd(getExampleCluster(), ImmutableSet.of(nodemanager.getName()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidServiceThrowsException() throws Exception {
    solver.validateServicesToAdd(getExampleCluster(), ImmutableSet.of("fakeservice"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIncompatibleServiceThrowsException() throws Exception {
    solver.validateServicesToAdd(getExampleCluster(), ImmutableSet.of(mysql.getName()));
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
    entityStoreService.getView(account).writeService(myapp1);
    entityStoreService.getView(account).writeService(myapp2);
    Cluster cluster = getBaseBuilder()
      .setClusterTemplate(template)
      .setServices(ImmutableSet.of(myapp1.getName()))
      .build();
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
    entityStoreService.getView(account).writeService(service1);
    entityStoreService.getView(account).writeService(service2);
    entityStoreService.getView(account).writeService(service3);
    Cluster cluster = getBaseBuilder()
      .setClusterTemplate(template)
      .setServices(ImmutableSet.of(service1.getName()))
      .build();
    solver.validateServicesToAdd(cluster, ImmutableSet.of(service2.getName(), service3.getName()));
  }

  @Test
  public void testEndToEnd() throws Exception {
    ClusterCreateRequest request = ClusterCreateRequest.builder()
      .setName("mycluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .setInitialLeaseDuration(0L)
      .build();
    Map<String, Node> nodes = solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(reactorTemplate).setProvider(provider).build(),
      request);

    Multiset<Set<String>> serviceSetCounts = HashMultiset.create();
    for (Node node : nodes.values()) {
      Set<String> serviceNames = Sets.newHashSet(
        Iterables.transform(node.getServices(), new Function<Service, String>() {
          @Override
          public String apply(Service input) {
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
  public void testDisallowedServicesThrowsException() throws Exception {
    ClusterCreateRequest request = ClusterCreateRequest.builder()
      .setName("mycluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .setProviderName("joyent")
      .setServiceNames(ImmutableSet.of("namenode", "datanode", "mysql", "httpd"))
      .setInitialLeaseDuration(-1L)
      .build();
    solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(reactorTemplate).setProvider(Entities.ProviderExample.JOYENT).build(),
      request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingServiceDependenciesThrowsException() throws Exception {
    ClusterCreateRequest request = ClusterCreateRequest.builder()
      .setName("mycluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .setProviderName("joyent")
      .setServiceNames(ImmutableSet.of("reactor", "datanode"))
      .setInitialLeaseDuration(-1L)
      .build();
    solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(reactorTemplate).setProvider(Entities.ProviderExample.JOYENT).build(),
      request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoAvailableHardwareTypesThrowsException() throws Exception {
    ClusterCreateRequest request = ClusterCreateRequest.builder()
      .setName("mycluster")
      .setClusterTemplateName(reactorTemplate.getName())
      .setNumMachines(5)
      .setProviderName("rackspace")
      .setServiceNames(ImmutableSet.of("namenode", "datanode"))
      .setInitialLeaseDuration(-1L)
      .build();
    solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(reactorTemplate).setProvider(Entities.ProviderExample.RACKSPACE).build(),
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
    entityStoreService.getView(account).writeClusterTemplate(simpleTemplate);

    // check required hardware types
    ClusterCreateRequest request = ClusterCreateRequest.builder()
      .setName("abc")
      .setClusterTemplateName("simple")
      .setNumMachines(5)
      .setProviderName("joyent")
      .setHardwareTypeName("medium")
      .setInitialLeaseDuration(0L)
      .build();
    Map<String, Node> nodes = solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(simpleTemplate).setProvider(Entities.ProviderExample.JOYENT).build(),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("medium", node.getProperties().getHardwaretype());
    }

    request = ClusterCreateRequest.builder()
      .setName("abc")
      .setClusterTemplateName("simple")
      .setNumMachines(5)
      .setProviderName("joyent")
      .setHardwareTypeName("large-mem")
      .setInitialLeaseDuration(0L)
      .build();
    nodes = solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(simpleTemplate).setProvider(Entities.ProviderExample.JOYENT).build(),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("large-mem", node.getProperties().getHardwaretype());
    }

    // check required image types
    request = ClusterCreateRequest.builder()
      .setName("abc")
      .setClusterTemplateName("simple")
      .setNumMachines(5)
      .setProviderName("joyent")
      .setImageTypeName("ubuntu12")
      .setInitialLeaseDuration(0L)
      .build();
    nodes = solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(simpleTemplate).setProvider(Entities.ProviderExample.JOYENT).build(),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("joyent-hash-of-ubuntu12", node.getProperties().getImage());
    }

    // test both
    request = ClusterCreateRequest.builder()
      .setName("abc")
      .setClusterTemplateName("simple")
      .setNumMachines(5)
      .setProviderName("joyent")
      .setHardwareTypeName("small")
      .setImageTypeName("centos6")
      .setInitialLeaseDuration(0L)
      .build();
    nodes = solver.solveClusterNodes(
      getBaseBuilder().setClusterTemplate(simpleTemplate).setProvider(Entities.ProviderExample.JOYENT).build(),
      request);
    Assert.assertEquals(5, nodes.size());
    for (Node node : nodes.values()) {
      Assert.assertEquals("small", node.getProperties().getHardwaretype());
      Assert.assertEquals("joyent-hash-of-centos6.4", node.getProperties().getImage());
    }

    entityStoreService.getView(account).deleteClusterTemplate(simpleTemplate.getName());
  }

  @Test
  public void testServiceConstraintsDontApplyWhenServiceNotOnCluster() throws Exception {
    ClusterTemplate template = Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED;
    Map<String, String> hwTypeMap = ImmutableMap.of("medium", "medium-flavor");
    Map<String, Map<String, String>> imgTypeMap =
      ImmutableMap.<String, Map<String, String>>of("ubuntu12", ImmutableMap.of("image", "ubunut12-image"));
    Set<String> services =
      ImmutableSet.of("firewall", "hosts", "namenode", "datanode", "nodemanager", "resourcemanager");
    Map<String, Service> serviceMap = Maps.newHashMap();
    for (String service : services) {
      serviceMap.put(service, new Service(service, "", Collections.<String>emptySet(),
                                          Collections.<ProvisionerAction, ServiceAction>emptyMap()));
    }

    Map<String, Node> nodes = Solver.solveConstraints("1", template, "name", 3,
                                                      hwTypeMap, imgTypeMap, services, serviceMap, null);
    Multiset<Set<String>> serviceSetCounts = HashMultiset.create();
    for (Map.Entry<String, Node> entry : nodes.entrySet()) {
      Node node = entry.getValue();
      Set<String> serviceNames = Sets.newHashSet(
        Iterables.transform(node.getServices(), new Function<Service, String>() {
          @Override
          public String apply(Service input) {
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
    Map<String, Map<String, String>> imgmap = ImmutableMap.<String, Map<String, String>>of(
      "centos6", ImmutableMap.<String, String>of("image", "img1"),
      "ubuntu12", ImmutableMap.<String, String>of("image", "img2")
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
          @Override
          public String apply(Service input) {
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

  private Cluster getExampleCluster() {
    return getBaseBuilder()
      .setClusterTemplate(reactorTemplate)
      .setServices(ImmutableSet.of(namenode.getName(), datanode.getName()))
      .build();
  }

  private Cluster.Builder getBaseBuilder() {
    return Cluster.builder()
      .setID("123")
      .setAccount(account)
      .setName("cluster")
      .setProvider(Entities.ProviderExample.RACKSPACE);
  }

  @BeforeClass
  public static void setup() throws Exception {
    solver = injector.getInstance(Solver.class);
  }
}
