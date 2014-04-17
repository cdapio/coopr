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
package com.continuuity.loom.layout;

import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.layout.change.ClusterLayoutChange;
import com.continuuity.loom.layout.change.ClusterLayoutTracker;
import com.continuuity.loom.scheduler.task.NodeService;
import com.continuuity.loom.store.EntityStore;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * The solver takes a cluster template, a number of machines, and figures out what services to put on what hardware
 * and images in order to satisfy the cluster constraints.
 *
 * TODO: add ability to grow/shrink, making sure services dont move.
 * TODO: add ability to add/remove services from an existing cluster
 * TODO: refactor into cleaner pieces with pluggable pieces for constraints
 */
public class Solver {
  private static final Logger LOG  = LoggerFactory.getLogger(Solver.class);
  private final EntityStore entityStore;
  private final ClusterLayoutUpdater updater;

  @Inject
  Solver(EntityStore entityStore, ClusterLayoutUpdater updater) {
    this.entityStore = entityStore;
    this.updater = updater;
  }

  public Queue<ClusterLayoutChange> addServiceToCluster(String clusterId, Set<String> servicesToAdd) throws Exception {
    ClusterLayoutTracker tracker = updater.addServicesToCluster(clusterId, servicesToAdd);
    return tracker == null ? null : tracker.getChanges();
  }

  /**
   * Given a {@link Cluster} and {@link ClusterCreateRequest}, return a mapping of node id to {@link Node} describing
   * how the cluster should be laid out. If multiple possible cluster layouts are possible, one will be chosen
   * deterministically.
   *
   * @param cluster Cluster to solve a layout for.
   * @param request Request to create a cluster containing cluster settings to use.
   * @return Mapping of node id to node for all nodes in the cluster.
   * @throws Exception
   */
  public Map<String, Node> solveClusterNodes(Cluster cluster, ClusterCreateRequest request) throws Exception {
    // make sure the template exists
    String clusterTemplateName = request.getClusterTemplate();
    ClusterTemplate template = entityStore.getClusterTemplate(clusterTemplateName);
    if (template == null) {
      throw new IllegalArgumentException("cluster template " + clusterTemplateName + " does not exist.");
    }

    cluster.setClusterTemplate(template);

    // make sure the provider exists
    String providerName = request.getProvider();
    if (providerName == null || providerName.isEmpty()) {
      providerName = template.getClusterDefaults().getProvider();
    }
    Provider provider = entityStore.getProvider(providerName);
    if (provider == null) {
      throw new IllegalArgumentException("provider " + providerName + " does not exist.");
    }

    cluster.setProvider(provider);

    // make sure there are hardware types that can be used
    String requiredHardwareType = request.getHardwareType();
    if (requiredHardwareType == null || requiredHardwareType.isEmpty()) {
      // this can be null too, which means no cluster wide required type
      requiredHardwareType = template.getClusterDefaults().getHardwaretype();
    }
    if (requiredHardwareType != null && requiredHardwareType.isEmpty()) {
      requiredHardwareType = null;
    }
    Map<String, String> hardwareTypeFlavors = getHardwareTypeMap(providerName, template, requiredHardwareType);
    if (hardwareTypeFlavors.isEmpty()) {
      throw new IllegalArgumentException("no hardware types are available to use with template "
                                           + template.getName() + " and provider " + providerName);
    }

    // make sure there are image types that can be used
    String requiredImageType = request.getImageType();
    if (requiredImageType == null || requiredImageType.isEmpty()) {
      // this can be null too, which means no cluster wide required type
      requiredImageType = template.getClusterDefaults().getImagetype();
    }
    if (requiredImageType != null && requiredImageType.isEmpty()) {
      requiredImageType = null;
    }
    Map<String, String> imageTypeMap = getImageTypeMap(providerName, template, requiredImageType);
    if (imageTypeMap.isEmpty()) {
      throw new IllegalArgumentException("no image types are available to use with template "
                                           + template.getName() + " and provider " + providerName);
    }

    // Determine valid lease duration for the cluster. It has to be less than the initial lease duration set in
    // template.
    long initialLeaseDuration = template.getAdministration().getLeaseDuration().getInitial();
    long effectiveRequestLeaseDuration = request.getInitialLeaseDuration() == 0
      ? Long.MAX_VALUE : request.getInitialLeaseDuration();
    long leaseDuration;

    if (request.getInitialLeaseDuration() == -1) {
      leaseDuration = initialLeaseDuration;
    } else if (initialLeaseDuration == 0 || initialLeaseDuration >= effectiveRequestLeaseDuration) {
      leaseDuration = request.getInitialLeaseDuration();
    } else {
      throw new IllegalArgumentException("lease duration cannot be greater than specified in template");
    }

    if (leaseDuration < 0) {
      throw new IllegalArgumentException("invalid lease duration: " + leaseDuration);
    }
    // Lease duration of 0 is forever.
    cluster.setExpireTime(leaseDuration == 0 ? 0 : cluster.getCreateTime() + leaseDuration);

    // make sure the services to place on the cluster are all valid
    Set<String> serviceNames = request.getServices();
    if (serviceNames == null || serviceNames.isEmpty()) {
      serviceNames = template.getClusterDefaults().getServices();
    }
    Set<String> allowedServices = template.getCompatibilities().getServices();
    if (!allowedServices.isEmpty()) {
      Sets.SetView<String> diff = Sets.difference(serviceNames, allowedServices);
      if (diff.size() > 1) {
        String badServices =
          Joiner.on(",").join(Sets.difference(serviceNames, template.getCompatibilities().getServices()));
        throw new IllegalArgumentException(
          "services " + badServices + " are not allowed with template " + clusterTemplateName);
      } else if (diff.size() > 0) {
        throw new IllegalArgumentException(
          "service " + diff.iterator().next() + " is not allowed with template " + clusterTemplateName);
      }
    }

    Map<String, Service> serviceMap = getServiceMap(serviceNames);
    for (Service service : serviceMap.values()) {
      for (String dependency : service.getDependsOn()) {
        if (!serviceNames.contains(dependency)) {
          throw new IllegalArgumentException("service " + service.getName() + " depends on " + dependency
          + ", which is not in the set of cluster services");
        }
      }
    }
    cluster.setServices(serviceNames);

    // TODO: move building of node properties to NodeService or Node or some place more sensible
    String dnsSuffix = request.getDnsSuffix();
    if (dnsSuffix == null || dnsSuffix.isEmpty()) {
      dnsSuffix = template.getClusterDefaults().getDnsSuffix();
    }

    Map<String, Node> nodes =
      solveConstraints(cluster.getId(), template, request.getName(), request.getNumMachines(), hardwareTypeFlavors,
                       imageTypeMap, serviceNames, serviceMap, dnsSuffix);

    // Update cluster object
    // TODO: this should happen outside Solver.
    cluster.setNodes(nodes == null ? ImmutableSet.<String>of() : nodes.keySet());

    return nodes;
  }

