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

import co.cask.coopr.client.rest.exception.HttpFailureException;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

  protected static final Gson GSON = new Gson();

  private final RestClientConnectionConfig config;
  private final URI baseUrl;
  private final String version;
  private final CloseableHttpClient httpClient;

  public RestClient(RestClientConnectionConfig config, CloseableHttpClient httpClient) {
    this.config = config;
    this.baseUrl = URI.create(String.format("%s://%s:%d", config.isSSL() ? HTTPS_PROTOCOL : HTTP_PROTOCOL,
                                            config.getHost(), config.getPort()));
    this.version = config.getVersion();
    this.httpClient = httpClient;
  }

  /**
   * Method for execute HttpRequest with authorized headers, if need.
   *
   * @param request {@link org.apache.http.client.methods.HttpRequestBase} initiated http request with entity, headers,
   *                                                                      request uri and all another required
   *                                                                      properties for successfully request
   * @return {@link org.apache.http.client.methods.CloseableHttpResponse} as a result of http request execution
   * @throws IOException in case of a problem or the connection was aborted
   */
  public CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
    if (StringUtils.isNotEmpty(config.getUserId())) {
      request.setHeader(COOPR_USER_ID_HEADER_NAME, config.getUserId());
    }
    if (StringUtils.isNotEmpty(config.getTenantId())) {
      request.setHeader(COOPR_TENANT_ID_HEADER_NAME, config.getTenantId());
    }
    //TODO: Remove API Key if it's not used anymore
    if (StringUtils.isNotEmpty(config.getAPIKey())) {
      request.setHeader(COOPR_API_KEY_HEADER_NAME, config.getAPIKey());
    }
    LOG.debug("Execute Http Request: {}", request);
    return httpClient.execute(request);
  }

  /**
   * Utility method for analysis http response status code and throw appropriate Java API Exception.
   *
   * @param response {@link org.apache.http.HttpResponse} http response
   */
  public static void responseCodeAnalysis(HttpResponse response) {
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
        throw new HttpFailureException(response.toString(), code);
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

  protected <T> List<T> getAll(String urlSuffix) throws IOException {
    HttpGet getRequest = new HttpGet(baseUrl.resolve(String.format("/%s/%s", version, urlSuffix)));
    CloseableHttpResponse httpResponse = execute(getRequest);
    List<T> resultList;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      resultList = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                                    new TypeToken<List<T>>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return resultList != null ? resultList : new ArrayList<T>();
  }

  protected <T> T getSingle(String urlSuffix, String name) throws IOException {
    HttpGet getRequest = new HttpGet(baseUrl.resolve(String.format("/%s/%s/%s", version, urlSuffix, name)));
    CloseableHttpResponse httpResponse = execute(getRequest);
    T result;
    try {
      RestClient.responseCodeAnalysis(httpResponse);
      result = GSON.fromJson(EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8),
                             new TypeToken<T>() { }.getType());
    } finally {
      httpResponse.close();
    }
    return result;
  }

  protected void delete(String urlSuffix, String name) throws IOException {
    HttpDelete deleteRequest = new HttpDelete(getBaseURL().resolve(String.format("/%s/%s/%s", version, urlSuffix,
                                                                                 name)));
    CloseableHttpResponse httpResponse = execute(deleteRequest);
    try {
      RestClient.responseCodeAnalysis(httpResponse);
    } finally {
      httpResponse.close();
    }
  }

  /**
   * @return the base URL of Rest Service API
   */
  public URI getBaseURL() {
    return baseUrl;
  }

  /**
   * @return the version of Rest Service API
   */
  public String getVersion() {
    return version;
  }
}
