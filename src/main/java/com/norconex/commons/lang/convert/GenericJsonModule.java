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
package com.norconex.commons.lang.convert;

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

import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.norconex.commons.lang.file.ContentType;

/**
 * Jackson module provideing (de)serializers for common types
 * not handled natively by Jackson modules, or handled differently.
 */
public class GenericJsonModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public GenericJsonModule() {
        super(VersionUtil.parseVersion(
                "3", "com.norconex.commons", "norconex-commons-lang"));

        // Serializers
        addSerializer(Dimension.class,
                new DimensionConverter.JsonSerializer());
        addSerializer(Duration.class,
                new DurationConverter.JsonSerializer());
        addSerializer(LocalDateTime.class,
                new LocalDateTimeConverter.JsonSerializer());
        addSerializer(File.class,
                new FileConverter.JsonSerializer());
        addSerializer(Path.class,
                new PathConverter.JsonSerializer());
        addSerializer(Pattern.class,
                new PatternConverter.JsonSerializer());
        addSerializer(ZonedDateTime.class,
                new ZonedDateTimeConverter.JsonSerializer());
        addSerializer(Instant.class,
                new InstantConverter.JsonSerializer());
        addSerializer(Charset.class,
                new CharsetConverter.JsonSerializer());
        addSerializer(ContentType.class,
                new ContentTypeConverter.JsonSerializer());
        addSerializer(Locale.class,
                new LocaleConverter.JsonSerializer());


        // Deserializers
        addDeserializer(Dimension.class,
                new DimensionConverter.JsonDeserializer());
        addDeserializer(Duration.class,
                new DurationConverter.JsonDeserializer());
        addDeserializer(LocalDateTime.class,
                new LocalDateTimeConverter.JsonDeserializer());
        addDeserializer(File.class,
                new FileConverter.JsonDeserializer());
        addDeserializer(Path.class,
                new PathConverter.JsonDeserializer());
        addDeserializer(Pattern.class,
                new PatternConverter.JsonDeserializer());
        addDeserializer(ZonedDateTime.class,
                new ZonedDateTimeConverter.JsonDeserializer());
        addDeserializer(Instant.class,
                new InstantConverter.JsonDeserializer());
        addDeserializer(Charset.class,
                new CharsetConverter.JsonDeserializer());
        addDeserializer(ContentType.class,
                new ContentTypeConverter.JsonDeserializer());
        addDeserializer(Locale.class,
                new LocaleConverter.JsonDeserializer());
    }
}