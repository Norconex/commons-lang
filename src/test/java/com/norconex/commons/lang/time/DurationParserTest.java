/* Copyright 2017-2022 Norconex Inc.
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

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DurationParserTest {

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long TEST_DURATION =
            54 * DAY + 18 * HOUR + 1 * MINUTE + 23 * SECOND;

    private final DurationParser englishParser = new DurationParser();
    private final DurationParser frenchParser =
            new DurationParser().withLocale(Locale.FRENCH);

    @Test
    void testParseToMilliseconds() {
        Assertions.assertEquals(TEST_DURATION, englishParser.parseToMillis(
                "54d18h1m23s"));
        Assertions.assertEquals(TEST_DURATION, englishParser.parseToMillis(
                "54 days, 18 hours, 1 minute, and 23 seconds"));
        Assertions.assertEquals(TEST_DURATION, englishParser.parseToMillis(
                "54 days, 18 hours, 1 minute, and 23 seconds"));
        Assertions.assertEquals(TEST_DURATION, englishParser.parseToMillis(
                "54days,18 hrs1min23 s"));

        Assertions.assertEquals(TEST_DURATION, frenchParser.parseToMillis(
                "54 jours, 18 heures, 1m et 23 secondes"));
    }

    @Test
    void testParseToDuration() {
        Duration duration = Duration.ofMillis(TEST_DURATION);
        Assertions.assertEquals(duration, englishParser.parse("54d18h1m23s"));
        Assertions.assertEquals(duration, englishParser.parse(
                "54 days, 18 hours, 1 minute, and 23 seconds"));
        Assertions.assertEquals(duration, englishParser.parse(
                "54 days, 18 hours, 1 minute, and 23 seconds"));
        Assertions.assertEquals(duration, englishParser.parse(
                "54days,18 hrs1min23 s"));

        Assertions.assertEquals(duration, frenchParser.parse(
                "54 jours, 18 heures, 1m et 23 secondes"));
    }


    @Test
    void testDefaultValue() {
        Assertions.assertEquals(2, englishParser.parseToMillis("-5", 2));
        try {
            englishParser.parse("-5");
            Assertions.fail("Should have thrown exception.");
        } catch (Exception e) {
            // swallow
        }
    }
}
