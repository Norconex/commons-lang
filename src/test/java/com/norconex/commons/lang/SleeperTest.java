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
package com.norconex.commons.lang;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SleeperTest {

    @Test
    void testSleeper() {

        long before = System.currentTimeMillis();
        Sleeper.sleepHours(0);
        assertThat(System.currentTimeMillis() - before)
            .isLessThanOrEqualTo(1000);

        before = System.currentTimeMillis();
        Sleeper.sleepMinutes(0);
        assertThat(elapsed(before)).isLessThanOrEqualTo(1000);

        before = System.currentTimeMillis();
        Sleeper.sleepSeconds(1);
        assertThat(elapsed(before)).isGreaterThanOrEqualTo(1000);

        before = System.currentTimeMillis();
        Sleeper.sleepMillis(1000);
        assertThat(elapsed(before)).isGreaterThanOrEqualTo(1000);

        before = System.currentTimeMillis();
        Sleeper.sleepNanos(1000);
        assertThat(elapsed(before)).isLessThanOrEqualTo(1000);
    }

    @Test
    void testSleeperException() {
        assertThat(new SleeperException(
                "blah", null).getMessage()).isEqualTo("blah");
    }

    private long elapsed(long before) {
        return System.currentTimeMillis() - before;
    }
}
