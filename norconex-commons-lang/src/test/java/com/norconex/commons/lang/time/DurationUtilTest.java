/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.time;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class DurationUtilTest {

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long TEST_DURATION = 
            54 * DAY + 18 * HOUR + 1 * MINUTE + 23 * SECOND;
    
    @Test
    public void testShortFormat() {
        Assert.assertEquals("54d18h1m23s", 
                DurationUtil.formatShort(Locale.CHINA, TEST_DURATION));
        Assert.assertEquals("54d18h", 
                DurationUtil.formatShort(Locale.CHINA, TEST_DURATION, 2));
        Assert.assertEquals("54j18h1m23s", 
                DurationUtil.formatShort(Locale.CANADA_FRENCH, TEST_DURATION));
    }

    @Test
    public void testLongFormat() {
        Assert.assertEquals("54 days 18 hours 1 minute 23 seconds", 
                DurationUtil.formatLong(Locale.CHINA, TEST_DURATION));
        Assert.assertEquals("54 days 18 hours", 
                DurationUtil.formatLong(Locale.CHINA, TEST_DURATION, 2));
        Assert.assertEquals("54 jours 18 heures 1 minute 23 secondes", 
                DurationUtil.formatLong(Locale.CANADA_FRENCH, TEST_DURATION));
    }
}
