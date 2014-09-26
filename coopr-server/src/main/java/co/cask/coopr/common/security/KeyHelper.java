/*
 * Copyright © 2014 Cask Data, Inc.
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
package co.cask.coopr.common.security;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

/**
 * Helper class to load {@link java.security.KeyStore KeyStores}.
 */
public final class KeyHelper {
  private static final Logger LOG  = LoggerFactory.getLogger(KeyHelper.class);

  public static KeyStore getKeyStore(String filePath, String password) throws IOException, GeneralSecurityException {
    return getKeyStore(filePath, KeyStore.getDefaultType(), password);
  }

  public static KeyStore getKeyStore(String filePath, String type, String password)
    throws IOException, GeneralSecurityException {
    Preconditions.checkArgument(filePath != null && !filePath.isEmpty(),
                                "Path to keystore must be specified.");
    Preconditions.checkArgument(password != null, "Keystore password must be specified.");
    String ksType = type == null ? KeyStore.getDefaultType() : type;
    KeyStore ks = KeyStore.getInstance(ksType);
    FileInputStream keystoreStream = new FileInputStream(filePath);
    try {
      ks.load(keystoreStream, password.toCharArray());
      return ks;
    } finally {
      try {
        keystoreStream.close();
      } catch (IOException e) {
        LOG.error("Exception closing stream for keystore.", e);
      }
    }
  }

  public static Key getKeyFromKeyStore(KeyStore keyStore, String keyAlias, String keyPassword)
    throws IOException, GeneralSecurityException {
    Preconditions.checkArgument(keyAlias != null, "Key alias must be specified.");
    Preconditions.checkArgument(keyPassword != null, "Key password must be specified.");
    return keyStore.getKey(keyAlias, keyPassword.toCharArray());
  }
}
