/* Copyright 2022 Norconex Inc.
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

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

class YearMonthDayTest {

    @Test
    void testYearMonthDay() {
        assertThat(new YearMonthDay()).isBetween(
                new YearMonthDay().addDays(-1), new YearMonthDay().addDays(1));

        YearMonthDay ymd2000 = new YearMonthDay(2000, 1, 1);
        Date date2000 = new Date(946713238000L); //2000-01-01T07:53:58
        Date date2000StartOfDay = new Date(946684800000L); //2000-01-01T00:00:00

        assertThat(ymd2000)
                .isEqualTo(new YearMonthDay(2000))
                .isEqualTo(new YearMonthDay(2000, 1))
                .isEqualTo(new YearMonthDay("2000-01-01"));
        assertThat(ymd2000.isBefore(new YearMonthDay(2000))).isFalse();
        assertThat(ymd2000.isBefore(new YearMonthDay(1999, 12, 3))).isFalse();
        assertThat(ymd2000.isBefore(new YearMonthDay(2000, 1, 2))).isTrue();
        assertThat(ymd2000.isBeforeDate(new Date())).isTrue();
        assertThat(ymd2000.isBeforeDate(date2000)).isFalse();
        assertThat(ymd2000.isAfter(new YearMonthDay(2000))).isFalse();
        assertThat(ymd2000.isAfter(new YearMonthDay(1999, 12, 3))).isTrue();
        assertThat(ymd2000.isAfter(new YearMonthDay(2000, 1, 2))).isFalse();
        assertThat(ymd2000.isAfterDate(new Date())).isFalse();
        assertThat(ymd2000.isAfterDate(date2000)).isFalse();
        assertThat(ymd2000.contains(new Date())).isFalse();
        assertThat(ymd2000.contains(date2000)).isTrue();
        assertThat(ymd2000.toDate()).isEqualTo(date2000StartOfDay);
        assertThat(ymd2000.toMillis()).isEqualTo(date2000StartOfDay.getTime());
        assertThat(ymd2000.toEndOfDayDate()).isEqualTo(
                DateUtils.addDays(date2000StartOfDay, 1));

        assertThat(ymd2000.addMonths(15))
                .isEqualTo(new YearMonthDay(2001, 4, 1));
        assertThat(ymd2000.addYears(-99))
                .isEqualTo(new YearMonthDay(1901, 1, 1));

        assertThat(ymd2000.compareTo(null)).isEqualTo(-1);

        assertThat(ymd2000.toString("MMM d, yyyy")).isEqualTo("Jan 1, 2000");

        assertThat(ymd2000.toLocalDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(ymd2000)
                .isEqualTo(new YearMonthDay(LocalDate.of(2000, 1, 1)));
    }

    @Test
    void testErrors() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new YearMonthDay((Date) null));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new YearMonthDay((Calendar) null));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new YearMonthDay((String) null));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new YearMonthDay((LocalDate) null));
    }
}
