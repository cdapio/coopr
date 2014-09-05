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
package com.continuuity.test.input;

import com.continuuity.loom.spec.template.ClusterTemplate;
import com.continuuity.loom.spec.HardwareType;
import com.continuuity.loom.spec.ImageType;
import com.continuuity.loom.spec.NamedEntity;
import com.continuuity.loom.spec.Provider;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.codec.json.guice.CodecModules;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Read examples files.
 */
public class ExampleReader {
  private static final Logger LOG = LoggerFactory.getLogger(ExampleReader.class);
  private final Gson gson;

  public ExampleReader() {
    Injector injector = Guice.createInjector(new CodecModules().getModule());
    gson = injector.getInstance(Gson.class);
  }

  public Map<String, ImageType> getImageTypes(String pathName) throws Exception {
    return getType(pathName, ImageType.class);
  }

  public Map<String, HardwareType> getHardwareTypes(String pathName) throws Exception {
    return getType(pathName, HardwareType.class);
  }

  public Map<String, Provider> getProviders(String pathName) throws Exception {
    return getType(pathName, Provider.class);
  }

  public Map<String, Service> getServices(String pathName) throws Exception {
    return getType(pathName, Service.class);
  }

  public Map<String, ClusterTemplate> getClusterTemplate(String pathName) throws Exception {
    return getType(pathName, ClusterTemplate.class);
  }

  private <V extends NamedEntity> Map<String, V> getType(String pathName, Class<V> typeClass) throws Exception {
    Map<String, V> result = Maps.newHashMap();
    File pathFile = new File(pathName);
    File[] files = pathFile.listFiles();
    for (File file : files) {
      try {
        String json = Files.toString(file, Charsets.UTF_8);
        V value = gson.fromJson(json, typeClass);
        result.put(value.getName(), value);
      } catch (IOException e) {
        LOG.error("{} can't open.", file.getName());
      } catch (JsonSyntaxException e) {
        LOG.error("Got exception while parsing JSON: ", e);
      }
    }
    return result;
  }
}
