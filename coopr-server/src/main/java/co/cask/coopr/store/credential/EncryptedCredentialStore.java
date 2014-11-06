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
package co.cask.coopr.store.credential;

import co.cask.coopr.common.conf.Configuration;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.common.security.CipherProvider;
import co.cask.coopr.common.security.Encryptor;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Abstract base for implementations of {@link co.cask.coopr.store.credential.CredentialStore} that encrypt values
 * before storing in whatever the storage engine is.
 */
public abstract class EncryptedCredentialStore extends AbstractIdleService implements CredentialStore {
  private static final Logger LOG  = LoggerFactory.getLogger(EncryptedCredentialStore.class);
  private static final Gson GSON = new Gson();
  private final boolean encryptionEnabled;
  private final Encryptor encryptor;

  protected EncryptedCredentialStore(Configuration conf) throws IOException, GeneralSecurityException,
    DecoderException {

    this.encryptionEnabled = conf.getBoolean(Constants.CredentialStore.ENCRYPT_ENABLED);
    if (encryptionEnabled) {
      CipherProvider cipherProvider = CipherProvider.builder()
        .setTransformation(conf.get(Constants.CredentialStore.ENCRYPT_TRANSFORMATION))
        .setIVHex(conf.get(Constants.CredentialStore.ENCRYPT_IV_HEX))
        .setKeystorePath(conf.get(Constants.CredentialStore.KEYSTORE_PATH))
        .setKeystorePassword(conf.get(Constants.CredentialStore.KEYSTORE_PASSWORD))
        .setKeystoreType(conf.get(Constants.CredentialStore.KEYSTORE_TYPE))
        .setKeyAlias(conf.get(Constants.CredentialStore.KEY_ALIAS))
        .setKeyPassword(conf.get(Constants.CredentialStore.KEY_PASSWORD))
        .build();
      this.encryptor = new Encryptor(cipherProvider);
    } else {
      this.encryptor = null;
    }
  }

  abstract void setValue(String tenantId, String clusterId, byte[] val) throws IOException;

  abstract byte[] getValue(String tenantId, String clusterId) throws IOException;

  @Override
  public void set(String tenantId, String clusterId, Map<String, Object> fields) throws IOException {
    try {
      String fieldsAsStr = GSON.toJson(fields);
      byte[] val = encryptionEnabled ?
        encryptor.encryptAndEncodeString(fieldsAsStr) : fieldsAsStr.getBytes(Charsets.UTF_8);
      setValue(tenantId, clusterId, val);
    } catch (GeneralSecurityException e) {
      LOG.error("Exception encrypting sensitive fields for tenant {} and cluster {}", tenantId, clusterId, e);
      throw new IOException("Unable to encrypt sensitive fields", e);
    }
  }

  @Override
  public Map<String, Object> get(String tenantId, String clusterId) throws IOException {
    try {
      byte[] val = getValue(tenantId, clusterId);
      if (val == null) {
        return Maps.newHashMap();
      }
      String resultStr = encryptionEnabled ? encryptor.decodeAndDecryptString(val) : new String(val, Charsets.UTF_8);
      return GSON.fromJson(resultStr, new TypeToken<Map<String, Object>>() { }.getType());
    } catch (GeneralSecurityException e) {
      LOG.error("Exception decrypting sensitive fields for tenant {} and cluster {}.", tenantId, clusterId, e);
      throw new IOException("Unable to decrypt sensitive fields", e);
    }
  }
}
