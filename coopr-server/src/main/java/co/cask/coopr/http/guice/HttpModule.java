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
package co.cask.coopr.http.guice;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.handler.AdminHandler;
import co.cask.coopr.http.handler.ClusterHandler;
import co.cask.coopr.http.handler.MetricHandler;
import co.cask.coopr.http.handler.NodeHandler;
import co.cask.coopr.http.handler.PluginHandler;
import co.cask.coopr.http.handler.ProvisionerHandler;
import co.cask.coopr.http.handler.RPCHandler;
import co.cask.coopr.http.handler.StatusHandler;
import co.cask.coopr.http.handler.SuperadminHandler;
import co.cask.coopr.http.handler.TaskHandler;
import co.cask.coopr.http.handler.UserHandler;
import co.cask.http.HttpHandler;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Guice bindings for http related classes.
 */
public class HttpModule extends AbstractModule {

  @Override
  protected void configure() {

    Multibinder<HttpHandler> externalHandlerBinder =
      Multibinder.newSetBinder(binder(), HttpHandler.class, Names.named(Constants.HandlersNames.EXTERNAL));
    externalHandlerBinder.addBinding().to(AdminHandler.class);
    externalHandlerBinder.addBinding().to(ClusterHandler.class);
    externalHandlerBinder.addBinding().to(NodeHandler.class);
    externalHandlerBinder.addBinding().to(StatusHandler.class);
    externalHandlerBinder.addBinding().to(RPCHandler.class);
    externalHandlerBinder.addBinding().to(SuperadminHandler.class);
    externalHandlerBinder.addBinding().to(PluginHandler.class);
    externalHandlerBinder.addBinding().to(UserHandler.class);
    externalHandlerBinder.addBinding().to(MetricHandler.class);

    Multibinder<HttpHandler> internalHandlerBinder =
      Multibinder.newSetBinder(binder(), HttpHandler.class, Names.named(Constants.HandlersNames.INTERNAL));
    internalHandlerBinder.addBinding().to(TaskHandler.class);
    internalHandlerBinder.addBinding().to(ProvisionerHandler.class);
  }
}
