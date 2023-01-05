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

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.Sleeper;

class TimerTest {

    @Test
    void testTime() {
        var ms = Timer.time(() -> {
            Sleeper.sleepMillis(500);
        });
        assertThat(ms).isBetween(500L, 750L);
    }

    @Test
    void testTimeDuration() {
        var duration = Timer.timeDuration(() -> {
            Sleeper.sleepMillis(500);
        });
        assertThat(duration).isBetween(ofMillis(500), ofMillis(750));
    }

    @Test
    void testTimeWatch() {
        var watch = Timer.timeWatch(() -> {
            Sleeper.sleepMillis(500);
        });
        assertThat(watch.getTime()).isBetween(500L, 750L);
    }
}
