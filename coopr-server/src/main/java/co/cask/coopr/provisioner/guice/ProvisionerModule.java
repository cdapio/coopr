package co.cask.coopr.provisioner.guice;

import co.cask.coopr.provisioner.HttpProvisionerRequestService;
import co.cask.coopr.provisioner.ProvisionerRequestService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Guice module for injection of provisioner related classes.
 */
public class ProvisionerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ProvisionerRequestService.class).to(HttpProvisionerRequestService.class).in(Scopes.SINGLETON);
  }
}
