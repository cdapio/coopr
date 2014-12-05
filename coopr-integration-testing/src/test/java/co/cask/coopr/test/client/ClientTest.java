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

package co.cask.coopr.test.client;

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.client.ClientManager;
import co.cask.coopr.client.rest.RestClientManager;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.HandlerServer;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.JobId;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.store.cluster.ClusterStoreView;
import co.cask.coopr.store.entity.EntityStoreView;
import co.cask.coopr.store.provisioner.PluginResourceTypeView;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;

/**
 * The parent class starts Coopr Handler Server
 */
public class ClientTest extends BaseTest {

  protected static final String TENANT_ID = "tenant1";
  protected static final String PROVISIONER_ID = "provisioner1";
  protected static final String TENANT = "tenant1";
  protected static final Account ADMIN_ACCOUNT = new Account(Constants.ADMIN_USER, TENANT_ID);
  protected static final Account SUPERADMIN_ACCOUNT = new Account(Constants.ADMIN_USER, Constants.SUPERADMIN_TENANT);
  protected static final String FIRST_TEST_CLUSTER_ID = "00000001";
  protected static final String SECOND_TEST_CLUSTER_ID = "00000002";
  protected static final String THIRD_TEST_CLUSTER_ID = "00000003";
  protected static final JobId FIRST_TEST_JOB_ID = new JobId(FIRST_TEST_CLUSTER_ID, 1);
  protected static final JobId SECOND_TEST_JOB_ID = new JobId(SECOND_TEST_CLUSTER_ID, 2);
  protected static final JobId THIRD_TEST_JOB_ID = new JobId(SECOND_TEST_CLUSTER_ID, 3);
  protected static final ClusterTemplate REACTOR_CLUSTER_TEMPLATE = Entities.ClusterTemplateExample.REACTOR;
  protected static final ClusterTemplate HDFS_CLUSTER_TEMPLATE = Entities.ClusterTemplateExample.HDFS;
  protected static final Provisioner TEST_PROVISIONER =
    new Provisioner(PROVISIONER_ID, "host1", 12345, 100, null, null);
  protected static final Tenant TEST_TENANT =
    new Tenant(TENANT_ID, new TenantSpecification(TENANT, 10, 100, 1000));
  protected static final Service ZOOKEEPER = ClientTestEntities.ZOOKEEPER;
  protected static final ClusterTemplate HADOOP_DISTRIBUTED_CLUSTER_TEMPLATE =
    Entities.ClusterTemplateExample.HADOOP_DISTRIBUTED;
  protected static final Cluster FIRST_TEST_CLUSTER =
    createTestCluster(FIRST_TEST_CLUSTER_ID, "cluster1", REACTOR_CLUSTER_TEMPLATE, FIRST_TEST_JOB_ID);
  protected static final Cluster SECOND_TEST_CLUSTER =
    createTestCluster(SECOND_TEST_CLUSTER_ID, "cluster2", HDFS_CLUSTER_TEMPLATE, SECOND_TEST_JOB_ID);
  protected static final Cluster THIRD_TEST_CLUSTER =
    createTestCluster(THIRD_TEST_CLUSTER_ID, "cluster3", HADOOP_DISTRIBUTED_CLUSTER_TEMPLATE, THIRD_TEST_JOB_ID);

  protected static HandlerServer handlerServer;
  protected static TenantProvisionerService tenantProvisionerService;
  protected static int port;
  protected static ClientManager adminClientManager;
  protected static ClientManager superadminCientManager;

  @BeforeClass
  public static void setupServiceBase() {
    handlerServer = injector.getInstance(HandlerServer.class);
    handlerServer.startAndWait();
    tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
    port = handlerServer.getBindAddress().getPort();
    adminClientManager = createClientManager(ADMIN_ACCOUNT);
    superadminCientManager = createClientManager(SUPERADMIN_ACCOUNT);
  }

  @Before
  public void setupServiceTest() throws IOException, IllegalAccessException {
    initData();
    tenantProvisionerService.writeProvisioner(TEST_PROVISIONER);
    tenantStore.writeTenant(TEST_TENANT);
  }

  @AfterClass
  public static void cleanupServiceBase() throws IOException {
    adminClientManager.close();
    superadminCientManager.close();
    handlerServer.stopAndWait();
  }

