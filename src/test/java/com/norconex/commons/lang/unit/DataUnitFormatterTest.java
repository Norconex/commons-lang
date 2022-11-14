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
package com.norconex.commons.lang.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.RoundingMode;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataUnitFormatterTest {

    @Test
    void testFormat() {
        DataUnitFormatter decFmt = new DataUnitFormatter();
        DataUnitFormatter binFmt =
                new DataUnitFormatter().setBinaryNotation(true);

        // non-breaking space: \u00A0

        Assertions.assertEquals("3\u00A0GB", decFmt.format(3, DataUnit.GB));
        Assertions.assertEquals("3\u00A0GiB", binFmt.format(3, DataUnit.GIB));

        Assertions.assertEquals("3\u00A0MB", decFmt.format(3000, DataUnit.KB));
        Assertions.assertEquals("3\u00A0MiB",
                binFmt.format(3000, DataUnit.KIB));

        Assertions.assertEquals("3\u00A0MB",
                decFmt.withDecimalPrecision(1).format(3000, DataUnit.KB));
        Assertions.assertEquals("2.9\u00A0MiB",
                binFmt.withDecimalPrecision(1).format(3000, DataUnit.KIB));

        Assertions.assertEquals("3,07\u00A0kB", decFmt
                .withLocale(Locale.CANADA_FRENCH)
                .setDecimalPrecision(2)
                .setRoundingMode(RoundingMode.DOWN)
                .format(3071, DataUnit.B));
        Assertions.assertEquals("2,99\u00A0KiB", binFmt
                .withLocale(Locale.CANADA_FRENCH)
                .setDecimalPrecision(2)
                .setRoundingMode(RoundingMode.DOWN)
                .format(3071, DataUnit.B));

        Assertions.assertEquals("10\u00A0000\u00A0kB", decFmt
                .withLocale(Locale.CANADA_FRENCH)
                .setDecimalPrecision(2)
                .setFixedUnit(true)
                .format(10000, DataUnit.KB));
        Assertions.assertEquals("10\u00A0000\u00A0KiB", binFmt
                .withLocale(Locale.CANADA_FRENCH)
                .setDecimalPrecision(2)
                .setFixedUnit(true)
                .format(10000, DataUnit.KIB));

        // Format binary to decimal
        Assertions.assertEquals("2.048\u00A0kB",
                decFmt.withDecimalPrecision(3).format(2, DataUnit.KIB));
    }

    @Test
    void testMisc() {
        DataUnitFormatter duf = new DataUnitFormatter();
        assertThat(duf.getLocale()).isNull();
        assertThat(duf.getDecimalPrecision()).isZero();
        assertThat(duf.isFixedUnit()).isFalse();
        duf = duf.withFixedUnit(true);
        assertThat(duf.isFixedUnit()).isTrue();
        assertThat(duf.isBinaryNotation()).isFalse();
        duf = duf.withBinaryNotation(true);
        assertThat(duf.isBinaryNotation()).isTrue();
        assertThat(duf.getRoundingMode()).isNull();
        duf = duf.withRoundingMode(RoundingMode.HALF_DOWN);
        assertThat(duf.getRoundingMode()).isSameAs(RoundingMode.HALF_DOWN);


    }
}
