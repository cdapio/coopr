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
package com.continuuity.loom.codec.json.guice;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.codec.json.LowercaseEnumTypeAdapterFactory;
import com.continuuity.loom.codec.json.current.AddServicesRequestCodec;
import com.continuuity.loom.codec.json.current.ClusterOperationRequestCodec;
import com.continuuity.loom.codec.json.current.TenantWriteRequestCodec;
import com.continuuity.loom.codec.json.current.AdministrationCodec;
import com.continuuity.loom.codec.json.current.AutomatorTypeCodec;
import com.continuuity.loom.codec.json.current.ClusterCodec;
import com.continuuity.loom.codec.json.current.ClusterConfigureRequestCodec;
import com.continuuity.loom.codec.json.current.ClusterCreateRequestCodec;
import com.continuuity.loom.codec.json.current.ClusterDefaultsCodec;
import com.continuuity.loom.codec.json.current.ClusterTemplateCodec;
import com.continuuity.loom.codec.json.current.ConstraintsCodec;
import com.continuuity.loom.codec.json.current.FieldSchemaCodec;
import com.continuuity.loom.codec.json.current.FinishTaskRequestCodec;
import com.continuuity.loom.codec.json.current.HardwareTypeCodec;
import com.continuuity.loom.codec.json.current.ImageTypeCodec;
import com.continuuity.loom.codec.json.current.LayoutConstraintCodec;
import com.continuuity.loom.codec.json.current.LeaseDurationCodec;
import com.continuuity.loom.codec.json.current.NodeCodec;
import com.continuuity.loom.codec.json.current.NodePropertiesRequestCodec;
import com.continuuity.loom.codec.json.current.ParametersSpecificationCodec;
import com.continuuity.loom.codec.json.current.PluginResourceMetaCodec;
import com.continuuity.loom.codec.json.current.ProviderCodec;
import com.continuuity.loom.codec.json.current.ProviderTypeCodec;
import com.continuuity.loom.codec.json.current.ProvisionerCodec;
import com.continuuity.loom.codec.json.current.ResourceCollectionCodec;
import com.continuuity.loom.codec.json.current.ResourceTypeSpecificationCodec;
import com.continuuity.loom.codec.json.current.ServiceActionCodec;
import com.continuuity.loom.codec.json.current.ServiceCodec;
import com.continuuity.loom.codec.json.current.ServiceConstraintCodec;
import com.continuuity.loom.codec.json.current.ServiceDependenciesCodec;
import com.continuuity.loom.codec.json.current.ServiceStageDependenciesCodec;
import com.continuuity.loom.codec.json.current.SizeConstraintCodec;
import com.continuuity.loom.codec.json.current.TakeTaskRequestCodec;
import com.continuuity.loom.codec.json.current.TaskConfigCodec;
import com.continuuity.loom.codec.json.current.TenantCodec;
import com.continuuity.loom.codec.json.current.TenantSpecificationCodec;
import com.continuuity.loom.codec.json.upgrade.ClusterUpgradeCodec;
import com.continuuity.loom.codec.json.upgrade.NodeUpgradeCodec;
import com.continuuity.loom.codec.json.upgrade.ProviderUpgradeCodec;
import com.continuuity.loom.codec.json.upgrade.ServiceActionUpgradeCodec;
import com.continuuity.loom.codec.json.upgrade.ServiceUpgradeCodec;
import com.continuuity.loom.http.request.AddServicesRequest;
import com.continuuity.loom.http.request.ClusterOperationRequest;
import com.continuuity.loom.http.request.TenantWriteRequest;
import com.continuuity.loom.http.request.ClusterConfigureRequest;
import com.continuuity.loom.http.request.ClusterCreateRequest;
import com.continuuity.loom.http.request.FinishTaskRequest;
import com.continuuity.loom.http.request.NodePropertiesRequest;
import com.continuuity.loom.http.request.TakeTaskRequest;
import com.continuuity.loom.provisioner.Provisioner;
import com.continuuity.loom.provisioner.plugin.ResourceCollection;
import com.continuuity.loom.provisioner.plugin.ResourceMeta;
import com.continuuity.loom.scheduler.task.TaskConfig;
import com.continuuity.loom.spec.HardwareType;
import com.continuuity.loom.spec.ImageType;
import com.continuuity.loom.spec.Provider;
import com.continuuity.loom.spec.Tenant;
import com.continuuity.loom.spec.TenantSpecification;
import com.continuuity.loom.spec.plugin.AutomatorType;
import com.continuuity.loom.spec.plugin.FieldSchema;
import com.continuuity.loom.spec.plugin.ParametersSpecification;
import com.continuuity.loom.spec.plugin.ProviderType;
import com.continuuity.loom.spec.plugin.ResourceTypeSpecification;
import com.continuuity.loom.spec.service.Service;
import com.continuuity.loom.spec.service.ServiceAction;
import com.continuuity.loom.spec.service.ServiceDependencies;
import com.continuuity.loom.spec.service.ServiceStageDependencies;
import com.continuuity.loom.spec.template.Administration;
import com.continuuity.loom.spec.template.ClusterDefaults;
import com.continuuity.loom.spec.template.ClusterTemplate;
import com.continuuity.loom.spec.template.Constraints;
import com.continuuity.loom.spec.template.LayoutConstraint;
import com.continuuity.loom.spec.template.LeaseDuration;
import com.continuuity.loom.spec.template.ServiceConstraint;
import com.continuuity.loom.spec.template.SizeConstraint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;

