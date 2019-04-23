/* Copyright 2010-2019 Norconex Inc.
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
package com.norconex.commons.lang.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class YearMonthDayIntervalTest {

    @Test
    public void testGetYears() {
        Assertions.assertEquals(3, interval("2000-01-15 - 2004-01-14").getYears());
        Assertions.assertEquals(4, interval("2000-01-15 - 2004-01-15").getYears());
        Assertions.assertEquals(4, interval("2000-01-15 - 2004-01-16").getYears());
    }

    @Test
    public void testGetMonths() {
        Assertions.assertEquals(
                24, interval("2000-03-15 - 2002-03-15").getMonths());
        Assertions.assertEquals(
                23, interval("2000-03-15 - 2002-03-14").getMonths());
        Assertions.assertEquals(
                27, interval("2000-03-01 - 2002-06-30").getMonths());
        Assertions.assertEquals(
                21, interval("2000-06-01 - 2002-03-30").getMonths());
    }

    @Test
    public void testGetDays() {
        Assertions.assertEquals(365, interval("2001-03-15 - 2002-03-15").getDays());
        Assertions.assertEquals(364, interval("2001-03-15 - 2002-03-14").getDays());
        Assertions.assertEquals(485, interval("2001-03-01 - 2002-06-30").getDays());
        Assertions.assertEquals(61, interval("2001-12-01 - 2002-01-31").getDays());
    }

    private YearMonthDayInterval interval(String interval) {
        return new YearMonthDayInterval(interval);
    }
}
