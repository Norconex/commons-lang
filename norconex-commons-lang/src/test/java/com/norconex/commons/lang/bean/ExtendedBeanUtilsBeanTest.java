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
package com.norconex.commons.lang.bean;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.unit.DataUnit;


public class ExtendedBeanUtilsBeanTest {

    private final ExtendedBeanUtilsBean b = new ExtendedBeanUtilsBean();

    @Test
    public void testLocaleConverter() {
        Assert.assertEquals(
                Locale.CANADA_FRENCH, b.convert("fr_CA", Locale.class));
        Assert.assertEquals(
                "fr_CA", b.convert(Locale.CANADA_FRENCH, String.class));
    }

    @Test
    public void testEnumConverter() {
        Assert.assertEquals(
                DataUnit.GB, b.convert("gB", DataUnit.class));
        Assert.assertEquals(
                "GB", b.convert(DataUnit.GB, String.class));
    }

    @Test
    public void testPathConverter() {
        Assert.assertEquals(
                Paths.get("/tmp/somefile.txt"),
                b.convert("/tmp/somefile.txt", Path.class));
        Assert.assertArrayEquals("/tmp/somefile.txt".split("[\\\\/]"),
                ((String) b.convert(Paths.get(
                        "/tmp/somefile.txt"), String.class)).split("[\\\\/]"));
    }

    @Test
    public void testEpochDateConverter() {
        Date now = new Date();
        String strTime = Long.toString(now.getTime());
        Assert.assertEquals(now, b.convert(strTime, Date.class));
        Assert.assertEquals(strTime, b.convert(now, String.class));
    }

    @Test
    public void testLocalDateTimeConverter() {
        LocalDateTime now = LocalDateTime.now();
        String strTime = now.toString();
        Assert.assertEquals(now, b.convert(strTime, LocalDateTime.class));
        Assert.assertEquals(strTime, b.convert(now, String.class));
    }
}
