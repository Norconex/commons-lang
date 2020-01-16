/* Copyright 2015-2020 Norconex Inc.
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

import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>Pointer to the an encryption key, or the encryption key itself. An
 * encryption key can be seen as equivalent to a secret key,
 * passphrase or password.</p>
 *
 * <p>
 * Because this class is immutable, it does not implement
 * {@link IXMLConfigurable} directly, but static methods
 * {@link #addToXML(XML, String, EncryptionKey)} and
 * {@link #getFromXML(XML, String, EncryptionKey)} can be used instead.
 * The usage example below is for when used embedded in a parent configuration.
 * </p>
 *
 * {@nx.xml.usage
 * <value>(The actual key or reference to it.)</value>
 * <source>[key|file|environment|property]</source>
 * <size>(Size in bits of encryption key. Default is 128.)</size>
 * }
 * <p>
 * These XML configurable options can be nested in a parent tag of any name.
 * The expected parent tag name is defined by the consuming classes.
 * </p>
 *
 * {@nx.xml.example
 * <sampleConfig>
 *   <username>goldorak</username>
 *   <password>3ncryp73d</password>
 *   <passwordKey>
 *     <value>/path/to/my.key</value>
 *     <source>file</source>
 *   </passwordKey>
 * </sampleConfig>
 * }
 * <p>
 * The above example has the encryption key configuration is nested in a
 * <code>&lt;passwordKey&gt;</code> tag. It uses a key store in a file to
 * decrypt a password for user credentials. </p>
 *
 * @author Pascal Essiembre
 * @since 1.9.0
 * @see EncryptionUtil
 */
public final class EncryptionKey implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_KEY_SIZE = 128;

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
    public EncryptionKey(String value, Source source, int size) {
        super();
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
        //TODO allow a flag to optionally throw an exception when null?
        return System.getenv(value);
    }

    private String fromProperty() {
        //TODO allow a flag to optionally throw an exception when null?
        return System.getProperty(value);
    }

    private String fromFile() {
        File file = new File(value);
        if (!file.isFile()) {
            throw new EncryptionException(
                    "Key file is not a file or does not exists: "
                    + file.getAbsolutePath());
        }
        try {
            //TODO allow a flag to optionally throw an exception when null?
            return new String(Files.readAllBytes(
                    Paths.get(value)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EncryptionException(
                    "Could not read key file.", e);
        }
    }

    /**
     * Gets an encryption key from an existing XML.
     * @param xml the XML to get the key from
     * @param elementName the key element name
     * @param defaultKey default encryption key if it does not exist in XML
     * @return encryption key
     * @since 2.0.0
     */
    public static EncryptionKey getFromXML(
            XML xml, String elementName, EncryptionKey defaultKey) {
        XML kxml = xml.getXML(elementName);
        if (kxml == null) {
            return defaultKey;
        }

        String value = kxml.getString("value");
        if (value != null && value.trim().length() > 0) {
            String source = kxml.getString("source");
            Integer size = kxml.getInteger("size", DEFAULT_KEY_SIZE);
            EncryptionKey.Source enumSource = null;
            if (source != null && source.trim().length() > 0) {
                enumSource = EncryptionKey.Source.valueOf(source.toUpperCase());
            }
            return new EncryptionKey(value, enumSource, size);
        }
        return defaultKey;
    }

    /**
     * Adds an encryption key to an existing XML.
     * @param xml the XML to add the key to
     * @param elementName the key element name
     * @param key encryption key
     * @since 2.0.0
     */
    public static void addToXML(
            XML xml, String elementName, EncryptionKey key) {
        if (key != null) {
            XML kxml = XML.of(elementName).create();
            kxml.addElement("value", key.value);
            kxml.addElement("size", key.size);
            if (key.source != null) {
                kxml.addElement("source", key.source.name().toLowerCase());
            }
            xml.addXML(kxml);
        }
    }

    //Do not use Apache Commons Lang below to avoid any dependency
    //when used on command-line with EncryptionUtil.
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + size;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EncryptionKey)) {
            return false;
        }
        EncryptionKey other = (EncryptionKey) obj;
        if (source != other.source) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        if (size == null) {
            if (other.size != null) {
                return false;
            }
        } else if (!size.equals(other.size)) {
           return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EncryptionKey [value=" + value
                + ", source=" + source + ", size=" + size + "]";
    }
}
