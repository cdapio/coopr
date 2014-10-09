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

package co.cask.coopr.client.rest.request;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * Http delete method request with ability to set content. Standard {@link org.apache.http.client.methods.HttpDelete}
 * class provided by the Apache HTTP Client doesn't allow to set body to the delete request.
 */
public class HttpDeleteWithContent extends HttpEntityEnclosingRequestBase {
  public static final String METHOD_NAME = "DELETE";

  public HttpDeleteWithContent(final String uri) {
    super();
    setURI(URI.create(uri));
  }

  public HttpDeleteWithContent(final URI uri) {
    super();
    setURI(uri);
  }

  public HttpDeleteWithContent() {
    super();
  }

  @Override
  public String getMethod() {
    return METHOD_NAME;
  }
}
