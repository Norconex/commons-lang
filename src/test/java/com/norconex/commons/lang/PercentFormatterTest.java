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

import java.util.Locale;

import org.junit.jupiter.api.Test;

class PercentFormatterTest {

    @Test
    void testPercentFormatter() {
        PercentFormatter pf = new PercentFormatter();
        assertThat(pf.format(0.123d)).isEqualTo("12%");
        assertThat(pf.format(0.125d)).isEqualTo("12%");
        assertThat(pf.format(0.135d)).isEqualTo("14%");
        assertThat(pf.format(1, 3)).isEqualTo("33%");
        assertThat(pf.format(3, 0)).isEqualTo("0%");

        pf = new PercentFormatter(3, new Locale("fr", "CA"));
        assertThat(pf.format(10000, 3)).isEqualTo("333 333,333 %");

        assertThat(PercentFormatter.format(0.33333d, 2,
                new Locale("en", "CA"))).isEqualTo("33.33%");
        assertThat(PercentFormatter.format(0.33333d, 2,
                new Locale("fr", "CA"))).isEqualTo("33,33 %");
        assertThat(PercentFormatter.format(1, 3, 2,
                new Locale("en", "CA"))).isEqualTo("33.33%");
        assertThat(PercentFormatter.format(1, 3, 2,
                new Locale("fr", "CA"))).isEqualTo("33,33 %");
    }
}
