/* Copyright 2018-2022 Norconex Inc.
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.norconex.commons.lang.collection.CollectionUtil;
import com.norconex.commons.lang.file.ContentType;

/**
 * <p>
 * Converts strings to objects and objects to strings.  Supported object types
 * are guaranteed to be convertible both ways if converted by this class.
 * </p>
 * <p>
 * Can only be initialized at construction time or the default instance
 * can be obtained statically.
 * </p>
 * <p>
 * Converted string values are NOT locale-specific. This class favors
 * consistency over format and string values are not always appropriate for
 * human-consumption.
 * </p>
 * <p>
 * Default instance has converters for the following types:
 * </p>
 * <ul>
 *   <li>{@link Byte}/byte</li>
 *   <li>{@link Short}/short</li>
 *   <li>{@link Integer}/int</li>
 *   <li>{@link Float}/float</li>
 *   <li>{@link Long}/long</li>
 *   <li>{@link Double}/double</li>
 *   <li>{@link BigInteger}</li>
 *   <li>{@link BigDecimal}</li>
 *   <li>{@link Boolean}/boolean</li>
 *   <li>{@link Character}/char</li>
 *   <li>{@link File}</li>
 *   <li>{@link Path}</li>
 *   <li>{@link Date}</li>
 *   <li>{@link LocalDateTime}</li>
 *   <li>{@link ZonedDateTime}</li>
 *   <li>{@link Instant}</li>
 *   <li>{@link Locale}</li>
 *   <li>{@link Enum}</li>
 *   <li>{@link Dimension}</li>
 *   <li>{@link Duration}</li>
 *   <li>{@link ContentType}</li>
 *   <li>{@link Charset}</li>
 *   <li>{@link URL}</li>
 *   <li>{@link Pattern}</li>
 * </ul>
 * <p>
 * By default {@link ConverterException} is thrown when conversion fails.
 * </p>
 *
 * @see ConverterException
 * @since 3.0.0 (renamed from 2.x "Converter")
 */
public final class GenericConverter implements Converter {

    private static final GenericConverter DEFAULT_INSTANCE =
            new GenericConverter(createDefaultConverters());

    private final Map<Class<?>, Converter> converters;

    public GenericConverter(Map<Class<?>, Converter> converters) {
        this.converters = Collections.unmodifiableMap(converters);
    }
    public static GenericConverter defaultInstance() {
        return DEFAULT_INSTANCE;
    }
    private static Map<Class<?>, Converter> createDefaultConverters() {
        Converter c;
        Map<Class<?>, Converter> cc = new HashMap<>();

        // numbers
        c = new NumberConverter();
        cc.put(Integer.class, c);
        cc.put(int.class, c);
        cc.put(Float.class, c);
        cc.put(float.class, c);
        cc.put(Long.class, c);
        cc.put(long.class, c);
        cc.put(Double.class, c);
        cc.put(double.class, c);
        cc.put(Short.class, c);
        cc.put(short.class, c);
        cc.put(Byte.class, c);
        cc.put(byte.class, c);
        cc.put(BigDecimal.class, c);
        cc.put(BigInteger.class, c);

        // files
        c = new FileConverter();
        cc.put(File.class, c);
        cc.put(Path.class, c);

        // dates
        cc.put(Date.class, new DateConverter());
        cc.put(LocalDateTime.class, new LocalDateTimeConverter());
        cc.put(ZonedDateTime.class, new ZonedDateTimeConverter());
        cc.put(Instant.class, new InstantConverter());

        // others
        cc.put(Locale.class, new LocaleConverter());
        cc.put(Enum.class, new EnumConverter());
        cc.put(Dimension.class, new DimensionConverter());
        cc.put(Boolean.class, new BooleanConverter());
        cc.put(boolean.class, new BooleanConverter());
        cc.put(Character.class, new CharacterConverter());
        cc.put(char.class, new CharacterConverter());
        cc.put(Class.class, new ClassConverter());
        cc.put(String.class, new StringConverter());
        cc.put(URL.class, new URLConverter());
        cc.put(Duration.class, new DurationConverter());
        cc.put(ContentType.class, new ContentTypeConverter());
        cc.put(Charset.class, new CharsetConverter());
        cc.put(Pattern.class, new PatternConverter());

        return cc;
    }

    public Map<Class<?>, Converter> getConverters() {
        return converters;
    }

    //--- Static ---
    public static <T> List<T> convert(List<String> values, Class<T> type) {
        return defaultInstance().toType(values, type);
    }
    public static <T> T convert(String value, Class<T> type) {
        return defaultInstance().toType(value, type);
    }
    public static <T> T convert(String value, Class<T> type, T defaultValue) {
        return defaultInstance().toType(value, type, defaultValue);
    }

    public static List<String> convert(List<Object> objects) {
        return defaultInstance().toString(objects);
    }
    public static String convert(Object object) {
        return defaultInstance().toString(object);
    }
    public static String convert(Object object, String defaultValue) {
        return defaultInstance().toString(object, defaultValue);
    }

    //--- To Type ---
    public <T> List<T> toType(List<String> values, Class<T> type) {
        return CollectionUtil.toTypeList(values, type);
    }
    @Override
    public <T> T toType(String value, Class<T> type) {
        return nullSafeConverter(type).toType(value, type);
    }
    @Override
    public <T> T toType(String value, Class<T> type, T defaultValue) {
        return nullSafeConverter(type).toType(value, type, defaultValue);
    }

    //--- To String ---
    public List<String> toString(List<?> objects) {
        if (objects == null) {
            return Collections.emptyList();
        }
        return CollectionUtil.toStringList(objects);
    }

    @Override
    public String toString(Object object) {
        if (object == null) {
            return null;
        }
        return nullSafeConverter(object.getClass()).toString(object);
    }
    @Override
    public String toString(Object object, String defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        return nullSafeConverter(
                object.getClass()).toString(object, defaultValue);
    }

    //--- Misc ---

    public boolean isConvertible(Class<?> type) {
        return getConverter(type) != null;
    }

    public Converter getConverter(Class<?> type) {
        var c = converters.get(type);
        if (c != null) {
            return c;
        }

        //Those are defined in converters mapping at the top.  Really needed??

        if (Enum.class.isAssignableFrom(type)) {
            c = converters.get(Enum.class);
        } else if (Path.class.isAssignableFrom(type)) {
            c = converters.get(Path.class);
        } else if (Charset.class.isAssignableFrom(type)) {
            c = converters.get(Charset.class);
        }
        return c;
    }

    private Converter nullSafeConverter(Class<?> type) {
        Objects.requireNonNull(type, "type must not be null");
        var c = getConverter(type);
        if (c == null) {
            throw new ConverterException(
                    "No converter found for type " + type.getTypeName());
        }
        return c;
    }
}
