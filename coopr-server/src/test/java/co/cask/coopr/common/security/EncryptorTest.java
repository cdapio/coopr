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

import org.junit.Assert;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.Random;

/**
 *
 */
public class EncryptorTest {

  @Test
  public void testEncryptDecrypt() throws Exception {
    SecretKey key = KeyGenerator.getInstance("AES").generateKey();
    CipherProvider cipherProvider = CipherProvider.builder()
      .setTransformation("AES/CBC/PKCS5Padding")
      .setKey(key)
      .setIVHex("22BA219FC88FC0826CCAC88C474801D3")
      .build();
    Encryptor encryptor = new Encryptor(cipherProvider);
    String input = "nlzvxAkc 98i30_a`1?/+d;mnlsjkn1";
    Assert.assertEquals(input, encryptor.decodeAndDecryptString(encryptor.encryptAndEncodeString(input)));
  }
}