/**
 * Guice module for binding serialization/deserialization related classes.
 */
public class CodecModules {

  /**
   * Get a module that binds gson to an object that serializes/deserializes current versions of objects.
   *
   * @return Module that binds gson to an object that serializes/deserializes current versions of objects.
   */
  public AbstractModule getModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(Gson.class).toInstance(createCurrentBuilder().create());
      }
    };
  }

  /**
   * Get a module that binds gson to an object that can deserialize older versions of objects.
   *
   * @param tenantId Id of the tenant to add to cluster objects.
   * @return Module for reading older versions of objects.
   */
  public AbstractModule getUpgradeModule(final String tenantId) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(Gson.class).toInstance(
          createCurrentBuilder()
            .registerTypeAdapter(Cluster.class, new ClusterUpgradeCodec(tenantId))
            .registerTypeAdapter(Provider.class, new ProviderUpgradeCodec())
            .registerTypeAdapter(Service.class, new ServiceUpgradeCodec())
            .registerTypeAdapter(ServiceAction.class, new ServiceActionUpgradeCodec())
            .registerTypeAdapter(Node.class, new NodeUpgradeCodec())
            .create()
        );
      }
    };
  }

  // get a gson builder with type adapters that serialize/deserialize current versions of objects.
  private GsonBuilder createCurrentBuilder() {
    return new GsonBuilder()
      .registerTypeAdapter(AddServicesRequest.class, new AddServicesRequestCodec())
      .registerTypeAdapter(Administration.class, new AdministrationCodec())
      .registerTypeAdapter(AutomatorType.class, new AutomatorTypeCodec())
      .registerTypeAdapter(Cluster.class, new ClusterCodec())
      .registerTypeAdapter(Node.class, new NodeCodec())
      .registerTypeAdapter(ClusterConfigureRequest.class, new ClusterConfigureRequestCodec())
      .registerTypeAdapter(ClusterCreateRequest.class, new ClusterCreateRequestCodec())
      .registerTypeAdapter(ClusterDefaults.class, new ClusterDefaultsCodec())
      .registerTypeAdapter(ClusterOperationRequest.class, new ClusterOperationRequestCodec())
      .registerTypeAdapter(ClusterTemplate.class, new ClusterTemplateCodec())
      .registerTypeAdapter(Constraints.class, new ConstraintsCodec())
      .registerTypeAdapter(FieldSchema.class, new FieldSchemaCodec())
      .registerTypeAdapter(FinishTaskRequest.class, new FinishTaskRequestCodec())
      .registerTypeAdapter(HardwareType.class, new HardwareTypeCodec())
      .registerTypeAdapter(ImageType.class, new ImageTypeCodec())
      .registerTypeAdapter(LayoutConstraint.class, new LayoutConstraintCodec())
      .registerTypeAdapter(LeaseDuration.class, new LeaseDurationCodec())
      .registerTypeAdapter(NodePropertiesRequest.class, new NodePropertiesRequestCodec())
      .registerTypeAdapter(ParametersSpecification.class, new ParametersSpecificationCodec())
      .registerTypeAdapter(ResourceMeta.class, new PluginResourceMetaCodec())
      .registerTypeAdapter(Provider.class, new ProviderCodec())
      .registerTypeAdapter(ProviderType.class, new ProviderTypeCodec())
      .registerTypeAdapter(Provisioner.class, new ProvisionerCodec())
      .registerTypeAdapter(ResourceCollection.class, new ResourceCollectionCodec())
      .registerTypeAdapter(ResourceTypeSpecification.class, new ResourceTypeSpecificationCodec())
      .registerTypeAdapter(Service.class, new ServiceCodec())
      .registerTypeAdapter(ServiceAction.class, new ServiceActionCodec())
      .registerTypeAdapter(ServiceConstraint.class, new ServiceConstraintCodec())
      .registerTypeAdapter(ServiceDependencies.class, new ServiceDependenciesCodec())
      .registerTypeAdapter(ServiceStageDependencies.class, new ServiceStageDependenciesCodec())
      .registerTypeAdapter(SizeConstraint.class, new SizeConstraintCodec())
      .registerTypeAdapter(TakeTaskRequest.class, new TakeTaskRequestCodec())
      .registerTypeAdapter(TaskConfig.class, new TaskConfigCodec())
      .registerTypeAdapter(Tenant.class, new TenantCodec())
      .registerTypeAdapter(TenantSpecification.class, new TenantSpecificationCodec())
      .registerTypeAdapter(TenantWriteRequest.class, new TenantWriteRequestCodec())
      .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
      .enableComplexMapKeySerialization();
  }
}
