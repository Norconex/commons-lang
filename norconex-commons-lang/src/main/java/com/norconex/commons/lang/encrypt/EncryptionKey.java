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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Pointer to the an encryption key, or the encryption key itself. An 
 * encryption key can be seen as equivalent to a secret key, 
 * passphrase or password.
 * @author Pascal Essiembre
 * @since 1.9.0
 * @see EncryptionUtil
 */
public class EncryptionKey {

    private static final Logger LOG = LogManager.getLogger(EncryptionKey.class);
    
    public enum Source { 
        /** Value is the actual key. */
        KEY, 
        /** Value is the path to a file containing the key. */
        FILE, 
        /** Value is the name of an environment variable containing the key. */
        ENVIRONMENT, 
        /** Value is the name of a JVM system property containing the key. */
        PROPERTY
    };

    private final String value;
    private final Source source;
    
    /**
     * Creates a new reference to an encryption key. The reference can either 
     * be the key itself, or a pointer to a file or environment variable
     * containing the key (as defined by the supplied value type).
     * @param value the encryption key
     * @param source the type of value
     */
    public EncryptionKey(String value, Source source) {
        super();
        this.value = value;
        this.source = source;
    }
    /**
     * Creates a new encryption key where the value is the actual key.
     * @param value the encryption key
     */
    public EncryptionKey(String value) {
        this(value, Source.KEY);
    }
    public String getValue() {
        return value;
    }
    public Source getSource() {
        return source;
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
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (source == null) {
            return value;
        }
        switch (source) {
        case KEY:
            return value;
        case FILE:
            return fromFile();
        case ENVIRONMENT:
            return fromEnv();
        case PROPERTY:
            return fromProperty();
        default:
            return null;
        }
    }
    
    private String fromEnv() {
        String key = System.getenv(value);
        if (StringUtils.isBlank(key)) {
            LOG.info("Key not found under environment variable \"" 
                    + value + "\".");
        }
        return key;
    }
    
    private String fromProperty() {
        String key = System.getProperty(value);
        if (StringUtils.isBlank(key)) {
            LOG.info("Key not found under system property \"" 
                    + value + "\".");
        }
        return key;
    }

    private String fromFile() {
        File file = new File(value);
        if (!file.isFile()) {
            LOG.info("Key file is not a file or does not exists: "
                    + file.getAbsolutePath());
            return null;
        }
        try {
            String key = FileUtils.readFileToString(file, CharEncoding.UTF_8);
            if (StringUtils.isBlank(key)) {
                LOG.info("Key not found under key file: "
                        + file.getAbsolutePath());
            }
            return key;
        } catch (IOException e) {
            throw new EncryptionException(
                    "Could not read key file.", e);
        }
    }    
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("value", value)
                .append("source", source)
                .toString();
    }
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof EncryptionKey)) {
            return false;
        }
        EncryptionKey castOther = (EncryptionKey) other;
        return new EqualsBuilder()
                .append(value, castOther.value)
                .append(source, castOther.source)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(value)
                .append(source)
                .toHashCode();
    } 
}
