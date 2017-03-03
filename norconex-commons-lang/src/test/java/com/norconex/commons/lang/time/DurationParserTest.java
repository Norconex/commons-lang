/* Copyright 2017 Norconex Inc.
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

import org.junit.Assert;
import org.junit.Test;

public class DurationParserTest {

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long TEST_DURATION = 
            54 * DAY + 18 * HOUR + 1 * MINUTE + 23 * SECOND;
    
    @Test
    public void testDurationParser() {
        Assert.assertEquals(TEST_DURATION, DurationParser.parse("54d18h1m23s"));
        Assert.assertEquals(TEST_DURATION, DurationParser.parse(
                "54 days, 18 hours, 1 minute, and 23 seconds"));
        Assert.assertEquals(TEST_DURATION, DurationParser.parse(
                "54 days, 18 hours, 1 minute, and 23 seconds"));
        Assert.assertEquals(TEST_DURATION, DurationParser.parse(
                "54days,18 hrs1min23 s"));
    }
}
