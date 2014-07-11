package com.continuuity.loom.management.guice;

import com.continuuity.loom.management.LoomStats;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module for binding management related classes.
 */
public class ManagementModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(LoomStats.class).in(Scopes.SINGLETON);
  }
}
