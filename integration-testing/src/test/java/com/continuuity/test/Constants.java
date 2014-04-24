/**
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
package com.continuuity.test;

import com.google.common.collect.ImmutableSet;
import org.openqa.selenium.By;

import java.io.File;
import java.net.ServerSocket;

/**
 *
 */
public class Constants {

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
  public static final String PROVIDER_INSTANCE_URI = PROVIDERS_URL + "/provider/joyent";
  public static final String PROVIDER_CREATE_URI = PROVIDERS_URL + "/create";
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
  public static final String PARENT_PATH = new File(System.getProperty("user.dir")).getParent();
  public static final String IMAGETYPES_PATH = PARENT_PATH + "/ui/test/imagetypes";
  public static final String HARDWARETYPES_PATH = PARENT_PATH + "/ui/test/hardwaretypes";
  public static final String PROVIDERS_PATH = PARENT_PATH + "/ui/test/providers";
  public static final String SERVICES_PATH = PARENT_PATH + "/ui/test/services";
  public static final String CLUSTERTEMPLATE_PATH = PARENT_PATH + "/ui/test/clustertemplates";

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
  public static final String CLUSTERS_FILE_NAME = PARENT_PATH + "/ui/test/clusters/clusters.json";
  public static final String CLUSTERDEF_FILE_NAME = PARENT_PATH + "/ui/test/clusters/clusterdefinitions.json";
  public static final String CLUSTER_CREATE_FILE_NAME = PARENT_PATH + "/ui/test/clusters/createcluster.json";

  public static final String ACTION_TABLE_CLASSNAME = ".node-actions-table";

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
