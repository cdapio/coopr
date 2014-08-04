package com.continuuity.loom.provisioner.plugin;

import java.util.Set;

/**
 * Class containing information needed to perform a resource sync, including the collection of resources that should be
 * sent to provisioners, as well as the resource types that should be updated in the meta store.
 */
public class ResourceSync {
  private final ResourceCollection resourceCollection;
  private final Set<ResourceType> resourceTypes;

  public ResourceSync(ResourceCollection resourceCollection, Set<ResourceType> resourceTypes) {
    this.resourceCollection = resourceCollection;
    this.resourceTypes = resourceTypes;
  }

  /**
   * Get the resource collection that should be sent to provisioners.
   *
   * @return Resource collection that should be sent to provisioners
   */
  public ResourceCollection getResourceCollection() {
    return resourceCollection;
  }

  /**
   * Get the types of resources that should be synced in the metadata store
   *
   * @return Types of resources that should be synced in the metadata store
   */
  public Set<ResourceType> getResourceTypes() {
    return resourceTypes;
  }
}
