/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.http.request;

import co.cask.coopr.common.utils.StringUtils;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * Request to create a cluster, containing values for optional cluster settings.
 */
public class ClusterCreateRequest extends ClusterOperationRequest {
  private final String name;
  private final String description;
  private final String clusterTemplate;
  private final int numMachines;
  private final String provider;
  private final Set<String> services;
  private final String hardwaretype;
  private final String imagetype;
  private final long initialLeaseDuration;
  private final String dnsSuffix;
  private final JsonObject config;

  private ClusterCreateRequest(String name, String description, String clusterTemplate,
                               Integer numMachines, String provider, Map<String, Object> providerFields,
                               Set<String> services, String hardwareType, String imageType, Long initialLeaseDuration,
                               String dnsSuffix, JsonObject config) {
    super(providerFields);
    // check that the arguments that don't have defaults are not null.
    Preconditions.checkArgument(name != null && !name.isEmpty(), "cluster name must be specified");
    Preconditions.checkArgument(clusterTemplate != null && !clusterTemplate.isEmpty(),
                                "cluster template must be specified");
    Preconditions.checkArgument(numMachines != null && numMachines > 0, "cluster size must be greater than 0");
    Preconditions.checkArgument(dnsSuffix == null || StringUtils.isValidDNSSuffix(dnsSuffix),
                                dnsSuffix + " is an invalid DNS suffix.");
    this.name = name;
    this.description = description == null ? "" : description;
    this.clusterTemplate = clusterTemplate;
    this.numMachines = numMachines;
    this.provider = provider;
    this.services = services;
    this.hardwaretype = hardwareType;
    this.imagetype = imageType;
    this.initialLeaseDuration = initialLeaseDuration == null ? -1 : initialLeaseDuration;
    this.dnsSuffix = dnsSuffix;
    this.config = config;
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
   * Get the name of the {@link co.cask.coopr.spec.template.ClusterTemplate} to use for cluster creation.
   *
   * @return Name of the cluster template to use for cluster creation.
   */
  public String getClusterTemplate() {
    return clusterTemplate;
  }

  /**
   * Get the name of the {@link co.cask.coopr.spec.HardwareType} to use across the entire cluster or null
   * if the template default should be used.
   *
   * @return Name of the hardware type to use across the entire cluster or null if the template default should be used.
   */
  public String getHardwareType() {
    return hardwaretype;
  }

  /**
   * Get the name of the {@link co.cask.coopr.spec.ImageType} to use across the entire cluster or null
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
   * Get the configuration to use for the cluster, with null meaning to use the template defaults.
   *
   * @return Configuration to use for the cluster, with null meaning to use the template defaults.
   */
  public JsonObject getConfig() {
    return config;
  }

  /**
   * Get a builder for creating cluster create requests.
   *
   * @return Builder for creating cluster create requests.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating cluster create requests.
   */
  public static class Builder {
    private String name;
    private String description;
    private String clusterTemplateName;
    private Integer numMachines;
    private String providerName;
    private Set<String> serviceNames;
    private String hardwareTypeName;
    private String imageTypeName;
    private Long initialLeaseDuration;
    private String dnsSuffix;
    private JsonObject config;
    private Map<String, Object> providerFields;

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setClusterTemplateName(String clusterTemplateName) {
      this.clusterTemplateName = clusterTemplateName;
      return this;
    }

    public Builder setNumMachines(Integer numMachines) {
      this.numMachines = numMachines;
      return this;
    }

    public Builder setProviderName(String providerName) {
      this.providerName = providerName;
      return this;
    }

    public Builder setServiceNames(Set<String> serviceNames) {
      this.serviceNames = serviceNames;
      return this;
    }

    public Builder setHardwareTypeName(String hardwareTypeName) {
      this.hardwareTypeName = hardwareTypeName;
      return this;
    }

    public Builder setImageTypeName(String imageTypeName) {
      this.imageTypeName = imageTypeName;
      return this;
    }

    public Builder setInitialLeaseDuration(Long initialLeaseDuration) {
      this.initialLeaseDuration = initialLeaseDuration;
      return this;
    }

    public Builder setDNSSuffix(String dnsSuffix) {
      this.dnsSuffix = dnsSuffix;
      return this;
    }

    public Builder setConfig(JsonObject config) {
      this.config = config;
      return this;
    }

    public Builder setProviderFields(Map<String, Object> providerFields) {
      this.providerFields = providerFields;
      return this;
    }

    public ClusterCreateRequest build() {
      return new ClusterCreateRequest(name, description, clusterTemplateName, numMachines, providerName,
                                      providerFields, serviceNames, hardwareTypeName, imageTypeName,
                                      initialLeaseDuration, dnsSuffix, config);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterCreateRequest other = (ClusterCreateRequest) o;
    return super.equals(other) &&
      Objects.equal(name, other.name) &&
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
    return Objects.hashCode(super.hashCode(), name, description, clusterTemplate, numMachines, provider,
                            services, hardwaretype, imagetype, initialLeaseDuration, dnsSuffix, config);
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
