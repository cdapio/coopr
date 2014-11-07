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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

/**
 * Creates {@link Cipher Ciphers} given some cipher parameters.
 */
public class CipherProvider {
  private final String transformation;
  private final Key key;
  private final AlgorithmParameterSpec parameterSpec;

  private CipherProvider(String transformation, Key key) throws GeneralSecurityException {
    this(transformation, key, null);
  }

  private CipherProvider(String transformation, Key key, AlgorithmParameterSpec parameterSpec)
    throws GeneralSecurityException {
    Preconditions.checkArgument(transformation != null, "A transformation must be specified.");
    Preconditions.checkArgument(key != null, "A key must be specified.");
    this.transformation = transformation;
    this.key = key;
    this.parameterSpec = parameterSpec;
    // die now if its broken since these will always break if input is invalid
    createInitializedCipher(Cipher.ENCRYPT_MODE);
  }

  /**
   * Create a cipher initialized to the given mode.
   *
   * @param mode mode to initialize the cipher to
   * @return initialized cipher
   * @throws GeneralSecurityException if there was an exception creating and initializing the cipher.
   */
  public Cipher createInitializedCipher(int mode) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(transformation);
    if (parameterSpec != null) {
      cipher.init(mode, key, parameterSpec);
    } else {
      cipher.init(mode, key);
    }
    return cipher;
  }

  /**
   * Get a builder for creating a cipher provider.
   *
   * @return builder for creating a cipher provider
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating a cipher provider.
   */
  public static class Builder {
    private String transformation;
    private String ivHex;
    private Key key;
    private String keystorePath;
    private String keystorePassword;
    private String keystoreType;
    private String keyAlias;
    private String keyPassword;

    public Builder setTransformation(String transformation) {
      this.transformation = transformation;
      return this;
    }

    public Builder setIVHex(String ivHex) {
      this.ivHex = ivHex;
      return this;
    }

    public Builder setKey(Key key) {
      this.key = key;
      return this;
    }

    public Builder setKeystorePath(String keystorePath) {
      this.keystorePath = keystorePath;
      return this;
    }

    public Builder setKeystorePassword(String keystorePassword) {
      this.keystorePassword = keystorePassword;
      return this;
    }

    public Builder setKeystoreType(String keystoreType) {
      this.keystoreType = keystoreType;
      return this;
    }

    public Builder setKeyAlias(String keyAlias) {
      this.keyAlias = keyAlias;
      return this;
    }

    public Builder setKeyPassword(String keyPassword) {
      this.keyPassword = keyPassword;
      return this;
    }

    public CipherProvider build() throws IOException, GeneralSecurityException, DecoderException {
      if (key == null) {
        key = getKeyFromKeyStore();
      }
      if (ivHex != null) {
        byte[] ivBytes = Hex.decodeHex(ivHex.toCharArray());
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
        return new CipherProvider(transformation, key, ivParameterSpec);
      } else {
        return new CipherProvider(transformation, key);
      }
    }

    private Key getKeyFromKeyStore() throws IOException, GeneralSecurityException {
      KeyStore keyStore = KeyHelper.getKeyStore(keystorePath, keystoreType, keystorePassword);
      return KeyHelper.getKeyFromKeyStore(keyStore, keyAlias, keyPassword);
    }
  }

}
