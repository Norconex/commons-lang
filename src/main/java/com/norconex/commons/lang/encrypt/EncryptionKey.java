/* Copyright 2015-2023 Norconex Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.norconex.commons.lang.security.Credentials;

import lombok.EqualsAndHashCode;

/**
 * <p>Pointer to the an encryption key, or the encryption key itself. An
 * encryption key can be seen as equivalent to a secret key,
 * passphrase or password.</p>
 *
 * {@nx.xml.usage
 * <value>(The actual key or reference to it.)</value>
 * <source>[key|file|environment|property]</source>
 * <size>(Size in bits of encryption key. Default is 256.)</size>
 * }
 * <p>
 * These XML configurable options can be nested in a parent tag of any name.
 * The expected parent tag name is defined by the consuming classes.
 * </p>
 *
 * {@nx.xml.example
 * <sampleKey>
 *   <value>/path/to/my.key</value>
 *   <source>file</source>
 * </sampleKey>
 * }
 * <p>
 * The above example has the encryption key configuration is nested in a
 * <code>&lt;passwordKey&gt;</code> tag. It uses a key stored in a file to
 * decrypt a password for user credentials. </p>
 *
 * @since 1.9.0
 * @see EncryptionUtil
 * @see Credentials
 */
@EqualsAndHashCode
public final class EncryptionKey implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_KEY_SIZE = 256;

    public enum Source {
        /** Value is the actual key. */
        KEY,
        /** Value is the path to a file containing the key. */
        FILE,
        /** Value is the name of an environment variable containing the key. */
        ENVIRONMENT,
        /** Value is the name of a JVM system property containing the key. */
        PROPERTY
    }

    private final String value;
    private final Integer size;
    private final Source source;

    /**
     * Creates a new reference to an encryption key. The reference can either
     * be the key itself, or a pointer to a file or environment variable
     * containing the key (as defined by the supplied value type).  The actual
     * value can be any sort of string, and it is converted to an encryption
     * key of length size using cryptographic algorithms. If the size is
     * specified, it must be supported by your version of Java.
     *
     * @param value the encryption key
     * @param size the size in bits of the encryption key
     * @param source the type of value
     */
    @JsonCreator
    public EncryptionKey(
            @JsonProperty("value")
            String value,
            @JsonProperty("source")
            Source source,
            @JsonProperty("size")
            int size) {
        this.value = value;
        this.source = source;
        this.size = size;
    }
    /**
     * Creates a new reference to an encryption key. The reference can either
     * be the key itself, or a pointer to a file or environment variable
     * containing the key (as defined by the supplied value type).
     * @param value the encryption key
     * @param source the type of value
     */
    public EncryptionKey(String value, Source source) {
        this(value, source, DEFAULT_KEY_SIZE);
    }
    /**
     * Creates a new encryption key where the value is the actual key, and the
     * number of key bits to generate is the size.
     * @param value the encrption key
     * @param size the encryption key size in bits
     */
    public EncryptionKey(String value, int size) {
        this(value, Source.KEY, size);
    }
    /**
     * Creates a new encryption key where the value is the actual key.
     * @param value the encryption key
     */
    public EncryptionKey(String value) {
        this(value, Source.KEY, DEFAULT_KEY_SIZE);
    }
    public String getValue() {
        return value;
    }
    public Source getSource() {
        return source;
    }
    /**
     * Gets the size in bits of the encryption key. Default is
     * {@value #DEFAULT_KEY_SIZE}.
     * @return size in bits of the encryption key
     * @since 1.15.0
     */
    public int getSize() {
        return  (size != null ? size : DEFAULT_KEY_SIZE);
    }

    /**
     * Locate the key according to its value type and return it.  This
     * method will always resolve the value each type it is invoked and
     * never caches the key, unless the key value specified at construction
     * time is the actual key.
     * @return encryption key or <code>null</code> if the key does not exist
     * for the specified type
     */
    public String resolve() {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        if (source == null) {
            return value;
        }
        return switch (source) {
        case KEY -> value;
        case FILE -> fromFile();
        case ENVIRONMENT -> fromEnv();
        case PROPERTY -> fromProperty();
        default -> null;
        };
    }

    private String fromEnv() {
        //MAYBE allow a flag to optionally throw an exception when null?
        return System.getenv(value);
    }

    private String fromProperty() {
        //MAYBE allow a flag to optionally throw an exception when null?
        return System.getProperty(value);
    }

    private String fromFile() {
        var file = new File(value);
        if (!file.isFile()) {
            throw new EncryptionException(
                    "Key file is not a file or does not exists: "
                    + file.getAbsolutePath());
        }
        try {
            //MAYBE allow a flag to optionally throw an exception when null?
            return new String(Files.readAllBytes(
                    Paths.get(value)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EncryptionException(
                    "Could not read key file.", e);
        }
    }

    @Override
    public String toString() {
        return "EncryptionKey [value=" + "********"
                + ", source=" + source + ", size=" + size + "]";
    }
}
