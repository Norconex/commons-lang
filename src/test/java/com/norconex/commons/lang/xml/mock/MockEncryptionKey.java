/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.xml.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.norconex.commons.lang.encrypt.EncryptionException;
import com.norconex.commons.lang.xml.XML;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class MockEncryptionKey {
    public static final int DEFAULT_KEY_SIZE = 256;
    public enum Source {
        KEY,
        FILE,
        ENVIRONMENT,
        PROPERTY
    }
    private final String value;
    private final Integer size;
    private final MockEncryptionKey.Source source;
    public MockEncryptionKey(String value, MockEncryptionKey.Source source, int size) {
        this.value = value;
        this.source = source;
        this.size = size;
    }
    public MockEncryptionKey(String value, MockEncryptionKey.Source source) {
        this(value, source, DEFAULT_KEY_SIZE);
    }
    public MockEncryptionKey(String value, int size) {
        this(value, Source.KEY, size);
    }
    public MockEncryptionKey(String value) {
        this(value, Source.KEY, DEFAULT_KEY_SIZE);
    }
    public String getValue() {
        return value;
    }
    public MockEncryptionKey.Source getSource() {
        return source;
    }
    public int getSize() {
        return  (size != null ? size : DEFAULT_KEY_SIZE);
    }
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
        return System.getenv(value);
    }

    private String fromProperty() {
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
    public static MockEncryptionKey loadFromXML(XML xml, MockEncryptionKey defaultKey) {
        if (xml == null) {
            return defaultKey;
        }

        var value = xml.getString("value");
        if (value != null && value.trim().length() > 0) {
            var source = xml.getString("source");
            var size = xml.getInteger("size", DEFAULT_KEY_SIZE);
            MockEncryptionKey.Source enumSource = null;
            if (source != null && source.trim().length() > 0) {
                enumSource = MockEncryptionKey.Source.valueOf(source.toUpperCase());
            }
            return new MockEncryptionKey(value, enumSource, size);
        }
        return defaultKey;
    }
    public static void saveToXML(XML xml, MockEncryptionKey key) {
        if (key != null) {
            xml.addElement("value", key.value);
            xml.addElement("size", key.size);
            if (key.source != null) {
                xml.addElement("source", key.source.name().toLowerCase());
            }
        }
    }
    @Override
    public String toString() {
        return "NonXMlConfigurableEncryptionKey [value=" + "********"
                + ", source=" + source + ", size=" + size + "]";
    }
}