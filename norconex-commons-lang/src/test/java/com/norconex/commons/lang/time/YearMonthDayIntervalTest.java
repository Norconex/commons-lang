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

import org.junit.Assert;
import org.junit.Test;

public class YearMonthDayIntervalTest {

    @Test
    public void testGetYears() {
        Assert.assertEquals(3, interval("2000-01-15 - 2004-01-14").getYears());
        Assert.assertEquals(4, interval("2000-01-15 - 2004-01-15").getYears());
        Assert.assertEquals(4, interval("2000-01-15 - 2004-01-16").getYears());
    }

    @Test
    public void testGetMonths() {
        Assert.assertEquals(
                24, interval("2000-03-15 - 2002-03-15").getMonths());
        Assert.assertEquals(
                23, interval("2000-03-15 - 2002-03-14").getMonths());
        Assert.assertEquals(
                27, interval("2000-03-01 - 2002-06-30").getMonths());
        Assert.assertEquals(
                21, interval("2000-06-01 - 2002-03-30").getMonths());
    }

    @Test
    public void testGetDays() {
        Assert.assertEquals(365, interval("2001-03-15 - 2002-03-15").getDays());
        Assert.assertEquals(364, interval("2001-03-15 - 2002-03-14").getDays());
        Assert.assertEquals(485, interval("2001-03-01 - 2002-06-30").getDays());
        Assert.assertEquals(61, interval("2001-12-01 - 2002-01-31").getDays());
    }
    
    private YearMonthDayInterval interval(String interval) {
        return new YearMonthDayInterval(interval);
    }
}
