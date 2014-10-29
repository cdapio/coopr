/**
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
package co.cask.coopr.test;

import com.google.common.collect.ImmutableSet;
import org.openqa.selenium.By;

import java.io.File;
import java.net.ServerSocket;

/**
 *
 */
public class Constants {

  // CLI commands for integration tests
  // Admin commands
  public static final String LIST_TEMPLATES_COMMAND = "list templates";
  public static final String GET_TEMPLATE_COMMAND = "get template \"%s\"";
  public static final String DELETE_TEMPLATE_COMMAND = "delete template \"%s\"";
  public static final String LIST_PROVIDERS_COMMAND = "list providers";
  public static final String GET_PROVIDER_COMMAND = "get provider \"%s\"";
  public static final String DELETE_PROVIDER_COMMAND = "delete provider \"%s\"";
  public static final String LIST_SERVICES_COMMAND = "list services";
  public static final String GET_SERVICE_COMMAND = "get service \"%s\"";
  public static final String DELETE_SERVICE_COMMAND = "delete service \"%s\"";
  public static final String LIST_HARDWARE_TYPES_COMMAND = "list hardware types";
  public static final String GET_HARDWARE_TYPE_COMMAND = "get hardware type \"%s\"";
  public static final String DELETE_HARDWARE_TYPE_COMMAND = "delete hardware type \"%s\"";
  public static final String LIST_IMAGE_TYPES_COMMAND = "list image types";
  public static final String GET_IMAGE_TYPE_COMMAND = "get image type \"%s\"";
  public static final String DELETE_IMAGE_TYPE_COMMAND = "delete image type \"%s\"";

  // Cluster commands
  public static final String LIST_CLUSTERS_COMMAND = "list clusters";
  public static final String GET_CLUSTER_COMMAND = "get cluster \"%s\"";
  public static final String DELETE_CLUSTER_COMMAND = "delete cluster \"%s\"";
  public static final String CREATE_CLUSTER_COMMAND = "create cluster \"%s\" with template \"%s\" of size %s";
  public static final String GET_CLUSTER_STATUS_COMMAND = "get cluster-status \"%s\"";
  public static final String GET_CLUSTER_CONFIG_COMMAND = "get cluster-config \"%s\"";
  public static final String SET_CLUSTER_CONFIG_COMMAND = "set config '%s' for cluster \"%s\"";
  public static final String LIST_CLUSTER_SERVICES_COMMAND = "list services \"%s\"";
  public static final String SYNC_CLUSTER_TEMPLATE_COMMAND = "sync cluster template \"%s\"";
  public static final String SET_CLUSTER_EXPIRE_TIME_COMMAND = "set expire time \"%s\" for cluster \"%s\"";
  public static final String START_SERVICE_ON_CLUSTER_COMMAND = "start service \"%s\" on cluster \"%s\"";
  public static final String STOP_SERVICE_ON_CLUSTER_COMMAND = "stop service \"%s\" on cluster \"%s\"";
  public static final String RESTART_SERVICE_ON_CLUSTER_COMMAND = "restart service \"%s\" on cluster \"%s\"";
  public static final String ADD_SERVICES_ON_CLUSTER_COMMAND = "add services '%s' on cluster \"%s\"";

  //Plugin commands
  public static final String LIST_ALL_AUTOMATORS_COMMAND = "list all automatortypes";
  public static final String LIST_ALL_PROVIDERS_COMMAND = "list all providertypes";
  public static final String GET_AUTOMATOR_TYPE_COMMAND = "get automatortype %s";
  public static final String GET_PROVIDER_TYPE_COMMAND = "get providertype %s";
  public static final String AUTOMATOR_TYPE_CHEF_NAME = "chef-solo";
  public static final String PROVIDER_TYPE_JOYENT_NAME = "joyent";
  public static final String LIST_ALL_AUTOMATOR_TYPE_RESOURCES_COMMAND =
                             "list resources from automator %s of type %s and status %s";
  public static final String LIST_ALL_PROVIDER_TYPE_RESOURCES_COMMAND =
                            "list resources from provider %s of type %s and status %s";
  public static final String STAGE_AUTOMATOR_TYPE_RESOURCE_COMMAND =
                            "stage resource from automator %s of type %s and name %s and version %d";
  public static final String STAGE_PROVIDER_TYPE_RESOURCE_COMMAND =
                            "stage resource from provider %s of type %s and name %s and version %d";
  public static final String RECALL_AUTOMATOR_TYPE_RESOURCE_COMMAND =
    "recall resource from automator %s of type %s and name %s and version %d";
  public static final String RECALL_PROVIDER_TYPE_RESOURCE_COMMAND =
    "recall resource from provider %s of type %s and name %s and version %d";
  public static final String DELETE_AUTOMATOR_TYPE_RESOURCE_COMMAND =
    "delete resource from automator %s of type %s and name %s and version %d";
  public static final String DELETE_PROVIDER_TYPE_RESOURCE_COMMAND =
    "delete resource from provider %s of type %s and name %s and version %d";

  // Provisioner commands
  public static final String LIST_PROVISIONERS_COMMAND = "list provisioners";
  public static final String GET_PROVISIONER_COMMAND = "get provisioner \"%s\"";