  // get a mapping of service name to service object for fast lookup later. Also check that each service actually
  // exists.
  private Map<String, Service> getServiceMap(Set<String> serviceNames) throws Exception {
    Map<String, Service> map = Maps.newHashMap();
    for (String serviceName : serviceNames) {
      Service service = entityStore.getService(serviceName);
      if (service == null) {
        throw new IllegalArgumentException("service " + serviceName + " does not exist");
      }
      map.put(serviceName, entityStore.getService(serviceName));
    }
    return map;
  }

  // get a mapping of hardware type name to flavor that can be used with the given provider and cluster template.
  private Map<String, String> getHardwareTypeMap(String providerName, ClusterTemplate template,
                                                 String requiredHardwareType) throws Exception {
    Map<String, String> flavorMap = Maps.newHashMap();

    Set<String> allowedHardwareTypes = template.getCompatibilities().getHardwaretypes();
    if (requiredHardwareType != null) {
      addProviderFlavor(flavorMap, providerName, allowedHardwareTypes,
                        entityStore.getHardwareType(requiredHardwareType));
      return flavorMap;
    }

    for (HardwareType hardwareType : entityStore.getAllHardwareTypes()) {
      addProviderFlavor(flavorMap, providerName, allowedHardwareTypes, hardwareType);
    }

    return flavorMap;
  }

  // Adds an hardwareType name -> flavor entry to the map given a hardware type, the name of the provider, and
  // a whitelist of allowed hardware types.  If the flavor does not exist for the provider and whitelisted
  // hardware type, nothing is added.
  private void addProviderFlavor(Map<String, String> map, String providerName,
                                 Set<String> allowedTypes, HardwareType hardwareType) {
    if (hardwareType != null) {
      Map<String, Map<String, String>> providerMap = hardwareType.getProviderMap();
      String name = hardwareType.getName();
      // empty allowed types means all types are allowed
      if ((allowedTypes.isEmpty() || allowedTypes.contains(name)) && providerMap.containsKey(providerName)) {
        String flavor = providerMap.get(providerName).get("flavor");
        if (flavor != null) {
          map.put(name, flavor);
        }
      }
    }
  }

