/* Copyright 2018 Norconex Inc.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.EqualsUtil;
import com.norconex.commons.lang.unit.DataUnit;


public class ConverterTest {

    @Test
    public void testNumberConverter() {
        assertConvert("64", (byte) 64, byte.class);
        assertConvert("64", Byte.valueOf((byte) 64), Byte.class);

        assertConvert("37", (short) 37, short.class);
        assertConvert("37", Short.valueOf((short) 37), Short.class);

        assertConvert("123", 123, int.class);
        assertConvert("123", Integer.valueOf(123), Integer.class);

        assertConvert("456.78", 456.78f, float.class);
        assertConvert("456.78", Float.valueOf(456.78f), Float.class);

        assertConvert("901", 901L, long.class);
        assertConvert("901", Long.valueOf(901L), Long.class);

        assertConvert("234.56", 234.56d, double.class);
        assertConvert("234.56", Double.valueOf(234.56d), Double.class);

        assertConvert("789012", BigInteger.valueOf(789012), BigInteger.class);
        assertConvert("34.6789", BigDecimal.valueOf(34.6789), BigDecimal.class);
    }


    @Test
    public void testLocaleConverter() {
        assertConvert("fr_CA", Locale.CANADA_FRENCH, Locale.class);
    }

    @Test
    public void testEnumConverter() {
        assertConvert("GB", DataUnit.GB, DataUnit.class);
    }

    @Test
    public void testFileConverter() {
        String filePath = new File("/tmp/filepath.txt").getAbsolutePath();
        assertConvert(filePath, new File(filePath), File.class);
        assertConvert(filePath, Paths.get(filePath), Path.class);
    }

    @Test
    public void testDateConverter() {
        Date now = new Date();
        assertConvert(Long.toString(now.getTime()), now, Date.class);
    }

    @Test
    public void testLocalDateTimeConverter() {
        LocalDateTime now = LocalDateTime.now();
        assertConvert(now.toString(), now, LocalDateTime.class);
    }

    @Test
    public void testDimensionConverter() {
        Dimension d = new Dimension(640, 480);

        //to string
        Assert.assertEquals("640x480", Converter.convert(d));

        //from string
        Assert.assertEquals(d, Converter.convert("640x480", Dimension.class));
        Assert.assertEquals(d, Converter.convert("640 480", Dimension.class));
        Assert.assertEquals(d, Converter.convert("width:640, h:480", Dimension.class));
        Assert.assertEquals(d, Converter.convert("aaa640b480cc10", Dimension.class));
    }

    @Test
    public void testBooleanConverter() {
        assertConvert("true", true, boolean.class);
        assertConvert("true", Boolean.TRUE, Boolean.class);
        assertConvert("false", false, boolean.class);
        assertConvert("false", Boolean.FALSE, Boolean.class);
        assertToType(true, "yes", boolean.class);
        assertToType(Boolean.FALSE, "no", Boolean.class);
    }

    @Test
    public void testCharacterConverter() {
        assertConvert("a", 'a', char.class);
        assertConvert("b", new Character('b'), Character.class);
    }

    @Test
    public void testClassConverter() {
        assertConvert("com.norconex.commons.lang.EqualsUtil",
                EqualsUtil.class, Class.class);
    }

    @Test
    public void testStringConverter() {
        assertConvert("blah", new String("blah"), String.class);
    }

    @Test
    public void testURLConverter() throws MalformedURLException {
        assertConvert("http://example.com/blah.html",
                new URL("http://example.com/blah.html"), URL.class);
        assertToType(new URL("http://example.com/%22fix%22"),
                "http://example.com/\"fix\"", URL.class);
    }

    private <T> void assertConvert(String strValue, T objValue, Class<T> type) {
        assertToString(strValue, objValue);
        assertToType(objValue, strValue, type);
    }
    private <T> void assertToType(T expected, String value, Class<T> type) {
        Assert.assertEquals(expected, Converter.convert(value, type));
    }
    private <T> void assertToString(String expected, Object obj) {
        Assert.assertEquals(expected, Converter.convert(obj));
    }
}
