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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class DurationUnitTest {

    @Test
    void testDurationUnit() {
        assertThat(DurationUnit.MILLISECOND.toSeconds()).isZero();
        assertThat(DurationUnit.MILLISECOND.toSeconds(6000)).isEqualTo(6);
        assertThat(DurationUnit.SECOND.toMilliseconds()).isEqualTo(1000);
        assertThat(DurationUnit.SECOND.toMilliseconds(2)).isEqualTo(2000);
        assertThat(DurationUnit.MINUTE.toSeconds()).isEqualTo(60);
        assertThat(DurationUnit.MINUTE.toSeconds(3)).isEqualTo(180);
        assertThat(DurationUnit.HOUR.toMinutes()).isEqualTo(60);
        assertThat(DurationUnit.HOUR.toMinutes(4)).isEqualTo(240);
        assertThat(DurationUnit.DAY.toHours()).isEqualTo(24);
        assertThat(DurationUnit.DAY.toHours(5)).isEqualTo(120);
        assertThat(DurationUnit.WEEK.toDays()).isEqualTo(7);
        assertThat(DurationUnit.WEEK.toDays(6)).isEqualTo(42);
        assertThat(DurationUnit.MONTH.toWeeks()).isEqualTo(4);
        assertThat(DurationUnit.MONTH.toWeeks(2)).isEqualTo(8);
        assertThat(DurationUnit.YEAR.toMonths()).isEqualTo(12);
        assertThat(DurationUnit.YEAR.toMonths(2)).isEqualTo(24);

        assertThat(DurationUnit.MONTH.toYears(36)).isEqualTo(3);
        assertThat(DurationUnit.MONTH.toYears()).isZero();

        assertThat(DurationUnit.MONTH.toTemporalUnit())
                .isSameAs(ChronoUnit.MONTHS);
        assertThat(DurationUnit.WEEK.toDuration()).isEqualTo(
                Duration.ofDays(7));
    }

    @Test
    void testFrom() {
        assertThat(DurationUnit.from(Duration.ofHours(75)))
                .isSameAs(DurationUnit.DAY);
        assertThat(DurationUnit.from((Duration) null)).isNull();
        assertThat(DurationUnit.from("week")).isSameAs(DurationUnit.WEEK);
        assertThat(DurationUnit.from((String) null)).isNull();
        assertThat(DurationUnit.from("invalid")).isNull();
        assertThat(DurationUnit.from(ChronoUnit.MINUTES))
                .isSameAs(DurationUnit.MINUTE);
        assertThat(DurationUnit.from((ChronoUnit) null)).isNull();
        assertThat(DurationUnit.from(ChronoUnit.CENTURIES)).isNull();
    }

    @Test
    void testReverse() {
        assertThat(DurationUnit.reverseValues()).containsExactly(
                DurationUnit.YEAR,
                DurationUnit.MONTH,
                DurationUnit.WEEK,
                DurationUnit.DAY,
                DurationUnit.HOUR,
                DurationUnit.MINUTE,
                DurationUnit.SECOND,
                DurationUnit.MILLISECOND);

        assertThat(DurationUnit.MONTH.reverseOrdinal()).isEqualTo(1);
        assertThat(DurationUnit.MILLISECOND.reverseOrdinal()).isEqualTo(7);
    }
}
