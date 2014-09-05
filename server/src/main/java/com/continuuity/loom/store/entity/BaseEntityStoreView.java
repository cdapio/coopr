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
package com.continuuity.loom.store.entity;

import com.continuuity.loom.spec.HardwareType;
import com.continuuity.loom.spec.ImageType;
import com.continuuity.loom.spec.Provider;
import com.continuuity.loom.spec.plugin.AutomatorType;
import com.continuuity.loom.spec.plugin.ProviderType;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.spec.template.ClusterTemplate;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.gson.Gson;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

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
    PROVIDER_TYPE("providerType"),
    AUTOMATOR_TYPE("automatorType");
    private final String id;

    EntityType(String id) {
      this.id = id;
    }

    String getId() {
      return id;
    }
  }

  protected BaseEntityStoreView(Gson gson) {
    this.gson = gson;
  }

  @Override
  public Provider getProvider(String providerName) throws IOException {
    return get(EntityType.PROVIDER, providerName, providerTransform);
  }

  @Override
  public Collection<Provider> getAllProviders() throws IOException {
    return getAllEntities(EntityType.PROVIDER, providerTransform);
  }

  @Override
  public void writeProvider(Provider provider) throws IOException, IllegalAccessException {
    writeEntity(EntityType.PROVIDER, provider.getName(), serialize(provider, Provider.class));
  }

  @Override
  public void deleteProvider(String providerName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PROVIDER, providerName);
  }

  @Override
  public HardwareType getHardwareType(String hardwareTypeName) throws IOException {
    return get(EntityType.HARDWARE_TYPE, hardwareTypeName, hardwareTypeTransform);
  }

  @Override
  public Collection<HardwareType> getAllHardwareTypes() throws IOException {
    return getAllEntities(EntityType.HARDWARE_TYPE, hardwareTypeTransform);
  }

  @Override
  public void writeHardwareType(HardwareType hardwareType) throws IOException, IllegalAccessException {
    writeEntity(EntityType.HARDWARE_TYPE, hardwareType.getName(), serialize(hardwareType, HardwareType.class));
  }

  @Override
  public void deleteHardwareType(String hardwareTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.HARDWARE_TYPE, hardwareTypeName);
  }

  @Override
  public ImageType getImageType(String imageTypeName) throws IOException {
    return get(EntityType.IMAGE_TYPE, imageTypeName, imageTypeTransform);
  }

  @Override
  public Collection<ImageType> getAllImageTypes() throws IOException {
    return getAllEntities(EntityType.IMAGE_TYPE, imageTypeTransform);
  }

  @Override
  public void writeImageType(ImageType imageType) throws IOException, IllegalAccessException {
    writeEntity(EntityType.IMAGE_TYPE, imageType.getName(), serialize(imageType, ImageType.class));
  }

  @Override
  public void deleteImageType(String imageTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.IMAGE_TYPE, imageTypeName);
  }

  @Override
  public Service getService(String serviceName) throws IOException {
    return get(EntityType.SERVICE, serviceName, serviceTransform);
  }

  @Override
  public Collection<Service> getAllServices() throws IOException {
    return getAllEntities(EntityType.SERVICE, serviceTransform);
  }

  @Override
  public void writeService(Service service) throws IOException, IllegalAccessException {
    writeEntity(EntityType.SERVICE, service.getName(), serialize(service, Service.class));
  }

  @Override
  public void deleteService(String serviceName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.SERVICE, serviceName);
  }

  @Override
  public ClusterTemplate getClusterTemplate(String clusterTemplateName) throws IOException {
    return get(EntityType.CLUSTER_TEMPLATE, clusterTemplateName, clusterTemplateTransform);
  }

  @Override
  public Collection<ClusterTemplate> getAllClusterTemplates() throws IOException {
    return getAllEntities(EntityType.CLUSTER_TEMPLATE, clusterTemplateTransform);
  }

  @Override
  public void writeClusterTemplate(ClusterTemplate clusterTemplate) throws IOException, IllegalAccessException {
    writeEntity(EntityType.CLUSTER_TEMPLATE, clusterTemplate.getName(),
                serialize(clusterTemplate, ClusterTemplate.class));
  }

  @Override
  public void deleteClusterTemplate(String clusterTemplateName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.CLUSTER_TEMPLATE, clusterTemplateName);
  }

  @Override
  public ProviderType getProviderType(String providerTypeName) throws IOException {
    return get(EntityType.PROVIDER_TYPE, providerTypeName, providerTypeTransform);
  }

  @Override
  public Collection<ProviderType> getAllProviderTypes() throws IOException {
    return getAllEntities(EntityType.PROVIDER_TYPE, providerTypeTransform);
  }

  @Override
  public void writeProviderType(ProviderType providerType) throws IOException, IllegalAccessException {
    writeEntity(EntityType.PROVIDER_TYPE, providerType.getName(), serialize(providerType, ProviderType.class));
  }

  @Override
  public void deleteProviderType(String providerTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.PROVIDER_TYPE, providerTypeName);
  }

  @Override
  public AutomatorType getAutomatorType(String automatorTypeName) throws IOException {
    return get(EntityType.AUTOMATOR_TYPE, automatorTypeName, automatorTypeTransform);
  }

  @Override
  public Collection<AutomatorType> getAllAutomatorTypes() throws IOException {
    return getAllEntities(EntityType.AUTOMATOR_TYPE, automatorTypeTransform);
  }

  @Override
  public void writeAutomatorType(AutomatorType automatorType) throws IOException, IllegalAccessException {
    writeEntity(EntityType.AUTOMATOR_TYPE, automatorType.getName(),
                serialize(automatorType, AutomatorType.class));
  }

  @Override
  public void deleteAutomatorType(String automatorTypeName) throws IOException, IllegalAccessException {
    deleteEntity(EntityType.AUTOMATOR_TYPE, automatorTypeName);
  }

  private <T> T get(EntityType entityType, String entityName, Function<byte[], T> transform) throws IOException {
    byte[] data = getEntity(entityType, entityName);
    return (data == null) ? null : transform.apply(data);
  }

  private <T> byte[] serialize(T object, Type type) {
    return gson.toJson(object, type).getBytes(Charsets.UTF_8);
  }

  private <T> T deserialize(byte[] bytes, Type type) {
    return gson.fromJson(new String(bytes, Charsets.UTF_8), type);
  }

  /**
   * Write the specified entity to some persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   * @param data Representation of the entity as bytes.
   */
  protected abstract void writeEntity(EntityType entityType, String entityName, byte[] data)
    throws IOException, IllegalAccessException;

  /**
   * Get the specified entity from some persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   * @return Entity of given type and name as bytes.
   */
  protected abstract byte[] getEntity(EntityType entityType, String entityName) throws IOException;

  /**
   * Get all entities of the given type from persistent store.
   *
   * @param entityType Type of entity.
   * @param transform Function used to transform the entity as bytes into a java class.
   * @param <T> Class of entity to get.
   * @return Collection of entities.
   */
  protected abstract <T> Collection<T> getAllEntities(EntityType entityType,
                                                      Function<byte[], T> transform) throws IOException;

  /**
   * Delete the entity of given type and name from persistent store.
   *
   * @param entityType Type of entity.
   * @param entityName Unique name of entity.
   */
  protected abstract void deleteEntity(EntityType entityType, String entityName)
    throws IOException, IllegalAccessException;
}
