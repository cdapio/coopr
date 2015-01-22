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
package co.cask.coopr.store.entity;

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.PartialTemplate;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Abstract {@link EntityStoreView} that represents entities as json.
 */
public abstract class BaseEntityStoreView implements EntityStoreView {
  private final Gson gson;
  private final Function<byte[], Provider> providerTransform =
    new Function<byte[], Provider>() {
      @Nullable
      @Override
      public Provider apply(@Nullable byte[] input) {
        return deserialize(input, Provider.class);
      }
    };
  private final Function<byte[], HardwareType> hardwareTypeTransform =
    new Function<byte[], HardwareType>() {
      @Nullable
      @Override
      public HardwareType apply(@Nullable byte[] input) {
        return deserialize(input, HardwareType.class);
      }
    };
  private final Function<byte[], ImageType> imageTypeTransform =
    new Function<byte[], ImageType>() {
      @Nullable
      @Override
      public ImageType apply(@Nullable byte[] input) {
        return deserialize(input, ImageType.class);
      }
  };
  private final Function<byte[], Service> serviceTransform =
    new Function<byte[], Service>() {
      @Nullable
      @Override
      public Service apply(@Nullable byte[] input) {
        return deserialize(input, Service.class);
      }
    };
  private final Function<byte[], ClusterTemplate> clusterTemplateTransform =
    new Function<byte[], ClusterTemplate>() {
      @Nullable
      @Override
      public ClusterTemplate apply(@Nullable byte[] input) {
        return deserialize(input, ClusterTemplate.class);
      }
    };
  private final Function<byte[], PartialTemplate> partialTemplateTransform =
    new Function<byte[], PartialTemplate>() {
      @Nullable
      @Override
      public PartialTemplate apply(@Nullable byte[] input) {
        return deserialize(input, PartialTemplate.class);
      }
    };
  private final Function<byte[], ProviderType> providerTypeTransform =
    new Function<byte[], ProviderType>() {
      @Nullable
      @Override
      public ProviderType apply(@Nullable byte[] input) {
        return deserialize(input, ProviderType.class);
      }
    };
  private final Function<byte[], AutomatorType> automatorTypeTransform =
    new Function<byte[], AutomatorType>() {
      @Nullable
      @Override
      public AutomatorType apply(@Nullable byte[] input) {
        return deserialize(input, AutomatorType.class);
      }
    };

  /**
   * Types of entities.
   */
  protected enum EntityType {
    PROVIDER("provider"),
    HARDWARE_TYPE("hardwareType"),
    IMAGE_TYPE("imageType"),
    SERVICE("service"),
    CLUSTER_TEMPLATE("clusterTemplate"),
    PARTIAL_TEMPLATE("partialTemplate"),
    PROVIDER_TYPE("providerType"),
    AUTOMATOR_TYPE("automatorType");
    
    private final String id;

    EntityType(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public String getBlobColumn() {
      return id;
    }

    public String getTableName() {
      return id + "s";
    }
  }

  protected BaseEntityStoreView(Gson gson) {
    this.gson = gson;
  }

  @Override
  public Provider getProvider(String providerName) throws IOException {
    return get(EntityType.PROVIDER, providerName, Constants.FIND_MAX_VERSION, providerTransform);
  }

  @Override
  public Provider getProvider(String providerName, int version) throws IOException {
    return get(EntityType.PROVIDER, providerName, version, providerTransform);
  }

  @Override
  public Collection<Provider> getAllProviders() throws IOException {
    return getAllLatestEntities(EntityType.PROVIDER, providerTransform);
  }

