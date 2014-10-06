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
package co.cask.coopr.layout;

import co.cask.coopr.Entities;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceDependencies;
import co.cask.coopr.spec.service.ServiceStageDependencies;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import com.google.common.base.Function;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
    ClusterTemplate template = ClusterTemplate.builder()
      .setName("name")
      .setClusterDefaults(ClusterDefaults.builder()
                            .setServices(services)
                            .setProvider("joyent")
                            .setConfig(Entities.ClusterTemplateExample.clusterConf).build())
      .setCompatibilities(Compatibilities.builder().setServices("myapp-1", "myapp-2").build())
      .build();
    Service myapp1 = Service.builder()
      .setName("myapp-1")
      .setDependencies(ServiceDependencies.builder().setProvides("myapp").setConflicts("myapp-2").build())
      .build();
    Service myapp2 = Service.builder()
      .setName("myapp-2")
      .setDependencies(ServiceDependencies.builder().setProvides("myapp").setConflicts("myapp-1").build())
      .build();
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
    ClusterTemplate template = ClusterTemplate.builder()
      .setName("name")
      .setClusterDefaults(ClusterDefaults.builder()
                            .setServices(services)
                            .setProvider("joyent")
                            .setConfig(Entities.ClusterTemplateExample.clusterConf).build())
      .setCompatibilities(Compatibilities.builder().setServices("service1", "service2", "service3").build())
      .build();
    Service service1 = Service.builder().setName("service1").build();
    // service2 uses service 1 at install time
    Service service2 = Service.builder()
      .setName("service2")
      .setDependencies(
        ServiceDependencies.builder()
          .setInstallDependencies(new ServiceStageDependencies(null, ImmutableSet.<String>of("service1")))
          .build())
      .build();
    // service 3 uses service 1 at runtime
    Service service3 = Service.builder()
      .setName("service3")
      .setDependencies(
        ServiceDependencies.builder()
          .setRuntimeDependencies(new ServiceStageDependencies(null, ImmutableSet.<String>of("service1")))
          .build())
      .build();
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
    ClusterTemplate simpleTemplate = ClusterTemplate.builder()
      .setName("simple")
      .setClusterDefaults(ClusterDefaults.builder()
                            .setServices(ImmutableSet.of("namenode", "datanode"))
                            .setProvider("joyent")
                            .setHardwaretype("medium")
                            .setImagetype("ubuntu12")
                            .build())
      .setCompatibilities(Compatibilities.builder()
                            .setHardwaretypes("small", "medium", "large-mem")
                            .setImagetypes("centos6", "ubuntu12")
                            .setServices("namenode", "datanode").build())
      .build();
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
      serviceMap.put(service, Service.builder().setName(service).build());
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
      serviceMap.put(serviceName, Service.builder().setName(serviceName).build());
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
