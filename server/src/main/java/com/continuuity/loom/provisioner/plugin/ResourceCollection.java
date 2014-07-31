package com.continuuity.loom.provisioner.plugin;

import com.continuuity.loom.admin.ResourceTypeFormat;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Collection of resources for a specific tenant.  This is sent to provisioners to let them know what resources
 * are available for each plugin.
 */
public class ResourceCollection {
  // plugin name: {
  //   resource type: {
  //     format: file | archive
  //     active: [ {name, version}, {name, version}, {name, version}, ... ]
  //   }
  // }
  private final Map<String, Map<String, ActiveResourceList>> automatortypes;
  private final Map<String, Map<String, ActiveResourceList>> providertypes;
  private final Set<ResourceType> resourceTypes;

  public ResourceCollection() {
    this.automatortypes = Maps.newHashMap();
    this.providertypes = Maps.newHashMap();
    this.resourceTypes = Sets.newHashSet();
  }

  /**
   * Add resources of the given type and format with the given metadata.
   *
   * @param type Type of resource
   * @param format Format of resource
   * @param metas Collection of resource metadata to add
   */
  public void addResources(ResourceType type, ResourceTypeFormat format, Collection<ResourceMeta> metas) {
    for (ResourceMeta meta : metas) {
      addResource(type, format, meta);
      resourceTypes.add(type);
    }
  }

  /**
   * Get an immutable set of all resource types in the collection.
   *
   * @return Immutable set of all resource types in the collection
   */
  public Set<ResourceType> getResourceTypes() {
    return ImmutableSet.copyOf(resourceTypes);
  }

  private void addResource(ResourceType type, ResourceTypeFormat format, ResourceMeta meta) {
    PluginType pluginType = type.getPluginType();
    if (pluginType == PluginType.AUTOMATOR) {
      add(automatortypes, type, format, meta);
    } else if (pluginType == PluginType.PROVIDER) {
      add(providertypes, type, format, meta);
    }
  }

  private void add(Map<String, Map<String, ActiveResourceList>> pluginTypeResources,
                   ResourceType type, ResourceTypeFormat format, ResourceMeta meta) {
    /*
     * this is a map of resource type to metadata of resources that are slated to be live.
     * for example:
     * cookbooks -> {
     *   format: archive,
     *   active: [
     *     { name: hadoop, version: 9},
     *     { name: mysql, version: 5}
     *   ]
     * },
     * databags -> {
     *   format: archive,
     *   active: [
     *     ...
     *   ]
     * }
     */
    Map<String, ActiveResourceList> resourceTypeCollection = pluginTypeResources.get(type.getPluginName());

    // if this is the first metadata for the plugin name, i.e. first time a resource for plugin "chef-solo" was added,
    // create the necessary objects
    if (resourceTypeCollection == null) {
      resourceTypeCollection = Maps.newHashMap();
      automatortypes.put(type.getPluginName(), resourceTypeCollection);
    }

    /*
     * this is the list of all resources that are slated to be active.
     * for example:
     * {
     *   format: archive,
     *   active: [
     *     { name: hadoop, version: 9},
     *     { name: mysql, version: 5}
     *   ]
     * }
     */
    ActiveResourceList activeResourceList = resourceTypeCollection.get(type.getTypeName());

    // if this is the first resource of the given resource type, i.e. first time a cookbook was added
    // for the "chef-solo" plugin, create the necessary objects
    if (activeResourceList == null) {
      activeResourceList = new ActiveResourceList(format);
      resourceTypeCollection.put(type.getTypeName(), activeResourceList);
    }

    // add the metadata to the list, i.e. add { name: hadoop, version: 9 } to the list of resources slated to be active
    activeResourceList.addMeta(meta);
  }
}
