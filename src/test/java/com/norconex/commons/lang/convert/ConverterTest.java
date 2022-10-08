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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Dimension;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.Operator;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.unit.DataUnit;


class ConverterTest {

    @Test
    void testNumberConverter() {
        assertConvert("64", (byte) 64, byte.class);
        assertConvert("64", (byte) 64, Byte.class);

        assertConvert("37", (short) 37, short.class);
        assertConvert("37", (short) 37, Short.class);

        assertConvert("123", 123, int.class);
        assertConvert("123", 123, Integer.class);

        assertConvert("456.78", 456.78f, float.class);
        assertConvert("456.78", 456.78f, Float.class);

        assertConvert("901", 901L, long.class);
        assertConvert("901", 901L, Long.class);

        assertConvert("234.56", 234.56d, double.class);
        assertConvert("234.56", 234.56d, Double.class);

        assertConvert("789012", BigInteger.valueOf(789012), BigInteger.class);
        assertConvert("34.6789", BigDecimal.valueOf(34.6789), BigDecimal.class);

        NumberConverter c = new NumberConverter();
        assertThrows(ConverterException.class,
                () -> c.toType("badOne", String.class));
    }

    @Test
    void testLocaleConverter() {
        assertConvert("fr_CA", Locale.CANADA_FRENCH, Locale.class);
    }

    @Test
    void testEnumConverter() {
        assertConvert("GB", DataUnit.GB, DataUnit.class);
        assertConvert("ge", Operator.GREATER_EQUAL, Operator.class);
        assertToType(Operator.GREATER_EQUAL, "GREATER_EQUAL", Operator.class);

        EnumConverter c = new EnumConverter();
        assertThrows(ConverterException.class,
                () -> c.toType("badOne", Operator.class));
    }

    @Test
    void testFileConverter() {
        String filePath = new File("/tmp/filepath.txt").getAbsolutePath();
        assertConvert(filePath, new File(filePath), File.class);
        assertConvert(filePath, Paths.get(filePath), Path.class);

        FileConverter c = new FileConverter();
        assertThrows(ConverterException.class,
                () -> c.toType("badOne", String.class));
    }

    @Test
    void testInstantConverter() {
        Instant now = Instant.now();
        assertConvert(now.toString(), now, Instant.class);
    }

    @Test
    void testDateConverter() {
        Date now = new Date();
        assertConvert(Long.toString(now.getTime()), now, Date.class);
    }

    @Test
    void testLocalDateTimeConverter() {
        LocalDateTime now = LocalDateTime.now();
        assertConvert(now.toString(), now, LocalDateTime.class);
    }

    @Test
    void testZonedDateTimeConverter() {
        ZonedDateTime now = ZonedDateTime.now();
        assertConvert(now.toString(), now, ZonedDateTime.class);
    }

    @Test
    void testDimensionConverter() {
        Dimension d = new Dimension(640, 480);

        //to string
        Assertions.assertEquals("640x480", Converter.convert(d));

        //from string
        Assertions.assertEquals(d, Converter.convert("640x480", Dimension.class));
        Assertions.assertEquals(d, Converter.convert("640 480", Dimension.class));
        Assertions.assertEquals(
                d, Converter.convert("width:640, h:480", Dimension.class));
        Assertions.assertEquals(
                d, Converter.convert("aaa640b480cc10", Dimension.class));

        d = new Dimension(1200, 1200);
        Assertions.assertEquals(d, Converter.convert("1200", Dimension.class));
        Assertions.assertEquals(
                d, Converter.convert("size:1200px", Dimension.class));

        assertThrows(ConverterException.class,
                () -> Converter.convert("badOne", Dimension.class));

        DimensionConverter c = new DimensionConverter();
        assertThrows(ConverterException.class, () -> c.toString("badOne"));
    }

    @Test
    void testBooleanConverter() {
        assertConvert("true", true, boolean.class);
        assertConvert("true", Boolean.TRUE, Boolean.class);
        assertConvert("false", false, boolean.class);
        assertConvert("false", Boolean.FALSE, Boolean.class);
        assertToType(true, "yes", boolean.class);
        assertToType(Boolean.FALSE, "no", Boolean.class);
        assertThrows(ConverterException.class,
                () -> Converter.convert("badOne", Boolean.class));
    }

    @Test
    void testCharacterConverter() {
        assertConvert("a", 'a', char.class);
        assertConvert("b", 'b', Character.class);
        assertThrows(ConverterException.class,
                () -> Converter.convert("badOne", Character.class));

        CharacterConverter c = new CharacterConverter();
        assertThrows(ConverterException.class, () -> c.toString("badOne"));
    }

    @Test
    void testClassConverter() {
        assertConvert("com.norconex.commons.lang.EqualsUtil",
                EqualsUtil.class, Class.class);
        assertThrows(ConverterException.class,
                () -> Converter.convert("badOne", Class.class));
    }

    @Test
    void testStringConverter() {
        assertConvert("blah", "blah", String.class);
    }

    @Test
    void testURLConverter() throws MalformedURLException {
        assertConvert("http://example.com/blah.html",
                new URL("http://example.com/blah.html"), URL.class);
        assertToType(new URL("http://example.com/%22fix%22"),
                "http://example.com/\"fix\"", URL.class);
    }

