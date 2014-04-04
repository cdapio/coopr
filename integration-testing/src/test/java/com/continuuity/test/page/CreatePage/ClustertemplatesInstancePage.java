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
package com.continuuity.test.page.CreatePage;

import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.Compatibilities;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.test.Constants;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.JSONException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.continuuity.test.drivers.Global.globalDriver;

/**
 * Get Clustertemplate instance data from the webpage.
 */
public class ClustertemplatesInstancePage extends GenericPage {
  // Constants in Default tab
  private static final Gson GSON = new JsonSerde().getGson();
  private static final By PROVIDER = By.name("inputProvider");
  private static final By HARDWARETYPE = By.name("inputHardwaretype");
  private static final By IMAGETYPE = By.name("inputImagetype");
  private static final By DNS_SUFFIX = By.cssSelector("#inputDnsSuffix");
  private static final By CONFIG = By.cssSelector("#inputConfig");
  private static final By DEFAULT_SERVICE = By.cssSelector(".default-service-entries");
  private static final By DEFAULT_SERVIE_ENTRY = By.cssSelector(".default-service-entry");

  // Constants in Compatibility tab
  private static final By ALLOWED_SERVICE = By.cssSelector(".allowed-service-entries");
  private static final By ALLOWED_HARDWARETYPE = By.cssSelector(".allowed-hardwaretype-entries");
  private static final By ALLOWED_IMAGETYPES = By.cssSelector(".allowed-imagetype-entries");

  // Constants in Constraints tab
  private static final By CONTRAINT_SERVICE = By.name("service-name");
  private static final By QUANTITY_MIN = By.cssSelector(".quantity-min");
  private static final By QUANTITY_MAX = By.cssSelector(".quantity-max");
  private static final By CONSTRAINT_TABLE = By.cssSelector(".table-main");
  private static final By SERVICE_HARDWARETYPE = By.name("service-hardwaretype");
  private static final By SERVICE_IMAGETYPE = By.name("service-imagetype");
  private static final By CANT_COEXIST_GROUP = By.cssSelector(".cant-coexist-group");
  private static final By CANT_COEXIST_ENTRY = By.cssSelector(".cant-coexist-entry");
  private static final By MUST_COEXIST_GROUP = By.cssSelector(".must-coexist-group");
  private static final By MUST_COEXIST_ENTRY = By.cssSelector(".must-coexist-entry");

  public ClusterDefaults getClusterDefaults() throws JSONException {
    Select select = new Select(globalDriver.findElement(PROVIDER));
    String provider = select.getFirstSelectedOption().getAttribute(Constants.TEXT);

    select = new Select(globalDriver.findElement(HARDWARETYPE));
    String hardwaretype = select.getFirstSelectedOption().getAttribute(Constants.TEXT);

    select = new Select(globalDriver.findElement(IMAGETYPE));
    String imagetype = select.getFirstSelectedOption().getAttribute(Constants.TEXT);

    String dnsSuffix = globalDriver.findElement(DNS_SUFFIX).getAttribute(Constants.VALUE);

    String config = globalDriver.findElement(CONFIG).getAttribute(Constants.VALUE);
    JsonObject configJson = GSON.fromJson(config, JsonObject.class);

    Set<String> services = Sets.newHashSet();
    for (WebElement element : globalDriver.findElement(DEFAULT_SERVICE).findElements(DEFAULT_SERVIE_ENTRY)) {
      services.add(element.getAttribute(Constants.INNER_HTML));
    }

    return new ClusterDefaults(services, provider, hardwaretype, imagetype, dnsSuffix, configJson);
  }

  public Compatibilities getCompatibility() {
    Set<String> services = Sets.newHashSet();
    for (WebElement element : globalDriver.findElement(ALLOWED_SERVICE).findElements(DEFAULT_SERVIE_ENTRY)) {
      services.add(element.getAttribute(Constants.INNER_HTML));
    }
    Set<String> hardwaretypes = Sets.newHashSet();
    for (WebElement element : globalDriver.findElement(ALLOWED_HARDWARETYPE).findElements(DEFAULT_SERVIE_ENTRY)) {
      hardwaretypes.add(element.getAttribute(Constants.INNER_HTML));
    }
    Set<String> imagetypes = Sets.newHashSet();
    for (WebElement element : globalDriver.findElement(ALLOWED_IMAGETYPES).findElements(DEFAULT_SERVIE_ENTRY)) {
      imagetypes.add(element.getAttribute(Constants.INNER_HTML));
    }
    return new Compatibilities(hardwaretypes, imagetypes, services);
  }

