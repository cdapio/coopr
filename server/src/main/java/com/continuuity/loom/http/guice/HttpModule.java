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
package com.continuuity.loom.http.guice;

import com.continuuity.http.HttpHandler;
import com.continuuity.loom.http.handler.AdminHandler;
import com.continuuity.loom.http.handler.ClusterHandler;
import com.continuuity.loom.http.handler.NodeHandler;
import com.continuuity.loom.http.handler.PluginHandler;
import com.continuuity.loom.http.handler.ProvisionerHandler;
import com.continuuity.loom.http.handler.RPCHandler;
import com.continuuity.loom.http.handler.StatusHandler;
import com.continuuity.loom.http.handler.SuperadminHandler;
import com.continuuity.loom.http.handler.TaskHandler;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Guice bindings for http related classes.
 */
public class HttpModule extends AbstractModule {

  @Override
  protected void configure() {

    Multibinder<HttpHandler> handlerBinder = Multibinder.newSetBinder(binder(), HttpHandler.class);
    handlerBinder.addBinding().to(AdminHandler.class);
    handlerBinder.addBinding().to(ClusterHandler.class);
    handlerBinder.addBinding().to(NodeHandler.class);
    handlerBinder.addBinding().to(TaskHandler.class);
    handlerBinder.addBinding().to(StatusHandler.class);
    handlerBinder.addBinding().to(RPCHandler.class);
    handlerBinder.addBinding().to(SuperadminHandler.class);
    handlerBinder.addBinding().to(ProvisionerHandler.class);
    handlerBinder.addBinding().to(PluginHandler.class);
  }
}
