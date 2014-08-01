/*
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
package com.continuuity.loom.store.guice;

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.store.provisioner.PluginStore;
import com.google.inject.Scopes;

/**
 * Guice module for binding store related classes.
 */
public class StoreModule extends AbstractStoreModule {
  private final Class pluginStoreClass;

  public StoreModule(Configuration conf) throws ClassNotFoundException {
    this.pluginStoreClass = Class.forName(conf.get(Constants.PLUGIN_STORE_CLASS));
  }

  @Override
  void bindPluginStore() {
    bind(PluginStore.class).to(pluginStoreClass).in(Scopes.SINGLETON);
  }

}
