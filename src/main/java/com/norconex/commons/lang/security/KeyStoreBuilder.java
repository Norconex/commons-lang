/* Copyright 2020-2022 Norconex Inc.
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
package com.norconex.commons.lang.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;

/**
 * Builder for creating a KeyStore.
 * @since 2.0.0
 */
public final class KeyStoreBuilder {

    //MAYBE: have new class KeyStoreSession or TrustingKeyStore that will
    // automatically trust everything and save back?

    private static final Logger LOG =
            LoggerFactory.getLogger(KeyStoreBuilder.class);

    private Path storeFile;
    private String provider;
    private String type;
    private String password;
    private EncryptionKey passwordKey;

    private KeyStoreBuilder(Path storeFile) {
        this.storeFile = storeFile;
    }

    /**
     * Sets the security provider name. When not set, uses default provider
     * for the key store type.
     * @param provider provider name
     * @return this builder
     */
    public KeyStoreBuilder setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Sets the key store type. When not set, uses default key store type.
     * @param type key store type
     * @return this builder
     */
    public KeyStoreBuilder setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the key store password. When not set, it is assumed the key store
     * does not require any password.
     * @param password key store password.
     * @return this builder
     */
    public KeyStoreBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Sets the password encryption key.  In case the password is encrypted.
     * @param passwordKey password key
     * @return this builder
     * @see EncryptionKey
     */
    public KeyStoreBuilder setPasswordKey(EncryptionKey passwordKey) {
        this.passwordKey = passwordKey;
        return this;
    }

    /**
     * Creates the key store.
     * @return key store
     * @throws NoSuchAlgorithmException problem creating key store
     * @throws CertificateException problem creating key store
     * @throws IOException problem creating key store
     * @throws KeyStoreException problem creating key store
     * @throws NoSuchProviderException problem creating key store
     */
    public KeyStore create() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            NoSuchProviderException {
        KeyStore ks;
        String ktype = orNotBlank(type, KeyStore::getDefaultType);
        if (StringUtils.isNotBlank(provider)) {
            ks = KeyStore.getInstance(ktype, provider);
        } else {
            ks = KeyStore.getInstance(ktype);
        }
        if (storeFile != null) {
            LOG.debug("Loading KeyStore {}...", storeFile.toAbsolutePath());
            try (InputStream in = Files.newInputStream(storeFile)) {
                ks.load(in, passwordArray());
            }
        } else {
            // Load a key store with a single dummy entry to avoid
            // "the trustAnchors parameter must be non-empty" error.
            ks.load(getClass().getResourceAsStream("empty-keystore.jks"),
                    passwordArray());
        }
        return ks;
    }

    private char[] passwordArray() {
        if (password == null) {
            return null; //NOSONAR  KeyStore API expects null to indicate empty
        }
        return EncryptionUtil.decrypt(password, passwordKey).toCharArray();
    }

    private String orNotBlank(
            String originalValue, Supplier<String> nonBlankValueSupplier) {
        if (StringUtils.isNotBlank(originalValue)) {
            return originalValue;
        }
        return nonBlankValueSupplier.get();
    }

    /**
     * A builder for a key store initialized from
     * <code>{JAVA_HOME}/lib/security/cacerts</code>.
     * @return a key store builder
     */
    public static KeyStoreBuilder fromJavaHome() {
        File javaHome = SystemUtils.getJavaHome();
        if (javaHome == null) {
            throw new IllegalStateException(
                    "The java.home system property must be set.");
        }
        File file = new File(javaHome,
                File.separatorChar + "lib"
                        + File.separatorChar + "security"
                        + File.separatorChar + "cacerts");
        return fromFile(file.toPath());
    }

    /**
     * A builder for a key store initialized from a key store file.
     * @param file key store file
     * @return a key store builder
     */
    public static KeyStoreBuilder fromFile(Path file) {
        if (file == null || !file.toFile().exists()) {
            throw new IllegalArgumentException(
                    "KeyStore file does not exist: " + file);
        }
        return new KeyStoreBuilder(file);
    }

    /**
     * A builder for an empty key store.
     * @return a key store builder
     */
    public static KeyStoreBuilder empty() {
        return new KeyStoreBuilder(null);
    }
}
