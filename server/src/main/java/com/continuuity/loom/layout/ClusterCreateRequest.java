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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * Request to create a cluster, containing values for optional cluster settings.
 */
public class ClusterCreateRequest {
  private final String name;
  private final String description;
  private final String clusterTemplate;
  private final int numMachines;
  private final String provider;
  private final Map<String, String> providerFields;
  private final Set<String> services;
  private final String hardwaretype;
  private final String imagetype;
  private final long initialLeaseDuration;
  private final String dnsSuffix;
  private final JsonObject config;

  public ClusterCreateRequest(String name, String description, String clusterTemplate,
                              int numMachines, String provider, Map<String, String> providerFields,
                              Set<String> services, String hardwareType, String imageType, Long initialLeaseDuration,
                              String dnsSuffix, JsonObject config) {
    // check that the arguments that don't have defaults are not null.
    Preconditions.checkArgument(name != null && !name.isEmpty(), "cluster name must be specified");
    Preconditions.checkArgument(clusterTemplate != null && !clusterTemplate.isEmpty(),
                                "cluster template must be specified");
    Preconditions.checkArgument(numMachines > 0, "cluster size must be greater than 0");
    this.name = name;
    this.description = description == null ? "" : description;
    this.clusterTemplate = clusterTemplate;
    this.numMachines = numMachines;
    this.provider = provider;
    this.providerFields = providerFields == null ? ImmutableMap.<String, String>of() : providerFields;
    this.services = services;
    this.hardwaretype = hardwareType;
    this.imagetype = imageType;
    this.initialLeaseDuration = initialLeaseDuration == null ? -1 : initialLeaseDuration;
    this.dnsSuffix = dnsSuffix;
    this.config = config == null ? new JsonObject() : config;
  }

  /**
   * Get the number of machines that should be used in the cluster.
   *
   * @return Number of machines in the cluster.
   */
  public int getNumMachines() {
    return numMachines;
  }

  /**
   * Get the name of the provider to use for node creation and deletion. If null, the default provider specified in the
   * template should be used.
   *
   * @return Name of the provider to user for node creation and deletion or null if the default from the template should
   *         be used.
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Get the set of service names to place on the cluster. If null, the default services specified in the template
   * should be used.
   *
   * @return Set of services to place on the cluster or null if the defaults from the template should be used.
   */
  public Set<String> getServices() {
    return services;
  }

  /**
   * Get the name of the cluster. Does not have to be unique.
   *
   * @return Name of the cluster.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the description of the cluster.
   *
   * @return Description of the cluster.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the name of the {@link com.continuuity.loom.admin.ClusterTemplate} to use for cluster creation.
   *
   * @return Name of the cluster template to use for cluster creation.
   */
  public String getClusterTemplate() {
    return clusterTemplate;
  }

  /**
   * Get the name of the {@link com.continuuity.loom.admin.HardwareType} to use across the entire cluster or null
   * if the template default should be used.
   *
   * @return Name of the hardware type to use across the entire cluster or null if the template default should be used.
   */
  public String getHardwareType() {
    return hardwaretype;
  }

  /**
   * Get the name of the {@link com.continuuity.loom.admin.ImageType} to use across the entire cluster or null
   * if the template default should be used.
   *
   * @return Name of the image type to use across the entire cluster or null if the template default should be used.
   */
  public String getImageType() {
    return imagetype;
  }

  /**
   * Get the lease duration to use for the cluster, with 0 meaning no lease.
   *
   * @return Lease duration to use for the cluster, with 0 meaning no lease.
   */
  public long getInitialLeaseDuration() {
    return initialLeaseDuration;
  }

  /**
   * Get the DNS suffix to use for hostnames of nodes in the cluster, with null meaning to use the template defaults.
   *
   * @return DNS suffix to use for hostnames of nodes in the cluster, with null meaning to use the template defaults.
   */
  public String getDnsSuffix() {
    return dnsSuffix;
  }

  /**
   * Get provider fields specified by the user at creation time.
   *
   * @return Provider fields specified by the user.
   */
  public Map<String, String> getProviderFields() {
    return providerFields;
  }

  /**
   * Get the configuration to use for the cluster, with null meaning to use the template defaults.
   *
   * @return Configuration to use for the cluster, with null meaning to use the template defaults.
   */
  public JsonObject getConfig() {
    return config;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ClusterCreateRequest)) {
      return false;
    }
    ClusterCreateRequest other = (ClusterCreateRequest) o;
    return Objects.equal(name, other.name) &&
      Objects.equal(description, other.description) &&
      Objects.equal(clusterTemplate, other.clusterTemplate) &&
      Objects.equal(numMachines, other.numMachines) &&
      Objects.equal(provider, other.provider) &&
      Objects.equal(services, other.services) &&
      Objects.equal(hardwaretype, other.hardwaretype) &&
      Objects.equal(imagetype, other.imagetype) &&
      Objects.equal(initialLeaseDuration, other.initialLeaseDuration) &&
      Objects.equal(dnsSuffix, other.dnsSuffix) &&
      Objects.equal(config, other.config);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, clusterTemplate, numMachines, provider, services,
                            hardwaretype, imagetype, initialLeaseDuration, dnsSuffix, config);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("description", description)
      .add("clusterTemplate", clusterTemplate)
      .add("numMachines", numMachines)
      .add("provider", provider)
      .add("services", services)
      .add("hardwareType", hardwaretype)
      .add("imageType", imagetype)
      .add("initialLeaseDuration", initialLeaseDuration)
      .add("dnsSuffix", dnsSuffix)
      .add("config", config)
      .toString();
  }
}
