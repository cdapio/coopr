package co.cask.coopr.management.guice;

import co.cask.coopr.management.ServerStats;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module for binding management related classes.
 */
public class ManagementModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerStats.class).in(Scopes.SINGLETON);
  }
}
