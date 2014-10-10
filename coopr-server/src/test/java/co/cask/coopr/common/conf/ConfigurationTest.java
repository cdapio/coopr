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

package co.cask.coopr.common.conf;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * ConfigurationTest checks that obtaining properties from coopr-security.xml works correctly.
 */
public class ConfigurationTest {

  @Test
  public void testSecurityXml() throws IOException {
    // create standard configuration and add DB login data
    Configuration siteConf = new Configuration();
    siteConf.addResource("coopr-default.xml");
    siteConf.addResource("coopr-site.xml");
    siteConf.set(Constants.DB_USER, "coopr");
    siteConf.set(Constants.DB_PASSWORD, "changeme");

    Map<String, String> siteConfMap = getPropertiesAsMap(siteConf);

    // create standard configuration and add DB login data from the coopr-security.xml
    Configuration securityConf = new Configuration();
    securityConf.addResource("coopr-default.xml");
    securityConf.addResource("coopr-site.xml");
    securityConf.addResource("coopr-security.xml");

    Map<String, String> siteAndSecurityConfMap = getPropertiesAsMap(securityConf);

    // compare configurations
    MapDifference<String, String> diff = Maps.difference(siteConfMap, siteAndSecurityConfMap);
    Assert.assertTrue(diff.areEqual());
  }

  private Map<String, String> getPropertiesAsMap(Configuration configuration) {
    Map<String, String> result = Maps.newHashMap();
    for (Map.Entry<String, String> confEntry : configuration) {
      result.put(confEntry.getKey(), confEntry.getValue());
    }
    return result;
  }

}
