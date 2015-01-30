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

package co.cask.coopr.client.rest;

import co.cask.coopr.client.TenantClient;
import co.cask.coopr.spec.TenantSpecification;
import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * The {@link co.cask.coopr.client.TenantClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class TenantRestClient extends RestClient implements TenantClient {

  private static final String TENANT_URL_SUFFIX = "tenants";
  private static final Type TENANTS_LIST_TYPE = new TypeToken<List<TenantSpecification>>() { }.getType();

  public TenantRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  public TenantRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient, Gson gson) {
    super(config, httpClient, gson);
  }

  @Override
  public List<TenantSpecification> getTenants() throws IOException {
    return getAll(TENANT_URL_SUFFIX, TENANTS_LIST_TYPE);
  }

  @Override
  public TenantSpecification getTenant(String tenantName) throws IOException {
    return getSingle(TENANT_URL_SUFFIX, tenantName, TenantSpecification.class);
  }

  @Override
  public void deleteTenant(String tenantName) throws IOException {
    delete(TENANT_URL_SUFFIX, tenantName);
  }
}
