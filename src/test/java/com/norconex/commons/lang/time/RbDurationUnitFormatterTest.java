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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class RbDurationUnitFormatterTest {

    @Test
    void testRBDurationUnitFormatter() {
        assertThat(RbDurationUnitFormatter.ABBREVIATED.format(
                DurationUnit.DAY, Locale.FRENCH, false))
                        .isEqualTo("jr");
        assertThat(RbDurationUnitFormatter.ABBREVIATED.format(
                DurationUnit.DAY, Locale.FRENCH, true))
                        .isEqualTo("jrs");
        assertThat(RbDurationUnitFormatter.ABBREVIATED.format(
                null, Locale.FRENCH, true)).isNull();
    }
}
