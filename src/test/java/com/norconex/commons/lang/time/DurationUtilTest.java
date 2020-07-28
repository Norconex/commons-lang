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

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Deprecated
public class DurationUtilTest {

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long TEST_DURATION =
            54 * DAY + 18 * HOUR + 1 * MINUTE + 23 * SECOND;

    @Test
    public void testShortFormat() {
        Assertions.assertEquals("54d18h1m23s",
                DurationUtil.formatShort(Locale.CHINA, TEST_DURATION));
        Assertions.assertEquals("54d18h",
                DurationUtil.formatShort(Locale.CHINA, TEST_DURATION, 2));
        Assertions.assertEquals("54j18h1m23s",
                DurationUtil.formatShort(Locale.CANADA_FRENCH, TEST_DURATION));
    }

    @Test
    public void testLongFormat() {
        Assertions.assertEquals("54 days 18 hours 1 minute 23 seconds",
                DurationUtil.formatLong(Locale.CHINA, TEST_DURATION));
        Assertions.assertEquals("54 days 18 hours",
                DurationUtil.formatLong(Locale.CHINA, TEST_DURATION, 2));
        Assertions.assertEquals("54 jours 18 heures 1 minute 23 secondes",
                DurationUtil.formatLong(Locale.CANADA_FRENCH, TEST_DURATION));
    }
}
