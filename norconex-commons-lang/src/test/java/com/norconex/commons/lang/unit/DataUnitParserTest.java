/* Copyright 2020 Norconex Inc.
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
package com.norconex.commons.lang.unit;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataUnitParserTest {

    private static final long TEST_AMOUNT_B = 54018001023L;
    private static final double TEST_AMOUNT_MB = 54018.001023D;

    @Test
    public void testParseToBytes() {

        Assertions.assertEquals(TEST_AMOUNT_B, parse("54GB18MB1kB23B"));
        Assertions.assertEquals(TEST_AMOUNT_B, parse(
                "54 gigabytes, 18 megabytes, 1 kilobyte, and 23 bytes"));
        Assertions.assertEquals(TEST_AMOUNT_B, parse(
                "54gigabytes,18 MB1KB23 B"));
        Assertions.assertEquals(TEST_AMOUNT_B, parse(
                "54 gigaoctets, 18 mégaoctets, 1kB et 23 octets."));
    }

    @Test
    public void testParseToMegabyte() {
        Assertions.assertEquals(TEST_AMOUNT_MB, parseMB("54GB18MB1kB23B"));
        Assertions.assertEquals(TEST_AMOUNT_MB, parseMB(
                "54 gigabytes, 18 megabytes, 1 kilobyte, and 23 bytes"));
        Assertions.assertEquals(TEST_AMOUNT_MB, parseMB(
                "54gigabytes,18 MB1KB23 B"));
        Assertions.assertEquals(TEST_AMOUNT_MB, parseMB(
                "54 gigaoctets, 18 mégaoctets, 1kB et 23 octets."));
    }

    @Test
    public void testDefaultValue() {
        Assertions.assertEquals(2,
                DataUnitParser.parse("-5", BigDecimal.valueOf(2)).intValue());
        try {
            DataUnitParser.parse("-5");
            Assertions.fail("Should have thrown exception.");
        } catch (Exception e) {
            // swallow
        }
    }

    private long parse(String text) {
        return DataUnitParser.parse(text).longValue();
    }
    private double parseMB(String text) {
        return DataUnitParser.parse(text, DataUnit.MB).doubleValue();
    }
}