  // get a mapping of image type name to image that can be used with the given provider
  private Map<String, String> getImageTypeMap(String providerName, ClusterTemplate template,
                                              String requiredImageType) throws Exception {
    Map<String, String> imageMap = Maps.newHashMap();

    Set<String> allowedImageTypes = template.getCompatibilities().getImagetypes();
    if (requiredImageType != null) {
      addProviderImage(imageMap, providerName, allowedImageTypes, entityStore.getImageType(requiredImageType));
      return imageMap;
    }

    for (ImageType imageType : entityStore.getAllImageTypes()) {
      addProviderImage(imageMap, providerName, allowedImageTypes, imageType);
    }

    return imageMap;
  }

  private void addProviderImage(Map<String, String> map, String providerName,
                                Set<String> allowedTypes, ImageType imageType) {
    if (imageType != null) {
      Map<String, Map<String, String>> providerMap = imageType.getProviderMap();
      String name = imageType.getName();
      // empty allowed types means all types are allowed
      if ((allowedTypes.isEmpty() || allowedTypes.contains(name)) && providerMap.containsKey(providerName)) {
        String image = providerMap.get(providerName).get("image");
        if (image != null) {
          map.put(name, image);
        }
      }
    }
  }

  // solves for a valid cluster layout based on the constraints. First finds all possible node layouts that can be
  // used in the cluster based on the services that need to be on the cluster and constraints. Then searches for a
  // valid number of each node layout based on the constraints.
  static Map<String, Node> solveConstraints(String clusterId, ClusterTemplate clusterTemplate, String clusterName,
                                            int numMachines,
                                            Map<String, String> hardwareTypeMap,
                                            Map<String, String> imageTypeMap,
                                            Set<String> serviceNames,
                                            Map<String, Service> serviceMap,
                                            String dnsSuffix) throws Exception {
    NodeLayoutGenerator nodeLayoutGenerator =
      new NodeLayoutGenerator(clusterTemplate, serviceNames, hardwareTypeMap.keySet(), imageTypeMap.keySet());

    // We need to deterministically choose the same cluster.  Nodelayouts earlier in the traversal order are
    // preferred.
    List<NodeLayout> traversalOrder = nodeLayoutGenerator.generateNodeLayoutPreferences();

    long start = System.nanoTime();
    ClusterLayoutFinder layoutFinder =
      new ClusterLayoutFinder(traversalOrder, clusterTemplate, serviceNames, numMachines);
    int[] clusterlayout = layoutFinder.findValidNodeCounts();
    long dur = (System.nanoTime() - start) / 1000000;
    LOG.debug("took {} ms to find cluster layout", dur);

    if (clusterlayout == null) {
      return null;
    }

    Map<String, Node> clusterNodes = Maps.newHashMap();
    int nodeNum = 1000;
    for (int i = 0; i < clusterlayout.length; i++) {
      NodeLayout nodeLayout = traversalOrder.get(i);
      for (int j = 0; j < clusterlayout[i]; j++) {
        String nodeId = UUID.randomUUID().toString();
        Set<Service> nodeServices = Sets.newHashSet();
        for (String serviceName : nodeLayout.getServiceNames()) {
          nodeServices.add(serviceMap.get(serviceName));
        }
        String hardwaretype = nodeLayout.getHardwareTypeName();
        String imagetype = nodeLayout.getImageTypeName();
        Map<String, String> nodeProperties = Maps.newHashMap();
        // TODO: these should be proper fields and logic for populating node properties should not be in the solver.
        nodeProperties.put(Node.Properties.HARDWARETYPE.name().toLowerCase(), hardwaretype);
        nodeProperties.put(Node.Properties.IMAGETYPE.name().toLowerCase(), imagetype);
        nodeProperties.put(Node.Properties.FLAVOR.name().toLowerCase(), hardwareTypeMap.get(hardwaretype));
        nodeProperties.put(Node.Properties.IMAGE.name().toLowerCase(), imageTypeMap.get(imagetype));
        // used for macro expansion and when expanding service numbers.  For every new node added to the cluster,
        // the nodenum should be greater than any other nodenum in the cluster.
        nodeProperties.put(Node.Properties.NODENUM.name().toLowerCase(), String.valueOf(nodeNum));
        nodeProperties.put(Node.Properties.HOSTNAME.name().toLowerCase(),
                           NodeService.createHostname(clusterName, clusterId, nodeNum, dnsSuffix));
        nodeNum++;
        clusterNodes.put(nodeId, new Node(nodeId, clusterId, nodeServices, nodeProperties));
      }
    }
    return clusterNodes;
  }
}