  @Override
  public void writeProvider(Provider provider) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.PROVIDER, provider.getName());
    provider.setVersion(version);
    writeEntity(EntityType.PROVIDER, provider.getName(), version,
                serialize(provider, Provider.class));
    provider.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteProvider(String providerName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PROVIDER, providerName);
  }

  @Override
  public void deleteProvider(String providerName, int version) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PROVIDER, providerName, version);
  }

  @Override
  public HardwareType getHardwareType(String hardwareTypeName) throws IOException {
    return get(EntityType.HARDWARE_TYPE, hardwareTypeName, Constants.FIND_MAX_VERSION, hardwareTypeTransform);
  }

  @Override
  public HardwareType getHardwareType(String hardwareTypeName, int version) throws IOException {
    return get(EntityType.HARDWARE_TYPE, hardwareTypeName, version, hardwareTypeTransform);
  }

  @Override
  public Collection<HardwareType> getAllHardwareTypes() throws IOException {
    return getAllLatestEntities(EntityType.HARDWARE_TYPE, hardwareTypeTransform);
  }

  @Override
  public void writeHardwareType(HardwareType hardwareType) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.HARDWARE_TYPE, hardwareType.getName());
    hardwareType.setVersion(version);
    writeEntity(EntityType.HARDWARE_TYPE, hardwareType.getName(), version,
                serialize(hardwareType, HardwareType.class));
    hardwareType.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteHardwareType(String hardwareTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.HARDWARE_TYPE, hardwareTypeName);
  }

  @Override
  public void deleteHardwareType(String hardwareTypeName, int version) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.HARDWARE_TYPE, hardwareTypeName, version);
  }

  @Override
  public ImageType getImageType(String imageTypeName) throws IOException {
    return get(EntityType.IMAGE_TYPE, imageTypeName, Constants.FIND_MAX_VERSION, imageTypeTransform);
  }

  @Override
  public ImageType getImageType(String imageTypeName, int version) throws IOException {
    return get(EntityType.IMAGE_TYPE, imageTypeName, version, imageTypeTransform);
  }

  @Override
  public Collection<ImageType> getAllImageTypes() throws IOException {
    return getAllLatestEntities(EntityType.IMAGE_TYPE, imageTypeTransform);
  }

  @Override
  public void writeImageType(ImageType imageType) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.IMAGE_TYPE, imageType.getName());
    imageType.setVersion(version);
    writeEntity(EntityType.IMAGE_TYPE, imageType.getName(), version,
                serialize(imageType, ImageType.class));
    imageType.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteImageType(String imageTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.IMAGE_TYPE, imageTypeName);
  }

  @Override
  public void deleteImageType(String imageTypeName, int version) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.IMAGE_TYPE, imageTypeName, version);
  }

  @Override
  public Service getService(String serviceName) throws IOException {
    return get(EntityType.SERVICE, serviceName, Constants.FIND_MAX_VERSION, serviceTransform);
  }

  @Override
  public Service getService(String serviceName, int version) throws IOException {
    return get(EntityType.SERVICE, serviceName, version, serviceTransform);
  }

  @Override
  public Collection<Service> getAllServices() throws IOException {
    return getAllLatestEntities(EntityType.SERVICE, serviceTransform);
  }

  @Override
  public void writeService(Service service) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.SERVICE, service.getName());
    service.setVersion(version);
    writeEntity(EntityType.SERVICE, service.getName(), version,
                serialize(service, Service.class));
    service.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteService(String serviceName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.SERVICE, serviceName);
  }

  @Override
  public void deleteService(String serviceName, int version) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.SERVICE, serviceName, version);
  }

  @Override
  public ClusterTemplate getClusterTemplate(String clusterTemplateName) throws IOException {
    return get(EntityType.CLUSTER_TEMPLATE, clusterTemplateName, Constants.FIND_MAX_VERSION, clusterTemplateTransform);
  }

  @Override
  public ClusterTemplate getClusterTemplate(String clusterTemplateName, int version) throws IOException {
    return get(EntityType.CLUSTER_TEMPLATE, clusterTemplateName, version, clusterTemplateTransform);
  }

  @Override
  public Collection<ClusterTemplate> getAllClusterTemplates() throws IOException {
    return getAllLatestEntities(EntityType.CLUSTER_TEMPLATE, clusterTemplateTransform);
  }

  @Override
  public void writeClusterTemplate(ClusterTemplate clusterTemplate) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.CLUSTER_TEMPLATE, clusterTemplate.getName());
    clusterTemplate.setVersion(version);
    writeEntity(EntityType.CLUSTER_TEMPLATE, clusterTemplate.getName(), version,
                serialize(clusterTemplate, ClusterTemplate.class));
    clusterTemplate.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteClusterTemplate(String clusterTemplateName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.CLUSTER_TEMPLATE, clusterTemplateName);
  }

  @Override
  public void deleteClusterTemplate(String clusterTemplateName, int version)
    throws IOException, IllegalAccessException {
    deleteEntity(EntityType.CLUSTER_TEMPLATE, clusterTemplateName, version);
  }

  public PartialTemplate getPartialTemplate(String partialTemplateName) throws IOException {
    return get(EntityType.PARTIAL_TEMPLATE, partialTemplateName, Constants.FIND_MAX_VERSION, partialTemplateTransform);
  }

  public PartialTemplate getPartialTemplate(String partialTemplateName, int version) throws IOException {
    return get(EntityType.PARTIAL_TEMPLATE, partialTemplateName, version, partialTemplateTransform);
  }

  @Override
  public Collection<PartialTemplate> getAllPartialTemplates() throws IOException {
    return getAllLatestEntities(EntityType.PARTIAL_TEMPLATE, partialTemplateTransform);
  }

  @Override
  public void writePartialTemplate(PartialTemplate partialTemplate) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.PARTIAL_TEMPLATE, partialTemplate.getName());
    partialTemplate.setVersion(version);
    writeEntity(EntityType.PARTIAL_TEMPLATE, partialTemplate.getName(), version,
                serialize(partialTemplate, PartialTemplate.class));
    partialTemplate.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deletePartialTemplate(String partialTemplateName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PARTIAL_TEMPLATE, partialTemplateName);
  }

  @Override
  public void deletePartialTemplate(String partialTemplateName, int version)
    throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PARTIAL_TEMPLATE, partialTemplateName, version);
  }

  @Override
  public ProviderType getProviderType(String providerTypeName) throws IOException {
    return get(EntityType.PROVIDER_TYPE, providerTypeName, Constants.FIND_MAX_VERSION, providerTypeTransform);
  }

  @Override
  public ProviderType getProviderType(String providerTypeName, int version) throws IOException {
    return get(EntityType.PROVIDER_TYPE, providerTypeName, version, providerTypeTransform);
  }

  @Override
  public Collection<ProviderType> getAllProviderTypes() throws IOException {
    return getAllLatestEntities(EntityType.PROVIDER_TYPE, providerTypeTransform);
  }

  @Override
  public void writeProviderType(ProviderType providerType) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.PROVIDER_TYPE, providerType.getName());
    providerType.setVersion(version);
    writeEntity(EntityType.PROVIDER_TYPE, providerType.getName(), version,
                serialize(providerType, ProviderType.class));
    providerType.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteProviderType(String providerTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PROVIDER_TYPE, providerTypeName);
  }

  @Override
  public void deleteProviderType(String providerTypeName, int version) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PROVIDER_TYPE, providerTypeName, version);
  }

  @Override
  public AutomatorType getAutomatorType(String automatorTypeName) throws IOException {
    return get(EntityType.AUTOMATOR_TYPE, automatorTypeName, Constants.FIND_MAX_VERSION, automatorTypeTransform);
  }

  @Override
  public AutomatorType getAutomatorType(String automatorTypeName, int version) throws IOException {
    return get(EntityType.AUTOMATOR_TYPE, automatorTypeName, version, automatorTypeTransform);
  }

  @Override
  public Collection<AutomatorType> getAllAutomatorTypes() throws IOException {
    return getAllLatestEntities(EntityType.AUTOMATOR_TYPE, automatorTypeTransform);
  }

  @Override
  public void writeAutomatorType(AutomatorType automatorType) throws IOException, IllegalAccessException {
    int version = getVersion(EntityType.AUTOMATOR_TYPE, automatorType.getName());
    automatorType.setVersion(version);
    writeEntity(EntityType.AUTOMATOR_TYPE, automatorType.getName(), version,
                serialize(automatorType, AutomatorType.class));
    automatorType.setVersion(Constants.DEFAULT_VERSION);
  }

  @Override
  public void deleteAutomatorType(String automatorTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.AUTOMATOR_TYPE, automatorTypeName);
  }

  @Override
  public void deleteAutomatorType(String automatorTypeName, int version) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.AUTOMATOR_TYPE, automatorTypeName, version);
  }

  private <T> T get(EntityType entityType, String entityName, int entityVersion,
                    Function<byte[], T> transform) throws IOException {
    byte[] data = getEntity(entityType, entityName, entityVersion);
    return (data == null) ? null : transform.apply(data);
  }

  private <T> byte[] serialize(T object, Type type) {
    return gson.toJson(object, type).getBytes(Charsets.UTF_8);
  }

  private <T> T deserialize(byte[] bytes, Type type) {
    return gson.fromJson(new String(bytes, Charsets.UTF_8), type);
  }

  /**
   * Retrieves highest version of entity.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   */
  protected abstract int getVersion(EntityType entityType, String entityName) throws IOException;

  /**
   * Write the specified entity to some persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   * @param version Version of entity.
   * @param data Representation of the entity as bytes.
   */
  protected abstract void writeEntity(EntityType entityType, String entityName, int version, byte[] data)
    throws IOException, IllegalAccessException;

  /**
   * Get the specified entity from some persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   * @param entityVersion Version of entity.
   * @return Entity of given type and name as bytes.
   */
  protected abstract byte[] getEntity(EntityType entityType, String entityName, int entityVersion) throws IOException;

  /**
   * Get all entities of the given type from persistent store.
   *
   * @param entityType Type of entity.
   * @param transform Function used to transform the entity as bytes into a java class.
   * @param <T> Class of entity to get.
   * @return Collection of entities.
   */
  protected abstract <T> Collection<T> getAllLatestEntities(EntityType entityType,
                                                            Function<byte[], T> transform) throws IOException;

  /**
   * Delete all entities of given type and name from persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   */
  protected abstract void deleteEntity(EntityType entityType, String entityName)
    throws IOException, IllegalAccessException;

  /**
   * Delete the entity of given type, name and version from persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   * @param entityVersion Version of entity.
   */
  protected abstract void deleteEntity(EntityType entityType, String entityName, int entityVersion)
    throws IOException, IllegalAccessException;
}
