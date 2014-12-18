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
package co.cask.coopr.codec.json.guice;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterDetails;
import co.cask.coopr.cluster.Node;
import co.cask.coopr.codec.json.LowercaseEnumTypeAdapterFactory;
import co.cask.coopr.codec.json.current.AddServicesRequestCodec;
import co.cask.coopr.codec.json.current.AdministrationCodec;
import co.cask.coopr.codec.json.current.AutomatorTypeCodec;
import co.cask.coopr.codec.json.current.ClusterCodec;
import co.cask.coopr.codec.json.current.ClusterConfigureRequestCodec;
import co.cask.coopr.codec.json.current.ClusterCreateRequestCodec;
import co.cask.coopr.codec.json.current.ClusterDefaultsCodec;
import co.cask.coopr.codec.json.current.ClusterDetailsCodec;
import co.cask.coopr.codec.json.current.ClusterOperationRequestCodec;
import co.cask.coopr.codec.json.current.ClusterTemplateCodec;
import co.cask.coopr.codec.json.current.CompatibilitiesCodec;
import co.cask.coopr.codec.json.current.ConstraintsCodec;
import co.cask.coopr.codec.json.current.FieldSchemaCodec;
import co.cask.coopr.codec.json.current.FinishTaskRequestCodec;
import co.cask.coopr.codec.json.current.HardwareTypeCodec;
import co.cask.coopr.codec.json.current.ImageTypeCodec;
import co.cask.coopr.codec.json.current.LayoutConstraintCodec;
import co.cask.coopr.codec.json.current.LeaseDurationCodec;
import co.cask.coopr.codec.json.current.NodeCodec;
import co.cask.coopr.codec.json.current.NodePropertiesRequestCodec;
import co.cask.coopr.codec.json.current.ParametersSpecificationCodec;
import co.cask.coopr.codec.json.current.PartialTemplateCodec;
import co.cask.coopr.codec.json.current.PluginResourceMetaCodec;
import co.cask.coopr.codec.json.current.ProviderCodec;
import co.cask.coopr.codec.json.current.ProviderTypeCodec;
import co.cask.coopr.codec.json.current.ProvisionerCodec;
import co.cask.coopr.codec.json.current.ResourceCollectionCodec;
import co.cask.coopr.codec.json.current.ResourceTypeSpecificationCodec;
import co.cask.coopr.codec.json.current.ServiceActionCodec;
import co.cask.coopr.codec.json.current.ServiceCodec;
import co.cask.coopr.codec.json.current.ServiceConstraintCodec;
import co.cask.coopr.codec.json.current.ServiceDependenciesCodec;
import co.cask.coopr.codec.json.current.ServiceStageDependenciesCodec;
import co.cask.coopr.codec.json.current.SizeConstraintCodec;
import co.cask.coopr.codec.json.current.TakeTaskRequestCodec;
import co.cask.coopr.codec.json.current.TaskConfigCodec;
import co.cask.coopr.codec.json.current.TenantCodec;
import co.cask.coopr.codec.json.current.TenantSpecificationCodec;
import co.cask.coopr.codec.json.current.TenantWriteRequestCodec;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.http.request.FinishTaskRequest;
import co.cask.coopr.http.request.NodePropertiesRequest;
import co.cask.coopr.http.request.TakeTaskRequest;
import co.cask.coopr.http.request.TenantWriteRequest;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.plugin.ResourceCollection;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.scheduler.task.TaskConfig;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.FieldSchema;
import co.cask.coopr.spec.plugin.ParametersSpecification;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.plugin.ResourceTypeSpecification;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.service.ServiceAction;
import co.cask.coopr.spec.service.ServiceDependencies;
import co.cask.coopr.spec.service.ServiceStageDependencies;
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.spec.template.LayoutConstraint;
import co.cask.coopr.spec.template.LeaseDuration;
import co.cask.coopr.spec.template.PartialTemplate;
import co.cask.coopr.spec.template.ServiceConstraint;
import co.cask.coopr.spec.template.SizeConstraint;
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

  // get a gson builder with type adapters that serialize/deserialize current versions of objects.
  protected GsonBuilder createCurrentBuilder() {
    return new GsonBuilder()
      .registerTypeAdapter(AddServicesRequest.class, new AddServicesRequestCodec())
      .registerTypeAdapter(Administration.class, new AdministrationCodec())
      .registerTypeAdapter(AutomatorType.class, new AutomatorTypeCodec())
      .registerTypeAdapter(Cluster.class, new ClusterCodec())
      .registerTypeAdapter(Node.class, new NodeCodec())
      .registerTypeAdapter(ClusterConfigureRequest.class, new ClusterConfigureRequestCodec())
      .registerTypeAdapter(ClusterCreateRequest.class, new ClusterCreateRequestCodec())
      .registerTypeAdapter(ClusterDefaults.class, new ClusterDefaultsCodec())
      .registerTypeAdapter(ClusterDetails.class, new ClusterDetailsCodec())
      .registerTypeAdapter(ClusterOperationRequest.class, new ClusterOperationRequestCodec())
      .registerTypeAdapter(ClusterTemplate.class, new ClusterTemplateCodec())
      .registerTypeAdapter(PartialTemplate.class, new PartialTemplateCodec())
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
      .registerTypeAdapter(CompatibilitiesCodec.class, new CompatibilitiesCodec())
      .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
      .enableComplexMapKeySerialization();
  }
}
