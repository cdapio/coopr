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

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;

import java.security.GeneralSecurityException;
import javax.crypto.Cipher;

/**
 * Transforms plaintext to and from ciphertext, also providing utilities to encode/decode ciphertext in Base64 so the
 * results can be used as strings.
 */
public class Encryptor {
  private final CipherProvider cipherProvider;

  public Encryptor(CipherProvider cipherProvider) {
    this.cipherProvider = cipherProvider;
  }

  /**
   * Encrypt then base64 encode the given input string.
   *
   * @param input string to encrypt and base64 encode
   * @return encrypted and encoded input
   * @throws GeneralSecurityException if there was an exception encrypting
   */
  public byte[] encryptAndEncodeString(String input) throws GeneralSecurityException {
    Cipher cipher = cipherProvider.createInitializedCipher(Cipher.ENCRYPT_MODE);
    byte[] ciphertext = cipher.doFinal(input.getBytes(Charsets.UTF_8));
    return Base64.encodeBase64(ciphertext);
  }

  /**
   * Base64 decode then decrypt the given bytes, assuming it was a string that was encrypted and encoded using the
   * same encryption transformation as this class.
   *
   * @param input bytes to decode and decrypt
   * @return decoded and decrypted input
   * @throws GeneralSecurityException if there was an exception decoding
   */
  public String decodeAndDecryptString(byte[] input) throws GeneralSecurityException {
    byte[] ciphertext = Base64.decodeBase64(input);
    Cipher cipher = cipherProvider.createInitializedCipher(Cipher.DECRYPT_MODE);
    byte[] plaintext = cipher.doFinal(ciphertext);
    return new String(plaintext, Charsets.UTF_8);
  }
}
