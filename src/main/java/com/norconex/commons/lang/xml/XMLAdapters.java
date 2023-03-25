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
package com.norconex.commons.lang.xml;

import java.awt.Dimension;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import com.norconex.commons.lang.convert.GenericConverter;
import com.norconex.commons.lang.file.ContentType;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>
 * JAXB adapters to be used with {@link XmlJavaTypeAdapter} annotation
 * supporting types found in {@link GenericConverter} not natively supported
 * by JAXB.
 * </p>
 *
 * @since 3.0.0
 */
public final class XMLAdapters {

    private XMLAdapters() {}

    public static class CharsetAdapter extends XmlAdapter<String, Charset> {
        @Override
        public String marshal(Charset obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Charset unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Charset.class);
        }
    }

    public static class ContentTypeAdapter
            extends XmlAdapter<String, ContentType> {
        @Override
        public String marshal(ContentType obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public ContentType unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, ContentType.class);
        }
    }

    public static class DimensionAdapter extends XmlAdapter<String, Dimension> {
        @Override
        public String marshal(Dimension obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Dimension unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Dimension.class);
        }
    }

    public static class DurationAdapter extends XmlAdapter<String, Duration> {
        @Override
        public String marshal(Duration obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Duration unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Duration.class);
        }
    }

    public static class FileAdapter extends XmlAdapter<String, File> {
        @Override
        public String marshal(File obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public File unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, File.class);
        }
    }

    public static class InstantAdapter extends XmlAdapter<String, Instant> {
        @Override
        public String marshal(Instant obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Instant unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Instant.class);
        }
    }

    public static class LocalDateTimeAdapter
            extends XmlAdapter<String, LocalDateTime> {
        @Override
        public String marshal(LocalDateTime obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public LocalDateTime unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, LocalDateTime.class);
        }
    }

    public static class LocaleAdapter extends XmlAdapter<String, Locale> {
        @Override
        public String marshal(Locale obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Locale unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Locale.class);
        }
    }

    public static class PathAdapter extends XmlAdapter<String, Path> {
        @Override
        public String marshal(Path obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Path unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Path.class);
        }
    }

    public static class PatternAdapter extends XmlAdapter<String, Pattern> {
        @Override
        public String marshal(Pattern obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public Pattern unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, Pattern.class);
        }
    }

    public static class ZonedDateTimeAdapter
            extends XmlAdapter<String, ZonedDateTime> {
        @Override
        public String marshal(ZonedDateTime obj) throws Exception {
            return GenericConverter.convert(obj);
        }
        @Override
        public ZonedDateTime unmarshal(String value) throws Exception {
            return GenericConverter.convert(value, ZonedDateTime.class);
        }
    }
}
