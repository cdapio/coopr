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

import co.cask.coopr.client.ProvisionerClient;
import co.cask.coopr.provisioner.Provisioner;
import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * The {@link co.cask.coopr.client.ProvisionerClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 */
public class ProvisionerRestClient extends RestClient implements ProvisionerClient {

  private static final String PROVISIONER_URL_SUFFIX = "provisioners";
  private static final Type PROVISIONERS_LIST_TYPE = new TypeToken<List<Provisioner>>() { }.getType();

  public ProvisionerRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient) {
    super(config, httpClient);
  }

  public ProvisionerRestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient, Gson gson) {
    super(config, httpClient, gson);
  }

  @Override
  public List<Provisioner> getAllProvisioners() throws IOException {
    return getAll(PROVISIONER_URL_SUFFIX, PROVISIONERS_LIST_TYPE);
  }

  @Override
  public Provisioner getProvisioner(String provisionerId) throws IOException {
    return getSingle(PROVISIONER_URL_SUFFIX, provisionerId, Provisioner.class);
  }
}
