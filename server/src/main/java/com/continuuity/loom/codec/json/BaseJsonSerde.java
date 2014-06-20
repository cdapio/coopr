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
package com.continuuity.loom.codec.json;

import com.continuuity.loom.admin.Administration;
import com.continuuity.loom.admin.AutomatorType;
import com.continuuity.loom.admin.ClusterDefaults;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.Constraints;
import com.continuuity.loom.admin.FieldSchema;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.LayoutConstraint;
import com.continuuity.loom.admin.LeaseDuration;
import com.continuuity.loom.admin.ParametersSpecification;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProviderType;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceConstraint;
import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.codec.json.current.AddServicesRequestCodec;
import com.continuuity.loom.codec.json.current.AdministrationCodec;
import com.continuuity.loom.codec.json.current.AutomatorTypeCodec;
import com.continuuity.loom.codec.json.current.ClusterCodec;
import com.continuuity.loom.codec.json.current.ClusterConfigureRequestCodec;
import com.continuuity.loom.codec.json.current.ClusterCreateRequestCodec;
import com.continuuity.loom.codec.json.current.ClusterDefaultsCodec;
import com.continuuity.loom.codec.json.current.ClusterTemplateCodec;
import com.continuuity.loom.codec.json.current.ConstraintsCodec;
import com.continuuity.loom.codec.json.current.FieldSchemaCodec;
import com.continuuity.loom.codec.json.current.HardwareTypeCodec;
import com.continuuity.loom.codec.json.current.ImageTypeCodec;
import com.continuuity.loom.codec.json.current.LayoutConstraintCodec;
import com.continuuity.loom.codec.json.current.LeaseDurationCodec;
import com.continuuity.loom.codec.json.current.ParametersSpecificationCodec;
import com.continuuity.loom.codec.json.current.ProviderCodec;
import com.continuuity.loom.codec.json.current.ProviderTypeCodec;
import com.continuuity.loom.codec.json.current.ServiceActionCodec;
import com.continuuity.loom.codec.json.current.ServiceCodec;
import com.continuuity.loom.codec.json.current.ServiceConstraintCodec;
import com.continuuity.loom.codec.json.current.ServiceDependenciesCodec;
import com.continuuity.loom.codec.json.current.ServiceStageDependenciesCodec;
import com.continuuity.loom.codec.json.current.TenantCodec;
import com.continuuity.loom.http.request.AddServicesRequest;
import com.continuuity.loom.http.request.ClusterConfigureRequest;
import com.continuuity.loom.layout.ClusterCreateRequest;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Base class that starts with a Gson builder that loads all current codecs.
 */
public abstract class BaseJsonSerde implements JsonSerde {

  public BaseJsonSerde() {
  }

  protected GsonBuilder createCurrentBuilder() {
    return new GsonBuilder()
      .registerTypeAdapter(AddServicesRequest.class, new AddServicesRequestCodec())
      .registerTypeAdapter(Administration.class, new AdministrationCodec())
      .registerTypeAdapter(AutomatorType.class, new AutomatorTypeCodec())
      .registerTypeAdapter(Cluster.class, new ClusterCodec())
      .registerTypeAdapter(ClusterConfigureRequest.class, new ClusterConfigureRequestCodec())
      .registerTypeAdapter(ClusterCreateRequest.class, new ClusterCreateRequestCodec())
      .registerTypeAdapter(ClusterDefaults.class, new ClusterDefaultsCodec())
      .registerTypeAdapter(ClusterTemplate.class, new ClusterTemplateCodec())
      .registerTypeAdapter(Constraints.class, new ConstraintsCodec())
      .registerTypeAdapter(FieldSchema.class, new FieldSchemaCodec())
      .registerTypeAdapter(HardwareType.class, new HardwareTypeCodec())
      .registerTypeAdapter(ImageType.class, new ImageTypeCodec())
      .registerTypeAdapter(LayoutConstraint.class, new LayoutConstraintCodec())
      .registerTypeAdapter(LeaseDuration.class, new LeaseDurationCodec())
      .registerTypeAdapter(ParametersSpecification.class, new ParametersSpecificationCodec())
      .registerTypeAdapter(Provider.class, new ProviderCodec())
      .registerTypeAdapter(ProviderType.class, new ProviderTypeCodec())
      .registerTypeAdapter(Service.class, new ServiceCodec())
      .registerTypeAdapter(ServiceAction.class, new ServiceActionCodec())
      .registerTypeAdapter(ServiceConstraint.class, new ServiceConstraintCodec())
      .registerTypeAdapter(ServiceDependencies.class, new ServiceDependenciesCodec())
      .registerTypeAdapter(ServiceStageDependencies.class, new ServiceStageDependenciesCodec())
      .registerTypeAdapter(Tenant.class, new TenantCodec())
      .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
      .enableComplexMapKeySerialization();
  }

  @Override
  public <T> byte[] serialize(T object, Type type) {
    return getGson().toJson(object, type).getBytes(Charsets.UTF_8);
  }

  @Override
  public <T> T deserialize(Reader reader, Type type) {
    return getGson().fromJson(reader, type);
  }

  @Override
  public <T> T deserialize(byte[] bytes, Type type) {
    return getGson().fromJson(new String(bytes, Charsets.UTF_8), type);
  }
}
