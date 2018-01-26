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

public class EncryptionUtilTest {

    @Test
    public void testEncrypt() {
        EncryptionKey key = new EncryptionKey("this is my secret key.");
        String text = "please encrypt this text.";
        String encryptedText = EncryptionUtil.encrypt(text, key);
        String decryptedText = EncryptionUtil.decrypt(encryptedText, key);
        Assert.assertEquals(text, decryptedText);
    }

    @Test
    public void testEncryptTwice() {
        EncryptionKey key = new EncryptionKey("this is my secret key.");
        String text = "please encrypt this text.";
        String encryptedText1 = EncryptionUtil.encrypt(text, key);
        String encryptedText2 = EncryptionUtil.encrypt(text, key);
        Assert.assertNotEquals(encryptedText1, encryptedText2);
    }

    @Test
    public void testDecryptLegacy() {
        EncryptionKey key = new EncryptionKey("This is an encryption key");
        String expectedClearText = "Please encrypt this text";
        String encryptedText = "aeEFKa0uXMUHT4UyeFtuHjm37NQw3vEaxY03EkkD2qM=";
        String actualClearText = EncryptionUtil.decrypt(encryptedText, key);
        Assert.assertEquals(expectedClearText, actualClearText);
    }
}
