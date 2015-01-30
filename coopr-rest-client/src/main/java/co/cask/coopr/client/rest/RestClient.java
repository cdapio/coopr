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

import co.cask.common.http.exception.HttpFailureException;
import co.cask.coopr.client.rest.exception.UnauthorizedAccessTokenException;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides way to execute http requests with Apache HttpClient {@link org.apache.http.client.HttpClient}.
 */
public class RestClient {

  private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String COOPR_API_KEY_HEADER_NAME = "Coopr-ApiKey";
  private static final String COOPR_TENANT_ID_HEADER_NAME = "Coopr-TenantID";
  private static final String COOPR_USER_ID_HEADER_NAME = "Coopr-UserID";

  private final Gson gson;
  private final Supplier<RestClientConnectionConfig> configSupplier;
  private final CloseableHttpClient httpClient;
  private final Set<Header> authHeaders;

  public RestClient(Supplier<RestClientConnectionConfig> config, CloseableHttpClient httpClient) {
    this(config, httpClient, new Gson());
  }

  public RestClient(Supplier<RestClientConnectionConfig> configSupplier, CloseableHttpClient httpClient, Gson gson) {
    RestClientConnectionConfig config = configSupplier.get();
    Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getUserId()), "User ID couldn't be null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getTenantId()), "Tenant ID couldn't be null");
    this.configSupplier = configSupplier;
    this.httpClient = httpClient;
    this.authHeaders = getAuthHeaders();
    this.gson = gson;
  }

  /**
   * Method for executing HttpRequest with authorized headers, if needed.
   *
   * @param request {@link org.apache.http.client.methods.HttpRequestBase} initiated http request with entity, headers,
   *                                                                      request uri and all other required properties
   * @return {@link org.apache.http.client.methods.CloseableHttpResponse} as a result of http request execution
   * @throws IOException in case of a problem or the connection was aborted
   */
  protected CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
    for (Header header : authHeaders) {
      request.addHeader(header);
    }
    LOG.debug("Execute Http Request: {}", request);
    return httpClient.execute(request);
  }

  /**
   * Utility method for analyzing http response status code and throwing the appropriate Java API Exception.
   *
   * @param response {@link org.apache.http.HttpResponse} http response
   */
  public static void analyzeResponseCode(HttpResponse response) {
    int code = response.getStatusLine().getStatusCode();
    switch (code) {
      case HttpStatus.SC_OK:
        LOG.debug("Success operation result code");
        break;
      case HttpStatus.SC_NOT_FOUND:
        throw new HttpFailureException("Not found HTTP code was received from the Coopr Server.", code);
      case HttpStatus.SC_CONFLICT:
        throw new HttpFailureException("Conflict HTTP code was received from the Coopr Server.", code);
      case HttpStatus.SC_BAD_REQUEST:
        throw new HttpFailureException("Bad request HTTP code was received from the Coopr Server.", code);
      case HttpStatus.SC_UNAUTHORIZED:
        throw new UnauthorizedAccessTokenException(response.toString());
      case HttpStatus.SC_FORBIDDEN:
        throw new HttpFailureException("Forbidden HTTP code was received from the Coopr Server", code);
      case HttpStatus.SC_METHOD_NOT_ALLOWED:
        throw new HttpFailureException(response.getStatusLine().getReasonPhrase(), code);
      case HttpStatus.SC_NOT_ACCEPTABLE:
        throw new HttpFailureException("Input was not acceptable", code);
      case HttpStatus.SC_INTERNAL_SERVER_ERROR:
        throw new HttpFailureException("Internal server exception during operation process.", code);
      case HttpStatus.SC_NOT_IMPLEMENTED:
      default:
        throw new UnsupportedOperationException("Operation is not supported by the Coopr Server");
    }
  }

  protected <T> List<T> getAll(String urlSuffix, Type type) throws IOException {
    return getAll(resolveURL(urlSuffix), type);
  }

  protected <T> List<T> getAll(URI url, Type type) throws IOException {
    HttpGet getRequest = new HttpGet(url);
    CloseableHttpResponse httpResponse = execute(getRequest);
    List<T> resultList;
    try {
      RestClient.analyzeResponseCode(httpResponse);
      resultList = gson.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8), type);
    } finally {
      httpResponse.close();
    }
    return resultList != null ? resultList : new ArrayList<T>();
  }

  protected <V, T> Map<V, Set<T>> getPluginTypeMap(String url, Type type) throws IOException {
    if (!url.startsWith("/")) {
      url = "/" + url;
    }

    String fullUrl = String.format("%s/%s%s", getBaseURL(), getConfig().getVersion(), url);
    HttpGet getRequest = new HttpGet(fullUrl);
    CloseableHttpResponse httpResponse = execute(getRequest);
    InputStreamReader reader = null;
    try {
      RestClient.analyzeResponseCode(httpResponse);
      reader = new InputStreamReader(httpResponse.getEntity().getContent(), Charsets.UTF_8);
      return gson.fromJson(reader, type);
    } finally {
      httpResponse.close();
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          LOG.error("Closing reader process failed.", e);
        }
      }
    }
  }

  protected void execPost(URI uri) throws IOException {
    HttpPost postRequest = new HttpPost(uri);
    CloseableHttpResponse httpResponse = execute(postRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  protected <T> T getSingle(String urlSuffix, String name, Type type) throws IOException {
    return getSingle(resolveURL(String.format("%s/%s", urlSuffix, name)), type);
  }

  protected <T> T getSingle(URI url, Type type) throws IOException {
    HttpGet getRequest = new HttpGet(url);
    CloseableHttpResponse httpResponse = execute(getRequest);
    T result;
    try {
      RestClient.analyzeResponseCode(httpResponse);
      result = gson.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8), type);
    } finally {
      httpResponse.close();
    }
    return result;
  }

  protected void delete(String urlSuffix, String name) throws IOException {
    HttpDelete deleteRequest = new HttpDelete(resolveURL(String.format("%s/%s", urlSuffix, name)));
    CloseableHttpResponse httpResponse = execute(deleteRequest);
    try {
      RestClient.analyzeResponseCode(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  protected <T> void addRequestBody(HttpEntityEnclosingRequestBase requestBase, T body) {
    if (body != null) {
      StringEntity stringEntity = new StringEntity(gson.toJson(body), Charsets.UTF_8);
      requestBase.setEntity(stringEntity);
      LOG.debug("Added the json request body: {}.", stringEntity);
    }
  }

  protected URI resolveURL(String postfix) {
    RestClientConnectionConfig config = getConfig();
    if (!postfix.startsWith("/")) {
      return getBaseURL().resolve("/" + config.getVersion() + "/" + postfix);
    } else {
      return getBaseURL().resolve("/" + config.getVersion() + postfix);
    }
  }

  private Set<Header> getAuthHeaders() {
    RestClientConnectionConfig config = getConfig();
    Set<Header> authHeaders = Sets.newLinkedHashSet();
    authHeaders.add(new BasicHeader(COOPR_USER_ID_HEADER_NAME, config.getUserId()));
    authHeaders.add(new BasicHeader(COOPR_TENANT_ID_HEADER_NAME, config.getTenantId()));
    //TODO: For now it is not a mandatory field
    if (!Strings.isNullOrEmpty(config.getAPIKey())) {
      authHeaders.add(new BasicHeader(COOPR_API_KEY_HEADER_NAME, config.getAPIKey()));
    }
    if (config.getAccessToken() != null) {
      authHeaders.add(new BasicHeader(HttpHeaders.Names.AUTHORIZATION,
                                                     String.format("%s: %s", config.getAccessToken().getTokenType(),
                                                                   config.getAccessToken().getValue())));
    }
    return authHeaders;
  }

  /**
   * @return the base URL of Rest Service API
   */
  public URI getBaseURL() {
    RestClientConnectionConfig config = getConfig();
    return URI.create(String.format("%s://%s:%d", config.isSSL() ? HTTPS_PROTOCOL : HTTP_PROTOCOL,
                                    config.getHost(), config.getPort()));
  }

  /**
   * @return the configured gson object
   */
  public Gson getGson() {
    return gson;
  }

  private RestClientConnectionConfig getConfig() {
    return configSupplier.get();
  }
}
