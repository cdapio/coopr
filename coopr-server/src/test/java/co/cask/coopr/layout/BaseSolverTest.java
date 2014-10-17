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

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceDependencies;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.LayoutConstraint;
import co.cask.coopr.spec.template.ServiceConstraint;
import co.cask.coopr.spec.template.SizeConstraint;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.BeforeClass;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class BaseSolverTest extends BaseTest {
  protected static Provider provider;
  protected static ClusterTemplate reactorTemplate;
  protected static ClusterTemplate reactorTemplate2;
  protected static Service namenode;
  protected static Service resourcemanager;
  protected static Service datanode;
  protected static Service nodemanager;
  protected static Service hbasemaster;
  protected static Service regionserver;
  protected static Service zookeeper;
  protected static Service reactor;
  protected static Service mysql;
  protected static Map<String, Service> serviceMap;
  protected static Account account = new Account(Constants.ADMIN_USER, "tenant1");

  @BeforeClass
  public static void setupBaseSolverTests() throws Exception {

    Set<String> services = ImmutableSet.of("namenode", "datanode", "resourcemanager", "nodemanager",
                                           "hbasemaster", "regionserver", "zookeeper", "reactor");
    reactorTemplate = ClusterTemplate.builder()
      .setName("reactor-medium")
      .setDescription("medium reactor cluster template")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setServices(services)
          .setProvider("joyent")
          .setConfig(Entities.ClusterTemplateExample.clusterConf).build())
      .setCompatibilities(Compatibilities.builder().setServices(services).build())
      .setConstraints(new Constraints(
        ImmutableMap.<String, ServiceConstraint>of(
          "namenode",
          new ServiceConstraint(
            ImmutableSet.of("large-mem"),
            ImmutableSet.of("centos6", "ubuntu12"), 1, 1),
          "datanode",
          new ServiceConstraint(
            ImmutableSet.of("medium", "large-cpu"),
            ImmutableSet.of("centos6", "ubuntu12"), 1, 50),
          "zookeeper",
          new ServiceConstraint(
            ImmutableSet.of("small", "medium"),
            ImmutableSet.of("centos6"), 1, 5),
          "reactor",
          new ServiceConstraint(
            ImmutableSet.of("medium", "large"),
            null, 1, 5)
        ),
        new LayoutConstraint(
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("datanode", "nodemanager", "regionserver"),
            ImmutableSet.of("namenode", "resourcemanager", "hbasemaster")
          ),
          ImmutableSet.<Set<String>>of(
            ImmutableSet.of("datanode", "namenode"),
            ImmutableSet.of("datanode", "zookeeper"),
            ImmutableSet.of("namenode", "zookeeper"),
            ImmutableSet.of("datanode", "reactor"),
            ImmutableSet.of("namenode", "reactor")
          )
        ),
        SizeConstraint.EMPTY
      )).build();
    reactorTemplate2 = Entities.ClusterTemplateExample.REACTOR2;

    EntityStoreView adminView = entityStoreService.getView(account);
    // create providers
    provider = Provider.builder().setProviderType(Entities.JOYENT).setName("joyent").build();
    adminView.writeProvider(provider);
    // create hardware types
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Small 2GB")))
      .setName("small")
      .build()
    );
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Medium 4GB")))
        .setName("medium")
        .build()
    );
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 32GB")))
        .setName("large-mem")
        .build()
    );
    adminView.writeHardwareType(
      HardwareType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 16GB")))
        .setName("large-cpu")
        .build()
    );
    // create image types
    adminView.writeImageType(
      ImageType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of(
          "joyent", ImmutableMap.<String, String>of("image", "joyent-hash-of-centos6.4")))
      .setName("centos6")
      .build()
    );
    adminView.writeImageType(
      ImageType.builder().setProviderMap(
        ImmutableMap.<String, Map<String, String>>of(
          "joyent", ImmutableMap.<String, String>of("image", "joyent-hash-of-ubuntu12")))
        .setName("ubuntu12")
        .build()
    );
    // create services
    namenode = Service.builder().setName("namenode").build();
    datanode = Service.builder()
      .setName("datanode")
      .setDependencies(ServiceDependencies.runtimeRequires("namenode"))
      .build();
    resourcemanager = Service.builder()
      .setName("resourcemanager")
      .setDependencies(ServiceDependencies.runtimeRequires("datanode"))
      .build();
    nodemanager = Service.builder()
      .setName("nodemanager")
      .setDependencies(ServiceDependencies.runtimeRequires("resourcemanager"))
      .build();
    hbasemaster = Service.builder()
      .setName("hbasemaster")
      .setDependencies(ServiceDependencies.runtimeRequires("datanode"))
      .build();
    regionserver = Service.builder()
      .setName("regionserver")
      .setDependencies(ServiceDependencies.runtimeRequires("hbasemaster"))
      .build();
    zookeeper = Service.builder().setName("zookeeper").build();
    reactor = Service.builder()
      .setName("reactor")
      .setDependencies(ServiceDependencies.runtimeRequires("zookeeper", "regionserver", "nodemanager"))
      .build();
    mysql = Service.builder().setName("mysql").build();
    serviceMap = Maps.newHashMap();
    serviceMap.put(namenode.getName(), namenode);
    serviceMap.put(datanode.getName(), datanode);
    serviceMap.put(resourcemanager.getName(), resourcemanager);
    serviceMap.put(nodemanager.getName(), nodemanager);
    serviceMap.put(hbasemaster.getName(), hbasemaster);
    serviceMap.put(regionserver.getName(), regionserver);
    serviceMap.put(zookeeper.getName(), zookeeper);
    serviceMap.put(reactor.getName(), reactor);
    serviceMap.put(mysql.getName(), mysql);

    adminView.writeService(namenode);
    adminView.writeService(datanode);
    adminView.writeService(resourcemanager);
    adminView.writeService(nodemanager);
    adminView.writeService(hbasemaster);
    adminView.writeService(regionserver);
    adminView.writeService(zookeeper);
    adminView.writeService(reactor);
    adminView.writeService(mysql);
    adminView.writeClusterTemplate(reactorTemplate);

    EntityStoreView superadminView =
      entityStoreService.getView(new Account(Constants.ADMIN_USER, Constants.SUPERADMIN_TENANT));
    superadminView.writeProviderType(Entities.ProviderTypeExample.JOYENT);
    superadminView.writeProviderType(Entities.ProviderTypeExample.RACKSPACE);
  }
}
