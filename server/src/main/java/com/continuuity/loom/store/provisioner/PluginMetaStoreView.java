package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.provisioner.plugin.ResourceType;

import java.io.IOException;
import java.util.Set;

/**
 * View of the plugin metadata persistent store for some account.
 */
public interface PluginMetaStoreView {

  /**
   * Get a view of the metadata store for the given resource type.
   *
   * @param type Type of plugin resource that will be accessed
   * @return View of the metadata store for the given account and resource type
   */
  PluginResourceTypeView getResourceTypeView(ResourceType type);

  /**
   * Atomically sync all resources in the account of the given resource types. Sync means that any resource that should
   * be live after a sync (STAGED or ACTIVE state) is made live (ACTIVE state), and any resource that should not be
   * live after a sync (UNSTAGED or INACTIVE state) is made not live (INACTIVE state).
   *
   * @param types Resource types to sync
   * @throws IOException
   */
  void syncResourceTypes(Set<ResourceType> types) throws IOException;
}