  public static void initData() throws IOException, IllegalAccessException {
    EntityStoreView adminView = entityStoreService.getView(ADMIN_ACCOUNT);
    EntityStoreView superadminView = entityStoreService.getView(SUPERADMIN_ACCOUNT);
    // write provider types
    superadminView.writeProviderType(Entities.ProviderTypeExample.JOYENT);
    superadminView.writeProviderType(Entities.ProviderTypeExample.RACKSPACE);
    // write automator types
    superadminView.writeAutomatorType(Entities.AutomatorTypeExample.CHEF);
    superadminView.writeAutomatorType(Entities.AutomatorTypeExample.PUPPET);
    superadminView.writeAutomatorType(Entities.AutomatorTypeExample.SHELL);
    //write resource types
    PluginResourceTypeView cookbooksResourceTypeView =
      metaStoreService.getResourceTypeView(SUPERADMIN_ACCOUNT, ClientTestEntities.COOKBOOKS_RESOURCE_TYPE);
    cookbooksResourceTypeView.add(ClientTestEntities.HADOOP_RESOURCE_META_V1);
    cookbooksResourceTypeView.add(ClientTestEntities.HADOOP_RESOURCE_META_V2);
    cookbooksResourceTypeView.add(ClientTestEntities.KAFKA_RESOURCE_META);
    cookbooksResourceTypeView.add(ClientTestEntities.MYSQL_RESOURCE_META);

    PluginResourceTypeView keysResourceTypeView =
      metaStoreService.getResourceTypeView(SUPERADMIN_ACCOUNT, ClientTestEntities.KEYS_RESOURCE_TYPE);
    keysResourceTypeView.add(ClientTestEntities.DEV_KEY_RESOURCE_META_V1);
    keysResourceTypeView.add(ClientTestEntities.DEV_KEY_RESOURCE_META_V2);
    keysResourceTypeView.add(ClientTestEntities.VIEW_KEY_RESOURCE_META);
    keysResourceTypeView.add(ClientTestEntities.RESEARCH_KEY_RESOURCE_META);

    // write providers
    adminView.writeProvider(Entities.ProviderExample.JOYENT);
    adminView.writeProvider(Entities.ProviderExample.RACKSPACE);
    // write hardware types
    adminView.writeHardwareType(Entities.HardwareTypeExample.LARGE);
    adminView.writeHardwareType(Entities.HardwareTypeExample.MEDIUM);
    adminView.writeHardwareType(Entities.HardwareTypeExample.SMALL);
    // write image types
    adminView.writeImageType(Entities.ImageTypeExample.CENTOS_6);
    adminView.writeImageType(Entities.ImageTypeExample.UBUNTU_12);
    // write services
    adminView.writeService(Entities.ServiceExample.DATANODE);
    adminView.writeService(Entities.ServiceExample.HOSTS);
    adminView.writeService(Entities.ServiceExample.NAMENODE);
    adminView.writeService(ZOOKEEPER);
    // write cluster templates
    adminView.writeClusterTemplate(REACTOR_CLUSTER_TEMPLATE);
    adminView.writeClusterTemplate(HDFS_CLUSTER_TEMPLATE);
    adminView.writeClusterTemplate(HADOOP_DISTRIBUTED_CLUSTER_TEMPLATE);
    // write clusters
    ClusterStoreView clusterStoreView = clusterStoreService.getView(ADMIN_ACCOUNT);
    clusterStoreView.writeCluster(FIRST_TEST_CLUSTER);
    clusterStoreView.writeCluster(SECOND_TEST_CLUSTER);
    clusterStoreView.writeCluster(THIRD_TEST_CLUSTER);
    // write cluster jobs
    clusterStore.writeClusterJob(new ClusterJob(FIRST_TEST_JOB_ID, ClusterAction.CLUSTER_CREATE));
    clusterStore.writeClusterJob(new ClusterJob(SECOND_TEST_JOB_ID, ClusterAction.CLUSTER_CREATE));
    clusterStore.writeClusterJob(new ClusterJob(THIRD_TEST_JOB_ID, ClusterAction.CLUSTER_CREATE));
    // write nodes
    clusterStore.writeNode(Entities.NodeExample.createNode("node1", FIRST_TEST_CLUSTER_ID));
    clusterStore.writeNode(Entities.NodeExample.createNode("node2", SECOND_TEST_CLUSTER_ID));
  }

  @Override
  protected boolean shouldClearDataBetweenTests() {
    return true;
  }

  private static Cluster createTestCluster(String id, String name, ClusterTemplate clusterTemplate, JobId jobId) {
    JsonObject clusterConfig = new JsonObject();
    clusterConfig.addProperty("property1", "value1");
    clusterConfig.addProperty("property2", "value2");
    clusterConfig.addProperty("property3", "value3");
    return Cluster.builder()
      .setName(name)
      .setID(id)
      .setLatestJobID(jobId.getId())
      .setAccount(ADMIN_ACCOUNT)
      .setProvider(Entities.ProviderExample.JOYENT)
      .setStatus(Cluster.Status.ACTIVE)
      .setServices(Sets.newHashSet(Entities.ServiceExample.DATANODE.getName(),
                                   Entities.ServiceExample.NAMENODE.getName(),
                                   Entities.ServiceExample.HOSTS.getName()))
      .setNodes(ImmutableSet.of("node1"))
      .setClusterTemplate(clusterTemplate)
      .setConfig(clusterConfig)
      .build();
  }

  private static ClientManager createClientManager(Account account) {
    return RestClientManager.builder(HOSTNAME, port)
      .userId(account.getUserId())
      .tenantId(account.getTenantId())
      .gson(injector.getInstance(Gson.class))
      .build();
  }
}
