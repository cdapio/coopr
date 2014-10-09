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
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.util.List;

/**
 * The {@link co.cask.coopr.client.ProvisionerClient} interface implementation based on the Rest requests to
 * the Coopr Rest API.
 *
 * TODO: Implementation
 */
public class ProvisionerRestClient implements ProvisionerClient {

  private final RestClient restClient;

  public ProvisionerRestClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public List<Provisioner> getAllProvisioners() throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }

  @Override
  public Provisioner getProvisioner(String provisionerId) throws IOException {
    throw new NotImplementedException("This method is not implemented yet.");
  }
}