    @Test
    void testDurationConverter() {
        // to duration
        assertToType(Duration.ofSeconds(3754), "1h2m34s", Duration.class);
        assertToType(Duration.ofSeconds(7382),
                "2 hours, 3 minutes, and 2 seconds", Duration.class);
        // to string
        assertToString("7382000", Duration.ofSeconds(7382));
    }

    @Test
    void testContentTypeConverter() {
        assertConvert("text/html", ContentType.HTML, ContentType.class);
        assertConvert("image/bmp", ContentType.BMP, ContentType.class);
        assertConvert("fake/one", ContentType.valueOf("fake/one"),
                ContentType.class);
    }

    @Test
    void testCharsetConverter() {
        assertConvert("UTF-8", StandardCharsets.UTF_8, Charset.class);
        assertConvert("ISO-8859-1", StandardCharsets.ISO_8859_1, Charset.class);
        assertThrows(ConverterException.class,
                () -> Converter.convert("badOne", Charset.class));
    }

    @Test
    void testWithDefaultValue() {
        TestStringConverter c = new TestStringConverter();

        assertThat(c.toString(null, "default")).isEqualTo("default");
        assertThat(c.toString("", "default")).isEqualTo("default");
        assertThat(c.toString("blah", "default")).isEqualTo("blah");

        assertThat(c.toType(
                null, String.class, "default")).isEqualTo("default");
        assertThat(c.toType("", String.class, "default")).isEqualTo("default");
        assertThat(c.toType("blah", String.class, "default")).isEqualTo("blah");

        assertThat(Converter.convert("blah", String.class, "default"))
            .isEqualTo("blah");

        assertThat(Converter.convert("blah", "default")).isEqualTo("blah");
    }

    @Test
    void testGetConverters() {
        assertThat(Converter.defaultInstance().getConverters())
            .containsEntry(Locale.class, new LocaleConverter());
    }

    @Test
    void testConvertListClass() {
        List<String> source = Arrays.asList("en_CA", "fr_CA", "it");
        List<Locale> result = Converter.convert(source, Locale.class);
        assertThat(result).containsExactly(
                new Locale("en",  "CA"),
                new Locale("fr",  "CA"),
                new Locale("it"));
    }

    @Test
    void testConvertList() {
        List<Object> source = Arrays.asList(
                new Locale("en",  "CA"),
                new Locale("fr",  "CA"),
                new Locale("it"));
        List<String> result = Converter.convert(source);
        assertThat(result).containsExactly("en_CA", "fr_CA", "it");
    }

    @Test
    void testToStringList() {
        List<Locale> source = Arrays.asList(
                new Locale("en",  "CA"),
                new Locale("fr",  "CA"),
                new Locale("it"));
        List<String> result = Converter.defaultInstance().toString(source);
        assertThat(result).containsExactly("en_CA", "fr_CA", "it");

        assertThat(Converter.defaultInstance().toString((List<?>) null))
            .isEmpty();
    }

    @Test
    void testToStringObject() {
        assertThat(Converter.defaultInstance().toString(
                (String) null)).isNull();
        assertThat(Converter.defaultInstance().toString(
                Integer.valueOf(42))).isEqualTo("42");

        Converter c = Converter.defaultInstance();
        Pattern p = Pattern.compile(".*");
        assertThrows(ConverterException.class, () -> c.toString(p));
    }

    @Test
    void testToStringObjectString() {
        assertThat(Converter.defaultInstance().toString(null, "42"))
            .isEqualTo("42");
        assertThat(Converter.defaultInstance().toString("7", "42"))
            .isEqualTo("7");
    }

    @Test
    void testIsConvertible() {
        assertThat(Converter.defaultInstance().isConvertible(String.class))
            .isTrue();
        assertThat(Converter.defaultInstance().isConvertible(Void.class))
            .isFalse();
    }

    @Test
    void testNullsAndErrors() {
        IConverter c = new AbstractConverter() {
            @Override
            protected <T> T nullSafeToType(String value, Class<T> type)
                    throws Exception {
                return type.cast("toType");
            }
            @Override
            protected String nullSafeToString(Object object) throws Exception {
                return "toString";
            }
        };
        assertThat(c.toType((String) null, String.class)).isNull();
        assertThat(c.toString((Object) null)).isNull();

        IConverter badC = new AbstractConverter() {
            @Override
            protected <T> T nullSafeToType(String value, Class<T> type)
                    throws Exception {
                throw new IllegalArgumentException("blah");
            }
            @Override
            protected String nullSafeToString(Object object) throws Exception {
                throw new IllegalArgumentException("blah");
            }
        };
        assertThrows(ConverterException.class,
                () -> badC.toType("toType", String.class));
        assertThrows(ConverterException.class,
                () -> badC.toString("toString"));
        assertThrows(NullPointerException.class,
                () -> badC.toType("nullType", null));
    }

    private <T> void assertConvert(String strValue, T objValue, Class<T> type) {
        assertToString(strValue, objValue);
        assertToType(objValue, strValue, type);
    }
    private <T> void assertToType(T expected, String value, Class<T> type) {
        Assertions.assertEquals(expected, Converter.convert(value, type));
    }
    private <T> void assertToString(String expected, Object obj) {
        Assertions.assertEquals(expected, Converter.convert(obj));
    }

    private static class TestStringConverter implements IConverter {
        @Override
        public String toString(Object object) {
            return StringUtils.trimToNull(Objects.toString(object, null));
        }
        @Override
        public <T> T toType(String value, Class<T> type) {
            return type.cast(StringUtils.trimToNull(value));
        }

    }
}
