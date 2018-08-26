/* Copyright 2010-2018 Norconex Inc.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.config.ConfigurationLoader;

public class PropertiesTest {

    private static final Logger LOG =
            LoggerFactory.getLogger(PropertiesTest.class);

    @Test
    public void testLoadUsingDefaultDelimiter() throws Exception {

        String key = "source";
        String value = "X^2";
        Properties properties = new Properties();
        properties.add(key, value);
        String stored = properties.toString();

        // The default multi-value separator should NOT be applied
        // when there is a single ^.
        properties = new Properties();
        properties.fromString(stored);
        List<String> values = properties.getStrings(key);
        assertEquals(1, values.size());
        assertEquals(values.get(0), value);
    }

    @Test
    public void testGetList() throws Exception {
        Properties properties = new Properties();
        List<String> list = asList("1", "2", "3");
        properties.put("key", list);
        assertEquals(asList(1, 2, 3), properties.getList("key", Integer.class));
    }

    @Test
    public void testGetValue() throws Exception {
        Properties properties = new Properties();
        List<String> list = asList("1", "2", "3");
        properties.put("key", list);
        assertEquals((Integer) 1, properties.getValue("key", Integer.class));
    }

    @Test
    public void testRemove() throws Exception {
        Properties properties = new Properties();
        List<String> list = asList("a", "b", "c");
        properties.put("key", list);
        assertEquals(list, properties.remove("key"));
    }

    @Test
    public void testRemoveCaseInsensitive() throws Exception {
        Properties properties = new Properties(true);
        List<String> list = asList("a", "b", "c");
        properties.put("KEY", list);
        assertEquals(list, properties.remove("key"));
    }

    @Test
    public void testRemoveCaseInsensitiveMultiple() throws Exception {
        Properties properties = new Properties(true);
        List<String> list1 = asList("a", "b", "c");
        List<String> list2 = asList("d", "e", "f");
        properties.put("Key", list1);
        properties.put("KEy", list2);
        assertEquals(list2, properties.remove("key"));
    }

    @Test
    public void testRemoveNonExistentKey() throws Exception {
        Properties properties = new Properties();
        assertNull(properties.remove("key"));
    }

    @Test
    public void testRemoveNonExistentKeyCaseInsensitive() throws Exception {
        Properties properties = new Properties(true);
        assertNull(properties.remove("key"));
    }

    @Test
    public void testAddDifferentCharacterCases() throws Exception {
        Properties properties = new Properties(true);
        properties.add("KEY", "value1");
        properties.add("key", "value2");

        assertEquals(1, properties.keySet().size());
        assertEquals(2, properties.get("kEy").size());
    }

    @Test
    public void testPutAll() throws Exception {
        Map<String, List<String>> m = new TreeMap<>();
        m.put("KEY", Arrays.asList("1", "2"));
        m.put("key", Arrays.asList("3", "4"));

        // Case insensitive
        Properties props1 = new Properties(true);
        props1.putAll(m);

        assertEquals(1, props1.keySet().size());
        assertEquals(4, props1.get("kEy").size());
        assertEquals(Arrays.asList("1", "2", "3", "4"), props1.get("kEy"));


        // Case sensitive
        Properties props2 = new Properties(false);
        props2.putAll(m);

        assertEquals(2, props2.keySet().size());
        assertEquals(null, props2.get("kEy"));
        assertEquals(Arrays.asList("1", "2"), props2.get("KEY"));

    }

    @Test
    public void testPut() throws Exception {
        List<String> list = Arrays.asList("1", null, "2", "");

        // Case insensitive
        Properties props1 = new Properties(true);
        props1.put("key", list);

        assertEquals(1, props1.keySet().size());
        assertEquals(4, props1.get("kEy").size());
        assertEquals(Arrays.asList("1", "", "2", ""), props1.get("kEy"));


        // Case sensitive
        Properties props2 = new Properties(false);
        props2.put("key", list);

        assertEquals(1, props2.keySet().size());
        assertEquals(null, props2.get("kEy"));
        assertEquals(Arrays.asList("1", "", "2", ""), props2.get("key"));
    }

    @Test
    public void testMultiValuesWriterNoDelim() throws Exception {
        StringWriter w = null;
        Properties p = null;

        Properties original = new Properties();
        original.add("KEYsingleValueABC", "singleValueABC");
        original.add("KEYmultiValues", "t", "e", "s", "t");
        original.add("KEYsingleValueXYZ", "singleValueXYZ");
//
//        // String + Default
//        w = new StringWriter();
//        p = new Properties();
//        original.storeToProperties(w);
//        p.loadFromProperties(new StringReader(w.toString()));
//        assertTrue(EqualsUtil.equalsMap(original, p));

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
    public void testMultiValuesWriterDelim() throws Exception {

        StringWriter w = null;
        Properties p = null;

        Properties original = new Properties();
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
    public void testBean() throws Exception {

        TestBean b = new TestBean();
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
        b.testStringArray = new String[] {"arrayVal-1of2", "arrayVal-2of2"};
        b.testIntArray = new int[] {10, 20, 30, 137};
        b.testDateList = new ArrayList<>(Arrays.asList(
                new Date(11111111), new Date(222222222), new Date(3333333)));
        b.testLocalDateTimeArray = new LocalDateTime[] {
                LocalDateTime.of(2001, 1, 1, 1, 1),
                LocalDateTime.of(1969, 8, 23, 17, 5)
        };

        Properties p = new Properties();
        p.add("testString", "carrot"); // should be overwritten

        p.loadFromBean(b);

        LOG.debug("PROPERTIES: " + p);

        TestBean newb = new TestBean();
        p.storeToBean(newb);

        b.testString = "carrot";
        assertEquals(b, newb);
    }

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
        LocalDateTime[] testLocalDateTimeArray;
        public BigDecimal getTestBigDecimal() {
            return testBigDecimal;
        }
        public void setTestBigDecimal(BigDecimal testBigDecimal) {
            this.testBigDecimal = testBigDecimal;
        }
        public boolean isTestBoolean() {
            return testBoolean;
        }
        public void setTestBoolean(boolean testBoolean) {
            this.testBoolean = testBoolean;
        }
        public Class<?> getTestClass() {
            return testClass;
        }
        public void setTestClass(Class<?> testClass) {
            this.testClass = testClass;
        }
        public Date getTestDate() {
            return testDate;
        }
        public void setTestDate(Date testDate) {
            this.testDate = testDate;
        }
        public double getTestDouble() {
            return testDouble;
        }
        public void setTestDouble(double testDouble) {
            this.testDouble = testDouble;
        }
        public File getTestFile() {
            return testFile;
        }
        public void setTestFile(File testFile) {
            this.testFile = testFile;
        }
        public Float getTestFloat() {
            return testFloat;
        }
        public void setTestFloat(Float testFloat) {
            this.testFloat = testFloat;
        }
        public Integer getTestInt() {
            return testInt;
        }
        public void setTestInt(Integer testInt) {
            this.testInt = testInt;
        }
        public LocalDateTime getTestLocalDateTime() {
            return testLocalDateTime;
        }
        public void setTestLocalDateTime(LocalDateTime testLocalDateTime) {
            this.testLocalDateTime = testLocalDateTime;
        }
        public Locale getTestLocale() {
            return testLocale;
        }
        public void setTestLocale(Locale testLocale) {
            this.testLocale = testLocale;
        }
        public Long getTestLong() {
            return testLong;
        }
        public void setTestLong(Long testLong) {
            this.testLong = testLong;
        }
        public String getTestString() {
            return testString;
        }
        public void setTestString(String testString) {
            this.testString = testString;
        }
        public String[] getTestStringArray() {
            return testStringArray;
        }
        public void setTestStringArray(String[] testStringArray) {
            this.testStringArray = testStringArray;
        }
        public int[] getTestIntArray() {
            return testIntArray;
        }
        public void setTestIntArray(int[] testIntArray) {
            this.testIntArray = testIntArray;
        }
        public List<Date> getTestDateList() {
            return testDateList;
        }
        public void setTestDateList(List<Date> testDateList) {
            this.testDateList = testDateList;
        }
        public LocalDateTime[] getTestLocalDateTimeArray() {
            return testLocalDateTimeArray;
        }
        public void setTestLocalDateTimeArray(
                LocalDateTime[] testLocalDateTimeArray) {
            this.testLocalDateTimeArray = testLocalDateTimeArray;
        }
        @Override
        public boolean equals(final Object other) {
            return EqualsBuilder.reflectionEquals(this, other);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
        @Override
        public String toString() {
            return new ReflectionToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
        }
    }
}
