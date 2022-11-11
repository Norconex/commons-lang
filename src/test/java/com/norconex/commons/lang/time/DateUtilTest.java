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

import static com.norconex.commons.lang.time.DateUtil.toDate;
import static com.norconex.commons.lang.time.DateUtil.toDateUTC;
import static com.norconex.commons.lang.time.DateUtil.toLocalDate;
import static com.norconex.commons.lang.time.DateUtil.toLocalDateTime;
import static com.norconex.commons.lang.time.DateUtil.toLocalDateTimeUTC;
import static com.norconex.commons.lang.time.DateUtil.toLocalDateUTC;
import static java.time.LocalDateTime.ofEpochSecond;
import static java.util.TimeZone.getTimeZone;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class DateUtilTest {

    private static final ZonedDateTime utcZonedDateTime = ZonedDateTime.of(
            2022, 11, 10, 4, 49, 51, 0, ZoneOffset.UTC);

    private static final ZoneOffset utcZoneOffset = ZoneOffset.UTC;
    private static final ZoneOffset sysZoneOffset =
            utcZonedDateTime.withZoneSameInstant(
                    ZoneOffset.systemDefault()).getOffset();
    private static final ZoneOffset vanZoneOffset = ZoneOffset.ofHours(-8);

    // Date & Time: 2022-11-10T04:49:51.000
    private static final long utcDateTimeAsEpoch = 1668055791000L;
    private static final int sysOffsetMs =
            getTimeZone(ZoneId.systemDefault()).getOffset(utcDateTimeAsEpoch);
    private static final int vanOffsetMs =
            getTimeZone(ZoneOffset.ofHours(-8)).getOffset(utcDateTimeAsEpoch);
    private static final long sysDateTimeAsEpoch =
            utcDateTimeAsEpoch - sysOffsetMs;
    private static final long vanDateTimeAsEpoch =
            utcDateTimeAsEpoch - vanOffsetMs;

    // Date (no time): 2022-11-10
    private static final long utcDateAsEpoch = 1668038400000L;
    private static final long sysDateAsEpoch = utcDateAsEpoch
            - getTimeZone(ZoneId.systemDefault()).getOffset(utcDateAsEpoch);
    private static final long vanDateAsEpoch = utcDateAsEpoch
            - getTimeZone(ZoneOffset.ofHours(-8)).getOffset(utcDateAsEpoch);

    static {
        LOG.debug("'2022-11-10T04:49:51' local date as EPOCH "
                + "if you are located in...");
        LOG.debug("  Greenwich: {} (at start of day: {})",
                utcDateTimeAsEpoch, utcDateAsEpoch);
        LOG.debug("  >>Here<< : {} (at start of day: {})",
                sysDateTimeAsEpoch, sysDateAsEpoch);
        LOG.debug("  Vancouver: {} (at start of day: {})",
                vanDateTimeAsEpoch, vanDateAsEpoch);
    }

    @Test
    void testDateToFromLocalDateTime() {
        Date utcDateTime = new Date(utcDateTimeAsEpoch);
        Date sysDateTime = new Date(sysDateTimeAsEpoch);
        Date vanDateTime = new Date(vanDateTimeAsEpoch);

        // Date to LocalDateTime
        assertEquals(toLocalDateTimeUTC(utcDateTime), utcLocalDateTime());
        assertEquals(toLocalDateTime(utcDateTime), sysLocalDateTime());
        assertEquals(toLocalDateTime(
                utcDateTime, vanZoneOffset), vanLocalDateTime());

        // LocalDateTime to Date
        assertEquals(toDateUTC(utcLocalDateTime()), utcDateTime);
        assertEquals(toDate(utcLocalDateTime()), sysDateTime);
        assertEquals(toDate(utcLocalDateTime(), vanZoneOffset), vanDateTime);

        // Epoch to LocalDateTime
        assertEquals(
                toLocalDateTimeUTC(utcDateTimeAsEpoch), utcLocalDateTime());
        assertEquals(toLocalDateTime(utcDateTimeAsEpoch), sysLocalDateTime());
        assertEquals(toLocalDateTime(
                utcDateTimeAsEpoch, vanZoneOffset), vanLocalDateTime());

        // Instant to Date
        assertEquals(toDateUTC(utcInstant()), utcDateTime);
        assertEquals(toDate(utcInstant()), sysDateTime);
        assertEquals(toDate(utcInstant(), vanZoneOffset), vanDateTime);


        // 1. UTC Date to LocalDateTime in each local,
        // 2. Each local LocalDateTime back to UTC Date
        assertEquals(toDateUTC(toLocalDateTimeUTC(utcDateTime)), utcDateTime);
        assertEquals(toDate(toLocalDateTime(utcDateTime)), utcDateTime);
        assertEquals(toDate(toLocalDateTime(
                utcDateTime, vanZoneOffset), vanZoneOffset), utcDateTime);

        // 1. UTC LocalDateTime to Date in each local,
        // 2. Each local Date back UTC LocalDateTime
        assertEquals(toLocalDateTimeUTC(
                toDateUTC(utcLocalDateTime())), utcLocalDateTime());
        assertEquals(toLocalDateTime(
                toDate(utcLocalDateTime())), utcLocalDateTime());
        assertEquals(toLocalDateTime(toDate(utcLocalDateTime(), vanZoneOffset),
                vanZoneOffset), utcLocalDateTime());

    }

    @Test
    void testDateToFromLocalDate() {
        Date utcDate = new Date(utcDateAsEpoch);
        Date sysDate = new Date(sysDateAsEpoch);
        Date vanDate = new Date(vanDateAsEpoch);

        // Date to LocalDateTime
        assertEquals(toLocalDateUTC(utcDate), utcLocalDate());
        assertEquals(toLocalDate(utcDate), sysLocalDate());
        assertEquals(toLocalDate(utcDate, vanZoneOffset), vanLocalDate());

        // LocalDateTime to Date
        assertEquals(toDateUTC(utcLocalDate()), utcDate);
        assertEquals(toDate(utcLocalDate()), sysDate);
        assertEquals(toDate(utcLocalDate(), vanZoneOffset), vanDate);

        // Epoch to LocalDateTime
        assertEquals(toLocalDateUTC(utcDateAsEpoch), utcLocalDate());
        assertEquals(toLocalDate(utcDateAsEpoch), sysLocalDate());
        assertEquals(toLocalDate(
                utcDateAsEpoch, vanZoneOffset), vanLocalDate());
    }

    private static void assertEquals(Object actual, Object expected) {
        LOG.debug("{} --> {}  ({})",
                actual, expected, actual.getClass().getSimpleName());
        assertThat(actual).isEqualTo(expected);
    }
    private static LocalDateTime utcLocalDateTime() {
        return ofEpochSecond(utcDateTimeAsEpoch / 1000L, 0, utcZoneOffset);
    }
    private static LocalDateTime sysLocalDateTime() {
        return ofEpochSecond(utcDateTimeAsEpoch / 1000L, 0, sysZoneOffset);
    }
    private static LocalDateTime vanLocalDateTime() {
        return ofEpochSecond(utcDateTimeAsEpoch / 1000L, 0, vanZoneOffset);
    }
    private static Instant utcInstant() {
        return Instant.ofEpochMilli(utcDateTimeAsEpoch);
    }
    private static LocalDate utcLocalDate() {
        return utcLocalDateTime().toLocalDate();
    }
    private static LocalDate sysLocalDate() {
        return sysLocalDateTime().toLocalDate();
    }
    private static LocalDate vanLocalDate() {
        return vanLocalDateTime().toLocalDate();
    }
}
