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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ZonedDateTimeParserTest {

    // UTC Constants: 2022-11-10T04:49:51.000
    private static final int Y = 2022;
    private static final int M = 11;
    private static final int D = 10;
    private static final int h = 4;
    private static final int m = 49;
    private static final int s = 51;

    private static final ZoneId vanZoneId = ZoneId.of("America/Vancouver");

    @Test
    void testOfFormat() {
        var b = ZonedDateTimeParser.builder()
                .format("yyyy-MM-dd['T'HH:mm:ss][.SSS][Z][z]");
        var zdtp = b.build();
        assertThat(zdtp.parse("2022-11-10T04:49:51.000"))
                .isEqualTo(DateModel.of(Y, M, D, h, m, s).toZonedDateTime());
        assertThat(zdtp.parse("2022-11-10T04:49:51.000-08:00"))
                .isEqualTo(DateModel.of(Y, M, D, h, m, s).withZoneId(vanZoneId)
                        .toZonedDateTime());
        assertThat(zdtp.parse("2022-11-10T04:49:51"))
                .isEqualTo(DateModel.of(Y, M, D, h, m, s).toZonedDateTime());
        assertThat(zdtp.parse("2022-11-10"))
                .isEqualTo(DateModel.of(Y, M, D).toZonedDateTime());
        assertThat(zdtp.parse("2022-11-10T00:00:00-08:00"))
                .isEqualTo(DateModel.of(Y, M, D)
                        .withZoneId(vanZoneId).toZonedDateTime());

        assertThatExceptionOfType(DateTimeException.class).isThrownBy(//NOSONAR
                () -> zdtp.parse("NOGOOD"));

        assertThat(b
                .format("EEE, dd MMM yyyy HH:mm:ss z")
                .locale(Locale.CANADA_FRENCH)
                .zoneId(vanZoneId)
                .build()
                .parse("jeu., 10 nov. 2022 04:49:51 PST"))
                        .isEqualTo(DateModel.of(Y, M, D, h, m, s)
                                .withZoneId(vanZoneId).toZonedDateTime());
    }

    @Test
    void testOfEpoch() {
        assertThat(ZonedDateTimeParser.builder()
                .zoneId(vanZoneId)
                .build()
                .parse("1668084591000"))
                        .isEqualTo(DateModel
                                .of(Y, M, D, h, m, s)
                                .withZoneId(vanZoneId)
                                .toZonedDateTime());

        assertThatExceptionOfType(DateTimeException.class).isThrownBy(//NOSONAR
                () -> ZonedDateTimeParser.builder()
                        .zoneId(vanZoneId)
                        .build()
                        .parse("1234567890123456789012345678901234567890"));
    }

    @Test
    void testOfRelative() {
        var expectedNow = ZonedDateTime.now(ZoneOffset.UTC).minusDays(7);
        var actualNow = ZonedDateTimeParser.builder()
                .build()
                .parse("NOW-7D");
        // Test within a 2 minute range given we deal with actual time
        assertThat(expectedNow.minusMinutes(1).compareTo(actualNow)
                * actualNow.compareTo(expectedNow.plusMinutes(1)))
                        .isNotNegative();

        // 9 hour am sharp
        var actualToday = ZonedDateTimeParser.builder()
                .build()
                .parse("TODAY+9h");
        // Test within a 2 minute range given we deal with actual time
        assertThat(actualToday.getHour()).isEqualTo(9);
    }
}
