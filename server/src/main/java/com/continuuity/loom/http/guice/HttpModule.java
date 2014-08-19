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
package com.continuuity.loom.http.guice;

import com.continuuity.http.HttpHandler;
import com.continuuity.loom.http.handler.LoomAdminHandler;
import com.continuuity.loom.http.handler.LoomClusterHandler;
import com.continuuity.loom.http.handler.LoomPluginHandler;
import com.continuuity.loom.http.handler.LoomNodeHandler;
import com.continuuity.loom.http.handler.LoomProvisionerHandler;
import com.continuuity.loom.http.handler.LoomRPCHandler;
import com.continuuity.loom.http.handler.LoomStatusHandler;
import com.continuuity.loom.http.handler.LoomSuperadminHandler;
import com.continuuity.loom.http.handler.LoomTaskHandler;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Guice bindings for http related classes.
 */
public class HttpModule extends AbstractModule {

  @Override
  protected void configure() {

    Multibinder<HttpHandler> handlerBinder = Multibinder.newSetBinder(binder(), HttpHandler.class);
    handlerBinder.addBinding().to(LoomAdminHandler.class);
    handlerBinder.addBinding().to(LoomClusterHandler.class);
    handlerBinder.addBinding().to(LoomNodeHandler.class);
    handlerBinder.addBinding().to(LoomTaskHandler.class);
    handlerBinder.addBinding().to(LoomStatusHandler.class);
    handlerBinder.addBinding().to(LoomRPCHandler.class);
    handlerBinder.addBinding().to(LoomSuperadminHandler.class);
    handlerBinder.addBinding().to(LoomProvisionerHandler.class);
    handlerBinder.addBinding().to(LoomPluginHandler.class);
  }
}
