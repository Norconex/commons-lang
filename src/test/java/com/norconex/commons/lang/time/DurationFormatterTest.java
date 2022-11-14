/* Copyright 2018-2022 Norconex Inc.
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

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DurationFormatterTest {

    private static final long TEST_DATETIME_DURATION =
            DurationUnit.YEAR.toMilliseconds(4)
          + DurationUnit.MONTH.toMilliseconds(1)
          + DurationUnit.WEEK.toMilliseconds(3)
          + DurationUnit.DAY.toMilliseconds(5)
          + DurationUnit.HOUR.toMilliseconds(18)
          + DurationUnit.MINUTE.toMilliseconds(1)
          + DurationUnit.SECOND.toMilliseconds(23)
          + DurationUnit.MILLISECOND.toMilliseconds(469);

    private static final long TEST_DAYTIME_DURATION =
          + DurationUnit.DAY.toMilliseconds(5)
          + DurationUnit.HOUR.toMilliseconds(18)
          + DurationUnit.MINUTE.toMilliseconds(1)
          + DurationUnit.SECOND.toMilliseconds(23)
          + DurationUnit.MILLISECOND.toMilliseconds(469);

    private DurationFormatter full;
    private DurationFormatter compact;
    private DurationFormatter abbr;

    @BeforeEach
    void before() {
        full = new DurationFormatter();
        compact = DurationFormatter.COMPACT;
        abbr = new DurationFormatter().withUnitFormatter(
                RBDurationUnitFormatter.ABBREVIATED);
    }

    @Test
    void testGetters() {
        IDurationUnitFormatter duf = (unit, locale, plural) -> "blah";
        assertThat(
            new DurationFormatter()
                .withHighestUnit(DurationUnit.MONTH)
                .withInnerSeparator("|")
                .withLocale(Locale.CANADA_FRENCH)
                .withLowestUnit(DurationUnit.MINUTE)
                .withNumberFormat(NumberFormat.getIntegerInstance())
                .withOuterLastSeparator("-")
                .withOuterSeparator("_")
                .withUnitFormatter(duf)
                .withUnitPrecision(66))
            .returns(DurationUnit.MONTH, DurationFormatter::getHighestUnit)
            .returns("|", DurationFormatter::getInnerSeparator)
            .returns(Locale.CANADA_FRENCH, DurationFormatter::getLocale)
            .returns(DurationUnit.MINUTE, DurationFormatter::getLowestUnit)
            .returns(NumberFormat.getIntegerInstance(),
                    DurationFormatter::getNumberFormat)
            .returns("-", DurationFormatter::getOuterLastSeparator)
            .returns("_", DurationFormatter::getOuterSeparator)
            .returns(duf, DurationFormatter::getUnitFormatter)
            .returns(66, DurationFormatter::getUnitPrecision)
            ;
    }

    @Test
    void testCompactAllUnitsFormat() {
        Assertions.assertEquals("4Y1M3W5D18h1m23s469ms", compact
                .withLocale(Locale.CHINA)
                .format(TEST_DATETIME_DURATION));
        Assertions.assertEquals("4A1M3S5J18h1m23s469ms", compact
                .withLocale(Locale.FRENCH)
                .format(TEST_DATETIME_DURATION));

    }
    @Test
    void testFullAllUnitsFormat() {
        Assertions.assertEquals("4 years 1 month 3 weeks 5 days "
                + "18 hours 1 minute 23 seconds 469 milliseconds", full
                .format(TEST_DATETIME_DURATION));
        Assertions.assertEquals("4 ans 1 mois 3 semaines 5 jours "
                + "18 heures 1 minute 23 secondes 469 millisecondes", full
                .withLocale(Locale.FRENCH)
                .format(TEST_DATETIME_DURATION));
    }
    @Test
    void testAbbrAllUnitsFormat() {
        Assertions.assertEquals("4 yrs 1 mo 3 wks 5 days "
                + "18 hrs 1 min 23 secs 469 msecs", abbr
                .format(TEST_DATETIME_DURATION));
        Assertions.assertEquals("4 ans 1 mo 3 sems 5 jrs "
                + "18 hrs 1 min 23 secs 469 msecs", abbr
                .withLocale(Locale.FRENCH)
                .format(TEST_DATETIME_DURATION));
    }
    @Test
    void testCompactDaysToSecondsFormat() {
        Assertions.assertEquals("5D18h1m23s", compact
                .withLocale(Locale.CHINA)
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND)
                .format(TEST_DAYTIME_DURATION));
        Assertions.assertEquals("5J18h1m23s", compact
                .withLocale(Locale.FRENCH)
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND)
                .format(TEST_DAYTIME_DURATION));
    }
    @Test
    void testFullDaysToSecondsFormat() {
        Assertions.assertEquals("5 days 18 hours 1 minute 23 seconds", full
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND)
                .format(TEST_DAYTIME_DURATION));
        Assertions.assertEquals("5 jours 18 heures 1 minute 23 secondes", full
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND)
                .withLocale(Locale.FRENCH)
                .format(TEST_DAYTIME_DURATION));
    }
    @Test
    void testAbbrDaysToSecondsFormat() {
        Assertions.assertEquals("5 days 18 hrs 1 min 23 secs", abbr
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND)
                .format(TEST_DAYTIME_DURATION));
        Assertions.assertEquals("5 jrs 18 hrs 1 min 23 secs", abbr
                .withHighestUnit(DurationUnit.DAY)
                .withLowestUnit(DurationUnit.SECOND)
                .withLocale(Locale.FRENCH)
                .format(TEST_DAYTIME_DURATION));
    }
    @Test
    void testHighestUnitsSmaller() {
        Assertions.assertEquals("138 hours 1 minute 23 seconds", full
                .withHighestUnit(DurationUnit.HOUR)
                .withLowestUnit(DurationUnit.SECOND)
                .format(TEST_DAYTIME_DURATION));
        Assertions.assertEquals("138 heures 1 minute 23 secondes", full
                .withHighestUnit(DurationUnit.HOUR)
                .withLowestUnit(DurationUnit.SECOND)
                .withLocale(Locale.FRENCH)
                .format(TEST_DAYTIME_DURATION));
    }

    @Test
    void testPrecision() {
        Assertions.assertEquals("138 hours", full
                .withHighestUnit(DurationUnit.HOUR)
                .withLowestUnit(DurationUnit.SECOND)
                .withUnitPrecision(1)
                .format(TEST_DAYTIME_DURATION));
        Assertions.assertEquals("138 hours 1 minute", full
                .withHighestUnit(DurationUnit.HOUR)
                .withLowestUnit(DurationUnit.SECOND)
                .withUnitPrecision(2)
                .format(TEST_DAYTIME_DURATION));
    }

    @Test
    void testNumberFormat() {
        Assertions.assertEquals("496,883 seconds 469 milliseconds", full
                .withHighestUnit(DurationUnit.SECOND)
                .withNumberFormat(
                        NumberFormat.getNumberInstance(Locale.ENGLISH))
                .withUnitPrecision(2)
                .format(TEST_DAYTIME_DURATION));
    }

    @Test
    void testFullFormatter() {
        DurationFormatter df =
                DurationFormatter.FULL.withOuterSeparator(", ");

        Assertions.assertEquals("4 years, 1 month, 3 weeks, 5 days, "
                + "18 hours, 1 minute, 23 seconds and 469 milliseconds", df
                .withOuterLastSeparator(" and ")
                .format(TEST_DATETIME_DURATION));

        Assertions.assertEquals("4 ans, 1 mois, 3 semaines, 5 jours, "
                + "18 heures, 1 minute, 23 secondes et 469 millisecondes", df
                .withOuterLastSeparator(" et ")
                .withLocale(Locale.FRENCH)
                .format(TEST_DATETIME_DURATION));
    }

    @Test
    void testFormatDuration() {
        assertThat(compact.format(Duration.ofDays(3))).isEqualTo("3D");
        assertThat(compact.format((Duration) null)).isNull();
    }
}
