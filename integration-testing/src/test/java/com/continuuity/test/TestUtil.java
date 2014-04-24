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

import com.continuuity.test.drivers.Global;
import com.google.common.collect.Sets;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;

import static com.continuuity.test.drivers.Global.globalDriver;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestUtil {
  private static final By TYPES = By.cssSelector(".list-types");

  public WebElement getTable(String tableCss) {
    By table = By.cssSelector(tableCss);
    return globalDriver.findElement(table);
  }

  public List<WebElement> getRows(WebElement table) {
    WebElement tbody = table.findElement(Constants.TBODY);
    return tbody.findElements(By.xpath("./tr"));
  }

  public List<WebElement> geTdsFromTr(WebElement tr) {
    return tr.findElements(By.xpath("./td"));
  }

  public String getTd(WebElement table, int rowInd, int tdInd) {
    return getTdElement(table, rowInd, tdInd).getText();
  }

  public String getTdAttribute(WebElement table, int rowInd, int tdInd, String attribute) {
    return getTdElement(table, rowInd, tdInd).getAttribute(attribute);
  }

  public WebElement getTdElement(WebElement table, int rowInd, int tdInd) {
    WebElement row = getRows(table).get(rowInd);
    List<WebElement> tds = row.findElements(Constants.TD);
    return tds.get(tdInd);
  }

  public int getRowCount(WebElement table) {
    return getRows(table).size();
  }

  public int getColCount(WebElement table) {
    WebElement row = getRows(table).get(0);
    List<WebElement> tds = row.findElements(Constants.TD);
    return tds.size();
  }

  public WebElement getPanelHead(String linkText) {
    return globalDriver.findElement(By.linkText(linkText));
  }

  public String convertSetToString(Set<String> set) {
    if (set == null || set.size() == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (String element : set) {
      sb.append(element);
      sb.append(",");
    }
    sb.deleteCharAt(sb.length() -1);
    return sb.toString();
  }

  public Set<String> getTopList(WebDriver driver) {
    Set<String> topList = Sets.newHashSet();
    for (WebElement element : driver.findElements(TYPES)) {
      topList.add(element.getAttribute(Constants.TEXT));
    }
    return topList;
  }

  public Set<String> getTopListUri(Set<String> topList, String uriPrefix) {
    Set<String> expectedTopListUri = Sets.newHashSet();
    for (String element : topList) {
      expectedTopListUri.add(uriPrefix + element);
    }
    return expectedTopListUri;
  }

  public Set<String> getTopListUri(WebDriver driver) {
    Set<String> topListUri = Sets.newHashSet();
    for (WebElement element : driver.findElements(TYPES)) {
      topListUri.add(element.getAttribute(Constants.HREF));
    }
    return topListUri;
  }

  public Set<String> getLeftPanel(WebDriver driver) {
    Set<String> leftPanel = Sets.newHashSet();
    leftPanel.add(driver.findElement(Constants.NAV_CLUSTERS).getText());
    leftPanel.add((driver.findElement(Constants.NAV_PROVIDERS).getText()));
    leftPanel.add(driver.findElement(Constants.NAV_HARDWARETYPES).getText());
    leftPanel.add(driver.findElement(Constants.NAV_IMAGETYPES).getText());
    leftPanel.add(driver.findElement(Constants.NAV_SERVICES).getText());
    return leftPanel;
  }

  public String getHTMLClasses(String cssSelector) {
    return globalDriver.findElement(By.cssSelector(cssSelector)).getAttribute("class");
  }

  public void genericDeleteTest(String expectedUrl) {
    WebElement deleteForm = globalDriver.findElement(By.cssSelector(".delete-form"));
    deleteForm.submit();
    Global.driverWait(1);
    globalDriver.findElement(By.cssSelector(".action-submit-delete")).click();
    Global.driverWait(1);
    assertEquals(expectedUrl, globalDriver.getCurrentUrl());
  }
}
