/* Copyright 2023 Norconex Inc.
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

import static com.norconex.commons.lang.time.DateModel.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import lombok.Data;
import lombok.experimental.Accessors;

class DateModelTest {

    // UTC Constants: 2022-11-10T04:49:51.000
    private static final int Y = 2022;
    private static final int M = 11;
    private static final int D = 10;
    private static final int h = 4;
    private static final int m = 49;
    private static final int s = 51;

    private static final ZoneId vanZoneId = ZoneId.of("America/Vancouver");

    static class ExpectedDates {
        private final long epoch;
        private final long epochMidnight;
        private final Date date;
        private final Date dateMidnight;
        private final ZonedDateTime zonedDateTime;
        private final ZonedDateTime zonedDateTimeMidnight;
        private final LocalDate localDate;
        private final LocalDateTime localDateTime;
        private final LocalDateTime localDateTimeMidnight;
        private final Instant instant;
        private final Instant instantMidnight;

        public ExpectedDates(long epoch, long epochMidnight, ZoneId zone) {
            this.epoch = epoch;
            this.epochMidnight = epochMidnight;
            date = new Date(epoch);
            dateMidnight = new Date(epochMidnight);
            zonedDateTime =
                    ZonedDateTime.of(Y, M, D, h, m, s, 0, zone);
            zonedDateTimeMidnight = ZonedDateTime.of(Y, M, D, 0, 0, 0, 0, zone);
            localDate = LocalDate.of(2022, 11, 10);
            localDateTime = LocalDateTime.of(Y, M, D, h, m, s, 0);
            localDateTimeMidnight = LocalDateTime.of(Y, M, D, 0, 0, 0, 0);
            instant = zonedDateTime.toInstant();
            instantMidnight = zonedDateTimeMidnight.toInstant();
        }
    }

    private static ExpectedDates utc = new ExpectedDates(
            1668055791000L, 1668038400000L, ZoneOffset.UTC);
    private static ExpectedDates van = new ExpectedDates(
            1668084591000L, 1668067200000L, vanZoneId);

    @Test
    void testUTCWithUTCZone() {
        var expected = new Expected("UTC with time")
                .date(utc.date)
                .epoch(utc.epoch)
                .instant(utc.instant)
                .localDate(utc.localDate)
                .localDateTime(utc.localDateTime)
                .zonedDateTime(utc.zonedDateTime);

        assertDates(of(utc.date), expected);
        assertDates(of(utc.epoch), expected);
        assertDates(of(utc.instant), expected);
        assertDates(of(utc.localDateTime), expected);
        assertDates(of(utc.zonedDateTime), expected);
    }

    @Test
    void testUTCWithVancouverZone() {
        var expected = new Expected("Vancouver with time")
                .date(van.date)
                .epoch(van.epoch)
                .instant(van.instant)
                .localDate(van.localDate)
                .localDateTime(van.localDateTime)
                .zonedDateTime(van.zonedDateTime);

        assertDates(of(utc.date).withZoneId(vanZoneId), expected);
        assertDates(of(utc.epoch).withZoneId(vanZoneId), expected);
        assertDates(of(utc.instant).withZoneId(vanZoneId), expected);
        assertDates(of(utc.localDateTime).withZoneId(vanZoneId), expected);
        assertDates(of(utc.zonedDateTime).withZoneId(vanZoneId), expected);
    }

    @Test
    void testUTCWithUTCZoneAndNoTime() {
        var expected = new Expected("UTC without time")
                .date(utc.dateMidnight)
                .epoch(utc.epochMidnight)
                .instant(utc.instantMidnight)
                .localDate(utc.localDate)
                .localDateTime(utc.localDateTimeMidnight)
                .zonedDateTime(utc.zonedDateTimeMidnight);

        assertDates(of(utc.date).withStartOfDay(true), expected);
        assertDates(of(utc.epoch).withStartOfDay(true), expected);
        assertDates(of(utc.instant).withStartOfDay(true), expected);
        assertDates(of(utc.localDate).withStartOfDay(true), expected);
        assertDates(of(utc.localDateTime).withStartOfDay(true), expected);
        assertDates(of(utc.zonedDateTime).withStartOfDay(true), expected);
    }

    @Test
    void testUTCWithVancouverZoneAndNoTime() {
        var expected = new Expected("Vancouver without time")
                .date(van.dateMidnight)
                .epoch(van.epochMidnight)
                .instant(van.instantMidnight)
                .localDate(van.localDate)
                .localDateTime(van.localDateTimeMidnight)
                .zonedDateTime(van.zonedDateTimeMidnight);

        assertDates(of(utc.date).withStartOfDay(true).withZoneId(
                vanZoneId), expected);
        assertDates(of(utc.epoch).withStartOfDay(true).withZoneId(
                vanZoneId), expected);
        assertDates(of(utc.instant).withStartOfDay(true).withZoneId(
                vanZoneId), expected);
        assertDates(of(utc.localDate).withStartOfDay(true).withZoneId(
                vanZoneId), expected);
        assertDates(of(utc.localDateTime).withStartOfDay(true).withZoneId(
                vanZoneId), expected);
        assertDates(of(utc.zonedDateTime).withStartOfDay(true).withZoneId(
                vanZoneId), expected);
    }

    @Test
    void testVancouverWithUTCZone() {
        // test when sources can be other than UTC
        assertThat(of(ZonedDateTime.of(Y, M, D, h, m, s, 0, vanZoneId))
                .withZoneId(ZoneOffset.UTC).toZonedDateTime())
                        .isEqualTo(utc.zonedDateTime);
    }

    @Test
    void testOfString() {
        assertThat(of("2022-11-10T04:49:51.000"))
                .isEqualTo(DateModel.of(Y, M, D, h, m, s));
        assertThat(of("2022-11-10T04:49:51.000-08:00").toZonedDateTime())
                .isEqualTo(DateModel.of(Y, M, D, h, m, s).withZoneId(vanZoneId)
                        .toZonedDateTime());
        assertThat(of("2022-11-10T04:49:51.000-08:00[America/Vancouver]"))
                .isEqualTo(
                        DateModel.of(Y, M, D, h, m, s).withZoneId(vanZoneId));
        assertThat(of("2022-11-10T04:49:51"))
                .isEqualTo(DateModel.of(Y, M, D, h, m, s));
        assertThat(of("2022-11-10")).isEqualTo(DateModel.of(Y, M, D));
        assertThat(of("2022-11-10T00:00:00-08:00").toZonedDateTime())
                .isEqualTo(DateModel.of(Y, M, D)
                        .withZoneId(vanZoneId).toZonedDateTime());
    }

    @Test
    void testOfStringZoneParser() {
        var builder = ZonedDateTimeParser.builder()
                .format("EEE, dd MMM yyyy HH:mm:ss z");

        assertThat(of("Thu, 10 Nov 2022 04:49:51 GMT", builder.build()))
                .isEqualTo(DateModel.of(Y, M, D, h, m, s)
                        .withZoneId(ZoneId.of("GMT")));

        assertThat(of("jeu., 10 nov. 2022 04:49:51 PST", builder
                .format("EEE, dd MMM yyyy HH:mm:ss z")
                .locale(Locale.CANADA_FRENCH)
                .build()).toZonedDateTime())
                        .isEqualTo(DateModel.of(Y, M, D, h, m, s)
                                .withZoneId(vanZoneId).toZonedDateTime());
    }

    private void assertDates(DateModel date, Expected expected) {
        var dsc = expected.name + " -> %s";
        assertThat(date.toDate()).as(dsc, "Date").isEqualTo(expected.date());
        assertThat(date.toEpoch()).as(
                dsc, "Epoch").isEqualTo(expected.epoch());
        assertThat(date.toInstant()).as(
                dsc, "Instant").isEqualTo(expected.instant());
        assertThat(date.toLocalDate()).as(
                dsc, "LocalDate").isEqualTo(expected.localDate());
        assertThat(date.toLocalDateTime()).as(
                dsc, "LocalDateTime").isEqualTo(expected.localDateTime());
        assertThat(date.toZonedDateTime()).as(
                dsc, "ZonedDateTime").isEqualTo(expected.zonedDateTime());
    }

    @Test
    void testErrors() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of((Date) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of((Instant) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of((LocalDate) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of((LocalDateTime) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of((ZonedDateTime) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of((String) null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of("", null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
                () -> DateModel.of(null, null));
    }

    @Test
    void testWithMisc() {
        assertThat(DateModel.of(utc.zonedDateTime)
                .withNano(123)
                .toLocalDateTime()
                .getNano())
                        .isEqualTo(123);
        assertThat(DateModel.of(utc.zonedDateTime)
                .withDate(1999, 12, 31)
                .toLocalDateTime())
                        .isEqualTo(LocalDateTime.of(1999, 12, 31, 4, 49, 51));
        assertThat(DateModel.of(utc.zonedDateTime)
                .withTime(17, 02, 56)
                .toLocalDateTime())
                        .isEqualTo(LocalDateTime.of(2022, 11, 10, 17, 02, 56));
    }

    @Test
    void testToString() {
        assertThat(DateModel.of(utc.zonedDateTime))
                .hasToString("2022-11-10T04:49:51Z");
        assertThat(DateModel.of(utc.zonedDateTime).withZoneId(vanZoneId))
                .hasToString("2022-11-10T04:49:51-08:00[America/Vancouver]");
    }

    @Data
    @Accessors(fluent = true)
    static class Expected {
        private final String name;
        private Date date;
        private long epoch;
        private Instant instant;
        private LocalDate localDate;
        private LocalDateTime localDateTime;
        private ZonedDateTime zonedDateTime;
    }
}
