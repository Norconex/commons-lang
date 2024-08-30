/* Copyright 2010-2022 Norconex Inc.
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
package com.norconex.commons.lang.map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.text.TextMatcher;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PropertiesTest {

    @Test
    void testWriteRead() throws IOException {
        var props = new Properties();
        props.add("aaa", 111);
        props.add("aaa", 222);
        props.add("bbb", 333);
        props.add("bbb", 444);
        props.add("ccc", 555);

        assertThatNoException().isThrownBy(() -> {
            BeanMapper.DEFAULT.assertWriteRead(props);
        });
    }

    @Test
    void testValueList() {
        var properties = sampleProps();
        assertThat(properties.valueList()).contains(
                "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    void testMiscGetters() {
        var props = new Properties();
        props.add("bool", "1", "0", "true", "FALSE", "tRuE",
                "yes", "nO", "On", "oFf");
        props.add("locale", "fr_CA", "es");
        props.add("file", "c:\\blah", "/opt/blah");
        props.add("class", "java.lang.Integer", "java.lang.Double");

        assertThat(props.getBoolean("bool")).isTrue();
        assertThat(props.getBoolean("bad", true)).isTrue();
        assertThat(props.getBooleans("bool")).containsExactly(
                true, false, true, false, true, true, false, true, false);

        assertThat(props.getLocale("locale")).isEqualTo(Locale.CANADA_FRENCH);
        assertThat(props.getLocale("bad", Locale.GERMANY))
                .isEqualTo(Locale.GERMANY);
        assertThat(props.getLocales("locale")).containsExactly(
                Locale.CANADA_FRENCH, new Locale("es"));

        assertThat(props.getFile("file")).isEqualTo(new File("c:\\blah"));
        assertThat(props.getFile("bad", new File("/tmp")))
                .isEqualTo(new File("/tmp"));
        assertThat(props.getFiles("file")).containsExactly(
                new File("c:\\blah"), new File("/opt/blah"));

        assertThat(props.getClass("class")).isEqualTo(Integer.class);
        assertThat(props.getClass("bad", Float.class)).isEqualTo(Float.class);
        assertThat(props.getClasses("class")).containsExactly(
                Integer.class, Double.class);
    }

    @Test
    void testNumberGetters() {
        var props = new Properties();
        props.add("numbers", "1", "2", "3");

        assertThat(props.getInteger("numbers")).isOne();
        assertThat(props.getInteger("bad", 2)).isEqualTo(2);
        assertThat(props.getIntegers("numbers")).containsExactly(1, 2, 3);

        assertThat(props.getDouble("numbers")).isEqualTo(1d);
        assertThat(props.getDouble("bad", 4D)).isEqualTo(4D);
        assertThat(props.getDoubles("numbers")).containsExactly(1D, 2D, 3D);

        assertThat(props.getLong("numbers")).isEqualTo(1L);
        assertThat(props.getLong("bad", 4L)).isEqualTo(4L);
        assertThat(props.getLongs("numbers")).containsExactly(1L, 2L, 3L);

        assertThat(props.getFloat("numbers")).isEqualTo(1F);
        assertThat(props.getFloat("bad", 4F)).isEqualTo(4F);
        assertThat(props.getFloats("numbers")).containsExactly(1F, 2F, 3F);

        assertThat(props.getBigDecimal("numbers")).isEqualTo(
                BigDecimal.valueOf(1));
        assertThat(props.getBigDecimal("bad",
                BigDecimal.valueOf(4))).isEqualTo(BigDecimal.valueOf(4));
        assertThat(props.getBigDecimals("numbers")).containsExactly(
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(3));
    }

    @Test
    void testDateGetters() {
        var props = new Properties();
        props.add("localDateTime",
                "2022-11-06T16:20:02", "2023-11-06T16:20:02");
        props.add("instant", "2022-11-06T16:20:02Z", "2023-11-06T16:20:02Z");
        props.add("date",
                "1667769602000", // 2022-11-06T16:20:02
                "1699305602000"); // 2023-11-06T16:20:02

        assertThat(props.getLocalDateTime("localDateTime")).isEqualTo(
                LocalDateTime.of(2022, 11, 6, 16, 20, 2));
        assertThat(props.getLocalDateTime("bad",
                LocalDateTime.of(2000, 12, 31, 1, 2, 3))).isEqualTo(
                        LocalDateTime.of(2000, 12, 31, 1, 2, 3));
        assertThat(props.getLocalDateTimes("localDateTime")).containsExactly(
                LocalDateTime.of(2022, 11, 6, 16, 20, 2),
                LocalDateTime.of(2023, 11, 6, 16, 20, 2));

        assertThat(props.getInstant("instant")).isEqualTo(
                Instant.parse("2022-11-06T16:20:02Z"));
        assertThat(props.getInstant(
                "bad", Instant.parse("2020-12-31T01:02:03Z")))
                        .isEqualTo(Instant.parse("2020-12-31T01:02:03Z"));
        assertThat(props.getInstants("instant")).containsExactly(
                Instant.parse("2022-11-06T16:20:02Z"),
                Instant.parse("2023-11-06T16:20:02Z"));

        assertThat(props.getDate("date")).isEqualTo(
                new Date(1667769602000L));
        assertThat(props.getDate("bad", new Date(1730928002000L)))
                .isEqualTo(new Date(1730928002000L));
        assertThat(props.getDates("date")).containsExactly(
                new Date(1667769602000L),
                new Date(1699305602000L));
    }

    @Test
    void testSetValue() {
        var properties = new Properties();
        properties.set("a", "1", "2", "3");
        properties.set("a", "4", "5", "6");
        assertThat(properties.get("a")).containsExactly("4", "5", "6");
    }

    @Test
    void testSetAddList() {
        var properties = new Properties();
        properties.set("a", "1", "2", "3");
        properties.setList("a", null);
        assertThat(properties.get("a")).isNull();

        properties.add("a", "1", "2", "3");
        properties.addList("a", null);
        assertThat(properties.get("a")).containsExactly("1", "2", "3");
    }

    @Test
    void testToJavaUtilProperties() {
        var properties = sampleProps();
        var javaProps = properties.toProperties();

        assertThat(javaProps).hasSize(3);
        assertThat(javaProps.getProperty("a")).isEqualTo("1\\u241E2\\u241E3");
        assertThat(javaProps.getProperty("b")).isEqualTo("4\\u241E5\\u241E6");
        assertThat(javaProps.getProperty("abc")).isEqualTo("7\\u241E8\\u241E9");
    }

    @Test
    void testStoreToLoadFromJson() throws IOException {
        var properties = sampleProps();
        var out = new ByteArrayOutputStream();
        properties.storeToJSON(out);

        var newProps = new Properties();
        newProps.loadFromJSON(new ByteArrayInputStream(out.toByteArray()));
        assertThat(newProps).isEqualTo(properties);

        assertDoesNotThrow(() -> properties.loadFromJSON((InputStream) null));
        assertDoesNotThrow(() -> properties.loadFromJSON((Reader) null));
    }

    @Test
    void testStoreToLoadFromXml() throws IOException {
        var properties = sampleProps();
        var out = new ByteArrayOutputStream();
        properties.storeToXML(out);

        var newProps = new Properties();
        newProps.loadFromXML(new ByteArrayInputStream(out.toByteArray()));
        assertThat(newProps).isEqualTo(properties);

        assertDoesNotThrow(() -> properties.loadFromXML((InputStream) null));
        assertDoesNotThrow(() -> properties.loadFromXML((Reader) null));
    }

    @Test
    void testStoreToLoadFromProperties() throws IOException {
        var properties = sampleProps();
        var out = new ByteArrayOutputStream();
        properties.storeToProperties(out);

        var newProps = new Properties();
        newProps.loadFromProperties(
                new ByteArrayInputStream(out.toByteArray()));
        assertThat(newProps).isEqualTo(properties);

        assertDoesNotThrow(
                () -> properties.loadFromProperties((InputStream) null));
        assertDoesNotThrow(() -> properties.loadFromProperties((Reader) null));
    }

    @Test
    void testMatchKeys() {
        var properties = sampleProps();
        assertThat(properties.matchKeys(TextMatcher.basic("a")))
                .containsExactlyEntriesOf(MapUtil.toMap(
                        "a", asList("1", "2", "3")));
        assertThat(properties.matchKeys(
                TextMatcher.regex("a").setPartial(true)))
                        .containsExactlyEntriesOf(MapUtil.toMap(
                                "a", asList("1", "2", "3"),
                                "abc", asList("7", "8", "9")));
        assertThat(properties.matchKeys(null)).isEmpty();
    }

    @Test
    void testMatchValues() {
        var properties = sampleProps();
        assertThat(properties.matchValues(TextMatcher.basic("2")))
                .containsExactlyEntriesOf(MapUtil.toMap(
                        "a", asList("2")));
        assertThat(properties.matchValues(
                TextMatcher.regex("1|3|8").setPartial(true)))
                        .containsExactlyEntriesOf(MapUtil.toMap(
                                "a", asList("1", "3"),
                                "abc", asList("8")));
        assertThat(properties.matchValues(null)).isEmpty();
    }

    @Test
    void testMatch() {
        var properties = sampleProps();
        assertThat(properties.match(
                TextMatcher.basic("a"), TextMatcher.basic("2")))
                        .containsExactlyEntriesOf(MapUtil.toMap(
                                "a", asList("2")));
        assertThat(properties.match(
                TextMatcher.basic("a"), TextMatcher.basic("6")))
                        .isEmpty();
        assertThat(properties.match(
                TextMatcher.regex("a").setPartial(true),
                TextMatcher.regex("1|3|8").setPartial(true)))
                        .containsExactlyEntriesOf(MapUtil.toMap(
                                "a", asList("1", "3"),
                                "abc", asList("8")));
        assertThat(properties.match(null)).isEmpty();
    }

    @Test
    void testLoadUsingDefaultDelimiter() {

        var key = "source";
        var value = "X^2";
        var properties = new Properties();
        properties.add(key, value);
        var stored = properties.toString();

        // The default multi-value separator should NOT be applied
        // when there is a single ^.
        properties = new Properties();
        properties.fromString(stored);
        var values = properties.getStrings(key);
        assertEquals(1, values.size());
        assertEquals(values.get(0), value);
    }

    @Test
    void testGetList() {
        var properties = new Properties();
        List<String> list = asList("1", "2", "3");
        properties.put("key", list);
        assertEquals(asList(1, 2, 3), properties.getList("key", Integer.class));

        assertThat(properties.getList(
                "key", String.class, Collections.emptyList()))
                        .containsExactly("1", "2", "3");

        assertThat(properties.getList(
                "nonExist", String.class, Arrays.asList("x", "y", "z")))
                        .containsExactly("x", "y", "z");
    }

    @Test
    void testGetValue() {
        var properties = new Properties(MapUtil.toMap(
                "key", asList("1", "2", "3")));
        assertEquals((Integer) 1, properties.get("key", Integer.class));

        assertThat(properties.get("nonExist", String.class, "some default"))
                .isEqualTo("some default");

        assertThat(properties.getString("nonExist", "some default"))
                .isEqualTo("some default");

    }

    @Test
    void testRemove() {
        var properties = new Properties();
        List<String> list = asList("a", "b", "c");
        properties.put("key", list);
        assertEquals(list, properties.remove("key"));
    }

    @Test
    void testRemoveCaseInsensitive() {
        var properties = new Properties(true);
        List<String> list = asList("a", "b", "c");
        properties.put("KEY", list);
        assertEquals(list, properties.remove("key"));
    }

    @Test
    void testRemoveCaseInsensitiveMultiple() {
        var properties = new Properties(true);
        List<String> list1 = asList("a", "b", "c");
        List<String> list2 = asList("d", "e", "f");
        properties.put("Key", list1);
        properties.put("KEy", list2);
        assertEquals(list2, properties.remove("key"));
    }

    @Test
    void testRemoveNonExistingKey() {
        var properties = new Properties();
        assertNull(properties.remove("key"));
    }

    @Test
    void testRemoveNonExistingKeyCaseInsensitive() {
        var properties = new Properties(true);
        assertNull(properties.remove("key"));
    }

    @Test
    void testAddDifferentCharacterCases() {
        var properties = new Properties(true);
        properties.add("KEY", "value1");
        properties.add("key", "value2");

        assertEquals(1, properties.size());
        assertEquals(2, properties.get("kEy").size());
    }

    @Test
    void testPutAll() {
        Map<String, List<String>> m = new TreeMap<>();
        m.put("KEY", Arrays.asList("1", "2"));
        m.put("key", Arrays.asList("3", "4"));

        // Case insensitive
        var props1 = new Properties(true);
        props1.putAll(m);

        assertEquals(1, props1.size());
        assertEquals(4, props1.get("kEy").size());
        assertEquals(Arrays.asList("1", "2", "3", "4"), props1.get("kEy"));

        // Case sensitive
        var props2 = new Properties(false);
        props2.putAll(m);

        assertEquals(2, props2.size());
        assertEquals(null, props2.get("kEy"));
        assertEquals(Arrays.asList("1", "2"), props2.get("KEY"));

    }

    @Test
    void testPut() {
        List<String> list = Arrays.asList("1", null, "2", "");

        // Case insensitive
        var props1 = new Properties(true);
        props1.put("key", list);

        assertEquals(1, props1.size());
        assertEquals(4, props1.get("kEy").size());
        assertEquals(Arrays.asList("1", "", "2", ""), props1.get("kEy"));

        // Case sensitive
        var props2 = new Properties(false);
        props2.put("key", list);

        assertEquals(1, props2.size());
        assertEquals(null, props2.get("kEy"));
        assertEquals(Arrays.asList("1", "", "2", ""), props2.get("key"));

        props2.put("key", null);
        assertThat(props2.get("key")).isNull();
    }

    @Test
    void testMultiValuesWriterNoDelim() throws Exception {
        StringWriter w;
        Properties p;

        var original = new Properties();
        original.add("KEYsingleValueABC", "singleValueABC");
        original.add("KEYmultiValues", "t", "e", "s", "t");
        original.add("KEYsingleValueXYZ", "singleValueXYZ");

        // XML
        w = new StringWriter();
        p = new Properties();
        original.storeToXML(w);
        p.loadFromXML(new StringReader(w.toString()));
        assertTrue(EqualsUtil.equalsMap(original, p));

        // JSON
        w = new StringWriter();
        p = new Properties();
        original.storeToJSON(w);
        p.loadFromJSON(new StringReader(w.toString()));
        assertTrue(EqualsUtil.equalsMap(original, p));
    }

    @Test
    void testMultiValuesWriterDelim() throws Exception {

        StringWriter w;
        Properties p;

        var original = new Properties();
        original.add("KEYsingleValueABC", "singleValueABC");
        original.add("KEYmultiValues", "t", "e", "s", "t");
        original.add("KEYsingleValueXYZ", "singleValueXYZ");

        // String + Default
        w = new StringWriter();
        p = new Properties();
        original.storeToProperties(w, "^^^");
        p.loadFromProperties(new StringReader(w.toString()), "^^^");
        assertTrue(EqualsUtil.equalsMap(original, p));

        // XML
        w = new StringWriter();
        p = new Properties();
        original.storeToXML(w, "^^^");
        p.loadFromXML(new StringReader(w.toString()), "^^^");
        assertTrue(EqualsUtil.equalsMap(original, p));

        // JSON
        // JSON does not support delimiters
    }

    @Test
    void testBean() throws Exception {

        var b = new TestBean();
        b.testBigDecimal = BigDecimal.valueOf(3.1416);
        b.testBoolean = true;
        b.testClass = ConfigurationLoader.class;
        b.testDate = new Date();
        b.testDouble = 2.7183;
        b.testFile = new File("/temp");
        b.testFloat = 1.4142f;
        b.testInt = 42;
        b.testLocalDateTime = LocalDateTime.now();
        b.testLocale = Locale.CANADA_FRENCH;
        b.testLong = 1L;
        b.testString = "potato";
        b.testStringArray = new String[] { "arrayVal-1of2", "arrayVal-2of2" };
        b.testIntArray = new int[] { 10, 20, 30, 137 };
        b.testDateList = new ArrayList<>(Arrays.asList(
                new Date(11111111), new Date(222222222), new Date(3333333)));
        b.testStringSet = new TreeSet<>(Arrays.asList("a", "b", "c"));
        b.testLocalDateTimeArray = new LocalDateTime[] {
                LocalDateTime.of(2001, 1, 1, 1, 1),
                LocalDateTime.of(1969, 8, 23, 17, 5)
        };
        b.nonWritableString = "def";

        var p = new Properties();
        p.add("testString", "carrot"); // should be overwritten

        p.loadFromBean(b);

        LOG.debug("PROPERTIES: " + p);

        var newb = new TestBean();
        p.storeToBean(newb);

        b.testString = "carrot";
        assertEquals(b, newb);

        assertDoesNotThrow(() -> p.storeToBean(null));
        assertDoesNotThrow(() -> p.loadFromBean(null));
    }

    private Properties sampleProps() {
        return new Properties(MapUtil.toMap(
                "a", asList("1", "2", "3"),
                "b", asList("4", "5", "6"),
                "abc", asList("7", "8", "9")));
    }

    @Data
    public static class TestBean {
        BigDecimal testBigDecimal;
        boolean testBoolean;
        Class<?> testClass;
        Date testDate;
        double testDouble;
        File testFile;
        Float testFloat;
        Integer testInt;
        LocalDateTime testLocalDateTime;
        Locale testLocale;
        Long testLong;
        String testString;
        String[] testStringArray;
        int[] testIntArray;
        List<Date> testDateList;
        Set<String> testStringSet;
        LocalDateTime[] testLocalDateTimeArray;
        @Getter
        String nonWritableString;
    }
}