  public Constraints getConstraints() {
    Map<String, ServiceConstraint> serviceConstraints = getServiceConstraints();
    Set<Set<String>> servicesThatMustCoexist = getLayoutConstraint(MUST_COEXIST_GROUP, MUST_COEXIST_ENTRY);
    Set<Set<String>> servicesThatMustNotCoexist = getLayoutConstraint(CANT_COEXIST_GROUP, CANT_COEXIST_ENTRY);
    LayoutConstraint layoutConstraint = new LayoutConstraint(servicesThatMustCoexist, servicesThatMustNotCoexist);
    return new Constraints(serviceConstraints, layoutConstraint);
  }

  public Map<String, ServiceConstraint> getServiceConstraints() {
    Map<String, ServiceConstraint> serviceConstraints = Maps.newHashMap();
    WebElement table = globalDriver.findElement(CONSTRAINT_TABLE);
    WebElement tbody = table.findElement(Constants.TBODY);
    List<WebElement> entries = tbody.findElements(Constants.TR);
    for (WebElement entry : entries) {
      List<WebElement> cols = entry.findElements(Constants.TD);

      WebElement selectElement = cols.get(0).findElement(CONTRAINT_SERVICE);
      String service = new Select(selectElement).getFirstSelectedOption().getAttribute(Constants.TEXT);

      Set<String> requiredHardwaretypes = getRequiredTypes(cols.get(1), SERVICE_HARDWARETYPE);
      if (requiredHardwaretypes.isEmpty()) {
        requiredHardwaretypes = null;
      }

      Set<String> requiredImagetypes = getRequiredTypes(cols.get(2), SERVICE_IMAGETYPE);
      if (requiredImagetypes.isEmpty()) {
        requiredImagetypes = null;
      }

      Integer minCount = getQuantity(cols.get(3), QUANTITY_MIN);
      Integer maxCount = getQuantity(cols.get(4), QUANTITY_MAX);

      serviceConstraints.put(service, new ServiceConstraint(requiredHardwaretypes, requiredImagetypes,
                                                            minCount, maxCount, null, null));
    }
    return serviceConstraints;
  }

  public Set<Set<String>> getMustCoexistLayoutConstraint() {
    return getLayoutConstraint(MUST_COEXIST_GROUP, MUST_COEXIST_ENTRY);
  }

  public Set<Set<String>> getCantCoexistLayoutConstraint() {
    return getLayoutConstraint(CANT_COEXIST_GROUP, CANT_COEXIST_ENTRY);
  }

  private Set<Set<String>> getLayoutConstraint(By group, By entry) {
    Set<Set<String>> constraints = Sets.newHashSet();
    for (WebElement groupElement : globalDriver.findElements(group)) {
      Set<String> constraint = Sets.newHashSet();
      for (WebElement element : groupElement.findElements(entry)) {
        constraint.add(element.getAttribute(Constants.INNER_HTML));
      }
      if (!constraint.isEmpty()) {
        constraints.add(constraint);
      }
    }
    return constraints;
  }

  public Set<String> getRequiredTypes(WebElement element, By type) {
    Set<String> types = Sets.newHashSet();
    for (WebElement select : new Select(element.findElement(type)).getAllSelectedOptions()) {
      String text = select.getAttribute(Constants.TEXT);
      if (text != null && !text.isEmpty()) {
        types.add(select.getAttribute(Constants.TEXT));
      }
    }
    return types;
  }

  private Integer getQuantity(WebElement element, By type) {
    Integer value = null;
    String text = element.findElement(type).getAttribute(Constants.VALUE);
    if (text != null && !text.isEmpty()) {
      value = Integer.valueOf(text);
    }
    return value;
  }

}
