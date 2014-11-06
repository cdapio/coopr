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

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility methods for working with HttpClient.
 */
public final class RestUtil {

  private RestUtil() { }

  /**
   * Creates and connects HTTP and HTTPS connection sockets.
   *
   * @return {@link org.apache.http.config.Registry} of {@link org.apache.http.conn.socket.ConnectionSocketFactory}
   * objects, keyed by low-case string ID
   * @throws KeyManagementException if SSL context initialization process fails
   * @throws NoSuchAlgorithmException when try to get SSLContext instance, if no Provider supports a
   * TrustManagerFactorySpi implementation for the specified protocol
   */
  public static Registry<ConnectionSocketFactory> getRegistryWithDisabledCertCheck()
    throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, new TrustManager[]{new X509TrustManager() {
      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s)
        throws CertificateException {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s)
        throws CertificateException {
      }
    }}, new SecureRandom());
    SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(
      sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    return RegistryBuilder
      .<ConnectionSocketFactory>create().register("https", sf)
      .register("http", PlainConnectionSocketFactory.getSocketFactory())
      .build();
  }


}
