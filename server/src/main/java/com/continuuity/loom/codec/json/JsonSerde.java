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
import com.continuuity.loom.http.AddServicesRequest;
import com.continuuity.loom.http.ClusterConfigureRequest;
import com.continuuity.loom.layout.ClusterCreateRequest;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Class for serializing and deserializing objects to/from json, using gson.
 */
public class JsonSerde {
  private final Gson gson;

  public JsonSerde() {
    gson = new GsonBuilder()
      .registerTypeAdapter(AddServicesRequest.class, new AddServicesRequestCodec())
      .registerTypeAdapter(Administration.class, new AdministrationCodec())
      .registerTypeAdapter(AutomatorType.class, new AutomatorTypeCodec())
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
      .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
      .enableComplexMapKeySerialization()
      .create();
  }

  /**
   * Serialize the object of specified type.
   *
   * @param object Object to serialize.
   * @param type Type of the object to serialize.
   * @param <T> Object class.
   * @return serialized object.
   */
  public <T> byte[] serialize(T object, Type type) {
    return gson.toJson(object, type).getBytes(Charsets.UTF_8);
  }

  /**
   * Deserialize an object given a reader for the object and the type of the object.
   *
   * @param reader Reader for reading the object to deserialize.
   * @param type Type of the object to deserialize.
   * @param <T> Object class.
   * @return deserialized object.
   */
  public <T> T deserialize(Reader reader, Type type) {
    return gson.fromJson(reader, type);
  }

  /**
   * Deserialize an object given the object as a byte array and the type of the object.
   *
   * @param bytes Serialized object.
   * @param type Type of the object to deserialize.
   * @param <T> Object class.
   * @return deserialized object.
   */
  public <T> T deserialize(byte[] bytes, Type type) {
    return gson.fromJson(new String(bytes, Charsets.UTF_8), type);
  }

  /**
   * Get the Gson used for serialization and deserialization.
   *
   * @return Gson used for serialization and deserialization.
   */
  public Gson getGson() {
    return gson;
  }
}
