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
package com.norconex.commons.lang.unit;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataUnitFormatterTest {

    @Test
    public void testFormat() {
        // non-breaking space: \u00A0
        Assertions.assertEquals("3\u00A0GB",
                new DataUnitFormatter().format(3, DataUnit.GB));
        Assertions.assertEquals("2\u00A0MB",
                new DataUnitFormatter().format(3000, DataUnit.KB));
        Assertions.assertEquals("2.9\u00A0MB",
                new DataUnitFormatter(1).format(3000, DataUnit.KB));
        Assertions.assertEquals("2,99\u00A0KB", new DataUnitFormatter(
                Locale.CANADA_FRENCH, 2).format(3071, DataUnit.B));
        Assertions.assertEquals("10\u00A0000\u00A0KB", new DataUnitFormatter(
                Locale.CANADA_FRENCH, 2, true).format(10000, DataUnit.KB));
    }
}