  // Tenant commands
  public static final String LIST_TENANTS_COMMAND = "list tenants";
  public static final String GET_TENANT_COMMAND = "get tenant \"%s\"";
  public static final String DELETE_TENANT_COMMAND = "delete tenant \"%s\"";

  // URI
  public static final int PORT = getPort();
  public static final String ROOT_URL = "http://localhost:" + PORT;
  public static final String INDEX_URL = ROOT_URL;
  public static final String LOGIN_URL = ROOT_URL + "/login";
  public static final String PROVIDERS_URL = ROOT_URL + "/providers";
  public static final String HARDWARETYPES_URL = ROOT_URL + "/hardwaretypes";
  public static final String IMAGETYPES_URL = ROOT_URL + "/imagetypes";
  public static final String CLUSTERTEMPLATES_URL = ROOT_URL + "/clustertemplates";
  public static final String SERVICES_URL = ROOT_URL + "/services";
  public static final String PROVIDER_INSTANCE_URI = PROVIDERS_URL + "/provider/joyent/#/edit";
  public static final String PROVIDER_CREATE_URI = PROVIDERS_URL + "/create#/";
  public static final String HARDWARETYPE_CREATE_URI = HARDWARETYPES_URL + "/create";
  public static final String HARDWARETYPE_INSTANCE_URI = HARDWARETYPES_URL + "/hardwaretype/small";
  public static final String IMAGETYPE_INSTANCE_URI = IMAGETYPES_URL + "/imagetype/centos6";
  public static final String IMAGETYPE_CREATE_URI = IMAGETYPES_URL + "/create";
  public static final String SERVICES_INSTANCE_URI = SERVICES_URL + "/service/reactor";
  public static final String SERVICE_CREATE_URI = SERVICES_URL + "/create";
  public static final String CLUSTERTEMPLATE_INSTANCE_URI = CLUSTERTEMPLATES_URL +
    "/clustertemplate/reactor-singlenode";
  public static final String CLUSTERTEMPLATE_CREATE_URI = CLUSTERTEMPLATES_URL + "/create";
  public static final String CLUSTERS_URL = ROOT_URL + "/admin/clusters";
  public static final String CLUSTER_CREATE_URL = ROOT_URL + "/user/clusters/create";
  public static final String CLUSTERS_INSTANCE_URL = ROOT_URL + "/user/clusters/cluster/00000139";
  public static final String USER_CLUSTERS = ROOT_URL + "/user/clusters";

  // Canonical paths for test examples
  public static final String PARENT_PATH = new File(System.getProperty("user.dir")).getParent() + "/coopr-ui";
  public static final String IMAGETYPES_PATH = PARENT_PATH + "/test/imagetypes";
  public static final String HARDWARETYPES_PATH = PARENT_PATH + "/test/hardwaretypes";
  public static final String PROVIDERS_PATH = PARENT_PATH + "/test/providers";
  public static final String SERVICES_PATH = PARENT_PATH + "/test/services";
  public static final String CLUSTERTEMPLATE_PATH = PARENT_PATH + "/test/clustertemplates";

  // Location properties in /
  public static final String CLUSTERTEMPLATE_TABLE = ".clustertemplates-table";
  public static final String CLUSTERTEMPLATE_HREF = "/clustertemplates";

  // Table structure properties
  public static final By TBODY = By.tagName("tbody");
  public static final By TR = By.tagName("tr");
  public static final By TD = By.tagName("td");

  // Table location properties
  public static final By TITLE_BY = By.cssSelector("#title h3");
  public static final String TABLE = "table.table-striped";
  public static final String VALUE = "value";
  public static final String AUTH = "auth";
  public static final String TEXT = "text";
  public static final String INNER_HTML = "innerHTML";
  public static final String DELTEST = "deltest";
  public static final String HREF = "href";

  // Left panel properties
  public static final By NAV_CLUSTERS = By.cssSelector("#nav-clusters");
  public static final By NAV_PROVIDERS = By.cssSelector("#nav-providers");
  public static final By NAV_HARDWARETYPES = By.cssSelector("#nav-hardwaretypes");
  public static final By NAV_IMAGETYPES = By.cssSelector("#nav-imagetypes");
  public static final By NAV_SERVICES = By.cssSelector("#nav-services");
  public static final By NAV_LOGIN = By.cssSelector("#nav-login");

  public static final ImmutableSet<String> LEFT_PANEL = ImmutableSet.of("Clusters",
                                                        "Providers", "Hardware", "Images", "Services");

  // Fixture properties
  public static final String CLUSTERS_FILE_NAME = PARENT_PATH + "/test/clusters/clusters.json";
  public static final String CLUSTERDEF_FILE_NAME = PARENT_PATH + "/test/clusters/clusterdefinitions.json";
  public static final String CLUSTER_CREATE_FILE_NAME = PARENT_PATH + "/test/clusters/createcluster.json";

  public static final String ACTION_TABLE_CLASSNAME = ".node-actions-table";
  public static final ImmutableSet<String> PROVIDERS_SET = ImmutableSet.of(
    "awesome", "rackspace", "joyent", "openstack");

  public static int getPort() {
    int port = -1;
    try {
      ServerSocket s = new ServerSocket(0);
      port = s.getLocalPort();
      s.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Could not find port");
    }
    return port;
  }
}
