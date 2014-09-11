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
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.template.LayoutConstraint;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.template.ServiceConstraint;
import co.cask.coopr.common.conf.Constants;
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
    reactorTemplate = new ClusterTemplate(
      "reactor-medium",
      "medium reactor cluster template",
      new ClusterDefaults(services, "joyent", null, null, null, Entities.ClusterTemplateExample.clusterConf),
      new Compatibilities(null, null, services),
      new Constraints(
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
      ),
      Administration.EMPTY_ADMINISTRATION
    );
    reactorTemplate2 = Entities.ClusterTemplateExample.REACTOR2;

    EntityStoreView adminView = entityStoreService.getView(account);
    // create providers
    provider = new Provider("joyent", "joyent provider", Entities.JOYENT, ImmutableMap.<String, String>of());
    adminView.writeProvider(provider);
    // create hardware types
    adminView.writeHardwareType(
      new HardwareType(
        "small",
        "small hardware",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Small 2GB"))
      )
    );
    adminView.writeHardwareType(
      new HardwareType(
        "medium",
        "medium hardware",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Medium 4GB"))
      )
    );
    adminView.writeHardwareType(
      new HardwareType(
        "large-mem",
        "hardware with a lot of memory",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 32GB"))
      )
    );
    adminView.writeHardwareType(
      new HardwareType(
        "large-cpu",
        "hardware with a lot of cpu",
        ImmutableMap.<String, Map<String, String>>of("joyent", ImmutableMap.<String, String>of("flavor", "Large 16GB"))
      )
    );
    // create image types
    adminView.writeImageType(
      new ImageType(
        "centos6",
        "CentOS 6.4 image",
        ImmutableMap.<String, Map<String, String>>of(
          "joyent", ImmutableMap.<String, String>of("image", "joyent-hash-of-centos6.4"))
      )
    );
    adminView.writeImageType(
      new ImageType(
        "ubuntu12",
        "Ubuntu 12 image",
        ImmutableMap.<String, Map<String, String>>of(
          "joyent", ImmutableMap.<String, String>of("image", "joyent-hash-of-ubuntu12"))
      )
    );
    // create services
    namenode = new Service("namenode", "", ImmutableSet.<String>of(),
                           ImmutableMap.<ProvisionerAction, ServiceAction>of());
    datanode = new Service("datanode", "", ImmutableSet.<String>of("namenode"),
                           ImmutableMap.<ProvisionerAction, ServiceAction>of());
    resourcemanager = new Service("resourcemanager", "", ImmutableSet.<String>of("datanode"),
                                  ImmutableMap.<ProvisionerAction, ServiceAction>of());
    nodemanager = new Service("nodemanager", "", ImmutableSet.<String>of("resourcemanager"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of());
    hbasemaster = new Service("hbasemaster", "", ImmutableSet.<String>of("datanode"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of());
    regionserver = new Service("regionserver", "", ImmutableSet.<String>of("hbasemaster"),
                               ImmutableMap.<ProvisionerAction, ServiceAction>of());
    zookeeper = new Service("zookeeper", "", ImmutableSet.<String>of(),
                            ImmutableMap.<ProvisionerAction, ServiceAction>of());
    reactor = new Service("reactor", "", ImmutableSet.<String>of("zookeeper", "regionserver", "nodemanager"),
                          ImmutableMap.<ProvisionerAction, ServiceAction>of());
    mysql = new Service("mysql", "", ImmutableSet.<String>of(), ImmutableMap.<ProvisionerAction, ServiceAction>of());
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
