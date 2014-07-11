package com.continuuity.loom.http.guice;

import com.continuuity.http.HttpHandler;
import com.continuuity.loom.http.handler.LoomAdminHandler;
import com.continuuity.loom.http.handler.LoomClusterHandler;
import com.continuuity.loom.http.handler.LoomRPCHandler;
import com.continuuity.loom.http.handler.LoomStatusHandler;
import com.continuuity.loom.http.handler.LoomTaskHandler;
import com.continuuity.loom.http.handler.LoomTenantHandler;
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
    handlerBinder.addBinding().to(LoomTaskHandler.class);
    handlerBinder.addBinding().to(LoomStatusHandler.class);
    handlerBinder.addBinding().to(LoomRPCHandler.class);
    handlerBinder.addBinding().to(LoomTenantHandler.class);
  }
}
