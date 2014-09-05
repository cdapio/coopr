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
package co.cask.coopr.store.guice;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.store.credential.CredentialStore;
import co.cask.coopr.store.provisioner.PluginStore;
import com.google.inject.Scopes;

/**
 * Guice module for binding store related classes.
 */
public class StoreModule extends AbstractStoreModule {
  private final Class pluginStoreClass;
  private final Class credentialStoreClass;

  public StoreModule(Configuration conf) throws ClassNotFoundException {
    this.pluginStoreClass = Class.forName(conf.get(Constants.PLUGIN_STORE_CLASS));
    this.credentialStoreClass = Class.forName(conf.get(Constants.CREDENTIAL_STORE_CLASS));
  }

  @Override
  void bindPluginStore() {
    bind(PluginStore.class).to(pluginStoreClass).in(Scopes.SINGLETON);
    bind(CredentialStore.class).to(credentialStoreClass).in(Scopes.SINGLETON);
  }

}
