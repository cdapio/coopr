/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.coopr.client.rest;

import co.cask.cdap.security.authentication.client.AccessToken;
import com.google.common.base.Supplier;

/**
 * Container for REST client configuration properties.
 */
public class RestClientConnectionConfig {

  private final String host;
  private final int port;
  private final String apiKey;
  private final boolean ssl;
  private final String version;
  private final String userId;
  private final String tenantId;
  private final boolean verifySSLCert;
  private Supplier<AccessToken> accessTokenSupplier;

  public RestClientConnectionConfig(String host, int port, String apiKey, boolean ssl, String version,
                                    String userId, String tenantId, boolean verifySSLCert,
                                    Supplier<AccessToken> accessTokenSupplier) {
    this.host = host;
    this.port = port;
    this.apiKey = apiKey;
    this.ssl = ssl;
    this.version = version;
    this.userId = userId;
    this.tenantId = tenantId;
    this.verifySSLCert = verifySSLCert;
    this.accessTokenSupplier = accessTokenSupplier;
  }

  public static Builder builder(String host, int port) {
    return new Builder(host, port);
  }

  public String getHost() {
    return host;
  }

  public String getVersion() {
    return version;
  }

  public boolean isSSL() {
    return ssl;
  }

  public String getAPIKey() {
    return apiKey;
  }

  public AccessToken getAccessToken() {
    if (accessTokenSupplier == null) {
      return null;
    }
    return accessTokenSupplier.get();
  }

  public int getPort() {
    return port;
  }

  public String getUserId() {
    return userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public boolean isVerifySSLCert() {
    return verifySSLCert;
  }

  /**
   * Class Builder for create RestClientManager instance.
   */
  public static class Builder {

    private static final String DEFAULT_VERSION = "v2";
    private static final boolean DEFAULT_SSL = false;
    private static final boolean DEFAULT_VERIFY_SSL_CERT = true;

    //mandatory
    private final int port;
    private final String host;

    //optional
    private String apiKey;
    private boolean ssl = DEFAULT_SSL;
    private boolean verifySSLCert = DEFAULT_VERIFY_SSL_CERT;
    private String version = DEFAULT_VERSION;
    private String userId;
    private String tenantId;
    private Supplier<AccessToken> accessTokenSupplier;

    public Builder(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public Builder ssl(boolean ssl) {
      this.ssl = ssl;
      return this;
    }

    public Builder verifySSLCert(boolean verifySSLCert) {
      this.verifySSLCert = verifySSLCert;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder accessToken(Supplier<AccessToken> accessTokenSupplier) {
      this.accessTokenSupplier = accessTokenSupplier;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public RestClientConnectionConfig build() {
      return new RestClientConnectionConfig(host, port, apiKey, ssl, version, userId, tenantId,
                                            verifySSLCert, accessTokenSupplier);
    }
  }
}
