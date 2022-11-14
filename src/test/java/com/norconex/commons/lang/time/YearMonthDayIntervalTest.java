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
package com.norconex.commons.lang.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.from;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

class YearMonthDayIntervalTest {

    @Test
    void testGetYears() {
        assertEquals(3, interval("2000-01-15 - 2004-01-14").getYears());
        assertEquals(4, interval("2000-01-15 - 2004-01-15").getYears());
        assertEquals(4, interval("2000-01-15 - 2004-01-16").getYears());
    }

    @Test
    void testGetMonths() {
        assertEquals(24, interval("2000-03-15 - 2002-03-15").getMonths());
        assertEquals(23, interval("2000-03-15 - 2002-03-14").getMonths());
        assertEquals(27, interval("2000-03-01 - 2002-06-30").getMonths());
        assertEquals(21, interval("2000-06-01 - 2002-03-30").getMonths());
    }

    @Test
    void testGetDays() {
        assertEquals(365, interval("2001-03-15 - 2002-03-15").getDays());
        assertEquals(364, interval("2001-03-15 - 2002-03-14").getDays());
        assertEquals(486, interval("2001-03-01 - 2002-06-30").getDays());
        assertEquals(61, interval("2001-12-01 - 2002-01-31").getDays());
    }

    @Test
    void testMisc() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy( //NOSONAR
                () -> new YearMonthDayInterval(
                        new YearMonthDay(2004, 1, 15),
                        new YearMonthDay(2000, 1, 14)));
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy( //NOSONAR
                () -> new YearMonthDayInterval(
                        null, new YearMonthDay(200, 1, 14)));
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy( //NOSONAR
                () -> new YearMonthDayInterval(
                        new YearMonthDay(2004, 1, 15), null));

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy( //NOSONAR
                    () -> interval(""));

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy( //NOSONAR
                () -> new YearMonthDayInterval(new Date(), null));
        assertThatNoException().isThrownBy(() -> new YearMonthDayInterval(
                new Date(), new Date()));

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy( //NOSONAR
                () -> new YearMonthDayInterval(Calendar.getInstance(), null));
        assertThatNoException().isThrownBy(() -> new YearMonthDayInterval(
                Calendar.getInstance(), Calendar.getInstance()));

        YearMonthDayInterval ymdi = new YearMonthDayInterval(
                new YearMonthDay(2000, 1, 15),
                new YearMonthDay(2004, 1, 14));
        assertThat(ymdi)
            .returns(new YearMonthDay(2000, 1, 15),
                    from(YearMonthDayInterval::getStart))
            .returns(new YearMonthDay(2004, 1, 14),
                    from(YearMonthDayInterval::getEnd))
            .returns(new YearMonthDay(2000, 1, 15).toDate(),
                    from(YearMonthDayInterval::getStartDate))
            .returns(new YearMonthDay(2004, 1, 14).toDate(),
                    from(YearMonthDayInterval::getEndDate))
            .returns(new YearMonthDay(2004, 1, 15).toDate(),
                    from(YearMonthDayInterval::getEndDateEndOfDay))
            .hasToString("2000-01-15 - 2004-01-14");
    }

    @Test
    void testContains() {
        YearMonthDayInterval ymdi = new YearMonthDayInterval(
                new YearMonthDay(2000, 1, 15),
                new YearMonthDay(2001, 1, 15));

        assertThat(ymdi.contains(ymd("2000-01-14"))).isFalse();
        assertThat(ymdi.contains(ymd("2000-01-14").toDate())).isFalse();

        assertThat(ymdi.contains(ymd("2000-01-15"))).isTrue();
        assertThat(ymdi.contains(ymd("2000-01-15").toDate())).isTrue();

        assertThat(ymdi.contains(ymd("2000-06-23"))).isTrue();
        assertThat(ymdi.contains(ymd("2000-06-23").toDate())).isTrue();

        assertThat(ymdi.contains(ymd("2001-01-14"))).isTrue();
        assertThat(ymdi.contains(ymd("2001-01-14").toDate())).isTrue();

        assertThat(ymdi.contains(ymd("2001-01-15"))).isTrue();
        assertThat(ymdi.contains(ymd("2001-01-15").toDate())).isTrue();

        assertThat(ymdi.contains(ymd("2001-01-16"))).isFalse();
        assertThat(ymdi.contains(ymd("2001-01-16").toDate())).isFalse();
    }

    private YearMonthDayInterval interval(String interval) {
        return new YearMonthDayInterval(interval);
    }
    private YearMonthDay ymd(String ymd) {
        return new YearMonthDay(ymd);
    }
}
