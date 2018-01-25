/* Copyright 2015 Norconex Inc.
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
package com.norconex.commons.lang.encrypt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Assume;

public class EncryptionUtilTest {

    public static final String ENVIRONMENT_KEY = "TEST_ENCRYPT_KEY";
    public static final String PROPERTY_KEY = "test.encrypt.key";
    public static final String CLEAR_TEXT = "please encrypt this text.";

    @Test
    public void testEncrypt() {
        EncryptionKey key = new EncryptionKey("this is my secret key.");
        String encryptedText = EncryptionUtil.encrypt(CLEAR_TEXT, key);
        String decryptedText = EncryptionUtil.decrypt(encryptedText, key);
        Assert.assertEquals(CLEAR_TEXT, decryptedText);
    }

    @Test
    public void testEnvironmentEncrypt() {
        Assume.assumeNotNull(System.getenv(ENVIRONMENT_KEY));
        EncryptionKey key = new EncryptionKey(ENVIRONMENT_KEY, EncryptionKey.Source.ENVIRONMENT);
        String encryptedText = EncryptionUtil.encrypt(CLEAR_TEXT, key);
        String decryptedText = EncryptionUtil.decrypt(encryptedText, key);
        Assert.assertEquals(CLEAR_TEXT, decryptedText);
    }

    @Test
    public void testPropertyEncrypt() {
        Assume.assumeNotNull(System.getProperty(PROPERTY_KEY));
        EncryptionKey key = new EncryptionKey(PROPERTY_KEY, EncryptionKey.Source.PROPERTY);
        String encryptedText = EncryptionUtil.encrypt(CLEAR_TEXT, key);
        String decryptedText = EncryptionUtil.decrypt(encryptedText, key);
        Assert.assertEquals(CLEAR_TEXT, decryptedText);
    }
}
