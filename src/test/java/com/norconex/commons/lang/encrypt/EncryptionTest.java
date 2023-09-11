/* Copyright 2018-2023 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.SystemUtil;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.encrypt.EncryptionKey.Source;
import com.norconex.commons.lang.security.Credentials;

class EncryptionTest {

    private static final String TEXT = "Please encrypt me.";
    private static final String KEY = "I am an encryption key.";
    private static final String ENCRYPTED_TEXT = EncryptionUtil.encrypt(
            TEXT, new EncryptionKey(KEY));

    @TempDir
    private Path tempDir;

    @Test
    void testEncryptTwice() {
        var key = new EncryptionKey("this is my secret key.");
        var text = "please encrypt this text.";
        var encryptedText1 = EncryptionUtil.encrypt(text, key);
        var encryptedText2 = EncryptionUtil.encrypt(text, key);
        Assertions.assertNotEquals(encryptedText1, encryptedText2);
    }

    @Test
    void testAes256bitEncryptionKey() throws NoSuchAlgorithmException {

        // NOTE: this test should be true on Java 8 u162+ or on Java 9, or on
        // any Java where JCE Unlimited Strength has been applied
        Assumptions.assumeTrue(Cipher.getMaxAllowedKeyLength("AES") >= 256);

        // Create round-trip encryption key
        var key = new EncryptionKey("This as an encryption key", 256);
        var text = "please encrypt this text";
        var encryptedText = EncryptionUtil.encrypt(text, key);
        var decryptedText = EncryptionUtil.decrypt(encryptedText, key);

        Assertions.assertEquals(text, decryptedText);
    }

    @Test
    void testPlainKey() {
        assertThat(EncryptionUtil.decrypt(
                ENCRYPTED_TEXT, new EncryptionKey(KEY))).isEqualTo(TEXT);
    }

    @Test
    void testFileKey() throws IOException {
        var keyFile = tempDir.resolve("keyFile").toAbsolutePath();
        Files.writeString(keyFile, KEY);

        assertThat(EncryptionUtil.decrypt(
                ENCRYPTED_TEXT,
                new EncryptionKey(keyFile.toString(), Source.FILE)))
            .isEqualTo(TEXT);
    }

    @Test
    void testSystemPropertyKey() throws IOException {
        SystemUtil.runWithProperty("myKey", KEY, () -> {
            assertThat(EncryptionUtil.decrypt(
                    ENCRYPTED_TEXT,
                    new EncryptionKey("myKey", Source.PROPERTY)))
                .isEqualTo(TEXT);
        });
    }

    @Test
    void testEnvironmentKey() {
        // Since Java does not offer clean ways to set the environment
        // variable for testing, let's test that it fails when not present.
        assertThat(EncryptionUtil.decrypt(
                ENCRYPTED_TEXT,
                new EncryptionKey("I/SHOULD/NOT/EXIST", Source.ENVIRONMENT)))
            .isNotEqualTo(TEXT);
    }

    @Test
    void testEncryptNullAndErrors() {
        assertThat(EncryptionUtil.encrypt(
                null, new EncryptionKey(KEY))).isNull();
        assertThat(EncryptionUtil.encrypt(TEXT, null)).isEqualTo(TEXT);
        assertThat(EncryptionUtil.encrypt(
                TEXT, new EncryptionKey(null))).isEqualTo(TEXT);
        assertThrows(EncryptionException.class, //NOSONAR
                () -> EncryptionUtil.encrypt(TEXT, new EncryptionKey(KEY, 0)));
    }

    @Test
    void testDecryptNullAndErrors() {
        assertThat(EncryptionUtil.decrypt(
                null, new EncryptionKey(KEY))).isNull();
        assertThat(EncryptionUtil.decrypt(
                ENCRYPTED_TEXT, null)).isEqualTo(ENCRYPTED_TEXT);
        assertThat(EncryptionUtil.decrypt(
                ENCRYPTED_TEXT,
                new EncryptionKey(null))).isEqualTo(ENCRYPTED_TEXT);
        assertThrows(EncryptionException.class, //NOSONAR
                () -> EncryptionUtil.decrypt(
                        ENCRYPTED_TEXT, new EncryptionKey(KEY, 0)));
        assertThrows(EncryptionException.class, //NOSONAR
                () -> new EncryptionKey(
                        "I/SHOULD/NOT/EXIST", Source.FILE).resolve());
        var captured = SystemUtil.runAndCaptureOutput(
                () -> EncryptionUtil.main(new String[] {}));
        assertThat(captured.getStdOut()).contains("<appName> encrypt|decrypt");
        assertThat(captured.getStdErr()).isBlank();

        captured = SystemUtil.runAndCaptureOutput(
                () -> EncryptionUtil.main(new String[] {"", "", "", ""}));
        assertThat(captured.getStdOut()).contains("<appName> encrypt|decrypt");
        assertThat(captured.getStdErr()).contains("Unsupported type of key");
    }

    @Test
    void testDecryptPassword() {
        assertThat(EncryptionUtil.decryptPassword(
                new Credentials()
                    .setUsername("u")
                    .setPassword(ENCRYPTED_TEXT)
                    .setPasswordKey(new EncryptionKey(KEY)))).isEqualTo(TEXT);
        assertThat(EncryptionUtil.decryptPassword(null)).isNull();
    }

    @Test
    void testDefaults() {
        assertThat(new EncryptionKey(KEY).getValue()).isEqualTo(KEY);
        assertThat(new EncryptionKey(KEY).getSource()).isEqualTo(Source.KEY);
        assertThat(new EncryptionKey(KEY).getSize())
            .isEqualTo(EncryptionKey.DEFAULT_KEY_SIZE);
        assertThat(EncryptionUtil.decryptPassword(null)).isNull();
    }

    @Test
    void testLoadSaveXML() {
        var key = new EncryptionKey(KEY, Source.PROPERTY, 256);
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(key));
    }
}
