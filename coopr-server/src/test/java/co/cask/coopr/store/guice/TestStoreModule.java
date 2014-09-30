package co.cask.coopr.store.guice;

import co.cask.coopr.store.credential.CredentialStore;
import co.cask.coopr.store.credential.InProcessCredentialStore;
import co.cask.coopr.store.provisioner.MemoryPluginStore;
import co.cask.coopr.store.provisioner.PluginStore;
import com.google.inject.Scopes;

/**
 *
 */
public class TestStoreModule extends AbstractStoreModule {

  @Override
  void bindPluginStore() {
    bind(PluginStore.class).to(MemoryPluginStore.class).in(Scopes.SINGLETON);
    bind(CredentialStore.class).to(InProcessCredentialStore.class).in(Scopes.SINGLETON);
    bind(InProcessCredentialStore.class).in(Scopes.SINGLETON);
    bind(MemoryPluginStore.class).in(Scopes.SINGLETON);
  }
}
